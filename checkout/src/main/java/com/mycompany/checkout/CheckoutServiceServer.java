package com.mycompany.checkout;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class CheckoutServiceServer {

    final String shippingServiceUrl;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    final static Random RANDOM = new Random();

    private Server server;

    public CheckoutServiceServer(String shippingServiceUrl) {
        this.shippingServiceUrl = shippingServiceUrl;
    }


    public static void main(String[] args) throws Exception {
        String shippingServiceUrl = Optional.ofNullable(System.getProperty("shippingServiceUrl", null)).or(() -> Optional.ofNullable(System.getenv("SHIPPING_SERVICE_URL"))).orElseThrow(() -> new RuntimeException("System property 'shippingServiceUrl' or environment variable 'SHIPPING_SERVICE_URL' not found"));
        final CheckoutServiceServer server = new CheckoutServiceServer(shippingServiceUrl);
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
                .addService(new CheckoutServiceImpl(this.shippingServiceUrl))
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
        final HttpClient httpClient;

        final URI shippingServiceUrl;

        CheckoutServiceImpl(String shippingServiceUrl) {
            this.httpClient = HttpClient.newBuilder().build();
            String separator = shippingServiceUrl.endsWith("/") ? "" : "/";
            this.shippingServiceUrl = URI.create(shippingServiceUrl + separator + "shipOrder");
        }

        @Override
        public void placeOrder(PlaceOrderRequest request, StreamObserver<PlaceOrderReply> responseObserver) {
            final int millis = 25 + RANDOM.nextInt(50);
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            HttpRequest shippingRequest = HttpRequest.newBuilder()
                    .uri(shippingServiceUrl)
                    .timeout(Duration.ofMinutes(2))
                    .POST(HttpRequest.BodyPublishers.ofString(request.getName()))
                    .build();

            String shippingResponse;
            try {
                HttpResponse<String> shippingResponseObject = this.httpClient.send(shippingRequest, HttpResponse.BodyHandlers.ofString());
                shippingResponse = shippingResponseObject.body();
            } catch (IOException | InterruptedException e) {
                shippingResponse = "Exception invoking " + shippingRequest + " - " + e;
                logger.error(shippingResponse);
            }

            logger.info("Order successfully placed customerId=" + request.getName() + " shipping=" + shippingResponse + " durationInMillis=" + millis);
            // StressTestUtils.incrementProgressBarSuccess();
            PlaceOrderReply placeOrderReply = PlaceOrderReply.newBuilder().setMessage("Order successfully placed!").build();
            responseObserver.onNext(placeOrderReply);
            responseObserver.onCompleted();
        }
    }
}
