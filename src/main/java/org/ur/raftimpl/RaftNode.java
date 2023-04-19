package org.ur.raftimpl;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;


public class RaftNode {

    /*

      This represent individual Raft Nodes by using a combination of gRPC client and server and attaching them
      to this class.

      This class kickstarts the server, which can respond, and starts the client, which can request

      This is where the most of the logic will take place,
      for example, node state (follower, candidate, leader), timeouts, logs, etc.

     */


    int nodeID; // id
    String host; // host
    int port; // port
    ConcurrentHashMap<Integer, RaftClient> accessibleClients; // map of all client objects
    AtomicInteger term = new AtomicInteger(1); // term of the node

    public RaftNode(int nodeID, String host, int port, ConcurrentHashMap<Integer, RaftClient> accessibleClients) throws IOException, InterruptedException {

        this.nodeID = nodeID;
        this.host = host;
        this.port = port;
        this.accessibleClients = accessibleClients;

        // if id already exist, do nothing
        if (accessibleClients.get(nodeID) != null) {
            System.out.println("nodeID: " + nodeID + "is taken, please try something else, exiting...");
        }

        // start server and client on port number
        this.start();

        // start cycle of counting timeout, if timed out, become candidate and request votes

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
                final RaftServer server = new RaftServer(host, port, accessibleClients, term);
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
        accessibleClients.putIfAbsent(this.nodeID, new RaftClient(host, port));
    }

    public void requestTest(int recipientID, int senderID, AtomicInteger term) {
        accessibleClients.get(nodeID).requestVote(senderID, recipientID, term.get(), "Sending testComms Request through Node1");
    }
}
