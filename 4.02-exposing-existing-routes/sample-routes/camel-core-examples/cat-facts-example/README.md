# Cat Facts Example

A pure Java Apache Camel application that fetches random cat facts, demonstrating integration with the Wanaku Camel Integration Capability plugin.

## Overview

This example exposes a Camel route (`cat-facts-route`) that calls the MeowFacts API to retrieve random cat facts. The route is registered as an MCP tool via the Wanaku plugin.

## Prerequisites

- Java 21+
- Maven 3.8+
- Wanaku Router running (for plugin registration)

## Building

### Build-time plugin loading (recommended)

```bash
mvn -Pbuild-time clean package
```

### Runtime plugin loading

```bash
mvn -Pruntime clean package
```

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

### With build-time plugin

```bash
REGISTRATION_URL=http://localhost:8080 \
REGISTRATION_ANNOUNCE_ADDRESS=localhost \
GRPC_PORT=9190 \
SERVICE_NAME=cat-facts-service \
ROUTES_RULES=file://$PWD/config/meow-rules.yaml \
CLIENT_ID=wanaku-service \
CLIENT_SECRET=<secret> \
java -jar target/cat-facts-example-1.0-SNAPSHOT-jar-with-dependencies.jar
```

### With runtime plugin

Download the plugin JAR and run with classpath:

```bash
java -cp /path/to/camel-integration-capability-plugin-0.1.0-SNAPSHOT-shaded.jar:target/cat-facts-example-1.0-SNAPSHOT-jar-with-dependencies.jar \
ai.wanaku.demos.camel.core.CatFactsMain
```

## MCP Tool

Once registered, the route is exposed as an MCP tool:

| Tool              | Description                      | Parameters                                            |
|-------------------|----------------------------------|-------------------------------------------------------|
| `cat-facts-route` | Retrieve random facts about cats | `count` (int, required) - number of facts to retrieve |
