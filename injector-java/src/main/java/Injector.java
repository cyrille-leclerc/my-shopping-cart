import io.grpc.Context;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.opentelemetry.OpenTelemetry;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.HttpTextFormat;
import io.opentelemetry.exporters.jaeger.JaegerGrpcSpanExporter;
import io.opentelemetry.exporters.logging.LoggingSpanExporter;
import io.opentelemetry.exporters.otlp.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.ResourceConstants;
import io.opentelemetry.sdk.trace.TracerSdkProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpansProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.trace.Span;
import io.opentelemetry.trace.Tracer;
import org.apache.http.HttpRequest;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
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

    // OTel API
    Tracer tracer;
    private static HttpTextFormat.Setter<HttpRequest> setter = (carrier, key, value) -> carrier.setHeader(key, value);

    public Injector() {
        // Get the tracer
        TracerSdkProvider tracerProvider = OpenTelemetrySdk.getTracerProvider();
        // Show that multiple exporters can be used

        LoggingSpanExporter loggingExporter = new LoggingSpanExporter();

        SpanExporter spanExporter = null;
        { // OTLP
            ManagedChannel otlpManagedChannel = ManagedChannelBuilder.forAddress("localhost", 55680).usePlaintext().build();
            SpanExporter otlpSpanExporter = OtlpGrpcSpanExporter.newBuilder().setChannel(otlpManagedChannel).build();
        }

        { // JAEGER
            ManagedChannel jaegerManagedChannel = ManagedChannelBuilder.forAddress("localhost", 14250).usePlaintext().build();
            SpanExporter jaegerSpanExporter = JaegerGrpcSpanExporter.newBuilder().setChannel(jaegerManagedChannel).setServiceName("injector").build();
            spanExporter = jaegerSpanExporter;
        }

        // Set to export the traces also to a log file
        tracerProvider.addSpanProcessor(SimpleSpansProcessor.create(spanExporter) /*MultiSpanExporter.create(Arrays.asList(loggingExporter, spanExporter)))*/);
        tracer = OpenTelemetry.getTracerProvider().get("com.mycompany.opentelemetry", "semver:1.0.0");
    }

    public void post(String url) throws IOException, InterruptedException {
        try (CloseableHttpClient client = HttpClients.createDefault()) {

            for (int i = 0; i < 1 /*100_000*/; i++) {
                int productIdx = RANDOM.nextInt(this.products.size());
                int quantity = 1 + RANDOM.nextInt(2);
                Product product = this.products.get(productIdx);

                // GET PRODUCT

                // Name convention for the Span is not yet defined.
                // See: https://github.com/open-telemetry/opentelemetry-specification/issues/270
                Span getProductSpan = tracer.spanBuilder("get_product").setSpanKind(Span.Kind.CLIENT).startSpan();
                try (Scope scope = tracer.withSpan(getProductSpan)) {
                    HttpGet getProductRequest = new HttpGet(url + "/api/products/" + product.id);
                    getProductRequest.setHeader("Accept", "application/json");
                    // OPEN TELEMETRY
                    // TODO provide semantic convention attributes to Span.Builder
                    getProductSpan.setAttribute("component", "http");
                    getProductSpan.setAttribute("http.method", "GET");
                    getProductSpan.setAttribute("http.url", url.toString());
                    getProductSpan.setAttribute(ResourceConstants.SERVICE_NAME, "injector");
                    getProductSpan.setAttribute(ResourceConstants.SERVICE_NAMESPACE, "com-shoppingcart");
                    getProductSpan.setAttribute(ResourceConstants.SERVICE_VERSION, "1.0-SNAPSHOT");
                    OpenTelemetry.getPropagators().getHttpTextFormat().inject(Context.current(), getProductRequest, setter);


                    try (CloseableHttpResponse response = client.execute(getProductRequest)) {
                        int statusCode = response.getStatusLine().getStatusCode();
                        if (statusCode == HttpStatus.SC_OK) {
                            // success
                        } else {
                            // failure
                        }
                    }
                } finally {
                    getProductSpan.end();
                }
                Span createOrderSpan = tracer.spanBuilder("create_order").setSpanKind(Span.Kind.CLIENT).startSpan();
                try (Scope scope = tracer.withSpan(createOrderSpan)) {
                    // CREATE ORDER
                    HttpPost createOrderRequest = new HttpPost(url + "/api/orders");
                    createOrderRequest.setHeader("Accept", "application/json");
                    createOrderRequest.setHeader("Content-type", "application/json");
                    // OPEN TELEMETRY
                    // TODO provide semantic convention attributes to Span.Builder
                    createOrderSpan.setAttribute("component", "http");
                    createOrderSpan.setAttribute("http.method", "GET");
                    createOrderSpan.setAttribute("http.url", url.toString());
                    createOrderSpan.setAttribute(ResourceConstants.SERVICE_NAME, "injector");
                    createOrderSpan.setAttribute(ResourceConstants.SERVICE_NAMESPACE, "com-shoppingcart");
                    createOrderSpan.setAttribute(ResourceConstants.SERVICE_VERSION, "1.0-SNAPSHOT");
                    OpenTelemetry.getPropagators().getHttpTextFormat().inject(Context.current(), createOrderRequest, setter);

                    String createOrderJsonPayload = product.toJson(quantity);
                    createOrderRequest.setEntity(new StringEntity(createOrderJsonPayload));


                    try (CloseableHttpResponse response = client.execute(createOrderRequest)) {
                        int statusCode = response.getStatusLine().getStatusCode();
                        if (statusCode == HttpStatus.SC_CREATED) {
                            StressTestUtils.incrementProgressBarSuccess();
                        } else {
                            StressTestUtils.incrementProgressBarFailure();
                        }
                    }
                } finally {
                    createOrderSpan.end();
                }
                Thread.sleep(RANDOM.nextInt(250));
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
