
# Application logs (SDK emitted logs emitted through OTLP)

## String template style log message

```java
logger.info("checkOrder: outcome={}, orderValue={}, shippingCountry={}, customerIpAddress={}, fraudScore={}, msg={}, tenant={}, paymentMethod={}",
            outcome, orderValueDollars, shippingCountry, customerIpAddress, fraudScore, msg, tenant.getShortCode(), paymentMethod);
```

```
checkOrder: outcome=approved, orderValue=4, shippingCountry=DE, customerIpAddress=127.0.0.1, fraudScore=-51, msg=ok, tenant=DE, paymentMethod=PAYPAL
```

```json
{
  "@timestamp": "2026-06-24T20:29:20.521Z",
  "container": {
    "id": "61b83fb6751e6f696998df2cf91d9ce849bcfad6215eb558988771d05173f3bf",
    "image": {
      "name": "ghcr.io/cyrille-leclerc/webshop-fraud-detection",
      "tag": "latest"
    }
  },
  "deployment": {
    "environment": {
      "name": "production"
    }
  },
  "host": {
    "arch": "aarch64",
    "name": "fraud-detection-6785bb5f85-hhgzt"
  },
  "hostname": "lima-rancher-desktop-my_k8s_cluster",
  "k8s": {
    "cluster": {
      "name": "my_k8s_cluster",
      "uid": "d4bdea09-4674-4085-b7ad-ea0897c69e02"
    },
    "container": {
      "name": "fraud-detection"
    },
    "deployment": {
      "name": "fraud-detection",
      "uid": "a36d3064-d65c-4a29-90fe-0bf3f48a75ba"
    },
    "namespace": {
      "name": "default"
    },
    "node": {
      "ip": "192.168.5.15",
      "name": "lima-rancher-desktop",
      "uid": "e6fd46dd-64c9-404e-885c-38f7286bfdf6"
    },
    "pod": {
      "ip": "10.42.0.90",
      "name": "fraud-detection-6785bb5f85-hhgzt",
      "start_time": "2026-06-24T15:44:08Z",
      "uid": "bd9e7035-5b47-4104-91f5-b49be00eab62"
    },
    "replicaset": {
      "name": "fraud-detection-6785bb5f85",
      "uid": "f992aa05-73f3-4659-9a02-a46039f82c45"
    }
  },
  "os": {
    "description": "Linux 6.6.137-0-virt",
    "type": "linux",
    "version": "6.6.137-0-virt"
  },
  "otel": {
    "severity_number": "9",
    "severity_text": "INFO",
    "span_id": "7bf62bd2554b603c",
    "timestamp": "1782332960521815843",
    "trace_id": "2348c3fa1207a821186d9ad1e5fcb43a"
  },
  "process": {
    "command_args": "[\"/usr/lib/jvm/java-25-amazon-corretto.aarch64/bin/java\",\"-jar\",\"./app.jar\"]",
    "executable": {
      "path": "/usr/lib/jvm/java-25-amazon-corretto.aarch64/bin/java"
    },
    "pid": "1",
    "runtime": {
      "description": "Amazon.com Inc. OpenJDK 64-Bit Server VM 25.0.3+9-LTS",
      "name": "OpenJDK Runtime Environment",
      "version": "25.0.3+9-LTS"
    }
  },
  "service": {
    "instance": {
      "id": "default.fraud-detection-6785bb5f85-hhgzt.fraud-detection"
    },
    "name": "fraud-detection",
    "namespace": "ecommerce",
    "version": "latest"
  },
  "status": "INFO",
  "telemetry": {
    "distro": {
      "name": "opentelemetry-java-instrumentation",
      "version": "2.28.1"
    },
    "sdk": {
      "language": "java",
      "name": "opentelemetry",
      "version": "1.62.0"
    }
  }
}
```

## Logs with exceptions

```java
logger.atWarn()
        .addKeyValue("customerId", placeOrderRequest.getName())
        .setCause(new RuntimeException("random failure"))
        .log("internal-error");
```

