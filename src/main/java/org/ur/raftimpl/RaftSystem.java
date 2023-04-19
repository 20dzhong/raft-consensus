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

        AtomicInteger totalNodes = new AtomicInteger(3);

        // this hashmap is where you store all the clients, this way RaftNode can communicate between different servers

        // it's vital to create the nodeID in incremental digits starting from 0, a lot of functions depends on assuming
        // that it has an incremental ordering

        ConcurrentHashMap<Integer, RaftClient> accessibleClients = new ConcurrentHashMap<>();

        RaftNode node0 = new RaftNode(0, 50051, accessibleClients, totalNodes);
        RaftNode node1 = new RaftNode(1, 50052, accessibleClients, totalNodes);
        RaftNode node2 = new RaftNode(2, 50053, accessibleClients, totalNodes);

        System.out.println(node0.nodeState);
        System.out.println(node0.term.get());
        System.out.println(node1.term.get());
        System.out.println(node2.term.get());

        node0.selfElect();

        // because 0 has highest term, the change is reflected once it becomes the leader
        System.out.println(node0.nodeState);
        System.out.println(node0.term.get());
        System.out.println(node1.term.get());
        System.out.println(node2.term.get());
    }
}
