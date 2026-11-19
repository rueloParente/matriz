# Observability delivery plan

1. **OBS-2, OBS-3, OBS-6:** Add the Prometheus registry, opt-in profile and isolated
   scrape authentication. Test successful/failed scraping and isolation from
   browser authentication.
2. **OBS-1, OBS-4, OBS-5:** Add Compose, Prometheus configuration, Grafana
   provisioning and dashboard; validate Compose and Prometheus configuration.
3. **OBS-1–OBS-6:** Document secrets, private connectivity, startup and checks.
   Run the configured Maven verification suite. Live homelab connectivity is a
   deployment-time check, not a prerequisite for local tests.
