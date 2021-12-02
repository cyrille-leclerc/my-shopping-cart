import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FrontendMonitor {

    final static Random RANDOM = new Random();

    final static int SLEEP_MAX_DURATION_MILLIS = 1_000;

    enum PaymentMethod {VISA, AMEX, PAYPAL}

    final int priceUpperBoundaryDollarsOnSmallShoppingCart = 10;
    final int priceUpperBoundaryDollarsOnMediumShoppingCarts = 100;

    private class Distribution {

        Map<PaymentMethod, AtomicInteger> largePaymentDistribution = new HashMap();
        Map<PaymentMethod, AtomicInteger> mediumPaymentDistribution = new HashMap();
        Map<PaymentMethod, AtomicInteger> smallPaymentDistribution = new HashMap();

        public void increment(PaymentMethod paymentMethod, double price) {
            Map<PaymentMethod, AtomicInteger> distributionToIncrement;
            if (price < priceUpperBoundaryDollarsOnSmallShoppingCart) {
                distributionToIncrement = smallPaymentDistribution;
            } else if (price < priceUpperBoundaryDollarsOnMediumShoppingCarts) {
                distributionToIncrement = mediumPaymentDistribution;
            } else {
                distributionToIncrement = largePaymentDistribution;
            }

            distributionToIncrement.computeIfAbsent(paymentMethod, method -> new AtomicInteger(0)).incrementAndGet();
        }

        public String toPrettyString() {
            return toPrettyString("small", smallPaymentDistribution) + "\n" +
                    toPrettyString("medium", mediumPaymentDistribution) + "\n" +
                    toPrettyString("large", largePaymentDistribution);
        }

        public String toPrettyString(String label, Map<PaymentMethod, AtomicInteger> distribution) {
            int total = distribution.values().stream().map(atomicInt -> atomicInt.get()).reduce(0, Integer::sum);
            String result = label + "\t: ";
            for (Map.Entry<PaymentMethod, AtomicInteger> entry : distribution.entrySet()) {
                result += entry.getKey().name() + ": " + (entry.getValue().get() * 100 / total) + "%, ";
            }
            result += "total=" + total;
            return result;
        }
    }

    final static PaymentMethod[] EVENLY_DISTRIBUTED_PAYMENT_METHODS = {
            PaymentMethod.PAYPAL,
            PaymentMethod.PAYPAL,
            PaymentMethod.PAYPAL,
            PaymentMethod.PAYPAL,
            PaymentMethod.VISA,
            PaymentMethod.VISA,
            PaymentMethod.VISA,
            PaymentMethod.VISA,
            PaymentMethod.AMEX,
    };
    final static PaymentMethod[] UNEVENLY_DISTRIBUTED_PAYMENT_METHODS = {
            PaymentMethod.AMEX,
            PaymentMethod.AMEX,
            PaymentMethod.AMEX,
            PaymentMethod.AMEX,
            PaymentMethod.AMEX,
            PaymentMethod.PAYPAL,
            PaymentMethod.VISA,
    };

    List<Product> products = Arrays.asList(
            new Product(1L, "TV Set", 300.00),
            new Product(2L, "Game Console", 200.00),
            new Product(3L, "Sofa", 100.00),
            new Product(4L, "Icecream", 5.00),
            new Product(5L, "Beer", 3.00),
            new Product(6L, "Phone", 500.00),
            new Product(7L, "Watch", 30.00),
            new Product(8L, "USB Cable", 4.00),
            new Product(9L, "USB-C Cable", 5.00),
            new Product(10L, "Micro USB Cable", 3.00),
            new Product(11L, "Lightning Cable", 9.00),
            new Product(12L, "USB C adapter", 5.00)
    );


    final List<String> urls;

    final Distribution distribution = new Distribution();

    public FrontendMonitor(List<String> frontBaseUrls) {
        this.urls = frontBaseUrls;
    }

    private PaymentMethod getPaymentMethod(Product product, int quantity) {
        double price = product.price * quantity;
        PaymentMethod result;

        if (price > priceUpperBoundaryDollarsOnMediumShoppingCarts) {
            result = UNEVENLY_DISTRIBUTED_PAYMENT_METHODS[RANDOM.nextInt(UNEVENLY_DISTRIBUTED_PAYMENT_METHODS.length)];
        } else {
            // evenly distributed probability
            result = EVENLY_DISTRIBUTED_PAYMENT_METHODS[RANDOM.nextInt(EVENLY_DISTRIBUTED_PAYMENT_METHODS.length)];
        }
        return result;
    }


    private void placeRandomOrder() {
        int productIdx = RANDOM.nextInt(products.size());
        int quantity = 1 + RANDOM.nextInt(2);
        String frontendBaseUrl = urls.get(RANDOM.nextInt(urls.size()));
        Product product = products.get(productIdx);
        PaymentMethod paymentMethod = getPaymentMethod(product, quantity);
        try {
            Thread.sleep(RANDOM.nextInt(SLEEP_MAX_DURATION_MILLIS));
            getProduct(product, frontendBaseUrl);
            createOrder(quantity, product, paymentMethod, frontendBaseUrl);
        } catch (ConnectException e) {
            StressTestUtils.incrementProgressBarConnectionFailure();
        } catch (Exception e) {
            StressTestUtils.incrementProgressBarFailure();
            System.err.println(e);
        }
    }

    public void createOrder(int quantity, Product product, PaymentMethod paymentMethod, String frontendBaseUrl) throws IOException {
        URL createProductUrl = new URL(frontendBaseUrl + "/api/orders");
        HttpURLConnection createOrderConnection = (HttpURLConnection) createProductUrl.openConnection();
        createOrderConnection.setRequestMethod("POST");
        createOrderConnection.addRequestProperty("Accept", "application/json");
        createOrderConnection.addRequestProperty("Content-type", "application/json");
        createOrderConnection.setDoOutput(true);

        distribution.increment(paymentMethod, product.price * quantity);
        //if (StressTestUtils.isEndOfLine()) {
        //    System.out.println();
        //    System.out.println(distribution.toPrettyString());
        //}

        String createOrderJsonPayload = product.toJson(quantity, paymentMethod.name());
        try (OutputStream os = createOrderConnection.getOutputStream()) {
            byte[] createOrderJsonPayloadAsBytes = createOrderJsonPayload.getBytes("utf-8");
            os.write(createOrderJsonPayloadAsBytes, 0, createOrderJsonPayloadAsBytes.length);
        }

        int statusCode = createOrderConnection.getResponseCode();
        if (statusCode == HttpURLConnection.HTTP_CREATED) {
            StressTestUtils.incrementProgressBarSuccess();
            InputStream responseStream = createOrderConnection.getInputStream();
            FrontendMonitorUtils.toString(responseStream, "utf-8");
            responseStream.close();
        } else {
            StressTestUtils.incrementProgressBarFailure();
        }
    }

    public void getProduct(Product product, String frontendBaseUrl) throws IOException {
        URL getProductUrl = new URL(frontendBaseUrl + "/api/products/" + product.id);
        HttpURLConnection getProductConnection = (HttpURLConnection) getProductUrl.openConnection();
        getProductConnection.addRequestProperty("Accept", "application/json");
        int statusCode = getProductConnection.getResponseCode();
        if (statusCode == HttpURLConnection.HTTP_OK) {
            // success
        } else {
            // failure
        }
        InputStream responseStream = getProductConnection.getInputStream();
        FrontendMonitorUtils.toString(responseStream, "utf-8");
        responseStream.close();
    }


    public static void main(String[] args) throws IOException, InterruptedException {
        String frontEndUrlsAsString = System.getProperty("frontend.urls", "http://localhost:8080");
        List<String> frontendUrls = Stream.of(frontEndUrlsAsString.split(",")).map(String::trim).collect(Collectors.toList());
        System.out.println("Frontend URLS: " + frontendUrls.stream().collect(Collectors.joining(", ")));
        FrontendMonitor frontendMonitor = new FrontendMonitor(frontendUrls);
        int parallelThreads = 20;
        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(parallelThreads);

        for (int i = 0; i < parallelThreads; i++) {
            executorService.scheduleAtFixedRate(() -> frontendMonitor.placeRandomOrder(), RANDOM.nextInt(5_000), 5_000, TimeUnit.MILLISECONDS);
        }
    }

    private static class Product {
        public Product(long id, String name, double price) {
            this.id = id;
            this.name = name;
            this.price = price;
        }

        long id;
        String name;
        double price;

        String toJson(int quantity, String paymentMethod) {
            return "{" +
                    "\"productOrders\":[" +
                    "   {\"product\":{\"id\":" + id + "," +
                    "   \"name\":\"" + name + "\"," +
                    "   \"price\":" + price + "," +
                    "   \"pictureUrl\":\"http://placehold.it/200x100\"}," +
                    "   \"quantity\":" + quantity + "}" +
                    "   ]," +
                    "\"paymentMethod\": \"" + paymentMethod + "\"" +
                    "}";
        }
    }
}