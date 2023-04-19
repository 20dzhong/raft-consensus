package org.ur.raftimpl;

import com.google.common.collect.Lists;
import io.grpc.stub.StreamObserver;
import java.util.List;
import java.util.stream.IntStream;

import org.ur.comms.RaftServerGrpc;
import org.ur.comms.VoteRequest;
import org.ur.comms.VoteResponse;

public class RaftImpl extends RaftServerGrpc.RaftServerImplBase {
    // term number
    long term = 1;

    // you would probably put stuff like logs, and other stuff here

    @Override
    public void requestVote(VoteRequest request, StreamObserver<VoteResponse> responseObserver) {
        String msg = "";
        boolean granted = false;

        // this node's term is lagging behind
        if (request.getTerm() >= term) {
            granted = true;
            term = request.getTerm();
        }

        VoteResponse reply = VoteResponse.newBuilder()
                .setTerm(term)
                .setGranted(granted)
                .setText("Response sent!")
                .build();

        responseObserver.onNext(reply);
        responseObserver.onCompleted();

    }


}
