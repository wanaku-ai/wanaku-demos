# Camel Integration Capability

Enterprise systems don't speak MCP. Your HR database, your inventory API, your
message queues — they're all stuck behind protocols AI agents can't call directly.
This guide shows you how to expose Apache Camel routes as MCP tools through Wanaku,
so agents can query backend systems without you writing glue code.

By the end, you will have an AI agent that can query an employee management system
using tools backed by Camel routes — no custom code in the agent, no hand-written
API wrappers.

## Prerequisites

- An OpenShift or Kubernetes cluster
- The [Wanaku Operator](../02-wanaku-on-the-cloud/README.md) installed
- `oc` (or `kubectl`) CLI, logged into your cluster
- Keycloak deployed and configured (see the [cloud guide](../02-wanaku-on-the-cloud/README.md))

## What Is the Camel Integration Capability?

The Camel Integration Capability (CIC) is a bridge between Apache Camel and the
Model Context Protocol (MCP). It takes Camel routes defined in YAML and exposes
them as MCP tools that any AI agent can invoke through the Wanaku Router.

This means you can integrate AI agents with any system that Camel supports — REST
APIs, databases, message queues, file systems, cloud services — without writing a
single line of agent code.

```
AI Agent
  |  (MCP Protocol)
  v
Wanaku MCP Router
  |  (gRPC)
  v
Camel Integration Capability
  |  (executes Camel routes)
  v
Backend Systems (REST APIs, databases, queues, ...)
```

### Why Use It?

Without the CIC, connecting an AI agent to a backend system requires writing a
custom capability service. That's fine for simple cases, but enterprise environments
have dozens of systems, each with its own API, authentication, and data formats.

The CIC lets you define integrations as Camel routes — the same way teams already
build integration flows — and expose them to AI agents with a YAML rules file.
Change a route, update the rules file, re-copy. No compilation step, no redeploy.

### How It Works

You give the CIC three things:

1. **Routes** — a Camel routes YAML file defining the integration logic. For example: "when this tool is invoked, call this HTTP endpoint and transform the response."
2. **Rules** — a YAML file declaring which routes to expose as MCP tools. This is your security boundary — routes that aren't listed stay invisible to agents.
3. **Dependencies** — a list of Camel components the routes need (e.g., `camel-http` for REST calls, `camel-jackson` for JSON parsing). The CIC downloads these from Maven Central at startup.

At runtime, when an AI agent invokes a tool, here's what happens:

- The Wanaku Router forwards the request to the CIC over gRPC.
- The CIC maps the tool's parameters to Camel headers (e.g., `employeeId: 3` becomes a header named `EMPLOYEE_ID`).
- The CIC triggers the corresponding Camel route. The route executes — calling an API, querying a database, whatever the route does.
- The route's output flows back through the CIC to the router, and the agent gets the result.

## What This Demo Does

<details>
<summary>Click to expand</summary>

This demo deploys two components:

- **employee-backend** — a Quarkus REST service that serves employee data (information,
  reviews, compensation). This simulates an existing enterprise system.
- **employee-system** — the CIC instance. It runs four Camel routes that call the
  employee backend and exposes three of them as MCP tools through Wanaku.

The result: an AI agent can ask questions like "show me employee 3's reviews" and
Wanaku routes the request through Camel to the backend.

</details>

## Understanding the Pieces

Before deploying, let's look at what the CIC actually runs. Understanding these
three files — routes, rules, dependencies — is the difference between copying
commands and actually designing integrations.

### The Camel Routes

The file `employee-system/employee-backend.camel.yaml` defines four routes:

| Route ID | What It Does |
|----------|-------------|
| `get-employee-information` | Calls `GET /employee/{id}/information` on the backend. Returns name, level, and days in level. Contains a content filter that redacts executive-level data. |
| `get-employee-reviews` | Calls `GET /employee/{id}/reviews` on the backend. Returns performance reviews and manager feedback. |
| `get-employee-compensation` | Calls `GET /employee/{id}/compensation` on the backend. Returns pay details. |
| `get-employee-complete-profile` | Orchestrates the three routes above into a single aggregated response. Demonstrates Camel's route composition. |

Each route follows a common pattern:

