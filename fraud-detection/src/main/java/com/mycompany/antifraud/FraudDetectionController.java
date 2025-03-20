package com.mycompany.antifraud;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.spi.LoggingEventBuilder;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Random;

@ManagedResource
@RestController()
public class FraudDetectionController {

    final static Random RANDOM = new Random();
    final static BigDecimal FIVE_PERCENT = new BigDecimal(5).divide(new BigDecimal(100), RoundingMode.HALF_UP);

    final Logger logger = LoggerFactory.getLogger(getClass());
    enum AnomalyType {
        ORDER_VALUE,
        TENANT
    }
    AnomalyType anomalyType = AnomalyType.TENANT;

    int averageDurationMillisOnSmallShoppingCarts = 50;
    int averageDurationMillisOnMediumShoppingCarts = 50;
    int averageDurationMillisOnLargeShoppingCart = 1000;

    int fraudPercentageOnSmallShoppingCarts = 0;
    int fraudPercentageOnMediumShoppingCarts = 0;
    int fraudPercentageOnLargeShoppingCarts = 15;

    int exceptionPercentageOnSmallShoppingCarts = 0;
    int exceptionPercentageOnMediumShoppingCarts = 0;
    int exceptionPercentageOnLargeShoppingCarts = 10;

    int valueUpperBoundaryDollarsOnSmallShoppingCart = 10;
    int valueUpperBoundaryDollarsOnMediumShoppingCarts = 100;

    final DoubleHistogram fraudDetectionHistogram;

    final DataSource dataSource;

    public FraudDetectionController(Meter meter, DataSource dataSource) {
        this.fraudDetectionHistogram = meter.histogramBuilder("fraud.check_order").setUnit("{dollars}").build();
        this.dataSource = dataSource;
    }

    static class FraudDetectionConfiguration {
        int durationOffsetInMillis;
        int fraudPercentage;
        int exceptionPercentage;
        LoggingEventBuilder loggingEventBuilder;
        String msg;

        @Override
        public String toString() {
            return "FraudDetectionConfiguration{" +
                    "durationOffsetInMillis=" + durationOffsetInMillis +
                    ", fraudPercentage=" + fraudPercentage +
                    ", exceptionPercentage=" + exceptionPercentage +
                    ", loggingEventBuilder=" + loggingEventBuilder +
                    ", msg='" + msg + '\'' +
                    '}';
        }
    }

    FraudDetectionConfiguration buildFraudDetectionConfiguration(int orderValueDollars, TenantFilter.Tenant tenant) {
        FraudDetectionConfiguration cfg = new FraudDetectionConfiguration();
        switch (anomalyType) {
            case TENANT -> {
                if ("FR".equalsIgnoreCase(tenant.getShortCode())) {
                    cfg.fraudPercentage = 35;
                    cfg.durationOffsetInMillis = averageDurationMillisOnLargeShoppingCart;
                    cfg.exceptionPercentage = exceptionPercentageOnLargeShoppingCarts;
                    cfg.loggingEventBuilder = logger.atWarn();
                    cfg.msg = "problem FD-654321 executing fraud detection";
                } else {
                    cfg.fraudPercentage = fraudPercentageOnSmallShoppingCarts;
                    cfg.durationOffsetInMillis = averageDurationMillisOnSmallShoppingCarts;
                    cfg.exceptionPercentage = exceptionPercentageOnSmallShoppingCarts;
                    cfg.loggingEventBuilder = logger.atInfo();
                    cfg.msg = "ok";
                }
            }
            case ORDER_VALUE -> {
                if (orderValueDollars < valueUpperBoundaryDollarsOnSmallShoppingCart) {
                    cfg.fraudPercentage = fraudPercentageOnSmallShoppingCarts;
                    cfg.durationOffsetInMillis = averageDurationMillisOnSmallShoppingCarts;
                    cfg.exceptionPercentage = exceptionPercentageOnSmallShoppingCarts;
                    cfg.loggingEventBuilder = logger.atInfo();
                    cfg.msg = "ok";
                } else if (orderValueDollars < valueUpperBoundaryDollarsOnMediumShoppingCarts) {
                    cfg.fraudPercentage = fraudPercentageOnMediumShoppingCarts;
                    cfg.durationOffsetInMillis = averageDurationMillisOnMediumShoppingCarts;
                    cfg.exceptionPercentage = exceptionPercentageOnMediumShoppingCarts;
                    cfg.loggingEventBuilder = logger.atInfo();
                    cfg.msg = "ok";
                } else {
                    cfg.fraudPercentage = fraudPercentageOnLargeShoppingCarts;
                    cfg.durationOffsetInMillis = averageDurationMillisOnLargeShoppingCart;
                    cfg.exceptionPercentage = exceptionPercentageOnLargeShoppingCarts;
                    cfg.loggingEventBuilder = logger.atWarn();
                    cfg.msg = "problem FD-123456 executing fraud detection";
                }
            }
        }
        return cfg;
    }

