package com.mycompany.antifraud;

import com.google.common.math.IntMath;
import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.metrics.DoubleCounter;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.text.DecimalFormat;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

@ManagedResource
@RestController()
public class FraudDetectionController {

    final static Random RANDOM = new Random();
    final static BigDecimal FIVE_PERCENT = new BigDecimal(5).divide(new BigDecimal(100), RoundingMode.HALF_UP);

    final Logger logger = LoggerFactory.getLogger(getClass());

    int exceptionPercentage = 20;

    int averageDurationMillisOnSmallShoppingCarts = 50;
    int averageDurationMillisOnMediumShoppingCarts = 50;
    int averageDurationMillisOnLargeShoppingCart = 1000;

    int fraudPercentageOnSmallShoppingCarts = 0;
    int fraudPercentageOnMediumShoppingCarts = 0;
    int fraudPercentageOnLargeShoppingCarts = 20;

    int priceUpperBoundaryDollarsOnSmallShoppingCart = 10;
    int priceUpperBoundaryDollarsOnMediumShoppingCarts = 100;

    final AtomicInteger fraudDetectionsCounter = new AtomicInteger();
    final AtomicInteger fraudDetectionsPriceInDollarsCounter = new AtomicInteger();
    final AtomicInteger fraudChecksCounter = new AtomicInteger();
    final AtomicInteger fraudChecksPriceInDollarsCounter = new AtomicInteger();

    final DoubleHistogram approvedOrderRecorder;
    final DoubleCounter approvedOrderValueCounter;
    final LongCounter approvedOrderCountCounter;
    final DoubleHistogram rejectedOrderRecorder;
    final DoubleCounter rejectedOrderValueCounter;
    final LongCounter rejectedOrderCountCounter;

    final DataSource dataSource;


    public FraudDetectionController(Meter meter, DataSource dataSource) {
        this.approvedOrderRecorder = meter.histogramBuilder("antifraud.approved.order").build();
        this.approvedOrderValueCounter = meter.counterBuilder("antifraud.approved.order_value").ofDoubles().build();
        this.approvedOrderCountCounter = meter.counterBuilder("antifraud.approved.order_count").build();

        this.rejectedOrderRecorder = meter.histogramBuilder("antifraud.rejected.order").build();
        this.rejectedOrderValueCounter = meter.counterBuilder("antifraud.rejected.order_value").ofDoubles().build();
        this.rejectedOrderCountCounter = meter.counterBuilder("antifraud.rejected.order_count").build();

        this.dataSource = dataSource;
    }

