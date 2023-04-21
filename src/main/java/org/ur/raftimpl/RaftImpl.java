package org.ur.raftimpl;

import com.google.common.collect.Lists;
import io.grpc.stub.StreamObserver;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import io.grpc.stub.StreamObservers;
import org.ur.comms.*;

public class RaftImpl extends RaftServerGrpc.RaftServerImplBase {

    /*

      This is the implementation of the proto file, this specifies behaviors for the server
      for example, if it receives a requestVote request, it will send back a VoteResponse object

      It is used by Raft Servers

     */

    AtomicInteger term;
    int port;
    AtomicInteger leaderTerm;
    AtomicBoolean receivedHeartbeat;

    public RaftImpl(AtomicInteger term, int port, AtomicInteger leaderTerm, AtomicBoolean receivedHeartbeat) {
        this.term = term;
        this.port = port;
        this.leaderTerm = leaderTerm;
        this.receivedHeartbeat = receivedHeartbeat;
    }


    @Override
    public void requestVote(VoteRequest request, StreamObserver<VoteResponse> responseObserver) {
        boolean granted = false;
        // TODO add more conditions
        if (request.getTerm() >= this.term.get()) {
            this.term.set(request.getTerm());
            granted = true;
        }

        VoteResponse response = VoteResponse.newBuilder()
                .setGranted(granted)
                .setTerm(this.term.get())
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public StreamObserver<AppendEntriesRequest> appendEntries(StreamObserver<AppendEntriesResponse> responseObserver) {
        return
                new StreamObserver<AppendEntriesRequest>() {
                    @Override
                    public void onNext(AppendEntriesRequest request) {
                        boolean success = true;

                        if (request.getTerm() < term.get()) {
                            success = false;
                        }

                        AppendEntriesResponse response = AppendEntriesResponse.newBuilder()
                                .setTerm(term.get())
                                .setSuccess(success)
                                .build();
                        responseObserver.onNext(response);
                    }

                    @Override
                    public void onError(Throwable t) {
                        System.out.println("Encountered error in appendEntries()");
                        t.printStackTrace();
                    }

                    @Override
                    public void onCompleted() {
                        responseObserver.onCompleted();
                    }
                };
    }


}
