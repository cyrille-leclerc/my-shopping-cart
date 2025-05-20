


```commandline
kubectl create configmap k6-load-test-webshop-frontend --from-file ./load-generator/src/main/k6/k6.js
```

```commandline
kubectl get configmaps k6-load-test-webshop-frontend -o yaml
```

```commandline
kubectl port-forward service/frontend 8080:8080
```



```commandline
helm repo add grafana https://grafana.github.io/helm-charts &&
  helm repo update &&
  helm upgrade --install --version ^2 --atomic --timeout 300s grafana-k8s-monitoring grafana/k8s-monitoring \
    --namespace "default" --create-namespace --values - <<'EOF'
cluster:
  name: prod-eu-west-1-cluster
destinations:
  - name: grafana-cloud-metrics
    type: prometheus
    url: https://prometheus-prod-01-eu-west-0.grafana.net/api/prom/push
    auth:
      type: basic
      username: "588311"
      password: "****"
  - name: grafana-cloud-logs
    type: loki
    url: https://logs-prod-eu-west-0.grafana.net/loki/api/v1/push
    auth:
      type: basic
      username: "293125"
      password: "****"
  - name: grafana-cloud-traces
    type: otlp
    url: https://tempo-eu-west-0.grafana.net:443
    protocol: grpc
    auth:
      type: basic
      username: "289638"
      password: "****"
    metrics:
      enabled: false
    logs:
      enabled: false
    traces:
      enabled: true
clusterMetrics:
  enabled: true
clusterEvents:
  enabled: true
podLogs:
  enabled: true
applicationObservability:
  enabled: true
  receivers:
    otlp:
      grpc:
        enabled: true
        port: 4317
      http:
        enabled: true
        port: 4318
    zipkin:
      enabled: true
      port: 9411
integrations:
  alloy:
    instances:
      - name: alloy
        labelSelectors:
          app.kubernetes.io/name:
            - alloy-metrics
            - alloy-singleton
            - alloy-logs
            - alloy-receiver
alloy-metrics:
  enabled: true
alloy-singleton:
  enabled: true
alloy-logs:
  enabled: true
alloy-receiver:
  enabled: true
  alloy:
    extraPorts:
      - name: otlp-grpc
        port: 4317
        targetPort: 4317
        protocol: TCP
      - name: otlp-http
        port: 4318
        targetPort: 4318
        protocol: TCP
      - name: zipkin
        port: 9411
        targetPort: 9411
        protocol: TCP
EOF
```