    @RequestMapping(path = "fraud/checkOrder", method = {RequestMethod.GET, RequestMethod.POST})
    public String checkOrder(
            @RequestParam double orderPrice,
            @RequestParam String shippingCountry,
            @RequestParam String customerIpAddress) {

        // for Mohan
        try (var baggageScope = Baggage.builder()
                .put("my.baggage.attribute", "I'm a teapot")
                .build()
                .storeInContext(Context.current()).makeCurrent()) {
            logger.warn("this log message should be enriched with a baggage[my.baggage.attribute]");
        }

        Span.current().setAttribute("order_price", orderPrice);
        Span.current().setAttribute("customer_ip_address", customerIpAddress);
        Span.current().setAttribute("shipping_country", shippingCountry);

        try {
            int durationOffsetInMillis;
            int fraudPercentage;
            if (orderPrice < priceUpperBoundaryDollarsOnSmallShoppingCart) {
                fraudPercentage = fraudPercentageOnSmallShoppingCarts;
                durationOffsetInMillis = averageDurationMillisOnSmallShoppingCarts;
            } else if (orderPrice < priceUpperBoundaryDollarsOnMediumShoppingCarts) {
                fraudPercentage = fraudPercentageOnMediumShoppingCarts;
                durationOffsetInMillis = averageDurationMillisOnMediumShoppingCarts;
            } else {
                fraudPercentage = fraudPercentageOnLargeShoppingCarts;
                durationOffsetInMillis = averageDurationMillisOnLargeShoppingCart;
            }

            int randomDurationInMillis = Math.max(5, new BigDecimal(durationOffsetInMillis).multiply(FIVE_PERCENT).intValue());
            int checkOrderDurationMillis = durationOffsetInMillis + RANDOM.nextInt(randomDurationInMillis);
            // positive means fraud
            int fraudScore = fraudPercentage - RANDOM.nextInt(100);
            Span.current().setAttribute("fraud_score", fraudScore);

            boolean rejected = fraudScore > 0;

            try (Connection cnn = dataSource.getConnection()) {
                try (Statement stmt = cnn.createStatement()) {

                    stmt.execute("select pg_sleep(0.05)");
                    Thread.sleep(checkOrderDurationMillis);
                }
                if (RANDOM.nextInt(IntMath.divide(100, exceptionPercentage, RoundingMode.CEILING)) == 1) {
                    throw new RuntimeException("Fraud Detection Processing Exception");
                }

                if (rejected) {
                    logger.warn("checkOrder(totalPrice={}, shippingCountry={}, customerIpAddress={}) fraudScore={}, status=REJECTED",
                            new DecimalFormat("000").format(orderPrice), shippingCountry, customerIpAddress, fraudScore);
                } else {
                    logger.info("checkOrder(totalPrice={}, shippingCountry={}, customerIpAddress={}) fraudScore={}, status=ACCEPTED",
                            new DecimalFormat("000").format(orderPrice), shippingCountry, customerIpAddress, fraudScore);
                }
            } catch (SQLException | InterruptedException | RuntimeException e) {
                Span.current().recordException(e);
                rejected = true;
                logger.warn("Exception processing checkOrder(orderPrice={}, shippingCountry={}, customerIpAddress={})", new DecimalFormat("000").format(orderPrice), shippingCountry, customerIpAddress, e);
            }

            String result;
            if (rejected) {
                result = "KO";
                this.fraudDetectionsCounter.incrementAndGet();
                this.fraudDetectionsPriceInDollarsCounter.addAndGet((int) Math.floor(orderPrice));
                this.rejectedOrderRecorder.record(orderPrice);
                this.rejectedOrderValueCounter.add(orderPrice);
                this.rejectedOrderCountCounter.add(1);
            } else {
                result = "OK";
                this.approvedOrderRecorder.record(orderPrice);
                this.approvedOrderValueCounter.add(orderPrice);
                this.approvedOrderCountCounter.add(1);
            }

            return result;
        } finally {
            this.fraudChecksPriceInDollarsCounter.addAndGet((int) Math.ceil(orderPrice));
            this.fraudChecksCounter.incrementAndGet();
        }
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
    public int getPriceUpperBoundaryDollarsOnSmallShoppingCart() {
        return priceUpperBoundaryDollarsOnSmallShoppingCart;
    }

    @ManagedAttribute
    public void setPriceUpperBoundaryDollarsOnSmallShoppingCart(int priceUpperBoundaryDollarsOnSmallShoppingCart) {
        this.priceUpperBoundaryDollarsOnSmallShoppingCart = priceUpperBoundaryDollarsOnSmallShoppingCart;
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
    public int getPriceUpperBoundaryDollarsOnMediumShoppingCarts() {
        return priceUpperBoundaryDollarsOnMediumShoppingCarts;
    }

    @ManagedAttribute
    public void setPriceUpperBoundaryDollarsOnMediumShoppingCarts(int priceUpperBoundaryDollarsOnMediumShoppingCarts) {
        this.priceUpperBoundaryDollarsOnMediumShoppingCarts = priceUpperBoundaryDollarsOnMediumShoppingCarts;
    }

    @ManagedAttribute
    public int getFraudDetectionsCount() {
        return fraudDetectionsCounter.get();
    }

    @ManagedAttribute
    public AtomicInteger getFraudDetectionsPriceSumInDollars() {
        return fraudDetectionsPriceInDollarsCounter;
    }

    @ManagedAttribute
    public int getFraudChecksCount() {
        return fraudChecksCounter.get();
    }

    @ManagedAttribute
    public AtomicInteger getFraudChecksPriceSumInDollars() {
        return fraudChecksPriceInDollarsCounter;
    }

}
