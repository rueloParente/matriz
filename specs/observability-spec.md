# Observability

## Requirements

- **OBS-1:** `observability/docker-compose.yml` starts a standalone Prometheus and
  Grafana stack, with persistent volumes and a provisioned Matriz dashboard.
- **OBS-2:** Prometheus scrapes `homelab-server-1:8085/actuator/prometheus` every
  15 seconds using a dedicated credential supplied through a local secret file.
- **OBS-3:** The application exposes Prometheus metrics only with the
  `observability` Spring profile. Missing/blank monitoring credentials prevent
  startup. Scraping uses stateless HTTP Basic authentication, not browser users
  or sessions. Monitoring credentials cannot authenticate browser routes.
- **OBS-4:** Grafana and Prometheus bind to loopback by default. Grafana requires
  an explicitly supplied admin password; Prometheus has no published host port.
  Remote scraping must use a trusted private network/VPN, or TLS via a proxy;
  HTTP Basic over plaintext is not appropriate on untrusted networks.
- **OBS-5:** The dashboard shows scrape availability, request rate, 5xx error
  rate, mean HTTP latency, JVM heap, process CPU and database connection usage.
  No user identifiers or raw request URLs are added as metric tags.
- **OBS-6:** Existing browser authentication, CSRF and public health behavior
  remain unchanged. Other Actuator endpoints are not accessible to browser users.

## Out of scope

Logs, distributed tracing, alert delivery, host exporters, deployment,
public network exposure and database migrations.
