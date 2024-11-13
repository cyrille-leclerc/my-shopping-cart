See 
* https://github.com/eugenp/tutorials/tree/master/spring-boot-angular
* https://www.baeldung.com/spring-angular-ecommerce

# OpenTelemetry Traces

TODO

# OpenTelemetry Metrics

OpenTelemetry metrics are demoed here providing:
* Business KPIs with the `OrderValueRecorder` (and `OrderValueWithTagsRecorder`) on `com.mycompany.ecommerce.controller.OrderController to provide the following commonly adopted ecommerce KPIs:
   * Sales: sum of the completed purchase orders per hour (per hour or per any desired unit of time)
   * Transaction: count of completed purchase orders per hour (per hour or per any desired unit of time)
   * Average Purchase Order Value: average value of the completed purchase orders
   * Note that this is a "per-request" metric
    

# Build

```
 mvn spring-boot:build-image
```