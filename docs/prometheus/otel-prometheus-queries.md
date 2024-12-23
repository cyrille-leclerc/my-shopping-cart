# Awesome Prometheus Queries for OpenTelemetry Metrics

## HTTP Server RED metrics

* Mostly `http.server.request.duration` - Histogram of the request duration in seconds per HTTP endpoint and response status code identified by the attributes `http.request.method`, `http.route`, and `http.response.status_code`
* Useful visualizations include
   * Percentile of request duration, with a graph of the 50th, 90th, and 99th percentile of the request duration in seconds.

```promql
histogram_quantile(0.90, sum by(le, job, http_request_method, http_route) (rate(http_server_request_duration_seconds_bucket{job="ecommerce/frontend"}[$__rate_interval])))
```

   * Distribution of request duration, with a histogram of the request duration in seconds.

```promql
sum by(le) (increase(http_server_request_duration_seconds_bucket{job="ecommerce/frontend"}[$__rate_interval]))
```

   * Error rate
 
```promql
sum by(job, http_request_method, http_route) (rate(http_server_request_duration_seconds_count{job="ecommerce/frontend", http_response_status_code=~"5.."}[$__rate_interval]))
/
sum by(job, http_request_method, http_route) (rate(http_server_request_duration_seconds_count{job="ecommerce/frontend"}[$__rate_interval])) * 100
```
  * Request rate

```promql
sum by(job, http_request_method) (rate(http_server_request_duration_seconds_count{job="ecommerce/frontend"}[$__rate_interval]))
```