```json
{
  "@timestamp": "2026-06-24T20:38:22.013Z",
  "container": {
    "id": "3964d2941728b4d8f3712de47b9567134f7c6e4d40ff453ce793db3f0710299b",
    "image": {
      "name": "ghcr.io/cyrille-leclerc/webshop-checkout",
      "tag": "latest"
    }
  },
  "customerId": "customer-37",
  "deployment": {
    "environment": {
      "name": "production"
    }
  },
  "exception": {
    "message": "random failure",
    "stacktrace": "java.lang.RuntimeException: random failure\n\tat com.mycompany.checkout.CheckoutServiceImpl.placeOrder(CheckoutServiceImpl.java:49)\n\tat com.mycompany.checkout.CheckoutServiceGrpc$MethodHandlers.invoke(CheckoutServiceGrpc.java:246)\n\tat io.grpc.stub.ServerCalls$UnaryServerCallHandler$UnaryServerCallListener.onHalfClose(ServerCalls.java:182)\n\tat io.grpc.PartialForwardingServerCallListener.onHalfClose(PartialForwardingServerCallListener.java:35)\n\tat io.grpc.ForwardingServerCallListener.onHalfClose(ForwardingServerCallListener.java:23)\n\tat io.grpc.ForwardingServerCallListener$SimpleForwardingServerCallListener.onHalfClose(ForwardingServerCallListener.java:40)\n\tat io.grpc.Contexts$ContextualizedServerCallListener.onHalfClose(Contexts.java:86)\n\tat io.opentelemetry.javaagent.shaded.instrumentation.grpc.v1_6.TracingServerInterceptor$TracingServerCall$TracingServerCallListener.onHalfClose(TracingServerInterceptor.java:183)\n\tat io.grpc.internal.ServerCallImpl$ServerStreamListenerImpl.halfClosed(ServerCallImpl.java:356)\n\tat io.grpc.internal.ServerImpl$JumpToApplicationThreadServerStreamListener$1HalfClosed.runInContext(ServerImpl.java:861)\n\tat io.grpc.internal.ContextRunnable.run(ContextRunnable.java:37)\n\tat io.grpc.internal.SerializingExecutor.run(SerializingExecutor.java:133)\n\tat java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1090)\n\tat java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:614)\n\tat java.base/java.lang.Thread.run(Thread.java:1474)\n",
    "type": "java.lang.RuntimeException"
  },
  "host": {
    "arch": "aarch64",
    "name": "checkout-bcbb7fbfc-sgpng"
  },
  "hostname": "lima-rancher-desktop-my_k8s_cluster",
  "k8s": {
    "cluster": {
      "name": "my_k8s_cluster",
      "uid": "d4bdea09-4674-4085-b7ad-ea0897c69e02"
    },
    "container": {
      "name": "checkout"
    },
    "deployment": {
      "name": "checkout",
      "uid": "755324ef-4c90-45a2-a0b1-d3a41fc7d600"
    },
    "namespace": {
      "name": "default"
    },
    "node": {
      "ip": "192.168.5.15",
      "name": "lima-rancher-desktop",
      "uid": "e6fd46dd-64c9-404e-885c-38f7286bfdf6"
    },
    "pod": {
      "ip": "10.42.0.90",
      "name": "checkout-bcbb7fbfc-sgpng",
      "start_time": "2026-06-24T15:44:08Z",
      "uid": "651980c1-577f-4c67-bed3-b490d5f0dde3"
    },
    "replicaset": {
      "name": "checkout-bcbb7fbfc",
      "uid": "1aed8460-3b26-486f-89f0-7a64fc783f30"
    }
  },
  "os": {
    "description": "Linux 6.6.137-0-virt",
    "type": "linux",
    "version": "6.6.137-0-virt"
  },
  "otel": {
    "severity_number": "13",
    "severity_text": "WARN",
    "span_id": "8248ce4f0152cd78",
    "timestamp": "1782333502013674227",
    "trace_id": "62a09428c91e32a9c123bfd1c8d7980c"
  },
  "process": {
    "command_args": "[\"/usr/lib/jvm/java-25-amazon-corretto.aarch64/bin/java\",\"-jar\",\"./app.jar\"]",
    "executable": {
      "path": "/usr/lib/jvm/java-25-amazon-corretto.aarch64/bin/java"
    },
    "pid": "1",
    "runtime": {
      "description": "Amazon.com Inc. OpenJDK 64-Bit Server VM 25.0.3+9-LTS",
      "name": "OpenJDK Runtime Environment",
      "version": "25.0.3+9-LTS"
    }
  },
  "service": {
    "instance": {
      "id": "default.checkout-bcbb7fbfc-sgpng.checkout"
    },
    "name": "checkout",
    "namespace": "ecommerce",
    "version": "latest"
  },
  "status": "WARN",
  "telemetry": {
    "distro": {
      "name": "opentelemetry-java-instrumentation",
      "version": "2.28.1"
    },
    "sdk": {
      "language": "java",
      "name": "opentelemetry",
      "version": "1.62.0"
    }
  }
}
```

