package org.ur.raftimpl;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class SharedStates {
    // shared values
    ConcurrentHashMap<Integer, RaftClient> accessibleClients; // map of all client objects
    AtomicInteger totalNodes; // total number of nodes
    AtomicInteger term = new AtomicInteger(1); // default term of the node
    AtomicInteger votedFor = new AtomicInteger(-1);
    AtomicBoolean receivedHeartBeat = new AtomicBoolean(false); // whether or not you received a heartbeat this term
    AtomicInteger leaderTerm = new AtomicInteger(0); // term of the leader
    AtomicReference<RaftNode.State> nodeState = new AtomicReference<>(RaftNode.State.FOLLOWER);
}
