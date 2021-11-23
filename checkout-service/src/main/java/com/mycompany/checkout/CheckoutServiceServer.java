package com.mycompany.checkout;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class CheckoutServiceServer {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    final static Random RANDOM = new Random();

    private Server server;

    public static void main(String[] args) throws Exception {
        final CheckoutServiceServer server = new CheckoutServiceServer();
        server.start();
        server.blockUntilShutdown();
    }

    private void stop() throws InterruptedException {
        if (server != null) {
            server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
        }
    }

    /**
     * Await termination on the main thread since the grpc library uses daemon threads.
     */
    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    public void start() throws IOException {
        /* The port on which the server should run */
        int port = 50051;
        server = ServerBuilder.forPort(port)
                .addService(new CheckoutServiceImpl())
                .build()
                .start();
        logger.info("GRPC server started, listening on " + port);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // Use stderr here since the logger may have been reset by its JVM shutdown hook.
            System.err.println("*** shutting down gRPC server since JVM is shutting down");
            try {
                CheckoutServiceServer.this.stop();
            } catch (InterruptedException e) {
                e.printStackTrace(System.err);
            }
            System.err.println("*** server shut down");
        }));
    }

    static class CheckoutServiceImpl extends CheckoutServiceGrpc.CheckoutServiceImplBase {

        private final Logger logger = LoggerFactory.getLogger(getClass());

        @Override
        public void placeOrder(PlaceOrderRequest request, StreamObserver<PlaceOrderReply> responseObserver) {
            final int millis = 25 + RANDOM.nextInt(50);
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            logger.info("Order successfully placed in " + millis + "ms");
            // StressTestUtils.incrementProgressBarSuccess();
            final PlaceOrderReply placeOrderReply = PlaceOrderReply.newBuilder().setMessage("Order successfully placed!").build();
            responseObserver.onNext(placeOrderReply);
            responseObserver.onCompleted();
        }
    }
}