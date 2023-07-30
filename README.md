
# Pre requisites

* A recent Java
* Postgresql with a database `jdbc:postgresql://localhost:5432/test` and a user `test/test`
    * Hibernate will create a bunch of tables in this `test` database. 
    * Configuration can be changed in the `application.properties` config files
* Redis
* RabbitMQ
* A DNS service that maps www.example.com like Google DNS `8.8.8.8`
* K6 installed
* Grafana Cloud
* `npm`
   * `npm install copyfiles -g`
# Architecture

![](https://github.com/cyrille-leclerc/my-shopping-cart/raw/open-telemetry/docs/images/shopping-cart.drawio.svg)

## Demoed OpenTelemetry capabilities

### Traces 

### Metrics

OpenTelemetry metrics are demoed here providing in the `frontend-java` app:
* Business KPIs with the `OrderValueRecorder` (and `OrderValueWithTagsRecorder`) on `com.mycompany.ecommerce.controller.OrderController to provide the following commonly adopted ecommerce KPIs:
   * Sales: sum of the completed purchase orders per hour (per hour or per any desired unit of time)
   * Transaction: count of completed purchase orders per hour (per hour or per any desired unit of time)
   * Average Purchase Order Value: average value of the completed purchase orders
   * Note that this is a "per-request" metric
* Framework instrumentation with the instrumentation of a Guava cache on `com.mycompany.ecommerce.service.ProductServiceImpl`, implemented in `com.mycompany.ecommerce.OpenTelemetryUtils.observeGoogleGuavaCache`.
   * Note that these are "per-interval" metrics.

![](https://github.com/cyrille-leclerc/my-shopping-cart/raw/open-telemetry/docs/images/ecommerce-system-dashboard.png)



### Logs

Auto instrumentation of logs that are seamlessly collected by the Otel agents

# Run the sample

* Install a recent java version: on Mac, see https://github.com/AdoptOpenJDK/homebrew-openjdk

* Install and start Postgresql

```
brew install postgresql
brew services start postgresql
psql postgre
create database my_shopping_cart;
CREATE USER my_shopping_cart WITH PASSWORD 'my_shopping_cart';

// TODO create role test
GRANT ALL PRIVILEGES ON DATABASE my_shopping_cart TO my_shopping_cart;

```

* Install and start Redis
 ```
cd redis/
./run-redis.sh  
```

* Install and start RabbitMQ

TODO

* shell : Fraud Detection service (HTTP endpoint, OTel auto instrumentation)
 
```
 cd fraud-detection/
 ./run-fraud-detection  
 ```

* shell 3: Checkout service (gRPC endpoint, OTel auto instrumentation)

```
 cd checkout/
 ./run-checkout 
 ```

* shell 4: Warehouse service (RabbitMQ consumer, OTel auto instrumentation)

```
 cd warehouse/
 ./run-warehouse 
 ```
* shell 5: Shipping service (HTTP endpoint, Jaeger instrumentation)

```
 cd shipping/
 ./run-shipping 
 ```


* shell 6: Frontend (HTTP endpoint, OTel auto instrumentation)
 
```
 cd fronten/
 ./run-frontend
 ```


* shell 7: Load generator to inject load on the frontend (K6)
 ```
cd load-generator
./run-load-generator  
```

* shell 6: Grafana Agent or OpenTelemetry Collector to send all the data to your Observability backend

# Sample execution


![](https://github.com/cyrille-leclerc/my-shopping-cart/raw/open-telemetry/docs/images/elastic-apm-distributed-trace-opentelemetry.png)

# Simulate caching service outage

The application relies on a cache for better latency. An outage in the cache will cause the application to always access the database rather than use the cached data and cause a steep increase of the user facing latency.
The symptom is the steep increase of the user facing latency. We want "find probable causes" to find the associated steep increase of the rate of log messages of the category `Cache⁕miss⁕for⁕product⁕load⁕from⁕database⁕in⁕`

* Augment PostgreSQL latency to find products by ID to make hte problem more visible: http://localhost:8080/chaos/attack/latency/enable
* Generate load for some time 
* Verify 
   * On Elastic APM that the "/api/orders" transaction very rarely performs a find product by id
   * On Elastic logs stream that the categorisation works
* Make caching access noop on the products, impacting the createOrder operation: http://localhost:8080/chaos/attack/cache/enable
* Verify
   * On the "/api/orders" latency chart, a steep increase from ~600ms to >2,000ms
   * On the log categorization, the steep increase of `Cache⁕miss⁕for⁕product⁕load⁕from⁕database⁕in⁕`
   * Logs
     * In the index `.ds-logs-apm.app-default-*` (data stream `logs-apm.app-default`, index template `logs-apm.app`, data view `logs-*`), count records where `service.name: "frontend" AND message: "cache miss for product"` 
   * Metrics
      * index `metrics-apm.app.*` (data stream `metrics-apm.app.frontend-default`, index template `metrics-apm.app`, data view `metrics-*`), `redis_cache_misses` and `redis_cache_puts` for `service.name: frontend` steeply increasing
      * TODO OpenTelemetry Collector Redis receiver 
![](https://github.com/cyrille-leclerc/my-shopping-cart/raw/open-telemetry/docs/images/find-probable-root-causes-redis-cache.png)
![](https://github.com/cyrille-leclerc/my-shopping-cart/raw/open-telemetry/docs/images/find-probable-root-causes-redis-cache-logs-categorization.png)

