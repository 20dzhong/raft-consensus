package org.ur.raftimpl;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class SharedVar {
    // shared values
    public ConcurrentHashMap<Integer, RaftClient> accessibleClients; // map of all client objects
    public AtomicInteger totalNodes; // total number of nodes
    public AtomicInteger term = new AtomicInteger(1); // default term of the node
    public AtomicInteger votedFor = new AtomicInteger(-1);
    public AtomicBoolean receivedHeartBeat = new AtomicBoolean(false); // whether or not you received a heartbeat this term
    public AtomicInteger leaderTerm = new AtomicInteger(0); // term of the leader
    public AtomicReference<RaftNode.State> nodeState = new AtomicReference<>(RaftNode.State.FOLLOWER);

}
