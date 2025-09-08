Run command

```
helm upgrade  --install opentelemetry-operator open-telemetry/opentelemetry-operator -f values.yaml
```

Pre requisite

```yaml
# see kubernetes/grafana-cloud-endpoint-config.yaml.template
---
apiVersion: v1
kind: Secret
metadata:
  name: grafana-cloud-auth
type: opaque
stringData:
---
apiVersion: v1
kind: ConfigMap
metadata:
  name: grafana-cloud-config
data:
  GRAFANA_CLOUD_INSTANCE_ID: "123456"
  GRAFANA_CLOUD_OTLP_ENDPOINT: "https://otlp-gateway-prod-xx-xxxx-i.grafana.net/otlp"
  GRAFANA_CLOUD_API_KEY: "glc_***"

```