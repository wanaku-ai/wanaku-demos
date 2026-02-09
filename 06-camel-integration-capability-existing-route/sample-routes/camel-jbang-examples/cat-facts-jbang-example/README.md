# Cat Facts JBang Example

A Camel JBang application using YAML DSL that fetches random cat facts, demonstrating integration with the Wanaku Camel Integration Capability plugin.

## Overview

This example exposes a Camel route (`cat-facts-route`) that calls the MeowFacts API to retrieve random cat facts. The route is registered as an MCP tool via the Wanaku plugin.

## Prerequisites

- Java 21+
- [Camel JBang](https://camel.apache.org/manual/camel-jbang.html) installed
- Wanaku Router running (for plugin registration)

## Configuration

### Environment Variables

| Variable                        | Description                       | Example                           |
|---------------------------------|-----------------------------------|-----------------------------------|
| `REGISTRATION_URL`              | Wanaku router URL                 | `http://localhost:8080`           |
| `REGISTRATION_ANNOUNCE_ADDRESS` | Address to announce for callbacks | `localhost`                       |
| `GRPC_PORT`                     | gRPC server port                  | `9190`                            |
| `SERVICE_NAME`                  | Service name for registration     | `cat-facts-service`               |
| `ROUTES_RULES`                  | Path to route rules file          | `file:///path/to/meow-rules.yaml` |
| `CLIENT_ID`                     | OAuth client ID                   | `wanaku-service`                  |
| `CLIENT_SECRET`                 | OAuth client secret               | `<secret>`                        |

### Route Rules

The `ROUTES_RULES` variable accepts:
- File path: `file:///path/to/meow-rules.yaml`
- Datastore reference: `datastore://meow-rules.yaml`

## Running

### Set environment variables

```bash
export REGISTRATION_URL=http://localhost:8080
export REGISTRATION_ANNOUNCE_ADDRESS=localhost
export GRPC_PORT=9190
export SERVICE_NAME=cat-facts-jbang-service
export ROUTES_RULES=file://$PWD/config/meow-rules.yaml
export CLIENT_ID=wanaku-service
export CLIENT_SECRET=<secret>
```

### Run with Camel JBang

```bash
camel run --dep ai.wanaku.sdk:capabilities-runtime-camel-plugin:0.1.0-SNAPSHOT cat-facts-route.camel.yaml
```

## MCP Tool

Once registered, the route is exposed as an MCP tool:

| Tool              | Description                      | Parameters                                            |
|-------------------|----------------------------------|-------------------------------------------------------|
| `cat-facts-route` | Retrieve random facts about cats | `count` (int, required) - number of facts to retrieve |
