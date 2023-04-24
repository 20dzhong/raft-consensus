package org.ur.raftimpl;

import org.ur.comms.AppendEntriesResponse;
import org.ur.comms.VoteResponse;

import java.io.IOException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;


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

    // a clump of Atomic values that needs to be passed around, clumped together for simplicity in code
    // not sure if best idea
    SharedVar sVar = new SharedVar();

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

        this.sVar.accessibleClients = accessibleClients;
        this.sVar.totalNodes = totalNodes;
        this.initDelay = initDelay;

        // if id already exist, do nothing
        if (accessibleClients.get(nodeID) != null) {
            System.out.println("nodeID: " + nodeID + "is taken, please try something else, exiting...");
        }

        // start server and client on port number
        this.start();

        timeoutCycle = () -> {
            if (!sVar.receivedHeartBeat.get()) {
                // timed out, no heartbeat received
                System.out.println("Heartbeat not received, timing out: " + nodeID);
                selfElect();
            } else {
                System.out.println("Heartbeat received: " + nodeID);
            }
            // reset heartbeat
            sVar.receivedHeartBeat.set(false);
        };

        heartbeatCycle = this::sendHeartBeat;

        executor = Executors.newScheduledThreadPool(2);
        this.startHeartBeatMonitor();
    }

    // starting the node
    private void start() {
        // kickstart server and client
        this.sVar.totalNodes.incrementAndGet();
        this.startClient();
        this.startServer();
    }

    // starting gRPC server
    private void startServer() {
        // kickstart server creating new thread
        Thread serverThread = new Thread(() -> {
            try {
                // setting up the server with those variables allows the server thread to edit those variables
                final RaftServer server = new RaftServer(port, sVar);
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
        sVar.accessibleClients.putIfAbsent(this.nodeID, new RaftClient("localhost", port));
    }

    // starting the timeout cycle
    private void startHeartBeatMonitor() {
        timeoutTask = executor.scheduleWithFixedDelay(timeoutCycle, this.initDelay, this.timeout, unit);
    }

    // stopping the timeout cycle
    private void stopHeartBeatMonitor() {
        if (sVar.nodeState.get() != State.LEADER) {
            System.out.println("Cannot stop timeout monitor since node is not a leader" + nodeID);
            return;
        }
        timeoutTask.cancel(false);
    }

    // starting the heartbeat from leader
    private void startHeartBeat() {
        if (sVar.nodeState.get() != State.LEADER) {
            System.out.println("Cannot start heartbeat since node is not a leader" + nodeID);
        }
        heartbeatTask = executor.scheduleWithFixedDelay(heartbeatCycle, 0, heartbeatInterval, unit);
    }

    // stopping the heartbeat, no longer leader
    private void stopHeartBeat() {
        heartbeatTask.cancel(false);
    }

    public void selfElect() {
        System.out.println("Election started for: " + nodeID);

        // this function is used to change state from a follower to a candidate and this will sent out requestVote messages to all followers
        // auto voted for self & increment self term by 1
        int totalVotes = 1;
        this.sVar.term.incrementAndGet();

        this.sVar.nodeState.set(State.CANDIDATE);

        // gather votes
        for (int i = 0; i < sVar.totalNodes.get(); i++) {
            if (i == nodeID) {
                continue;
            }

            VoteResponse response = sVar.accessibleClients.get(i).requestVote(i, sVar.term.get());

            if (response.getGranted()) {
                totalVotes++;
            }
        }

        // if candidate gets the majority of votes, then becomes leader
        if (totalVotes > (sVar.totalNodes.get() / 2)) {
            if (sVar.receivedHeartBeat.get()) {
                // heart beat was received from new / previous leader during requesting votes
                if (sVar.leaderTerm.get() >= this.sVar.term.get()) {
                    // if leader term is at least as large as candidate current term, candidate recognize leader and return to follower
                    this.sVar.nodeState.set(State.FOLLOWER);
                    this.sVar.term.set(sVar.leaderTerm.get());
                    return;
                }
            } else {
                this.sVar.nodeState.set(State.LEADER);
                System.out.println("Votes: " + totalVotes + " " + nodeID);
                System.out.println("New leader established: " + nodeID);
                // send heartbeat to others to assert dominance once leader
                // stop heartbeat monitor
                this.stopHeartBeatMonitor();
                this.startHeartBeat();
                return;
            }
        }

        System.out.println("Voting tie/failed, candidate reverting to follower " + nodeID);
        this.sVar.nodeState.set(State.FOLLOWER);
        return;
    }

    public void sendHeartBeat() {
        System.out.println("Sending heartbeat from: " + nodeID);
        // once a candidate becomes a leader, it sends heartbeat messages to establish authority
        for (int i = 0; i < sVar.totalNodes.get(); i++) {
            if (i == nodeID) {
                continue;
            }
            // todo replace commit index
            AppendEntriesResponse response = sVar.accessibleClients.get(i)
                    .appendEntry(this.nodeID, this.sVar.term.get(), 0, 0, 0, "", "");
        }
    }

}
