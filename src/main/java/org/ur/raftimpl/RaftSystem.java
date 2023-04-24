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


        // this hashmap is where you store all the clients, this way RaftNode can communicate between different servers

        // it's vital to create the nodeID in incremental digits starting from 0, a lot of functions depends on assuming
        // that it has an incremental ordering

        AtomicInteger totalNodes = new AtomicInteger(0);
        AtomicInteger leaderID = new AtomicInteger(-1);
        ConcurrentHashMap<Integer, RaftClient> accessibleClients = new ConcurrentHashMap<>();
        UniversalVar uV = new UniversalVar(accessibleClients, totalNodes, leaderID);

        RaftNode node0 = new RaftNode(0, 50051, uV, 7, 3);
        RaftNode node1 = new RaftNode(1, 50052, uV, 8, 5);
        RaftNode node2 = new RaftNode(2, 50053, uV, 9, 3);

        // since each node starts a server and client, give time for server to boot up
        Thread.sleep(2000);
    }
}
