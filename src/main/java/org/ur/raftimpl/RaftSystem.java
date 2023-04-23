package org.ur.raftimpl;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class RaftSystem {

    /*

      This is the driver class that runs everything
      This kick starts N RaftNodes, node.start starts running the server

     */

    public static void main(String[] args) throws IOException, InterruptedException {

        AtomicInteger totalNodes = new AtomicInteger(0);

        // this hashmap is where you store all the clients, this way RaftNode can communicate between different servers

        // it's vital to create the nodeID in incremental digits starting from 0, a lot of functions depends on assuming
        // that it has an incremental ordering

        ConcurrentHashMap<Integer, RaftClient> accessibleClients = new ConcurrentHashMap<>();

        RaftNode node0 = new RaftNode(0, 50051, accessibleClients, totalNodes, 7, 3);
        RaftNode node1 = new RaftNode(1, 50052, accessibleClients, totalNodes, 8, 5);
        RaftNode node2 = new RaftNode(2, 50053, accessibleClients, totalNodes, 9, 3);
        RaftNode node3 = new RaftNode(3, 50054, accessibleClients, totalNodes, 5, 7);
        RaftNode node4 = new RaftNode(4, 50055, accessibleClients, totalNodes, 3, 8);
//        RaftNode node2 = new RaftNode(2, 50053, accessibleClients, totalNodes, 60);

        // since each node starts a server and client, give time for server to boot up
        Thread.sleep(2000);
    }
}