## Structured logging

```java
logger.atInfo()
    .addKeyValue("orderId", order.getId())
    .addKeyValue("customerId", customerId)
    .addKeyValue("price", orderValue)
    .addKeyValue("paymentMethod", paymentMethod)
    .addKeyValue("shippingMethod", shippingMethod)
    .addKeyValue("shippingCountry", shippingCountry)
    .log("Success placeOrder (structured logging example)");
```


```
Success placeOrder
```

```json
{
  "@timestamp": "2026-06-24T20:34:37.684Z",
  "container": {
    "id": "d36ef1852fedeb7dbc93b2a57da7c0bcbed72641b3b5829cd66f753abff5f452",
    "image": {
      "name": "ghcr.io/cyrille-leclerc/webshop-frontend",
      "tag": "latest"
    }
  },
  "customerId": "customer-24",
  "deployment": {
    "environment": {
      "name": "production"
    }
  },
  "host": {
    "arch": "aarch64",
    "name": "frontend-79f6f4cfd6-cdk7r"
  },
  "hostname": "lima-rancher-desktop-my_k8s_cluster",
  "k8s": {
    "cluster": {
      "name": "my_k8s_cluster",
      "uid": "d4bdea09-4674-4085-b7ad-ea0897c69e02"
    },
    "container": {
      "name": "frontend"
    },
    "deployment": {
      "name": "frontend",
      "uid": "b6d97985-aa16-458d-9b90-6b89e1a6d121"
    },
    "namespace": {
      "name": "default"
    },
    "node": {
      "ip": "192.168.5.15",
      "name": "lima-rancher-desktop",
      "uid": "e6fd46dd-64c9-404e-885c-38f7286bfdf6"
    },
    "pod": {
      "ip": "10.42.0.90",
      "name": "frontend-79f6f4cfd6-cdk7r",
      "start_time": "2026-06-24T15:44:08Z",
      "uid": "5739cd87-d09b-431d-955a-da7e6892a52e"
    },
    "replicaset": {
      "name": "frontend-79f6f4cfd6",
      "uid": "6dcfb626-42ba-4076-83fe-b86fdc2078a1"
    }
  },
  "orderId": 7629,
  "os": {
    "description": "Linux 6.6.137-0-virt",
    "type": "linux",
    "version": "6.6.137-0-virt"
  },
  "otel": {
    "severity_number": "9",
    "severity_text": "INFO",
    "span_id": "e3461e03fd67dbc4",
    "timestamp": "1782333277684473370",
    "trace_id": "755339fc770825baaa59837c6ecc0fd5"
  },
  "paymentMethod": "VISA",
  "price": 300,
  "process": {
    "command_args": "[\"/usr/lib/jvm/java-25-amazon-corretto.aarch64/bin/java\",\"-jar\",\"./app.jar\"]",
    "executable": {
      "path": "/usr/lib/jvm/java-25-amazon-corretto.aarch64/bin/java"
    },
    "pid": "1",
    "runtime": {
      "description": "Amazon.com Inc. OpenJDK 64-Bit Server VM 25.0.3+9-LTS",
      "name": "OpenJDK Runtime Environment",
      "version": "25.0.3+9-LTS"
    }
  },
  "service": {
    "instance": {
      "id": "default.frontend-79f6f4cfd6-cdk7r.frontend"
    },
    "name": "frontend",
    "namespace": "ecommerce",
    "version": "latest"
  },
  "shippingCountry": "US",
  "shippingMethod": "express",
  "status": "INFO",
  "telemetry": {
    "distro": {
      "name": "opentelemetry-java-instrumentation",
      "version": "2.28.1"
    },
    "sdk": {
      "language": "java",
      "name": "opentelemetry",
      "version": "1.62.0"
    }
  }
}
```

