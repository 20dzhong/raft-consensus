package org.ur.raftimpl;

import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;

public class RaftServer {
    private Server server;
    String host;
    int port;

    public RaftServer(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void start() throws IOException {
        /* The port on which the server should run */
        server = ServerBuilder.forPort(port)
                .addService(new RaftImpl())
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
