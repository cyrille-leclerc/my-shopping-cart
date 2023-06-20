package com.mycompany.ecommerce.service;

import com.mycompany.checkout.PlaceOrderReply;
import com.mycompany.checkout.PlaceOrderRequest;

public interface CheckoutService {
    PlaceOrderReply placeOrder(PlaceOrderRequest request);
}
