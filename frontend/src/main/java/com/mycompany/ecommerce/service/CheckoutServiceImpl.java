package com.mycompany.ecommerce.service;

import com.mycompany.checkout.CheckoutServiceGrpc;
import com.mycompany.checkout.PlaceOrderReply;
import com.mycompany.checkout.PlaceOrderRequest;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.TimeUnit;

@Service
public class CheckoutServiceImpl implements CheckoutService {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    String grpcEndpoint;
    ManagedChannel managedChannel;
    CheckoutServiceGrpc.CheckoutServiceBlockingStub checkoutServiceStub;

    @PostConstruct
    public void postConstruct(){
        managedChannel = ManagedChannelBuilder.forTarget(grpcEndpoint)
                .usePlaintext()
                .build();
        this.checkoutServiceStub = CheckoutServiceGrpc.newBlockingStub(managedChannel);
        logger.info("Checkout Service: {}", grpcEndpoint);
    }
    @Override
    public PlaceOrderReply placeOrder(PlaceOrderRequest request) {
        return checkoutServiceStub.placeOrder(request);
    }

    @Value("${checkoutService.grpcEndpoint}")
    public void setGrpcEndpoint(String grpcEndpoint) {
        this.grpcEndpoint = grpcEndpoint;
    }

    @PreDestroy
    public void preDestroy() throws InterruptedException {
        managedChannel.shutdown();
        managedChannel.awaitTermination(5, TimeUnit.SECONDS);
    }

    @Override
    public String toString() {
        return "CheckoutServiceImpl{" +
                "managedChannel=" + managedChannel +
                '}';
    }
}