```yaml
- route:
    id: get-employee-reviews
    from:
      uri: direct:employee-reviews
      steps:
        - setHeader:
            constant: GET
            name: CamelHttpMethod
        - toD:
            uri: http://employee-backend-service:8081/employee/${header.EMPLOYEE_ID}/reviews
            parameters:
              bridgeEndpoint: true
        - convertBodyTo:
            type: String
```

Key points:

- The `direct:` endpoint is how the CIC triggers the route internally
- `${header.EMPLOYEE_ID}` is a Camel header — the CIC sets this from the MCP tool parameters
- `bridgeEndpoint: true` stops Camel from rewriting HTTP headers. Without this, Camel's HTTP component tries to be helpful by normalizing headers, which breaks some backend APIs. When in doubt, leave this set to `true`.
- `convertBodyTo: String` ensures the response is text the AI agent can read

> [!TIP]
> You can design these routes visually using [Kaoto](https://kaoto.io) or
> [Camel Karavan](https://camel.apache.org/karavan) instead of editing YAML by hand.

### The Rules File

The file `employee-system/employee-backend-rules.yaml` declares which routes become
MCP tools. Without this file, the routes exist but are invisible to AI agents. This
is deliberate: you don't want an agent accidentally invoking a destructive route
because it guessed the route ID. The rules file is an explicit allowlist.

```yaml
mcp:
  tools:
    - get-employee-information:
        route:
          id: "get-employee-information"
        description: "Fetches core profile data for a specific employee ..."
        properties:
          - name: employeeId
            type: int
            description: The employee ID to retrieve information for
            required: true
            mapping:
              type: header
              name: EMPLOYEE_ID
```

Each tool definition has:

- **`route.id`** — links the tool to a Camel route by its ID (must match exactly)
- **`description`** — tells the AI agent what the tool does and when to use it
- **`properties`** — defines the parameters the agent can pass

#### Parameter Mapping

The `mapping` section controls how MCP parameters become Camel headers. In this
demo, the MCP parameter `employeeId` maps to the Camel header `EMPLOYEE_ID`:

```
AI agent sends:  { "employeeId": 3 }
         |
         v
CIC creates Camel header:  EMPLOYEE_ID = 3
         |
         v
Route uses it:  http://backend:8081/employee/${header.EMPLOYEE_ID}/reviews
```

There are two mapping strategies:

| Strategy | When to Use | How It Works |
|----------|-------------|-------------|
| **Explicit** (this demo) | Production, or any time you care about security. You control exactly which parameters reach the route. | Define `properties` with `mapping` entries. Only listed parameters are passed. Everything else is dropped. |
| **Automatic** | Prototyping, or internal-only routes where you trust the agent. | Omit `properties` entirely. All parameters become Camel headers with a `Wanaku.` prefix (e.g., `Wanaku.employeeId`). Your route reads them with `${header.Wanaku.employeeId}`. |

> [!NOTE]
> Notice that `get-employee-complete-profile` is defined in the routes file but is **not**
> listed in the rules file. It exists as internal plumbing — the rules file acts as an
> allowlist for what AI agents can see.

### The Dependencies

The CIC ships with a minimal set of Camel components. Any additional ones the routes
need must be listed so the CIC can download them at startup. In this demo, the
deployment manifest specifies:

```
org.apache.camel:camel-http:4.14.1,org.apache.camel:camel-jackson:4.14.1
```

- `camel-http` — needed because the routes make HTTP calls to the backend
- `camel-jackson` — needed for JSON processing

These are standard Maven coordinates. The CIC resolves them from Maven Central at
startup.

## Step 1: Deploy the Employee Backend

The employee backend is a Quarkus REST service that serves employee data —
information, reviews, and compensation. It's a stand-in for the kind of internal
API you'd have in an actual enterprise environment. First, build and push the
container image:

```bash
cd employee-backend
mvn -B clean package \
    -Dquarkus.container-image.build=true \
    -Dquarkus.container-image.push=true \
    -Dquarkus.container-image.group=<your-registry>
```

Replace `<your-registry>` with your container registry (e.g., `quay.io/your-username`).

Then deploy it to the cluster:

```bash
oc apply -f employee-system/openshift-deployment.yaml
```

This creates:

- A `Deployment` running the backend on port 8081
- A `Service` named `employee-backend-service` (this is the hostname the Camel routes use)
- An OpenShift `Route` for external access (optional — the Camel routes use the internal service)

Wait for the backend to be ready:

```bash
oc wait --for=condition=ready pod -l app=employee-backend --timeout=120s
```

## Step 2: Deploy Wanaku with the CIC

Apply the Wanaku custom resource that includes the CIC:

```bash
oc apply -f employee-system/wanaku-demo-employee-system.yaml
```

This tells the Wanaku Operator to deploy:

- The **Wanaku Router** (the MCP entry point)
- The **HTTP capability** (built-in, for general HTTP tools)
- The **employee-system** CIC instance

The CIC section of the manifest looks like this:

```yaml
- name: employee-system
  type: camel-integration-capability
  image: quay.io/wanaku/camel-integration-capability:latest
  env:
    - name: ROUTES_PATH
      value: "/data/employee-backend.camel.yaml"
    - name: ROUTES_RULES
      value: "/data/employee-backend-rules.yaml"
    - name: DEPENDENCIES
      value: "org.apache.camel:camel-http:4.14.1,org.apache.camel:camel-jackson:4.14.1"
```

- `type: camel-integration-capability` tells the operator this is a CIC (not a regular capability)
- `ROUTES_PATH` and `ROUTES_RULES` point to where the route and rules files will be inside the container
- `DEPENDENCIES` lists the Maven artifacts to download at startup

## Step 3: Copy the Route Files

The CIC container starts with an empty `/data` directory. In this demo, you copy the
routes and rules files into the pod manually — in production, you'd use a service
catalog or mount them from a ConfigMap, but this approach lets you iterate on the
files without rebuilding the container.

```bash
cd employee-system
./copy-files.sh
```

This script finds the CIC pod and uses `oc cp` to upload:

- `employee-backend.camel.yaml` (the routes)
- `employee-backend-rules.yaml` (the rules)

After copying, the CIC detects the files, loads the routes, applies the rules, and
registers the tools with the Wanaku Router.

> [!TIP]
> For production deployments, consider using a [service catalog](https://github.com/wanaku-ai/camel-core-downstream-service/blob/main/docs/service-catalog-guide.md)
> instead of manually copying files. A service catalog bundles routes, rules, and
> dependencies into a single versioned artifact stored in Wanaku's Data Store.

## Step 4: Verify the Tools

Check that the tools are registered:

```bash
wanaku tools list --host $WANAKU_HOST
```

You should see three tools: `get-employee-information`, `get-employee-reviews`, and
`get-employee-compensation`.

Check that the CIC registered as a capability:

```bash
wanaku capabilities list --host $WANAKU_HOST
```

## Step 5: Connect an Agent and Test

Connect any MCP-compatible client to the Wanaku Router (see the
[getting started guide](../01-getting-started/README.md#step-5-connect-an-ai-agent)
for client-specific instructions).

Then ask your agent something like:

> Show me the performance reviews for employee 3.

The agent will discover the `get-employee-reviews` tool, call it with `employeeId: 3`,
and return the results from the backend.

## What Happens When Things Go Wrong?

If a route fails — the backend returns a 500, the route throws an exception, the
HTTP call times out — the CIC returns an error response to the agent. The agent
sees this as a tool failure and can decide whether to retry, report the error to the
user, or try a different tool.

The CIC doesn't automatically retry failed routes. If you need retry logic (e.g.,
"retry 3 times with exponential backoff"), build it into the Camel route using
Camel's error handling DSL. The rules file doesn't control this — it's part of the
route definition.

## What's Next?

- **Add more routes** — add new routes to the YAML, declare them in the rules file,
  and re-copy. No code changes, no rebuild.
- **Use a service catalog** — package routes, rules, and dependencies into a versioned
  artifact for reproducible deployments.
- **Try the plugin mode** — if you already have a Camel application, you can embed the
  CIC as a plugin instead of running it as a separate service. See the
  [plugin usage guide](https://github.com/wanaku-ai/camel-core-downstream-service/blob/main/docs/plugin-usage.md).
- **Design routes visually** — use [Kaoto](https://kaoto.io) to build routes in a
  visual editor and export them as YAML.