    @RequestMapping(path = "fraud/checkOrder", method = {RequestMethod.GET, RequestMethod.POST})
    public String checkOrder(
            @RequestParam double orderValue,
            @RequestParam String shippingCountry,
            @RequestParam String customerIpAddress) {
        TenantFilter.Tenant tenant = TenantFilter.Tenant.current();

        Span.current().setAttribute("order_value", orderValue);
        Span.current().setAttribute("customer_ip_address", customerIpAddress);
        //Span.current().setAttribute("shipping_country", shippingCountry);
        Span.current().setAttribute("database_pool", tenant.shortCode);

        int orderValueDollars = Double.valueOf(orderValue).intValue();

        FraudDetectionConfiguration cfg = buildFraudDetectionConfiguration(orderValueDollars, tenant);

        int checkOrderDurationMillis = cfg.durationOffsetInMillis + RANDOM.nextInt(Math.max(5, new BigDecimal(cfg.durationOffsetInMillis).multiply(FIVE_PERCENT).intValue()));
        // positive score means fraud
        int fraudScore = cfg.fraudPercentage - RANDOM.nextInt(100);
        Span.current().setAttribute("fraud_score", fraudScore);

        String outcome;
        try (Connection cnn = dataSource.getConnection()) {
            try (Statement stmt = cnn.createStatement()) {

                stmt.execute("select pg_sleep(0.05)");
                Thread.sleep(checkOrderDurationMillis);
            }
            if (cfg.exceptionPercentage - RANDOM.nextInt(100) > 0) {
                throw new RuntimeException("Fraud Detection Processing Exception");
            }
            outcome = fraudScore > 0 ? "denied" : "approved";

        } catch (SQLException | InterruptedException | RuntimeException e) {
            Span.current().recordException(e);
            cfg.loggingEventBuilder = logger.atWarn().setCause(e);
            outcome = "error";
        }
        this.fraudDetectionHistogram.record(orderValueDollars, Attributes.of(AttributeKey.stringKey("outcome"), outcome));
        cfg.loggingEventBuilder.log("checkOrder: outcome={}, orderValue={}, shippingCountry={}, customerIpAddress={}, fraudScore={}, msg={}, tenant={}",
                outcome, orderValueDollars, shippingCountry, customerIpAddress, fraudScore, cfg.msg, tenant.getShortCode());
        return outcome;
    }


    @ManagedAttribute
    public int getFraudPercentageOnLargeShoppingCarts() {
        return fraudPercentageOnLargeShoppingCarts;
    }

    @ManagedAttribute
    public void setFraudPercentageOnLargeShoppingCarts(int fraudPercentageOnLargeShoppingCarts) {
        this.fraudPercentageOnLargeShoppingCarts = fraudPercentageOnLargeShoppingCarts;
    }

    @ManagedAttribute
    public int getAverageDurationMillisOnLargeShoppingCart() {
        return averageDurationMillisOnLargeShoppingCart;
    }

    @ManagedAttribute
    public void setAverageDurationMillisOnLargeShoppingCart(int averageDurationMillisOnLargeShoppingCart) {
        this.averageDurationMillisOnLargeShoppingCart = averageDurationMillisOnLargeShoppingCart;
    }

    @ManagedAttribute
    public int getFraudPercentageOnSmallShoppingCarts() {
        return fraudPercentageOnSmallShoppingCarts;
    }

    @ManagedAttribute
    public void setFraudPercentageOnSmallShoppingCarts(int fraudPercentageOnSmallShoppingCarts) {
        this.fraudPercentageOnSmallShoppingCarts = fraudPercentageOnSmallShoppingCarts;
    }

    @ManagedAttribute
    public int getAverageDurationMillisOnSmallShoppingCarts() {
        return averageDurationMillisOnSmallShoppingCarts;
    }

    @ManagedAttribute
    public void setAverageDurationMillisOnSmallShoppingCarts(int averageDurationMillisOnSmallShoppingCarts) {
        this.averageDurationMillisOnSmallShoppingCarts = averageDurationMillisOnSmallShoppingCarts;
    }

    @ManagedAttribute
    public int getValueUpperBoundaryDollarsOnSmallShoppingCart() {
        return valueUpperBoundaryDollarsOnSmallShoppingCart;
    }

    @ManagedAttribute
    public void setValueUpperBoundaryDollarsOnSmallShoppingCart(int valueUpperBoundaryDollarsOnSmallShoppingCart) {
        this.valueUpperBoundaryDollarsOnSmallShoppingCart = valueUpperBoundaryDollarsOnSmallShoppingCart;
    }

    @ManagedAttribute
    public int getFraudPercentageOnMediumShoppingCarts() {
        return fraudPercentageOnMediumShoppingCarts;
    }

    @ManagedAttribute
    public void setFraudPercentageOnMediumShoppingCarts(int fraudPercentageOnMediumShoppingCarts) {
        this.fraudPercentageOnMediumShoppingCarts = fraudPercentageOnMediumShoppingCarts;
    }

    @ManagedAttribute
    public int getAverageDurationMillisOnMediumShoppingCarts() {
        return averageDurationMillisOnMediumShoppingCarts;
    }

    @ManagedAttribute
    public void setAverageDurationMillisOnMediumShoppingCarts(int averageDurationMillisOnMediumShoppingCarts) {
        this.averageDurationMillisOnMediumShoppingCarts = averageDurationMillisOnMediumShoppingCarts;
    }

    @ManagedAttribute
    public int getValueUpperBoundaryDollarsOnMediumShoppingCarts() {
        return valueUpperBoundaryDollarsOnMediumShoppingCarts;
    }

    @ManagedAttribute
    public void setValueUpperBoundaryDollarsOnMediumShoppingCarts(int valueUpperBoundaryDollarsOnMediumShoppingCarts) {
        this.valueUpperBoundaryDollarsOnMediumShoppingCarts = valueUpperBoundaryDollarsOnMediumShoppingCarts;
    }
}
