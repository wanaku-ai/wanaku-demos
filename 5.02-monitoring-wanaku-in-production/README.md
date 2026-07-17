---
title: "Monitoring Wanaku in Production"
description: "Set up observability for a Wanaku deployment: Prometheus metrics, health checks, and centralized logging."
---

# Monitoring Wanaku in Production

Production deployments of Wanaku need more than a running pod — you need to know whether the router is healthy, how fast tools respond, and whether capability services are doing their jobs. This guide shows you how to wire up Prometheus scraping, configure Kubernetes health probes, tune log levels, and build a single Grafana dashboard for the entire Wanaku stack on OpenShift.

## What You Will Learn

- How to expose Wanaku's built-in `/q/metrics` endpoint with a Prometheus `ServiceMonitor`
- How Kubernetes liveness and readiness probes work against Wanaku's `/q/health/*` endpoints
- How to configure log levels for the router and capability services via Quarkus properties
- How to read and filter router and capability logs using `oc`
- How to assemble a basic but functional Grafana dashboard from the metrics Wanaku already emits

## What You Will Need

- **OpenShift cluster access** with `cluster-admin` or a user that can create `Monitor` and `Route` resources
- **`oc` CLI** installed and configured
- **Helm CLI** installed (v3.x or later)
- **Prometheus stack** (the `kube-prometheus-stack` Helm chart) installed in the cluster, or an external Prometheus server that can reach Wanaku routes
- **Completed [demo 5.01](../5.01-camel-assistant/README.md)** — you should have a working Wanaku deployment with at least one capability already registered
- Background reading:
- [Performance tests](https://github.com/wanaku-ai/wanaku/blob/main/docs/performance-tests.md) — explains load-testing the Wanaku router
- [Camel Integration Capability operations](https://github.com/wanaku-ai/camel-integration-capability/blob/main/docs/operations.md) — health checks, scaling, and container best practices

## Architecture Overview

The monitoring path uses the observability components that are already built into Wanaku's Quarkus foundations. No application code changes are needed.

```
┌─────────────────────────────────────────────────────────────────┐
│                        OpenShift Cluster                         │
│                                                                  │
│  ┌──────────────┐   scrape  ┌────────────────────────────────┐  │
│  │   Prometheus  │──────────▶│  Wanaku Router Backend         │  │
│  │   (kube-      │  /q/      │  - /q/metrics (Micrometer)    │  │
│  │  prom-       │ metrics   │  - /q/health/* (SmallRye)     │  │
│  │  stack)      │◀──────────│  - gRPC 9090 (tool calls)     │  │
│  └──────┬───────┘           └─────────────────┬──────────────┘  │
│         │                                    ▲                  │
│         │   scrape   ┌──────────────────────┘                  │
│         │  /q/       │  ┌──────────────────────────────┐       │
│         │  metrics   │  │  Camel Integration Capability  │       │
│         │◀───────────│  │  - gRPC health :9190          │       │
│         │            │  │  - application logs           │       │
│         │            │  └──────────────────────────────┘       │
│    ┌────┴────┐         │                                        │
│    │ Grafana │         │  ┌───────────────────────────┐        │
│    │ (UI)    │         │  │  Other Capability Services │        │
│    └─────────┘         │  └───────────────────────────┘        │
│                                                                  │
│  Collect side-effects through the same endpoints:               │
│    - Alertmanager routes on-call alerts                         │
│    - Loki (or OpenSearch) ingests structured JSON logs           │
│    - Jaeger receives traces when tracing is enabled              │
└─────────────────────────────────────────────────────────────────┘
```

Wanaku's router backend already ship with:
- **Micrometer + Prometheus** — the `quarkus-micrometer-registry-prometheus` extension is on the classpath, exposing JVM, HTTP, and gRPC metrics at `/q/metrics` with zero custom code
- **SmallRye Health** — liveness, readiness, and startup probes at `/q/health/live`, `/q/health/ready`, `/q/health/started`, all unauthenticated
- **OpenTelemetry** — a fully instrumented gRPC pipeline with an OTLP gRPC exporter, disabled by default, toggled at runtime; send the trace stream to any OTLP receiver (Jaeger, Tempo, vendor APM)

## Step 1: Expose the Metrics Endpoint

Wanaku's router already listens on `/q/metrics` on `8080`. You need two Kubernetes resources to make Prometheus scrape it:

1. A `ServiceMonitor` that tells Prometheus where to look.

> [!NOTE]
> The `/q/metrics` endpoint is already accessible without authentication. The router's Quarkus configuration ships with a catch-all `permit` rule matching `/*`, which covers `/q/metrics` without conflicting with the more specific authenticated paths (`/api/v1/*`, `/mcp/*`, `/admin/*`). No additional auth permission is required.

```yaml
# wanaku-monitoring/servicemonitor-router.yaml
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: wanaku-router
  namespace: <wanaku-namespace>
  labels:
    app.kubernetes.io/name: wanaku-router
    app.kubernetes.io/part-of: wanaku
spec:
  namespaceSelector:
    matchNames:
      - <wanaku-namespace>
  selector:
    matchLabels:
      app.kubernetes.io/name: wanaku-router
  endpoints:
    - port: http
      path: /q/metrics
      scheme: http
      interval: 15s
      scrapeTimeout: 10s
```

Apply it:

```shell
oc apply -f wanaku-monitoring/servicemonitor-router.yaml
```

### 1.2 Verify That Prometheus Is Scraping

In the Prometheus UI (traffic through an OpenShift Route or port-forward):

```shell
oc -n <prometheus-namespace> port-forward svc/prometheus-k8s 9090:9090
```

Then open `http://localhost:9090/targets` and confirm that the `wanaku-router` job shows the `UP` state. If it shows `DOWN`, check the `last_error` column — the most common failure is that the ServiceMonitor `port` value (`http`) does not match the router `Service` port name. Use `oc -n <ns> get svc wanaku-router -o yaml` to verify.

## Step 2: Configure Kubernetes Health Probes

Wanaku's router backend exposes three health endpoints out of the box:

| Endpoint | Kubernetes probe role |
| --- | --- |
| `/q/health/live` | Liveness — kills the pod if the JVM crashes |
| `/q/health/ready` | Readiness — removes the pod from service routing if any declared dependency is down |
| `/q/health/started` | Startup — holds the pod out of readiness until the JVM is warmed up |

The specific checks registered under each endpoint (OIDC connectivity, Infinispan status, service registry presence) may vary between versions. Run `curl http://localhost:8080/q/health/ready | jq .` inside the router pod to see the live list.

The operator's generated `Deployment` already includes HTTP probes for all three. For a manual or operator-managed deployment, the probe configuration looks like this:

```yaml
# snippet from wanaku-operator Deployment template
livenessProbe:
  httpGet:
    path: /q/health/live
    port: 8080
  initialDelaySeconds: 15
  periodSeconds: 20
  timeoutSeconds: 5
  failureThreshold: 5
readinessProbe:
  httpGet:
    path: /q/health/ready
    port: 8080
  initialDelaySeconds: 10
  periodSeconds: 5
  timeoutSeconds: 3
  failureThreshold: 3
startupProbe:
  httpGet:
    path: /q/health/started
    port: 8080
  initialDelaySeconds: 10
  periodSeconds: 10
  timeoutSeconds: 3
  failureThreshold: 30
```

> [!NOTE]
> The `periodSeconds` of `5s` on the readiness probe is useful for fast failover. Swapping it to `10s` or `15s` reduces load on OIDC discovery lookups in high-latency network environments.

You can check readiness manually during a deployment:

```shell
POD=$(oc get pods -n <wanaku-namespace> -l app=wanaku-router -o jsonpath="{.items[0].metadata.name}")
oc exec "$POD" -n <wanaku-namespace> -- curl -s http://localhost:8080/q/health/ready
```

Expected output:

```text
{"status":"UP","checks":[{"name":"wanaku-router","state":"UP"}, ...]}
```

## Step 3: Configure Capability Health Checks

Camel Integration Capabilities run a separate gRPC health server on port `9190`. Refer to the [Camel Integration Capability operations](https://github.com/wanaku-ai/camel-integration-capability/blob/main/docs/operations.md) guide for the canonical probe YAML.

On Kubernetes 1.24+, use the native gRPC probe:

```yaml
# wanaku-monitoring/probes-camel-capability.yaml
livenessProbe:
  grpc:
    port: 9190
  initialDelaySeconds: 15
  periodSeconds: 10
  timeoutSeconds: 5
  failureThreshold: 3
readinessProbe:
  grpc:
    port: 9190
  initialDelaySeconds: 10
  periodSeconds: 5
  timeoutSeconds: 3
  failureThreshold: 2
```

On older clusters, use the `grpc_health_probe` binary (install it into a scratch container first):

```yaml
readinessProbe:
  exec:
    command: ["/grpc_health_probe", "-addr=:9190"]
  initialDelaySeconds: 5
  periodSeconds: 5
```

The operator's generated `Deployment` for `WanakuCapability` CRs (including Camel Integration Capabilities) uses the same probe pattern. If you manage the `Deployment` yourself for a custom capability, add the probe to your own template.

The router also performs periodic gRPC health probes against registered capabilities every 60 seconds (configurable via `wanaku.router.health-check.interval-seconds`). The result feeds back into readiness: if all registered capabilities become unhealthy, the router's `/q/health/ready` will surface DEGRADED state.

## Step 4: Tune Log Levels and Read Logs

### 4.1 Router Log Configuration

The router uses Quarkus SmallRye Config for log levels. The default `application.properties` ships with `INFO` for everything:

```properties
quarkus.log.level=INFO
quarkus.log.category."ai.wanaku".level=INFO
quarkus.log.category."io.quarkus.oidc.proxy".level=INFO
quarkus.log.category."io.quarkiverse.mcp".level=INFO

# Trace format includes traceId/requestId when OTel is enabled:
quarkus.log.console.format=%d{yyyy-MM-dd HH:mm:ss} %-5p [%c] (%t) [traceId=%X{traceId}, requestId=%X{requestId}] %s%e%n
```

To turn up router verbosity temporarily:

```shell
# Runtime override — no restart required for most levels
oc -n <wanaku-namespace> set env deployment/wanaku-router \
  QUARKUS_LOG_CATEGORY__AI_WANAKU__LEVEL=DEBUG
oc -n <wanaku-namespace> rollout restart deployment/wanaku-router
```

Useful categories for debugging:

| Logger pattern | What you see |
| --- | --- |
| `ai.wanaku'` | Router business logic, capability registration |
| `ai.wanaku.backend.health` | Periodic gRPC health probe results |
| `io.quarkiverse.mcp` | MCP server request/deserialization |
| `io.quarkus.oidc` | OIDC token validation (when auth is on) |
| `org.apache.camel` | Camel route startup inside a capability |
| `io.grpc` | gRPC transport internals |

### 4.2 Capability Log Configuration

Camel Integration Capabilities default to `INFO` with a JSON layout. The capability's `ConfigMap` contains the `log4j2.xml` definition:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Configuration status="WARN">
  <Appenders>
    <Console name="Console" target="SYSTEM_OUT">
      <JsonLayout compact="true" eventEol="true"/>
    </Console>
  </Appenders>
  <Loggers>
    <Root level="info">
      <AppenderRef ref="Console"/>
    </Root>
    <Logger name="ai.wanaku.capability.camel" level="debug" additivity="false">
      <AppenderRef ref="Console"/>
    </Logger>
  </Loggers>
</Configuration>
```

> [!NOTE]
> CIC uses Log4j2 structured JSON output. The Wanaku router uses a completely different logging runtime (Quarkus JBoss LogManager) with a plain-text console format. The two are not interchangeable — do not attempt to pipe router logs through `jq` or apply the same config patterns to both.

To adjust levels on a running capability through the `WanakuCapability` custom resource, use `env` injection on the CR spec:

```yaml
spec:
  env:
    - name: QUARKUS_LOG_CATEGORY__AI_WANAKU_CAPABILITY_CAMEL__LEVEL
      value: TRACE
```

### 4.3 Reading Logs from the CLI

Tail the router, which uses plain-text format (do not pipe through `jq`):

```shell
oc -n <wanaku-namespace> logs -f deployment/wanaku-router --all-containers
```

Filter ERROR entries from a capability pod (CIC emits structured JSON via Log4j2):

```shell
CAP_POD=$(oc get pods -n <wanaku-namespace> -l app=camel-integration-capability -o jsonpath="{.items[0].metadata.name}")
oc -n <wanaku-namespace> logs -f "$CAP_POD" | jq 'select(.level == "ERROR")'
```

Correlate a single MCP request by `requestId` across pods (using grep on the plain-text router + JSON capability output — works because the `requestId` field appears verbatim in both formats):

```shell
oc -n <wanaku-namespace> logs -l app=wanaku-router,app=camel-integration-capability | grep "<requestId>"
```

## Step 5: Build a Basic Grafana Dashboard

Wanaku emits these metrics out of the box once Prometheus is scraping `/q/metrics`:

| Metric name | What it tells you |
| --- | --- |
| `http_server_requests_seconds` | Router HTTP latency with `method`, `uri`, and `status_code` labels |
| `grpc_server_handled_total` | Total gRPC calls to the router, labeled by `grpc_status` |
| `grpc_server_handling_seconds` | gRPC call duration |
| `jvm_memory_used_bytes` | Heap and non-heap usage by JVM memory pool |
| `jvm_gc_pause_seconds` | Time the JVM spent in GC |
| `process_cpu_usage` | CPU usage of the router JVM as a 0-1 ratio |
| `process_uptime_seconds` | How long the process has been running |

### 5.1 Add a Data Source in Grafana

Import the standard Prometheus data source pointing at your Prometheus server (`http://prometheus-k8s:9090` within the cluster, or the Route URL externally).

### 5.2 Minimal Dashboard Panels

Create a new dashboard with at least these five panels:

1. **HTTP request rate** — query `sum(rate(http_server_requests_seconds_count[1m])) by (uri)`
2. **HTTP p95 latency** — query `histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[1m])) by (le, uri))`
3. **gRPC handled count** — query `sum(rate(grpc_server_handled_total[1m])) by (grpc_status)`
4. **JVM memory** — query `jvm_memory_used_bytes{area="heap"}`
5. **CPU usage** — query `avg(process_cpu_usage) by (pod)`

### 5.3 Capture a Baseline Before Changing Anything

Refer to [performance tests](https://github.com/wanaku-ai/wanaku/blob/main/docs/performance-tests.md) for the load-test harness. Run a short `k6` scenario against the router before and after any configuration change so that the Grafana panels show a real regression signal.

## Step 6: End-to-End Verification

### 6.1 Verify Metrics Streaming

```shell
POD=$(oc get pods -n <wanaku-namespace> -l app=wanaku-router -o jsonpath="{.items[0].metadata.name}")
oc -n <wanaku-namespace> exec "$POD" -- curl -s http://localhost:8080/q/metrics | head -20
```

Expected output starts with:

```text
# HELP jvm_memory_used_bytes ...
# TYPE jvm_memory_used_bytes gauge
jvm_memory_used_bytes{area="heap",id="G1 Survivor Space", ...} 1.22944E7
...
http_server_request_seconds_count{method="GET",uri="/q/health/ready",status_code="200"} 42
```

### 6.2 Verify Health Responses

```shell
oc -n <wanaku-namespace> exec "$POD" -- curl -s http://localhost:8080/q/health/live
oc -n <wanaku-namespace> exec "$POD" -- curl -s http://localhost:8080/q/health/ready
oc -n <wanaku-namespace> exec "$POD" -- curl -s http://localhost:8080/q/health/started
```

All three should return JSON with `"status":"UP"`.

### 6.3 Verify Logs Are Emitted Correctly

The router and capabilities both use different logging runtimes. Verify each independently:

```shell
# Router: plain-text output — verify the format includes traceId and requestId
oc -n <wanaku-namespace> logs -l app=wanaku-router --tail=5

# Capability (CIC): structured JSON output — verify the format is parseable
CAP_POD=$(oc get pods -n <wanaku-namespace> -l app=camel-integration-capability -o jsonpath="{.items[0].metadata.name}")
oc -n <wanaku-namespace> logs "$CAP_POD" --tail=1 | jq .
```

Expected log format for the router:

```text
2025-06-15 10:32:41 INFO  [ai.wanaku.backend.health] (executor-thread-1) [traceId=abc123..., requestId=def456...] Periodic health sweep completed: 3 healthy, 0 degraded, 0 unhealthy
```

## What's Next?

- [Production deployment on OpenShift](../3.01-wanaku-on-the-cloud/README.md) — configures Keycloak authentication and operator-managed resources
- [Performance tests](https://github.com/wanaku-ai/wanaku/blob/main/docs/performance-tests.md) — run k6 load tests to baseline router throughput
- [Camel Integration Capability operations](https://github.com/wanaku-ai/camel-integration-capability/blob/main/docs/operations.md) — deeper coverage of health checks, resource sizing, and container best practices for individual capability pods

## Troubleshooting

### Prometheus shows the `wanaku-router` target as DOWN

Check the `last_error` in the Prometheus Targets page. The most common cause is that the ServiceMonitor `port` value (`http`) does not match the router `Service` port name. Use `oc -n <ns> get svc wanaku-router -o yaml` to confirm the port name and update the ServiceMonitor.

### `/q/health/ready` returns DEGRADED with an `oidc` failure

The readiness check pings your OIDC provider's `/.well-known/openid-configuration`. If the provider is down or unreachable, readiness reports DEGRADED.

**Symptoms:** `oc exec <pod> -- curl /q/health/ready` shows `oidc` in `state: DOWN`.

**Mitigation:**
1. If you do not need OIDC for the current test run, disable it at the router level: `wanaku.http.auth=none`. The readiness check will skip OIDC automatically.
2. If OIDC is required, ensure the network policy or OpenShift `NetworkPolicy` allows the router to reach the Keycloak/OIDC host on port `443`.
3. Restart the router after changing the auth mode: `oc rollout restart deployment/wanaku-router`.

### Capability health probes fail on `grpc_health_probe` with connection refused

On Kubernetes 1.23 and earlier, the native gRPC probe is not available and you may be using an `exec` probe with `grpc_health_probe`.

**Symptoms:** Capability pod is OOMKilled or stays in `NotReady`.

**Fix:**
1. Use the `quay.io/grpc-ecosystem/grpc-health-probe:latest` image in a sidecar or init container to test connectivity.
2. Confirm that the capability's internal service exposes port `9190` in the `port` field, not only `targetPort`.
3. Verify the capability logs are not flooding errors at `TRACE`, which can starve the gRPC health thread.

### Grafana shows no data but Prometheus shows UP targets

The most frequent cause is that the Prometheus data source in Grafana is configured to query the wrong namespace. In the data source settings, set the namespace to `<wanaku-namespace>` (or leave it blank for `all`).

Also verify that the `prometheus` ServiceAccount has a `Role` or `ClusterRole` that includes `get`, `list`, and `watch` on `endpoints` and `pods` in the Wanaku namespace. The `kube-prometheus-stack` Helm chart ships a `prometheus-k8s` role; bind it:

```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
  name: prometheus-k8s-wanaku
  namespace: <wanaku-namespace>
subjects:
  - kind: ServiceAccount
    name: prometheus-k8s
    namespace: <prometheus-namespace>
roleRef:
  kind: ClusterRole
  name: prometheus-k8s
  apiGroup: rbac.authorization.k8s.io
```

> [!TIP]
> The `wc` command is not needed in OpenShift — use `oc` equivalents shown above.

If you find a bug, please [report it](https://github.com/wanaku-ai/wanaku/issues). To get in touch with the community, visit the [Wanaku project](https://github.com/wanaku-ai/wanaku).
