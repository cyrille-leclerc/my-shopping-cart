package com.mycompany.antifraud;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.incubator.metrics.ExtendedDoubleHistogramBuilder;
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
import java.util.List;
import java.util.Random;

@ManagedResource
@RestController()
public class FraudDetectionController {

    final static Random RANDOM = new Random();
    final static BigDecimal FIVE_PERCENT = new BigDecimal(5).divide(new BigDecimal(100), RoundingMode.HALF_UP);

    final Logger logger = LoggerFactory.getLogger(getClass());

    enum AnomalyType {
        ORDER_VALUE,
        TENANT,
        PAYMENT_METHOD
    }

    AnomalyType anomalyType = AnomalyType.ORDER_VALUE;

    int averageDurationMillisSmall = 50;
    int averageDurationMillisMedium = 50;
    int averageDurationMillisLarge = 1000;

    int fraudPercentageSmall = 0;
    int fraudPercentageMedium = 0;
    int fraudPercentageLarge = 15;

    int exceptionPercentageSmall = 0;
    int exceptionPercentageMedium = 0;
    int exceptionPercentageLarge = 10;

    int valueUpperBoundaryDollarsOnSmallShoppingCart = 10;
    int valueUpperBoundaryDollarsOnMediumShoppingCarts = 100;

    final DoubleHistogram fraudDetectionHistogram;

    final DataSource dataSource;

