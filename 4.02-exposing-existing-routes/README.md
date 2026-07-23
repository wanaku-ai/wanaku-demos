---
title: "Exposing Existing Camel Routes"
description: "Take existing Apache Camel routes and expose them as MCP tools through the Wanaku Camel Integration Capability plugin or a standalone capability."
---

# Exposing Existing Camel Routes

You already have Apache Camel routes running in your infrastructure. They call your HR API, query databases, send emails. Now you want AI agents to use them through Wanaku — without rewriting those routes or building a custom capability from scratch.

This guide shows you two ways to make that happen: the plugin approach (embed the capability in your existing Camel application) and the standalone approach (run the Camel Integration Capability as its own process). Both use a rules YAML file to tell Wanaku which routes to expose and how to map parameters.

## What You Will Learn

- How to expose an existing Camel route as an MCP tool using the CIC plugin
- How to write a rules YAML file to map MCP parameters to Camel headers
- When to use a plugin vs running a second standalone instance

## What You Will Need

- **Wanaku running locally** via `wanaku start local` (see [demo 1.01](../1.01-your-first-tool/README.md))
- **Wanaku CLI** installed ([releases page](https://github.com/wanaku-ai/wanaku/releases/tag/v0.2.0))
- **Java 21+**
- **Apache Maven 3.8+**
- **Camel JBang** installed (for the JBang example only) — instructions at [camel.apache.org](https://camel.apache.org/manual/camel-jbang.html)

You do not need to have completed any prior Camel demo, but familiarity with [demo 3.02](../3.02-camel-integration-capability/README.md) (Camel Integration Capability) will help.

## Architecture Overview

```text
┌──────────────────────────────┐
│ Existing Camel Route         │
│ (CatFactsRoute.java)         │
│ from("direct:catFacts")      │
│   .toD("...count=${header..}")│
└──────────────────────────────┘
            │
            │ CIC Plugin embedded OR standalone
            ↓
┌──────────────────────────────┐
│ Camel Integration Capability │
│  - loads routes               │
│  - applies meow-rules.yaml    │
│  - registers MCP tools        │
│  - starts gRPC server         │
└──────────────────────────────┘
            │ gRPC
            ↓
┌──────────────────────────────┐
│ Wanaku MCP Router            │
│  (route "cat-facts-route")   │
└──────────────────────────────┘
            │ MCP
            ↓
┌──────────────────────────────┐
│ AI Agent                     │
│ calls: count=3               │
└──────────────────────────────┘
```

## Understanding the Pieces

### The Route

Your existing Camel route is unchanged. In this demo, `CatFactsRoute.java` fetches random cat facts from an external API:

```java
from("direct:catFacts")
    .routeId("cat-facts-route")
    .log("Fetching ${header.COUNT} cat facts...")
    .toD("https://meowfacts.herokuapp.com/?count=${header.COUNT}")
    .convertBodyTo(String.class)
```

The route expects a `COUNT` header. The job of the rules file is to map the MCP `count` parameter to that header.

### The Rules File

The rules file (`meow-rules.yaml`) is the bridge between MCP and Camel. It declares which routes to expose and how to map parameters:

```yaml
mcp:
  tools:
    - cat-facts-route:
        route:
          id: "cat-facts-route"
        description: "Retrieve random facts about cats"
        properties:
          - name: count
            type: int
            description: Number of facts to retrieve
            required: true
            mapping:
              type: header
              name: COUNT
```

### Plugin vs Standalone

| Approach | How It Works | Use When |
|----------|--------------|----------|
| **CIC Plugin** | Drop the plugin JAR into your existing Camel app's classpath. No code changes. | You already run a Camel app and just want to add capability |
| **Standalone** | Run the CIC as a separate process with its own `Main` class. | You want an isolated capability service, similar to demo 4.01 |

This demo covers both.

## Step 1: Navigate to the Sample Routes

The samples are in `sample-routes/` inside this directory:

```text
4.02-exposing-existing-routes/
  sample-routes/
    camel-core-examples/
      cat-facts-example/          ← plugin approach + standalone (Java)
    camel-jbang-examples/
      cat-facts-jbang-example/    ← plugin approach (JBang YAML)
```

## Step 2: Write the Rules File

Create a file named `meow-rules.yaml` (relative to the example's working directory):

```yaml
mcp:
  tools:
    - cat-facts-route:
      route:
        id: "cat-facts-route"
      description: "Retrieve random facts about cats"
      properties:
        - name: count
          type: int
          description: Number of cat facts to retrieve
          required: true
          mapping:
            type: header
            name: COUNT
```

> [!IMPORTANT]
> The `route.id` must match the `routeId` set in your Camel route definition exactly (case-sensitive).

## Step 3: Run the Cat Facts Example (Plugin Approach — Core)

This approach runs your Camel application with the CIC plugin on the classpath. The auto-discovery mechanism (Java ServiceLoader) finds and starts the plugin automatically.

### 3a. Build the Application

```shell
cd sample-routes/camel-core-examples/cat-facts-example
```

Build the fat JAR (the build-time profile is the default and bundles the plugin automatically):

```shell
mvn clean package
```

This produces `target/cat-facts-example-1.0-SNAPSHOT-jar-with-dependencies.jar`.

### 3b. Run the Application

```shell
REGISTRATION_URL=http://localhost:8080 \
REGISTRATION_ANNOUNCE_ADDRESS=localhost \
GRPC_PORT=9190 \
SERVICE_NAME=cat-facts-service \
ROUTES_RULES=file://$PWD/config/meow-rules.yaml \
java -jar target/cat-facts-example-1.0-SNAPSHOT-jar-with-dependencies.jar
```

You should see logs indicating the plugin loaded, the route was found, and the tool was registered.

### 3c. Verify Registration

In a separate terminal:

```shell
wanaku capabilities list
```

You should see `cat-facts-service` listed.

```shell
wanaku tools list
```

You should see `cat-facts-route` listed.

### 3d. Test the Tool

Connect an MCP client such as the [MCP Inspector](https://github.com/modelcontextprotocol/inspector) to the Wanaku router and invoke the tool. Use the router's MCP endpoint (e.g., `http://localhost:8080/mcp/sse`) and call `cat-facts-route` with `count=2`.

Alternatively, use the Wanaku CLI directly:

```shell
wanaku mcp tool \
  --uri http://localhost:8080/mcp/sse \
  --name cat-facts-route \
  --param count=2
```

Expected output (cat facts in JSON):

```text
[
  "Fact 1 about cats...",
  "Fact 2 about cats..."
]
```

## Step 4: Run the Cat Facts Example (Plugin Approach — JBang)

If you prefer defining routes in YAML DSL instead of Java, use the JBang variant.

### 4a. Set Environment Variables

```shell
export REGISTRATION_URL=http://localhost:8080
export REGISTRATION_ANNOUNCE_ADDRESS=localhost
export GRPC_PORT=9190
export SERVICE_NAME=cat-facts-jbang-service
export ROUTES_RULES=file://$PWD/config/meow-rules.yaml
```

> [!NOTE]
> The JBang example currently ships without a `meow-rules.yaml` file. Copy the one from the core example into `sample-routes/camel-jbang-examples/config/` or create one with the same content from Step 2.

### 4b. Run with Camel JBang

```shell
camel run --dep ai.wanaku.sdk:capabilities-runtime-camel-plugin:0.2.0 \
  sample-routes/camel-jbang-examples/cat-facts-jbang-example/cat-facts-route.camel.yaml
```

The route is defined in `cat-facts-route.camel.yaml`:

```yaml
- route:
    id: cat-facts-route
    from:
      uri: direct:catFacts
    steps:
      - log:
          message: "Fetching ${header.COUNT} cat facts..."
      - toD:
          uri: "https://meowfacts.herokuapp.com/?count=${header.COUNT}"
      - convertBodyTo:
          type: String
      - log:
          message: "Response: ${body}"
```

## Step 5: Run as a Second Instance (Independent Registration)

This sample uses the same fat JAR for both the plugin approach and the standalone pattern. When you run it with a different `SERVICE_NAME` and `GRPC_PORT`, the embedded plugin registers a separate capability — a second, independent entry in the router, even though the underlying binary is identical. This is useful when you want route isolation without launching a host Camel application.

### 5a. Build the Fat JAR

```shell
cd sample-routes/camel-core-examples/cat-facts-example
mvn clean package
```

### 5b. Run as Standalone

```shell
REGISTRATION_URL=http://localhost:8080 \
REGISTRATION_ANNOUNCE_ADDRESS=localhost \
GRPC_PORT=9191 \
SERVICE_NAME=cat-facts-standalone \
ROUTES_RULES=file://$PWD/config/meow-rules.yaml \
java -jar target/cat-facts-example-1.0-SNAPSHOT-jar-with-dependencies.jar
```

Using port `9191` here avoids conflicts if you already have the plugin version running.

### 5c. Verify and Test

```shell
wanaku capabilities list
wanaku tools list
```

You should see both `cat-facts-service` (plugin) and `cat-facts-standalone` (standalone) as separate capabilities, each exposing `cat-facts-route`.

> [!TIP]
> This is the key difference from demo 4.01: you didn't write a single line of tool logic. The CIC handles parameter mapping, gRPC serving, and registration. You only wrote the rules YAML.

## Route Exposure Rules

### Parameter Mapping Modes

The rules file supports two mapping modes (see the [full schema reference](https://github.com/wanaku-ai/camel-integration-capability/blob/main/docs/rules-schema.md)):

**Automatic mapping** (no `properties` defined): all MCP parameters are mapped to Camel headers with a `Wanaku.` prefix.

```yaml
mcp:
  tools:
    - my-route-tool:
      route:
        id: "my-route"
        description: "Does something"
```

**Explicit mapping** (with `properties`): only defined parameters are mapped, with custom header names.

```yaml
mcp:
  tools:
    - my-route-tool:
      route:
        id: "my-route-id"
        description: "Does something"
      properties:
        - name: param1
          type: string
          required: true
          mapping:
            type: header
            name: MY_HEADER
```

> [!IMPORTANT]
> Never mix automatic and explicit mapping for the same tool. Either define `properties` with mappings, or omit `properties` entirely.

### Resources

You can also expose Camel routes as **resources** (passive data sources). Resources don't execute the route body — they fetch data from the endpoint URI using a consumer template. The route must have `autoStartup: false`.

This feature is not part of this demo's flow, but the structure is the same as for tools:

```yaml
mcp:
  resources:
    - cat-facts-template:
      route:
        id: "cat-facts-template-route"
        description: "Cat facts template data"
```

## Choosing Between Plugin and Standalone

| | Plugin | Standalone |
|---|--------|------------|
| Existing Camel app | Embed — no code changes | N/A — deploy as a new service |
| Independent deployment | No — shares host app lifecycle | Yes — isolated process |
| Container support | Via host app's Dockerfile | Own Dockerfile |
| CI/CD impact | Minimal — redeploy host app | Dedicated pipeline |
| Use case | Add capability to existing integration | Dedicated capability service |

**Decision rule**: if you already run Camel in production, use the plugin. If you want the capability isolated from your integration app, use standalone.

## Troubleshooting

### Plugin not loading

If the plugin is not discovered at startup, verify the JAR is on the classpath:

```text
WARN No ContextServicePlugin found on classpath
```

The plugin requires `camel-integration-capability-plugin` as a dependency. With Maven, include it in your `pom.xml`:

```xml
<dependency>
  <groupId>ai.wanaku</groupId>
  <artifactId>camel-integration-capability-plugin</artifactId>
  <version>0.2.0</version>
</dependency>
```

### Route not found at startup

```text
ERROR Route 'wrong-route-id' not found
```

Check that the `route.id` in `meow-rules.yaml` matches the `routeId()` call in your Java route or the `id:` field in your YAML route.

### Plugin registration fails

```text
ERROR Registration failed: connection refused
```

Confirm Wanaku is running on the port in `REGISTRATION_URL`:

```shell
curl http://localhost:8080/health
```

If using Podman/Docker, set `REGISTRATION_ANNOUNCE_ADDRESS` to an address reachable from the container (e.g., `host.docker.internal` or your machine IP).

## What's Next?

- [Building a Resource Provider](../4.03-building-a-resource-provider/README.md) (demo 4.03) — create a custom file resource provider for the Wanaku MCP Router
- [Camel Assistant](../5.01-camel-assistant/README.md) (demo 5.01) — build an AI assistant backed by Apache Camel documentation and Wanaku tools
- [Camel Integration Capability](../3.02-camel-integration-capability/README.md) (demo 3.02) — full cloud deployment of the CIC on OpenShift

If you find a bug, please [report it](https://github.com/wanaku-ai/wanaku/issues).
To get in touch with the community, visit the [Wanaku project](https://github.com/wanaku-ai/wanaku).
