package org.ur.raftimpl;

import com.google.common.collect.Lists;
import io.grpc.stub.StreamObserver;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import org.ur.comms.RaftServerGrpc;
import org.ur.comms.VoteRequest;
import org.ur.comms.VoteResponse;

public class RaftImpl extends RaftServerGrpc.RaftServerImplBase {

    /*

      This is the implementation of the proto file, this specifies behaviors for the server
      for example, if it receives a requestVote request, it will send back a VoteResponse object

      It is used by Raft Servers

     */

    AtomicInteger term;

    public RaftImpl(AtomicInteger term) {
        this.term = term;
    }


    @Override
    public void requestVote(VoteRequest request, StreamObserver<VoteResponse> responseObserver) {
        String msg = "";
        boolean granted = false;
        msg = "Request vote from node " + request.getSenderId() + " to " + request.getRecipientId() + " succesfully received";

        // this node's term is lagging behind
        if (request.getTerm() >= term.get()) {
            granted = true;
            term.set(request.getTerm());
        } else {
            msg = "Term is too low, rejected!";
        }

        VoteResponse reply = VoteResponse.newBuilder()
                .setTerm(term.get())
                .setGranted(granted)
                .setText(msg)
                .build();

        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }


}
