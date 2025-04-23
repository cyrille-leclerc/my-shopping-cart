package com.mycompany.antifraud;

import io.opentelemetry.api.common.AttributeKey;

public class FraudDetectionAttributes {
    public static final AttributeKey<String> OUTCOME = AttributeKey.stringKey("outcome");
    public static final AttributeKey<String> TENANT_ID = AttributeKey.stringKey("tenant.id");
    public static final AttributeKey<String> TENANT_SHORTCODE = AttributeKey.stringKey("tenant.short_code");
    public static final AttributeKey<String> PAYMENT_METHOD = AttributeKey.stringKey("payment_method");
}
