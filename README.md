
# Pre requisites

* A recent Java
* Postgresql with a database `jdbc:postgresql://localhost:5432/test` and a user `test/test`
    * Hibernate will create a bunch of tables in this `test` database. 
    * Configuration can be changed in the `application.properties` config files
* Redis
* Elastic APM
* `npm`
   * `npm install copyfiles -g`
# Architecture

![](https://github.com/cyrille-leclerc/my-shopping-cart/raw/open-telemetry/docs/images/demo-architecture.png)

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

Kibana dashboard definition: https://github.com/cyrille-leclerc/my-shopping-cart/blob/open-telemetry/src/main/kibana/ecommerce-dashboard.ndjson


### Logs

Auto instrumentation of logs that are seamlessly collected by the Otel agents

# Run the sample

* Install a recent java version: on Mac, see https://github.com/AdoptOpenJDK/homebrew-openjdk

* Install and start Postgresql

```
brew install postgresql
brew services start postgresql
psql postgre
create database test;
CREATE USER test WITH PASSWORD 'test';

// TODO create role test
GRANT ALL PRIVILEGES ON DATABASE test TO test;

```

* Install and start Redis
 ```
cd redis/
./run-redis.sh  
```

* shell 1: start OpenTelemetry collector
   * Run the collector
 ```
cd opentelemetry-collector/
./run-opentelemetry-collector.sh  
```

* shell 2: Anti Fraud service
 
```
 cd anti-fraud-java/
 ./run-anti-fraud.sh  
 ```

* shell 3: Checkout Service

```
 cd checkout-service/
 ./run-checkout-service.sh 
 ```

* shell 4: Frontend
 
```
 cd fronten-java/
 ./run-frontend.sh  
 ```


* shell 5: Monitor to inject load on the application
 ```
cd monitor-java
./run-monitor.sh  
```


# Sample execution



* shell 1: start OpenTelemetry collector

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

