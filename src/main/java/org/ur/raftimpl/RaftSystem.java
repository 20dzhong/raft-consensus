package org.ur.raftimpl;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.protobuf.ByteString.Output;

import java.io.BufferedReader;
import java.io.OutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class RaftSystem {

    /*
     * 
     * This is the driver class that runs everything
     * This kick starts N RaftNodes, node.start starts running the server
     * 
     */

    UniversalVar uV;
    ArrayList<RaftNode> nodeList = new ArrayList<>();

    RaftSystem(int nodeNum, int portBase, UniversalVar uV, int initDelay) throws InterruptedException {
        this.uV = uV;

        Random rand = new Random();
        for (int i = 0; i < nodeNum; i++) {
            // increment by 1 for delay and port base to prevent overlapping initialization
            nodeList.add(new RaftNode(i, portBase + i, uV, 5, initDelay + i));
        }

        Thread.sleep((initDelay + 1) * 1000L);
    }

    public String get(String key) {
        return nodeList.get(uV.leaderID.get()).get(key);
    }

    public void put(String key, String value) {
        nodeList.get(uV.leaderID.get()).put(key, value);
    }

    public static void main(String[] args) throws InterruptedException {

        // this hashmap is where you store all the clients, this way RaftNode can
        // communicate between different servers

        // it's vital to create the nodeID in incremental digits starting from 0, a lot
        // of functions depends on assuming
        // that it has an incremental ordering

        AtomicInteger totalNodes = new AtomicInteger(0);
        AtomicInteger leaderID = new AtomicInteger(-1);
        ConcurrentHashMap<Integer, RaftClient> accessibleClients = new ConcurrentHashMap<>();
        UniversalVar uV = new UniversalVar(accessibleClients, totalNodes, leaderID);

        RaftSystem raft = new RaftSystem(4, 50051, uV, 5);

        // some starter values for the key-val store
        raft.put("Hello", "World");
        raft.put("Hello", "Qasim");
        raft.put("Donovan", "Zhong");
        raft.put("Raft", "gRPC");
        raft.put("Damn", "it");

        Thread.sleep(500);

        // Since client is communicating over the internet, client communication is
        // always over HTTP. So, we use TCP listener
        int port = 8000; // choose a port number that is not already in use
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            System.out.println("TCP server is listening on port " + port);

            while (true) {
                Socket socket = serverSocket.accept(); // wait for incoming connection
                System.out.println("Incoming connection from " + socket.getInetAddress().getHostAddress());
                // handle the incoming connection here
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                String parts[] = new String[3];
                String tmp;
                int count = 0;
                while ((tmp = reader.readLine()) != null && tmp.length() != 0) {
                    parts[count++] = tmp;
                }

                System.out.println("Received message from client: " + parts[0] + ", " + parts[1] + ", " + parts[2]);

                OutputStream output = socket.getOutputStream();

                // raft.put(parts[1], parts[2]);
                if (parts[0].equals("put")) {
                    String key = parts[1];
                    String val = parts[2];
                    System.out.println("putting (" + key + ", " + val + ")...");
                    raft.put(key, val);
                    String response = "put " + key + "-" + val;
                    output.write(response.getBytes());
                } else if (parts[0].equals("get")) {
                    String key = parts[1];
                    System.out.println("getting " + key + "...");
                    String val = raft.get(key);
                    System.out.println("got key-val pair: " + key + "-" + val);
                    String response = "got " + key + "-" + val;
                    output.write(response.getBytes());
                }
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
