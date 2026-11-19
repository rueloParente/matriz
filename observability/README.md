# Matriz observability

Standalone **Prometheus + Grafana** metrics stack. Prometheus scrapes
`http://homelab-server-1:8085/actuator/prometheus`; Grafana comes with the
**Matriz → Matriz — application overview** dashboard at `/d/matriz`.
No deployment or infrastructure changes are performed by these files.

## 1. Enable application metrics

Rebuild/redeploy Matriz through your normal approved deployment process with:

```dotenv
SPRING_PROFILES_ACTIVE=observability
METRICS_USERNAME=prometheus
METRICS_PASSWORD=<a-long-random-password>
```

If other Spring profiles are already active, append `observability` to the
comma-separated list. `docker-compose.prd.yml` forwards these variables from its
deployment environment/`.env.prd`. Missing or blank credentials fail startup when
the profile is enabled. Without it, metrics are not exposed.

Only this dedicated credential can scrape metrics. Application users and their
sessions cannot; the monitoring credential cannot log in to the application.
Other Actuator endpoints remain blocked except the existing public health endpoint.

### Private connectivity

The Prometheus container must resolve `homelab-server-1` and reach port `8085`.
Use LAN/VPN DNS; Docker does not inherit custom entries from the host's
`/etc/hosts`. If needed, add an `extra_hosts` entry to Prometheus using the
server's actual private IP.

The production app Compose file intentionally binds to `127.0.0.1` (and defaults
to port 8080). Setting `APP_PORT=8085` alone does **not** make it remotely
reachable. Arrange an approved private reverse proxy/VPN path to that listener,
or an explicitly reviewed private-interface binding/firewall rule. Do not bind
the app publicly just for monitoring.

The default scrape uses HTTP Basic over HTTP and is suitable **only for a
trusted private network or encrypted VPN**. For untrusted networks, configure
HTTPS on a proxy and update Prometheus `scheme`/`tls_config`; do not disable
certificate verification. Keep metrics restricted to monitoring hosts even
when authenticated.

## 2. Configure the monitoring host

From this directory:

```sh
mkdir -p secrets
chmod 700 secrets
```

Using your secret manager/editor, create:

- `secrets/metrics_password`: exactly the same password as `METRICS_PASSWORD`
  on Matriz (plain text, not a hash).
- `.env`: `GRAFANA_ADMIN_PASSWORD=<a-different-long-random-password>`.
  Optional: `GRAFANA_ADMIN_USER` (default `admin`) and `GRAFANA_PORT` (default
  `3000`).

Both locations are Git-ignored. The mounted password must be readable by the
Prometheus container user (UID 65534); for example mode `644` on the file inside
the host's owner-only `secrets/` directory. Local Compose secrets are bind-mounted
files, not an encrypted secret store. Protect `.env` with mode `600`.
Never commit real credentials.

## 3. Start

```sh
docker compose config --quiet
docker compose up -d
```

Open **http://localhost:3000**, log in, and open the Matriz dashboard.
When running on a remote host, keep the loopback binding and use an SSH tunnel:

```sh
ssh -L 3000:127.0.0.1:3000 <monitoring-host>
```

Prometheus is internal to Compose (no published port). Metrics retain 15 days;
both services use named volumes. `docker compose down` preserves data; do not
add `-v` unless you intend to delete it. Grafana's admin environment variables
initialize a new database only; rotate existing passwords through Grafana.

## 4. Verify and troubleshoot

```sh
docker compose ps
docker compose logs --tail=100 prometheus grafana
docker compose exec prometheus promtool check config /etc/prometheus/prometheus.yml
```

In Grafana Explore, query `up{job="matriz"}`: **1** means a successful scrape;
**0** means DNS, connectivity, authentication or endpoint problems. It does not
replace a readiness probe. Wait for two scrapes and generate application traffic
for rate/latency panels; an idle app can show no latency data.

From a host with the same private network access, check the endpoint (curl
prompts for the password so it need not appear in command history):

```sh
curl --fail --user prometheus http://homelab-server-1:8085/actuator/prometheus
```

Expect Prometheus text including `jvm_memory_used_bytes`. Without credentials,
expect HTTP 401, not a login-page redirect. A 403 can mean the profile is not
enabled; connection refusal can mean the app is still bound only to loopback.

Dashboard panels cover HTTP throughput/5xx/mean latency (excluding scrapes),
JVM heap, process CPU and Hikari database connections. These are framework
metrics, not business metrics. No logs, tracing or alert notifications are
configured.
