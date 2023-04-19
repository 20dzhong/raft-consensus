package org.ur.raftimpl;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.ur.comms.HelloRequest;
import org.ur.comms.RaftServerGrpc;
import org.ur.comms.VoteRequest;
import org.ur.comms.VoteResponse;

public class RaftClient implements Closeable {

    /*

      This is the client class for gRPC, not related to Raft
      This is used in RaftNode

      Clients in gRPC are used for sending requests to servers, so the functions
      here are used for communicating with their own server based on the channel
      and asking for a response

     */


    private final ManagedChannel channel;
    private final RaftServerGrpc.RaftServerBlockingStub blockingStub;
    private final RaftServerGrpc.RaftServerStub asyncStub;

    public RaftClient(ManagedChannel channel) {
        this.channel = channel;
        this.blockingStub = RaftServerGrpc.newBlockingStub(channel);
        this.asyncStub = RaftServerGrpc.newStub(channel);
    }

    public RaftClient(String host, int port) {
        this(ManagedChannelBuilder.forAddress(host, port)
                // Channels are secure by default (via SSL/TLS). For the example we disable TLS to avoid
                // needing certificates.
                .usePlaintext()
                .build());
    }

    public void requestVote(int sendID, int recpID, int term, String text) {
        VoteRequest request = VoteRequest.newBuilder()
                .setSenderId(sendID)
                .setRecipientId(recpID)
                .setTerm(term)
                .setText(text)
                .build();

        VoteResponse response = this.blockingStub.requestVote(request);
        System.out.println("Vote Request Status: " + response.getGranted());
        System.out.printf("requestVote() response: %s\n%n", response.getText());
    }

    @Override
    public void close() throws IOException {
        try {
            channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new IOException(e.getMessage());
        }
    }
}
