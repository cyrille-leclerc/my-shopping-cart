package com.mycompany.checkout;

import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

class CheckoutServiceImpl extends CheckoutServiceGrpc.CheckoutServiceImplBase {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    final HttpClient httpClient;

    final URI shippingServiceUrl;

    final int exceptionPercentage = 1;

    CheckoutServiceImpl(String shippingServiceUrl) {
        this.httpClient = HttpClient.newBuilder().build();
        String separator = shippingServiceUrl.endsWith("/") ? "" : "/";
        this.shippingServiceUrl = URI.create(shippingServiceUrl + separator + "shipOrder");
    }

    @Override
    public void placeOrder(PlaceOrderRequest placeOrderRequest, StreamObserver<PlaceOrderReply> responseObserver) {
        final int millis = 25 + CheckoutServiceServer.RANDOM.nextInt(50);
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            logger.warn("Thread interrupted", e);
        }

        if (CheckoutServiceServer.RANDOM.nextInt(100) == 0) {
            logger.atWarn()
                    .addKeyValue("customerId", placeOrderRequest.getName())
                    .setCause(new RuntimeException("random failure"))
                    .log("internal-error");
        }
        HttpRequest shippingRequest = HttpRequest.newBuilder()
                .uri(shippingServiceUrl)
                .timeout(Duration.ofMinutes(2))
                .POST(HttpRequest.BodyPublishers.ofString(placeOrderRequest.getName()))
                .build();

        String shippingResponse;
        try {
            HttpResponse<String> shippingResponseObject = this.httpClient.send(shippingRequest, HttpResponse.BodyHandlers.ofString());
            shippingResponse = shippingResponseObject.body();
        } catch (IOException | InterruptedException e) {
            shippingResponse = "Exception invoking " + shippingRequest + " - " + e;
            logger.error(shippingResponse);
            logger.atError()
                    .addKeyValue("customerId", placeOrderRequest.getName())
                    .addKeyValue("shippingSvcResponse", shippingResponse)
                    .addKeyValue("durationInMillis", millis)
                    .log("Order shipping failed");
        }

        MDC.put("customerId", placeOrderRequest.getName());
        logger.atInfo()
                .addKeyValue("outcome", "success")
                .addKeyValue("customerId", placeOrderRequest.getName())
                .addKeyValue("shippingSvcResponse", shippingResponse)
                .addKeyValue("durationInMillis", millis)
                .log("placeOrder");
        logger.info("Order {} successfully placed", "order-" + CheckoutServiceServer.RANDOM.nextInt(1_00000));

        if (CheckoutServiceServer.RANDOM.nextInt(100) <= exceptionPercentage) {
            RuntimeException exception = new RuntimeException("Checkout failure");
            responseObserver.onError(exception);
        }
        // StressTestUtils.incrementProgressBarSuccess();
        PlaceOrderReply placeOrderReply = PlaceOrderReply.newBuilder().setMessage("Order successfully placed!").build();
        responseObserver.onNext(placeOrderReply);
        responseObserver.onCompleted();
    }
}
