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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FrontendLoadGenerator {

    final static Random RANDOM = new Random();

    final static int SLEEP_MAX_DURATION_MILLIS = 50;

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

    public FrontendLoadGenerator(List<String> urls) {
        this.urls = urls;
    }

    public void post() {
        ExecutorService executorService = Executors.newFixedThreadPool(4);

        for (int i = 0; i < 100_000; i++) {
            int productIdx = RANDOM.nextInt(this.products.size());
            int quantity = 1 + RANDOM.nextInt(2);
            Product product = this.products.get(productIdx);
            PaymentMethod paymentMethod = getPaymentMethod(product, quantity);
            executorService.execute(() -> {
                try {
                    placeOrder(quantity, product, paymentMethod);
                } catch (ConnectException e) {
                    StressTestUtils.incrementProgressBarConnectionFailure();
                } catch (Exception e) {
                    StressTestUtils.incrementProgressBarFailure();
                    System.err.println(e);
                } finally {
                    try {
                        Thread.sleep(RANDOM.nextInt(SLEEP_MAX_DURATION_MILLIS));
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        }

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

    public void placeOrder(int quantity, Product product, PaymentMethod paymentMethod) throws IOException {
        getProduct(product);
        createOrder(quantity, product, paymentMethod);
    }

    public void createOrder(int quantity, Product product, PaymentMethod paymentMethod) throws IOException {
        URL createProductUrl = new URL(getRandomUrl() + "/api/orders");
        HttpURLConnection createOrderConnection = (HttpURLConnection) createProductUrl.openConnection();
        createOrderConnection.setRequestMethod("POST");
        createOrderConnection.addRequestProperty("Accept", "application/json");
        createOrderConnection.addRequestProperty("Content-type", "application/json");
        createOrderConnection.setDoOutput(true);

        distribution.increment(paymentMethod, product.price * quantity);
        if (StressTestUtils.isEndOfLine()) {
            System.out.println();
            System.out.println(distribution.toPrettyString());
        }

        String createOrderJsonPayload = product.toJson(quantity, paymentMethod.name());
        try (OutputStream os = createOrderConnection.getOutputStream()) {
            byte[] createOrderJsonPayloadAsBytes = createOrderJsonPayload.getBytes("utf-8");
            os.write(createOrderJsonPayloadAsBytes, 0, createOrderJsonPayloadAsBytes.length);
        }

        int statusCode = createOrderConnection.getResponseCode();
        if (statusCode == HttpURLConnection.HTTP_CREATED) {
            StressTestUtils.incrementProgressBarSuccess();
            InputStream responseStream = createOrderConnection.getInputStream();
            InjectorUtils.toString(responseStream, "utf-8");
            responseStream.close();
        } else {
            StressTestUtils.incrementProgressBarFailure();
        }
    }

    public void getProduct(Product product) throws IOException {
        URL getProductUrl = new URL(getRandomUrl() + "/api/products/" + product.id);
        HttpURLConnection getProductConnection = (HttpURLConnection) getProductUrl.openConnection();
        getProductConnection.addRequestProperty("Accept", "application/json");
        int statusCode = getProductConnection.getResponseCode();
        if (statusCode == HttpURLConnection.HTTP_OK) {
            // success
        } else {
            // failure
        }
        InputStream responseStream = getProductConnection.getInputStream();
        InjectorUtils.toString(responseStream, "utf-8");
        responseStream.close();
    }


    public static void main(String[] args) throws IOException, InterruptedException {
        String frontEndUrlsAsString = System.getProperty("frontend.urls", "http://localhost:8080");
        List<String> frontendUrls = Stream.of(frontEndUrlsAsString.split(",")).map(String::trim).collect(Collectors.toList());
        System.out.println("Frontend URLS: " + frontendUrls.stream().collect(Collectors.joining(", ")));
        FrontendLoadGenerator frontendLoadGenerator = new FrontendLoadGenerator(frontendUrls);
        frontendLoadGenerator.post();

    }

    public String getRandomUrl() {
        return urls.get(RANDOM.nextInt(urls.size()));
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