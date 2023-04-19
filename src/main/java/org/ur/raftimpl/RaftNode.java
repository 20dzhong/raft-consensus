package org.ur.raftimpl;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
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
    AtomicInteger term = new AtomicInteger(1); // default term of the node
    AtomicInteger totalNodes;

    public RaftNode(int nodeID, int port, ConcurrentHashMap<Integer, RaftClient> accessibleClients, AtomicInteger totalNodes)
            throws IOException, InterruptedException {

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

        // TODO implement timeout cycle (the request vote function is written here in selfElect
        // TODO implement
    }

    public void start() throws IOException, InterruptedException {
        // kick start server and client
        this.startClient();
        this.startServer();

    }

    private void startServer() throws IOException, InterruptedException {
        // kickstart server creating new thread
        Thread serverThread = new Thread(() -> {
            try {
                final RaftServer server = new RaftServer(port, accessibleClients, term);
                server.start();
                server.blockUntilShutdown();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        });

        serverThread.start();

    }

    private void startClient() throws IOException {
        // put new client into hashmap to create an entry point into this node
        accessibleClients.putIfAbsent(this.nodeID, new RaftClient("localhost", port));
    }

    public void requestTest(int recipientID, int senderID, AtomicInteger term) {
        accessibleClients.get(nodeID).requestVote(senderID, recipientID, term.get(), "Sending test Comms Request through Node1");
    }

    public void selfElect() {
        // this function is used to change state from a follower to a candidate and this will sent out requestVote messages
        // to all other followers
        nodeState = State.CANDIDATE;
        for (int i = 0; i < totalNodes.get(); i++) {
            if (i == nodeID) {
                continue;
            }
            accessibleClients.get(nodeID).requestVote(1, i, term.get(), "Requesting Votes");
        }
    }
}
