package com.mycompany.antifraud;

import co.elastic.apm.api.ElasticApm;
import io.micrometer.core.instrument.Metrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@ManagedResource
@RestController()
public class AntiFraudController {

    final static Random RANDOM = new Random();
    final static BigDecimal FIVE_PERCENT = new BigDecimal(5).divide(new BigDecimal(100));

    final Logger logger = LoggerFactory.getLogger(getClass());

    DataSource dataSource;

    int averageDurationMillisOnSmallShoppingCarts = 50;
    int averageDurationMillisOnMediumShoppingCarts = 50;
    int averageDurationMillisOnLargeShoppingCart = 1000;

    int fraudPercentageOnSmallShoppingCarts = 0;
    int fraudPercentageOnMediumShoppingCarts = 0;
    int fraudPercentageOnLargeShoppingCarts = 10;

    int priceUpperBoundaryDollarsOnSmallShoppingCart = 10;
    int priceUpperBoundaryDollarsOnMediumShoppingCarts = 100;

    final AtomicInteger fraudDetectionsCounter = new AtomicInteger();
    final AtomicInteger fraudDetectionsPriceInDollarsCounter = new AtomicInteger();
    final AtomicInteger fraudChecksCounter = new AtomicInteger();
    final AtomicInteger fraudChecksPriceInDollarsCounter = new AtomicInteger();

    @RequestMapping(path = "fraud/checkOrder", method = {RequestMethod.GET, RequestMethod.POST})
    public String checkOrder(
            @RequestParam double orderPrice,
            @RequestParam String shippingCountry,
            @RequestParam String customerIpAddress) {

        String priceRange = getPriceRange(orderPrice);
        ElasticApm.currentSpan().setName("checkOrder");

        ElasticApm.currentSpan().setLabel("orderPrice", orderPrice);
        ElasticApm.currentSpan().setLabel("priceRange", priceRange);
        ElasticApm.currentSpan().setLabel("customerIpAddress", customerIpAddress);
        ElasticApm.currentSpan().setLabel("shippingCountry", shippingCountry);

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
            ElasticApm.currentSpan().setLabel("fraudScore", fraudScore);

            boolean rejected = fraudScore > 0;


            try (Connection cnn = dataSource.getConnection()) {
                try (Statement stmt = cnn.createStatement()) {

                    BigDecimal checkoutDurationInSeconds = new BigDecimal(checkOrderDurationMillis).divide(new BigDecimal(1000));;
                    long nanosBefore = System.nanoTime();
                    String checkoutDurationInSecondsAsString = checkoutDurationInSeconds.toPlainString();
                    stmt.execute("select pg_sleep(0.05)");
                    Thread.sleep(checkOrderDurationMillis);
                    long actualSleepInNanos = System.nanoTime() - nanosBefore;
                    long actualSleepInMillis = TimeUnit.MILLISECONDS.convert(actualSleepInNanos, TimeUnit.NANOSECONDS);

                    long deltaPercents = Math.abs(actualSleepInMillis - checkOrderDurationMillis) * 100 / checkOrderDurationMillis;
                    logger.info("checkOrder(totalPrice: {}, shippingCountry: {}, customerIpAddress: {}): fraudScore: {}, rejected: {}, " +
                                    "expectedSleep: {}ms, actualSleep: {}ms, delta:{}%",
                            new DecimalFormat("000").format(orderPrice), shippingCountry, customerIpAddress, fraudScore, rejected,
                            checkOrderDurationMillis, actualSleepInMillis, deltaPercents);

                }
                // Thread.sleep(checkOrderDurationMillis);
            } catch (SQLException | InterruptedException e) {
                ElasticApm.currentSpan().captureException(e);
                e.printStackTrace();
            }


            Metrics.counter(
                    "antifraud_order_check",
                    "antifraud_order_check_success", Boolean.toString(!rejected),
                    "antifraud_order_check_price_range", priceRange,
                    "antifraud_order_check_shipping_country", shippingCountry).
                    increment();

            String result;
            if (rejected) {
                result = "KO";
                this.fraudDetectionsCounter.incrementAndGet();
                this.fraudDetectionsPriceInDollarsCounter.addAndGet((int) Math.floor(orderPrice));
            } else {
                result = "OK";
            }

            return result;
        } finally {
            this.fraudChecksPriceInDollarsCounter.addAndGet((int) Math.ceil(orderPrice));
            this.fraudChecksCounter.incrementAndGet();
        }
    }

    public String getPriceRange(double price) {
        if (price < 10) {
            return "small";
        } else if (price < 100) {
            return "medium";
        } else {
            return "large";
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

    @Autowired
    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }
}