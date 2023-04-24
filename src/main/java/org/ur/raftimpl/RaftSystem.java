package org.ur.raftimpl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class RaftSystem {

    /*

      This is the driver class that runs everything
      This kick starts N RaftNodes, node.start starts running the server

     */
    UniversalVar uV;
    ArrayList<RaftNode> nodeList = new ArrayList<>();

    RaftSystem(int nodeNum, int portBase, UniversalVar uV, int initDelay) throws InterruptedException {
        this.uV = uV;

        Random rand = new Random();
        for (int i = 0; i < nodeNum; i++) {
            // increment by 1 for delay and port base to prevent overlapping initialization
            nodeList.add(new RaftNode(i, portBase + i, uV, rand.nextInt(10), initDelay + i));
        }

        Thread.sleep((initDelay + 1) * 1000L);
    }

    public String get(String key) {
       return nodeList.get(uV.leaderID.get()).get(key);
    }

    public void put(String key, String value) {
        System.out.println("Put: " + key + " : " + value);
        nodeList.get(uV.leaderID.get()).put(key, value);
    }

    public static void main(String[] args) throws InterruptedException {


        // this hashmap is where you store all the clients, this way RaftNode can communicate between different servers

        // it's vital to create the nodeID in incremental digits starting from 0, a lot of functions depends on assuming
        // that it has an incremental ordering

        AtomicInteger totalNodes = new AtomicInteger(0);
        AtomicInteger leaderID = new AtomicInteger(-1);
        ConcurrentHashMap<Integer, RaftClient> accessibleClients = new ConcurrentHashMap<>();
        UniversalVar uV = new UniversalVar(accessibleClients, totalNodes, leaderID);

        RaftSystem raft = new RaftSystem(4, 50051, uV, 5);

        raft.put("Hello", "World");
        Thread.sleep(500);
        System.out.println("Get: " + raft.get("Hello"));

        // since each node starts a server and client, give time for server to boot up
    }
}
