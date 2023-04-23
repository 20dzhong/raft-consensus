package org.ur.raftimpl;

import org.ur.comms.AppendEntriesResponse;
import org.ur.comms.VoteResponse;

import java.io.IOException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;


public class RaftNode {

    /*

      This represent individual Raft Nodes by using a combination of gRPC client and server and attaching them
      to this class.

      This is where most stuff happens

      This class kickstarts the server, which can respond, and starts the client, which can request

      This is where the most of the logic will take place,
      for example, node state (follower, candidate, leader), timeouts, logs, etc.

     */

    enum State {
        FOLLOWER,
        LEADER,
        CANDIDATE
    }

    int nodeID; // id
    int port; // port


    // shared values
    ConcurrentHashMap<Integer, RaftClient> accessibleClients; // map of all client objects
    AtomicInteger totalNodes; // total number of nodes
    AtomicInteger term = new AtomicInteger(1); // default term of the node
    AtomicInteger votedFor = new AtomicInteger(-1);
    AtomicBoolean receivedHeartBeat = new AtomicBoolean(false); // whether or not you received a heartbeat this term
    AtomicInteger leaderTerm = new AtomicInteger(0); // term of the leader
    AtomicReference<State> nodeState = new AtomicReference<>(State.FOLLOWER);

    // all variables related to the task of time out loop and heartbeat loop
    ScheduledExecutorService executor;

    ScheduledFuture<?> timeoutTask;
    ScheduledFuture<?> heartbeatTask;

    Runnable timeoutCycle;
    Runnable heartbeatCycle;

    int timeout;
    int initDelay;
    int heartbeatInterval = 1;
    TimeUnit unit = TimeUnit.SECONDS;

    public RaftNode(int nodeID, int port, ConcurrentHashMap<Integer, RaftClient> accessibleClients, AtomicInteger totalNodes, int timeout, int initDelay) {

        this.nodeID = nodeID;
        this.port = port;
        this.timeout = timeout;

        this.accessibleClients = accessibleClients;
        this.totalNodes = totalNodes;
        this.initDelay = initDelay;

        // if id already exist, do nothing
        if (accessibleClients.get(nodeID) != null) {
            System.out.println("nodeID: " + nodeID + "is taken, please try something else, exiting...");
        }

        // start server and client on port number
        this.start();

        timeoutCycle = () -> {
            if (!receivedHeartBeat.get()) {
                // timed out, no heartbeat received
                System.out.println("Heartbeat not received, timing out: " + nodeID);
                selfElect();
            } else {
                System.out.println("Heartbeat received: " + nodeID);
            }
            // reset heartbeat
            receivedHeartBeat.set(false);
        };

        heartbeatCycle = this::sendHeartBeat;

        executor = Executors.newScheduledThreadPool(2);
        this.startHeartBeatMonitor();
    }

    // starting the node
    private void start() {
        // kickstart server and client
        this.totalNodes.incrementAndGet();
        this.startClient();
        this.startServer();
    }

    // starting gRPC server
    private void startServer() {
        // kickstart server creating new thread
        Thread serverThread = new Thread(() -> {
            try {
                // setting up the server with those variables allows the server thread to edit those variables
                final RaftServer server = new RaftServer(port, term, leaderTerm, receivedHeartBeat, nodeState);
                server.start();
                server.blockUntilShutdown();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        });

        serverThread.start();

    }

    // starting gRPC client
    private void startClient() {
        // put new client into hashmap to create an entry point into this node
        accessibleClients.putIfAbsent(this.nodeID, new RaftClient("localhost", port));
    }

    // starting the timeout cycle
    private void startHeartBeatMonitor() {
        timeoutTask = executor.scheduleWithFixedDelay(timeoutCycle, this.initDelay, this.timeout, unit);
    }

    // stopping the timeout cycle
    private void stopHeartBeatMonitor() {
        if (nodeState.get() != State.LEADER) {
            System.out.println("Cannot stop timeout monitor since node is not a leader" + nodeID);
            return;
        }
        timeoutTask.cancel(false);
    }

    // starting the heartbeat from leader
    private void startHeartBeat() {
        if (nodeState.get() != State.LEADER) {
            System.out.println("Cannot start heartbeat since node is not a leader" + nodeID);
        }
        heartbeatTask = executor.scheduleWithFixedDelay(heartbeatCycle, 0, heartbeatInterval, unit);
    }

    // stopping the heartbeat, no longer leader
    private void stopHeartBeat() {
        heartbeatTask.cancel(false);
    }

    public boolean selfElect() {
        System.out.println("Election started for: " + nodeID);

        // this function is used to change state from a follower to a candidate and this will sent out requestVote messages to all followers
        // auto voted for self & increment self term by 1
        int totalVotes = 1;
        this.term.incrementAndGet();

        this.nodeState.set(State.CANDIDATE);

        // gather votes
        for (int i = 0; i < totalNodes.get(); i++) {
            if (i == nodeID) {
                continue;
            }

            VoteResponse response = accessibleClients.get(i).requestVote(i, term.get());

            if (response.getGranted()) {
                totalVotes++;
            }
        }

        // if candidate gets the majority of votes, then becomes leader
        if (totalVotes > (totalNodes.get() / 2)) {
            if (receivedHeartBeat.get()) {
                // heart beat was received from new / previous leader during requesting votes
                if (leaderTerm.get() >= this.term.get()) {
                    // if leader term is at least as large as candidate current term, candidate recognize leader and return to follower
                    this.nodeState.set(State.FOLLOWER);
                    this.term.set(leaderTerm.get());
                    return false;
                }
            } else {
                this.nodeState.set(State.LEADER);
                System.out.println("Votes: " + totalVotes + " " + nodeID);
                System.out.println("New leader established: " + nodeID);
                // send heartbeat to others to assert dominance once leader
                // stop heartbeat monitor
                this.stopHeartBeatMonitor();
                this.startHeartBeat();
                return true;
            }
        }

        System.out.println("Voting tie/error, candidate reverting to follower " + nodeID);
        this.nodeState.set(State.FOLLOWER);
        return false;
    }

    public void sendHeartBeat() {
        System.out.println("Sending heartbeat from: " + nodeID);
        // once a candidate becomes a leader, it sends heartbeat messages to establish authority
        for (int i = 0; i < totalNodes.get(); i++) {
            if (i == nodeID) {
                continue;
            }
            // todo replace commit index
            AppendEntriesResponse response = accessibleClients.get(i)
                    .appendEntry(this.nodeID, this.term.get(), 0, 0, 0, "", "");
        }
    }

}
