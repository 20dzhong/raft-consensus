package org.ur.raftimpl;

import io.grpc.stub.StreamObserver;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.ur.comms.*;

public class RaftImpl extends RaftServerGrpc.RaftServerImplBase {

    /*

      This is the implementation of the proto file, this specifies behaviors for the server
      for example, if it receives a requestVote request, it will send back a VoteResponse object

      It is used by Raft Servers

     */

    int port; // debugging
    AtomicInteger term;
    AtomicBoolean receivedHeartbeat;
    AtomicReference<RaftNode.State> nodeState;
    AtomicInteger votedFor;

    public RaftImpl(int port, NodeVar sVar) {
        this.port = port;
        this.term = sVar.term;
        this.votedFor = sVar.votedFor;
        this.nodeState = sVar.nodeState;
        this.receivedHeartbeat = sVar.receivedHeartBeat;
    }


    @Override
    public void requestVote(VoteRequest request, StreamObserver<VoteResponse> responseObserver) {
        boolean granted = false;
        // cannot request votes from candidate or leaders
        if (nodeState.get() != RaftNode.State.CANDIDATE && nodeState.get() != RaftNode.State.LEADER) {
            // cannot request votes from those who already voted
            if (votedFor.get() == -1) {
                // vote is false from those with higher term than candidates
                if (request.getTerm() >= this.term.get()) {
                    this.votedFor.set(request.getCandidateId());
                    this.term.set(request.getTerm());
                    granted = true;
                }
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
