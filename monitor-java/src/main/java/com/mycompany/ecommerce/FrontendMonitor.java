package com.mycompany.ecommerce;

import co.elastic.apm.api.CaptureSpan;
import co.elastic.apm.api.CaptureTransaction;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FrontendMonitor {

    final static Random RANDOM = new Random();

    final static int SLEEP_MAX_DURATION_MILLIS = 250;

    List<Product> products = Arrays.asList(
            new Product(1L, "TV Set", 300.00),
            new Product(2L, "Game Console", 200.00),
            new Product(3L, "Sofa", 100.00),
            new Product(4L, "Icecream", 5.00),
            new Product(5L, "Beer", 3.00),
            new Product(6L, "Phone", 500.00),
            new Product(7L, "Watch", 30.00),
            new Product(8L, "USB Cable", 4.00)

    );

    final List<String> urls;

    public FrontendMonitor(List<String> urls) {
        this.urls = urls;
    }

    public void post() {
        for (int i = 0; i < 100_000; i++) {
            int productIdx = RANDOM.nextInt(this.products.size());
            int quantity = 1 + RANDOM.nextInt(2);
            try {
                Product product = this.products.get(productIdx);
                placeOrder(quantity, product);
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
        }

    }

    @CaptureTransaction("placeOrder")
    public void placeOrder(int quantity, Product product) throws IOException {
        getProduct(product);
        createOrder(quantity, product);
    }

    @CaptureSpan("createOrder")
    public void createOrder(int quantity, Product product) throws IOException {
        URL createProductUrl = new URL(getRandomUrl() + "/api/orders");
        HttpURLConnection createOrderConnection = (HttpURLConnection) createProductUrl.openConnection();
        createOrderConnection.setRequestMethod("POST");
        createOrderConnection.addRequestProperty("Accept", "application/json");
        createOrderConnection.addRequestProperty("Content-type", "application/json");
        createOrderConnection.setDoOutput(true);

        String createOrderJsonPayload = product.toJson(quantity);
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

    @CaptureSpan("getProduct")
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
        FrontendMonitor frontendMonitor = new FrontendMonitor(frontendUrls);
        frontendMonitor.post();

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

        String toJson(int quantity) {
            return "{\"productOrders\":[" +
                    "{\"product\":{\"id\":" + id + "," +
                    "\"name\":\"" + name + "\"," +
                    "\"price\":" + price + "," +
                    "\"pictureUrl\":\"http://placehold.it/200x100\"}," +
                    "\"quantity\":" + quantity + "}" +
                    "]}";
        }
    }
}
