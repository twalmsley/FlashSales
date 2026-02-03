# Monitoring and Metrics (Issue #75)

The Flash Sales application exposes operational and business metrics via Spring Boot Actuator and Micrometer, with a Prometheus-compatible scrape endpoint for dashboards and alerting.

## Scraping Prometheus

- **Endpoint**: `GET /actuator/prometheus`
- **Port**: Management server runs on port **8081** (configurable via `management.server.port`).
- **Security**: The Prometheus endpoint is **public** (no authentication) for internal scraping only; it is not intended to be exposed to the internet.
- **Scrape config example** (Prometheus `prometheus.yml`):

```yaml
scrape_configs:
  - job_name: 'flash'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['localhost:8081']
```

## Metric Tags

All metrics are tagged with `application=flash` (configurable via `management.metrics.tags.application` in `application.yaml`).

## Business and Operational Metrics

| Metric name | Type | Description |
|-------------|------|-------------|
| `flash.orders.created` | Counter | Total orders created (incremented on successful order creation). |
| `flash.payments.success` | Counter | Total successful payments. |
| `flash.payments.failure` | Counter | Total failed payments. |
| `flash.payments.duration` | Timer | Payment processing duration; tag `outcome=success` or `outcome=failure`. |
| `flash.errors` | Counter | API errors handled by `GlobalExceptionHandler`; tags `exception=<simple class name>`, `status=<HTTP status code>`. |
| `flash.rabbitmq.queue.depth` | Gauge | Current message count for order queues; tag `queue=<queue name>` (e.g. `order.processing`, `order.payment.failed`, `order.dispatch`, `order.refund`). |

## Built-in Metrics (Spring Boot / Micrometer)

With `micrometer-registry-prometheus` on the classpath you also get:

- **HTTP**: `http.server.requests` (method, uri, status) for API latency.
- **JVM**: JVM memory, threads, GC, etc.
- **Process**: CPU, file descriptors.
- **Data source / HikariCP**: Connection pool and JDBC metrics when applicable.
- **Cache**: Cache metrics (gets, puts, evictions) when the cache implementation supports them and Spring Boot auto-configures them.

## Recommended Dashboard Panels (Grafana / Prometheus)

- **Order rate**: `rate(flash_orders_created_total[5m])` (orders per second).
- **Payment success rate**: `rate(flash_payments_success_total[5m]) / (rate(flash_payments_success_total[5m]) + rate(flash_payments_failure_total[5m]))`.
- **API latency**: `http_server_requests_seconds` (e.g. p50, p95, p99 by uri).
- **Error rate**: `rate(flash_errors_total[5m])` by `exception` or `status`.
- **Queue depth**: `flash_rabbitmq_queue_depth` by `queue` (alert if backlog grows).
- **Cache**: Use cache metrics for hit ratio and evictions if available.

## Suggested Alert Rules (Prometheus)

- **Payment failure rate**: `rate(flash_payments_failure_total[5m]) / (rate(flash_payments_success_total[5m]) + rate(flash_payments_failure_total[5m])) > 0.1`
- **High error rate**: `rate(flash_errors_total[5m]) > 10`
- **Queue depth**: `flash_rabbitmq_queue_depth > 1000` (per queue, adjust threshold as needed)
- **API latency**: e.g. `histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m])) > 2`

## Implementation Notes

- **Metrics config**: Custom metrics (queue depth, business counters/timers) are registered in `MetricsConfig` and in the services/exception handler. Cache metrics are left to Spring Boot auto-configuration where supported.
- **Security**: `/actuator/prometheus` is public for internal scraping; keep it on an internal network and do not expose it to the internet.
- **Cardinality**: Avoid high-cardinality tags (e.g. order ID, user ID) to prevent metric explosion.
