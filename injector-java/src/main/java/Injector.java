

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class Injector {

    final static Random RANDOM = new Random();

    List<Product> products = Arrays.asList(
            new Product(1L, "TV Set", 300.00),
            new Product(2L, "Game Console", 200.00),
            new Product(3L, "Sofa", 100.00),
            new Product(4L, "Icecream", 5.00),
            new Product(5L, "Beer", 3.00),
            new Product(6L, "Phone", 500.00),
            new Product(7L, "Watch", 30.00)
    );


    public void post(String url) throws IOException, InterruptedException {
        try (CloseableHttpClient client = HttpClients.createDefault()) {

            for (int i = 0; i < 100_000; i++) {
                int productIdx = RANDOM.nextInt(this.products.size());
                int quantity = 1 + RANDOM.nextInt(2);

                String json = this.products.get(productIdx).toJson(quantity);

                HttpPost httpPost = new HttpPost(url + "/api/orders");
                StringEntity entity = new StringEntity(json);
                httpPost.setEntity(entity);
                httpPost.setHeader("Accept", "application/json");
                httpPost.setHeader("Content-type", "application/json");

                try (CloseableHttpResponse response = client.execute(httpPost)) {
                    int statusCode = response.getStatusLine().getStatusCode();
                    if (statusCode == HttpStatus.SC_CREATED) {
                        StressTestUtils.incrementProgressBarSuccess();
                    } else {
                        StressTestUtils.incrementProgressBarFailure();
                    }
                }

                Thread.sleep(300);
            }
        }
    }


    public static void main(String[] args) throws IOException, InterruptedException {
        Injector injector = new Injector();
        injector.post("http://localhost:8080");
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
