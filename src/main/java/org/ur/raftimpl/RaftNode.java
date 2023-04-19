package org.ur.raftimpl;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This represents the individual RAFT nodes, which will be comprised of a Raft Server on a port and a shared instant of n RaftClient
 * The node can send as well as receive messages
 */

public class RaftNode {
    int nodeID;
    String host;
    int port;
    ConcurrentHashMap<Integer, RaftClient> accessibleClients;

    public RaftNode(int nodeID, String host, int port, ConcurrentHashMap<Integer, RaftClient> accessibleClients) {

        this.nodeID = nodeID;
        this.host = host;
        this.port = port;
        this.accessibleClients = accessibleClients;

        // if id already exist, do nothing
        if (accessibleClients.get(nodeID) != null) {
            System.out.println("nodeID: " + nodeID + "is taken, please try something else, exiting...");
            return;
        }
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
                final RaftServer server = new RaftServer(host, port);
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

    public void requestTest(int recipientID, int senderID) {
        accessibleClients.get(nodeID).requestVote(senderID, recipientID, 2, "Sending testComms Request through Node1");
    }
}
