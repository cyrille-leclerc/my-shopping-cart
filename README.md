
# Pre requisites

* A recent Java
* Postgresql with a database `jdbc:postgresql://localhost:5432/test` and a user `test/test`
    * Hibernate will create a bunch of tables in this `test` database. 
    * Configuration can be changed in the `application.properties` config files
* Elastic APM
* `npm`
   * `npm install copyfiles -g`
# Architecture

![](https://github.com/cyrille-leclerc/my-shopping-cart/raw/open-telemetry/docs/images/demo-architecture.png)

## Demoed OpenTelemetry capabilities

## Demoed OpenTelemetry Traces capabilities

TODO

## Demoed OpenTelemetry Metrics capabilities

OpenTelemetry metrics are demoed here providing in the `frontend-java` app:
* Business KPIs with the `OrderValueRecorder` (and `OrderValueWithTagsRecorder`) on `com.mycompany.ecommerce.controller.OrderController to provide the following commonly adopted ecommerce KPIs:
   * Sales: sum of the completed purchase orders per hour (per hour or per any desired unit of time)
   * Transaction: count of completed purchase orders per hour (per hour or per any desired unit of time)
   * Average Purchase Order Value: average value of the completed purchase orders
   * Note that this is a "per-request" metric
* Framework instrumentation with the instrumentation of a Guava cache on `com.mycompany.ecommerce.service.ProductServiceImpl`, implemented in `com.mycompany.ecommerce.OpenTelemetryUtils.observeGoogleGuavaCache`.
   * Note that these are "per-interval" metrics.

# Run the sample

* Install a recent java version: on Mac, see https://github.com/AdoptOpenJDK/homebrew-openjdk

* Install Postgresql

```
brew install postgresql
brew services start postgresql
psql postgre
create database test;
CREATE USER test WITH PASSWORD 'test';

// TODO create role test
GRANT ALL PRIVILEGES ON DATABASE test TO test;

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

 ```
WARNING hardcoded opentelemetry collector executable: otelcontribcol-dc17498, to change the executable path, edit ./run-opentelemetry-collector.sh
+ otelcontribcol-dc17498 --config ./opentelemetry-collector-exporter-elastic.yaml
2020-05-31T16:05:00.734+0200    INFO    service/service.go:382  Starting OpenTelemetry Contrib Collector...     {"Version": "latest", "GitHash": "dc17498", "NumCPU": 8}
2020-05-31T16:05:00.734+0200    INFO    service/service.go:221  Setting up own telemetry...
2020-05-31T16:05:00.735+0200    INFO    service/telemetry.go:94 Serving Prometheus metrics      {"address": "localhost:8888", "legacy_metrics": true, "new_metrics": false, "level": 3, "service.instance.id": ""}
2020-05-31T16:05:00.735+0200    INFO    service/service.go:259  Loading configuration...
2020-05-31T16:05:00.736+0200    INFO    service/service.go:270  Applying configuration...
2020-05-31T16:05:00.736+0200    INFO    service/service.go:291  Starting extensions...
2020-05-31T16:05:00.736+0200    INFO    builder/exporters_builder.go:302        Exporter is enabled.    {"component_kind": "exporter", "exporter": "elastic"}
2020-05-31T16:05:00.736+0200    INFO    builder/exporters_builder.go:302        Exporter is enabled.    {"component_kind": "exporter", "exporter": "logging"}
2020-05-31T16:05:00.736+0200    INFO    builder/exporters_builder.go:302        Exporter is enabled.    {"component_kind": "exporter", "exporter": "jaeger"}
2020-05-31T16:05:00.736+0200    INFO    service/service.go:306  Starting exporters...
2020-05-31T16:05:00.736+0200    INFO    builder/exporters_builder.go:90 Exporter is starting... {"component_kind": "exporter", "component_type": "elastic", "component_name": "elastic"}
2020-05-31T16:05:00.736+0200    INFO    builder/exporters_builder.go:95 Exporter started.       {"component_kind": "exporter", "component_type": "elastic", "component_name": "elastic"}
2020-05-31T16:05:00.736+0200    INFO    builder/exporters_builder.go:90 Exporter is starting... {"component_kind": "exporter", "component_type": "logging", "component_name": "logging"}
2020-05-31T16:05:00.736+0200    INFO    builder/exporters_builder.go:95 Exporter started.       {"component_kind": "exporter", "component_type": "logging", "component_name": "logging"}
2020-05-31T16:05:00.736+0200    INFO    builder/exporters_builder.go:90 Exporter is starting... {"component_kind": "exporter", "component_type": "jaeger", "component_name": "jaeger"}
2020-05-31T16:05:00.736+0200    INFO    builder/exporters_builder.go:95 Exporter started.       {"component_kind": "exporter", "component_type": "jaeger", "component_name": "jaeger"}
2020-05-31T16:05:00.736+0200    INFO    builder/pipelines_builder.go:205        Pipeline is enabled.    {"pipeline_name": "traces", "pipeline_datatype": "traces"}
2020-05-31T16:05:00.736+0200    INFO    service/service.go:319  Starting processors...
2020-05-31T16:05:00.736+0200    INFO    builder/pipelines_builder.go:52 Pipeline is starting... {"pipeline_name": "traces", "pipeline_datatype": "traces"}
2020-05-31T16:05:00.736+0200    INFO    builder/pipelines_builder.go:62 Pipeline is started.    {"pipeline_name": "traces", "pipeline_datatype": "traces"}
2020-05-31T16:05:00.737+0200    INFO    builder/receivers_builder.go:234        Receiver is enabled.    {"component_kind": "receiver", "component_type": "otlp", "component_name": "otlp", "datatype": "traces"}
2020-05-31T16:05:00.737+0200    INFO    service/service.go:331  Starting receivers...
2020-05-31T16:05:00.737+0200    INFO    builder/receivers_builder.go:74 Receiver is starting... {"component_kind": "receiver", "component_type": "otlp", "component_name": "otlp"}
2020-05-31T16:05:00.737+0200    INFO    builder/receivers_builder.go:79 Receiver started.       {"component_kind": "receiver", "component_type": "otlp", "component_name": "otlp"}
2020-05-31T16:05:00.737+0200    INFO    service/service.go:233  Everything is ready. Begin running and processing data.
2020-05-31T16:06:05.331+0200    INFO    loggingexporter/logging_exporter.go:90  TraceExporter   {"#spans": 38}`
...
```

![](https://github.com/cyrille-leclerc/my-shopping-cart/raw/open-telemetry/docs/images/elastic-apm-distributed-trace-opentelemetry.png)
