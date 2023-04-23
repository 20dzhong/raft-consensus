package org.ur.raftimpl;

import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class RaftServer {

    /*

      This is the server for gRPC, not related to Raft
      This is used in RaftNode

      Server just adds the gRPC services and runs, this file should not require any edits
      unless it's to change the behavior of the threads. For server functions, look at RaftImpl

     */

    private Server server;
    int port;
    SharedVar sVar;

    public RaftServer(int port, SharedVar sVar) {
        this.port = port;
        this.sVar = sVar;
    }

    public void start() throws IOException {
        /* The port on which the server should run */
        server = ServerBuilder.forPort(port)
                .addService(new RaftImpl(port, sVar))
                .build()
                .start();

        System.out.printf("Server started, listening on %d%n", port);
        Runtime.getRuntime().addShutdownHook(
                new Thread(() -> {
                    // Use stderr here since the logger may have been reset by its JVM shutdown hook.
                    System.err.println("*** shutting down gRPC server since JVM is shutting down");
                    RaftServer.this.stop();
                    System.err.println("*** server shut down");
                }));
    }

    public void stop() {
        if (server != null) {
            server.shutdown();
        }
    }

    /**
     * Await termination on the main thread since the grpc library uses daemon threads.
     */
    public void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }
}