# OTel Collector Logs
## Filelog receiver

```yaml
receivers:
  filelog:
    exclude: []
    include:
      - /var/log/pods/*/*/*.log
    include_file_name: false
    include_file_path: true
    operators:
      - id: container-parser
        max_log_size: 102400
        type: container
    retry_on_failure:
      enabled: true
    start_at: end
```

```
2026-06-24T21:11:51.873Z  INFO 1 --- [frontend] [nio-8080-exec-2] c.m.e.controller.OrderController         : Success placeOrder (structured logging example)
```

```json
{
  "@timestamp": "2026-06-24T21:11:51.874Z",
  "container": {
    "image": {
      "name": "ghcr.io/cyrille-leclerc/webshop-frontend",
      "tag": "latest"
    }
  },
  "host": {
    "name": "lima-rancher-desktop"
  },
  "hostname": "lima-rancher-desktop-my_k8s_cluster",
  "k8s": {
    "cluster": {
      "name": "my_k8s_cluster",
      "uid": "d4bdea09-4674-4085-b7ad-ea0897c69e02"
    },
    "container": {
      "name": "frontend",
      "restart_count": "0"
    },
    "deployment": {
      "name": "frontend"
    },
    "namespace": {
      "name": "default"
    },
    "node": {
      "ip": "192.168.5.15",
      "name": "lima-rancher-desktop",
      "uid": "e6fd46dd-64c9-404e-885c-38f7286bfdf6"
    },
    "pod": {
      "ip": "10.42.0.116",
      "name": "frontend-79f6f4cfd6-cdk7r",
      "start_time": "2026-06-24T15:44:08Z",
      "uid": "5739cd87-d09b-431d-955a-da7e6892a52e"
    },
    "replicaset": {
      "name": "frontend-79f6f4cfd6",
      "uid": "6dcfb626-42ba-4076-83fe-b86fdc2078a1"
    }
  },
  "log": {
    "file": {
      "path": "/var/log/pods/default_frontend-79f6f4cfd6-cdk7r_5739cd87-d09b-431d-955a-da7e6892a52e/frontend/0.log"
    },
    "iostream": "stdout"
  },
  "otel": {
    "timestamp": "1782335511874056935"
  },
  "service": {
    "instance": {
      "id": "default.frontend-79f6f4cfd6-cdk7r.frontend"
    },
    "name": "frontend",
    "namespace": "ecommerce",
    "version": "latest"
  },
  "status": ""
}
```

## Internal logs

```
Started collecting
```

```json
{
  "mode": "pull",
  "gvr": "apps/v1, Resource=statefulsets",
  "code": {
    "file": {
      "path": "github.com/open-telemetry/opentelemetry-collector-contrib/internal/k8sinventory@v0.154.0/pull/observer.go"
    },
    "line": {
      "number": 45
    },
    "function": {
      "name": "github.com/open-telemetry/opentelemetry-collector-contrib/internal/k8sinventory/pull.(*Observer).Start"
    }
  },
  "k8s": {
    "cluster": {
      "name": "my_k8s_cluster"
    },
    "node": {
      "ip": "192.168.5.15",
      "name": "lima-rancher-desktop"
    },
    "pod": {
      "ip": "10.42.0.115",
      "name": "opentelemetry-stack-daemon-collector-brqz6"
    },
    "namespace": {
      "name": "opentelemetry-operator-system"
    }
  },
  "otel": {
    "library": {
      "name": "go.opentelemetry.io/collector/service"
    },
    "service": {
      "instance": {
        "id": "4813b683-315a-47fb-8611-059ce7d42e5a"
      },
      "name": "otelcol-contrib",
      "version": "0.154.0"
    },
    "severity_text": "info",
    "severity_number": 9,
    "user_agent": "OTel Go OTLP over HTTP/protobuf logs exporter/0.14.0",
    "timestamp": "1782333897605412499"
  },
  "namespaces": "",
  "deployment": {
    "environment": {
      "name": "production"
    }
  }
}
```