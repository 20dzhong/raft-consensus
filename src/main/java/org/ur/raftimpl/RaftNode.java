package org.ur.raftimpl;

import org.ur.comms.VoteResponse;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
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

    State nodeState = State.FOLLOWER; // not used for anything right now, just debugging

    // shared values
    ConcurrentHashMap<Integer, RaftClient> accessibleClients; // map of all client objects
    AtomicInteger totalNodes; // total number of nodes

    AtomicInteger term = new AtomicInteger(1); // default term of the node
    AtomicBoolean receivedHeartBeat = new AtomicBoolean(false); // whether or not you received a heartbeat this term
    AtomicInteger leaderTerm = new AtomicInteger(0); // term of the leader

    public RaftNode(int nodeID, int port, ConcurrentHashMap<Integer, RaftClient> accessibleClients, AtomicInteger totalNodes) {

        this.nodeID = nodeID;
        this.port = port;

        this.accessibleClients = accessibleClients;
        this.totalNodes = totalNodes;

        // if id already exist, do nothing
        if (accessibleClients.get(nodeID) != null) {
            System.out.println("nodeID: " + nodeID + "is taken, please try something else, exiting...");
        }

        // start server and client on port number
        this.start();

        Runnable timeoutCycle = new Runnable() {
            public void run() {
                System.out.println("I'm going to time out" + nodeID);
                // TODO finish functions
                // once timeout, self elect and become a candidate
                // selfElect();
            }
        };

        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        executor.scheduleAtFixedRate(timeoutCycle, 0, 3, TimeUnit.SECONDS);
    }

    private void start() {
        // kick start server and client
        this.totalNodes.incrementAndGet();
        this.startClient();
        this.startServer();
    }

    private void startServer() {
        // kickstart server creating new thread
        Thread serverThread = new Thread(() -> {
            try {
                // setting up the server with those variables allows the server thread to edit those variables
                final RaftServer server = new RaftServer(port, term, leaderTerm, receivedHeartBeat);
                server.start();
                server.blockUntilShutdown();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        });

        serverThread.start();

    }

    private void startClient() {
        // put new client into hashmap to create an entry point into this node
        accessibleClients.putIfAbsent(this.nodeID, new RaftClient("localhost", port));
    }

    public boolean selfElect() {
        // this function is used to change state from a follower to a candidate and this will sent out requestVote messages to all followers
        // auto voted for self & increment self term by 1
        int totalVotes = 1;
        this.term.incrementAndGet();

        this.nodeState = State.CANDIDATE;

        // gather votes
        for (int i = 0; i < totalNodes.get(); i++) {
            if (i == nodeID) {
                continue;
            }
            VoteResponse response = accessibleClients.get(i).requestVote(i, term.get(), "Requesting Votes");

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
                    this.nodeState = State.FOLLOWER;
                    this.term.set(leaderTerm.get());
                    return false;
                }
            } else {
                this.nodeState = State.LEADER;
                return true;
            }
        }

        this.nodeState = State.FOLLOWER;
        return false;
    }

    public void sendHeartBeat() {
        // once a candidate becomes a leader, it sends heartbeat messages to establish authority
    }

}