    public FraudDetectionController(Meter meter, DataSource dataSource) {
        this.fraudDetectionHistogram = ((ExtendedDoubleHistogramBuilder) meter.histogramBuilder("fraud.check_order"))
                .setAttributesAdvice(List.of(FraudDetectionAttributes.OUTCOME, FraudDetectionAttributes.TENANT_SHORTCODE, FraudDetectionAttributes.PAYMENT_METHOD))
                .setUnit("usd").build();
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

    FraudDetectionConfiguration buildFraudDetectionConfiguration(int orderValueDollars, TenantFilter.Tenant tenant, String paymentMethod) {
        FraudDetectionConfiguration cfg = new FraudDetectionConfiguration();
        switch (anomalyType) {
            case TENANT -> {
                if ("FR".equalsIgnoreCase(tenant.getShortCode())) {
                    cfg.fraudPercentage = fraudPercentageLarge;
                    cfg.durationOffsetInMillis = averageDurationMillisLarge;
                    cfg.exceptionPercentage = exceptionPercentageLarge;
                    cfg.loggingEventBuilder = logger.atWarn();
                    cfg.msg = "problem FD-654321 executing fraud detection";
                } else {
                    cfg.fraudPercentage = fraudPercentageSmall;
                    cfg.durationOffsetInMillis = averageDurationMillisSmall;
                    cfg.exceptionPercentage = exceptionPercentageSmall;
                    cfg.loggingEventBuilder = logger.atInfo();
                    cfg.msg = "ok";
                }
            }
            case ORDER_VALUE -> {
                if (orderValueDollars < valueUpperBoundaryDollarsOnSmallShoppingCart) {
                    cfg.fraudPercentage = fraudPercentageSmall;
                    cfg.durationOffsetInMillis = averageDurationMillisSmall;
                    cfg.exceptionPercentage = exceptionPercentageSmall;
                    cfg.loggingEventBuilder = logger.atInfo();
                    cfg.msg = "ok";
                } else if (orderValueDollars < valueUpperBoundaryDollarsOnMediumShoppingCarts) {
                    cfg.fraudPercentage = fraudPercentageMedium;
                    cfg.durationOffsetInMillis = averageDurationMillisMedium;
                    cfg.exceptionPercentage = exceptionPercentageMedium;
                    cfg.loggingEventBuilder = logger.atInfo();
                    cfg.msg = "ok";
                } else {
                    cfg.fraudPercentage = fraudPercentageLarge;
                    cfg.durationOffsetInMillis = averageDurationMillisLarge;
                    cfg.exceptionPercentage = exceptionPercentageLarge;
                    cfg.loggingEventBuilder = logger.atWarn();
                    cfg.msg = "problem FD-123456 executing fraud detection";
                }
            }
            case PAYMENT_METHOD -> {
                if ("AMEX".equals(paymentMethod)) {
                    cfg.fraudPercentage = fraudPercentageLarge;
                    cfg.durationOffsetInMillis = averageDurationMillisLarge;
                    cfg.exceptionPercentage = exceptionPercentageLarge;
                    cfg.loggingEventBuilder = logger.atWarn();
                    cfg.msg = "problem FD-0987654 executing fraud detection";
                } else {
                    cfg.fraudPercentage = fraudPercentageSmall;
                    cfg.durationOffsetInMillis = averageDurationMillisSmall;
                    cfg.exceptionPercentage = exceptionPercentageSmall;
                    cfg.loggingEventBuilder = logger.atInfo();
                    cfg.msg = "ok";
                }
            }
        }
        return cfg;
    }

    @RequestMapping(path = "fraud/checkOrder", method = {RequestMethod.GET, RequestMethod.POST})
    public String checkOrder(
            @RequestParam double orderValue,
            @RequestParam String shippingCountry,
            @RequestParam String customerIpAddress,
            @RequestParam String paymentMethod) {
        TenantFilter.Tenant tenant = TenantFilter.Tenant.current();

        Span.current().setAttribute("order_value", orderValue);
        Span.current().setAttribute("customer_ip_address", customerIpAddress);
        //Span.current().setAttribute("shipping_country", shippingCountry);
        Span.current().setAttribute(FraudDetectionAttributes.PAYMENT_METHOD, paymentMethod);
        Span.current().setAttribute("database_pool", tenant.shortCode);

        int orderValueDollars = Double.valueOf(orderValue).intValue();

        FraudDetectionConfiguration cfg = buildFraudDetectionConfiguration(orderValueDollars, tenant, paymentMethod);

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
        this.fraudDetectionHistogram.record(
                orderValueDollars,
                Attributes.of(
                        FraudDetectionAttributes.OUTCOME, outcome,
                        FraudDetectionAttributes.TENANT_SHORTCODE, tenant.getShortCode(),
                        FraudDetectionAttributes.PAYMENT_METHOD, paymentMethod));
        cfg.loggingEventBuilder.log("checkOrder: outcome={}, orderValue={}, shippingCountry={}, customerIpAddress={}, fraudScore={}, msg={}, tenant={}, paymentMethod={}",
                outcome, orderValueDollars, shippingCountry, customerIpAddress, fraudScore, cfg.msg, tenant.getShortCode(), paymentMethod);

        Span.current().setAttribute("outcome", outcome);

        return outcome;
    }


    @ManagedAttribute
    public int getFraudPercentageLarge() {
        return fraudPercentageLarge;
    }

    @ManagedAttribute
    public void setFraudPercentageLarge(int fraudPercentageLarge) {
        this.fraudPercentageLarge = fraudPercentageLarge;
    }

    @ManagedAttribute
    public int getAverageDurationMillisLarge() {
        return averageDurationMillisLarge;
    }

    @ManagedAttribute
    public void setAverageDurationMillisLarge(int averageDurationMillisLarge) {
        this.averageDurationMillisLarge = averageDurationMillisLarge;
    }

    @ManagedAttribute
    public int getFraudPercentageSmall() {
        return fraudPercentageSmall;
    }

    @ManagedAttribute
    public void setFraudPercentageSmall(int fraudPercentageSmall) {
        this.fraudPercentageSmall = fraudPercentageSmall;
    }

    @ManagedAttribute
    public int getAverageDurationMillisSmall() {
        return averageDurationMillisSmall;
    }

    @ManagedAttribute
    public void setAverageDurationMillisSmall(int averageDurationMillisSmall) {
        this.averageDurationMillisSmall = averageDurationMillisSmall;
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
    public int getFraudPercentageMedium() {
        return fraudPercentageMedium;
    }

    @ManagedAttribute
    public void setFraudPercentageMedium(int fraudPercentageMedium) {
        this.fraudPercentageMedium = fraudPercentageMedium;
    }

    @ManagedAttribute
    public int getAverageDurationMillisMedium() {
        return averageDurationMillisMedium;
    }

    @ManagedAttribute
    public void setAverageDurationMillisMedium(int averageDurationMillisMedium) {
        this.averageDurationMillisMedium = averageDurationMillisMedium;
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
