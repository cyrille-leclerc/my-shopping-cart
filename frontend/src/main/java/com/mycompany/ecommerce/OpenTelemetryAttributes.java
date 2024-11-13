package com.mycompany.ecommerce;

import io.opentelemetry.api.common.AttributeKey;

public class OpenTelemetryAttributes {
    public static final AttributeKey<String> OUTCOME = AttributeKey.stringKey("outcome");
    public static final AttributeKey<String> CUSTOMER_ID = AttributeKey.stringKey("customer_id");
    public static final AttributeKey<Double> ORDER_PRICE = AttributeKey.doubleKey("order_price");
    public static final AttributeKey<String> ORDER_PRICE_RANGE = AttributeKey.stringKey("order_price_range");
    public static final AttributeKey<String> SHIPPING_COUNTRY = AttributeKey.stringKey("shipping_country");
    public static final AttributeKey<String> SHIPPING_METHOD = AttributeKey.stringKey("shipping_method");
    public static final AttributeKey<String> PAYMENT_METHOD = AttributeKey.stringKey("payment_method");
}