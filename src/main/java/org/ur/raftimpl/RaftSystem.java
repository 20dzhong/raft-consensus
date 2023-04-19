package org.ur.raftimpl;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

public class RaftSystem {


    public static void main(String[] args) throws IOException, InterruptedException {
        /**
         * Kick start n servers
         * As new servers are created, new clients are created as well and are added into a hashmap
         */

        int totalNodes = 3;

        // this hashmap is where you store all the clients, this way RaftNode can communicate between different servers
        ConcurrentHashMap<Integer, RaftClient> accessibleClients = new ConcurrentHashMap<>();
        RaftNode node1 = new RaftNode(1, "localhost",50051, accessibleClients);
        RaftNode node2 = new RaftNode(2, "localhost",50052, accessibleClients);
        RaftNode node3 = new RaftNode(3, "localhost",50053, accessibleClients);

        node1.start();
        node2.start();
        node3.start();

        node1.requestTest(3, 1);
        node2.requestTest(1, 2);

    }
}
