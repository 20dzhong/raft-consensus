package org.ur.raftimpl;

import com.google.common.collect.Lists;
import io.grpc.stub.StreamObserver;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import io.grpc.stub.StreamObservers;
import org.ur.comms.*;

public class RaftImpl extends RaftServerGrpc.RaftServerImplBase {

    /*

      This is the implementation of the proto file, this specifies behaviors for the server
      for example, if it receives a requestVote request, it will send back a VoteResponse object

      It is used by Raft Servers

     */

    int port; // debugging
    AtomicInteger term;
    AtomicInteger leaderTerm;
    AtomicBoolean receivedHeartbeat;
    AtomicReference<RaftNode.State> nodeState;

    public RaftImpl(AtomicInteger term, int port, AtomicInteger leaderTerm, AtomicBoolean receivedHeartbeat, AtomicReference<RaftNode.State> nodeState) {
        this.term = term;
        this.port = port;
        this.leaderTerm = leaderTerm;
        this.receivedHeartbeat = receivedHeartbeat;
        this.nodeState = nodeState;
    }



    @Override
    public void requestVote(VoteRequest request, StreamObserver<VoteResponse> responseObserver) {
        boolean granted = false;
        // cannot request votes from candidate or leaders
        if (nodeState.get() != RaftNode.State.CANDIDATE && nodeState.get() != RaftNode.State.LEADER) {
            if (request.getTerm() >= this.term.get()) {
                this.term.set(request.getTerm());
                granted = true;
            }
        }


        VoteResponse response = VoteResponse.newBuilder()
                .setGranted(granted)
                .setTerm(this.term.get())
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void appendEntries(AppendEntriesRequest request, StreamObserver<AppendEntriesResponse> responseObserver) {
        String newKey = request.getNewLogEntryKey();
        String newValue = request.getNewLogEntryValue();
        int term = request.getTerm();

        AppendEntriesResponse response;

        // if log entry is empty then it is a heartbeat request
        if (newKey.isEmpty() || newValue.isEmpty()) {
            this.receivedHeartbeat.set(true);
            response = AppendEntriesResponse.newBuilder()
                    .setTerm(this.term.get())
                    .setSuccess(true)
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }
}
