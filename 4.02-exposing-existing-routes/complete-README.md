# Exposing Existing Camel Routes as MCP Capabilities

This guide explains how to expose existing Apache Camel routes as MCP capabilities in Wanaku, without rewriting them.

## What You Will Learn

- How to wrap an existing Camel route as a Wanaku capability
- Creating a service catalog from existing routes
- Deploying and testing the capability locally
- Common patterns for exposing enterprise routes

## What You Will Need

- **Wanaku CLI** installed (download from [releases page](https://github.com/wanaku-ai/wanaku/releases))
  - **Java 21+** and **Maven 3.8+**
  - **Wanaku running** via `wanaku start local`
  - An existing Camel route (or use the example below)

  ## Step 1: Understanding Route Exposure

  Wanaku exposes Camel routes as MCP tools through the **Camel Integration Capability (CIC)**. You write a YAML rules file that maps tool names to Camel route URIs. When an AI agent calls a tool, the CIC executes the corresponding Camel route and returns the result.

  The workflow is:

  1. You have an existing Camel route (e.g., a REST call, a database query)
  2. You create a rules file that says "tool X maps to route endpoint Y"
  3. You package routes + rules into a Service Catalog
  4. You deploy the catalog and start the CIC

  ## Step 2: Example — Exposing a REST Route

  Suppose you have a Camel route that fetches exchange rates:

  ```yaml
  # routes/exchange-rate.yaml
  - route:
      id: exchange-rate-route
      from:
        uri: direct:getExchangeRate
      steps:
        - to:
            uri: https://api.exchangerate.host/latest?base={{header.base}}&symbols={{header.target}}
        - unmarshal:
            json: {}
  ```

  ## Step 3: Creating the Rules File

  Create a `rules.yaml` that exposes this route as a Wanaku tool:

  ```yaml
  # rules.yaml
  tools:
    - name: get-exchange-rate
      description: Get the exchange rate between two currencies
      uri: direct:getExchangeRate
      parameters:
        - name: base
          description: The base currency (e.g. USD)
          type: string
          required: true
        - name: target
          description: The target currency (e.g. EUR)
          type: string
          required: true
  ```

  ## Step 4: Packaging the Service Catalog

  Use the Wanaku CLI to package your routes and rules into a Service Catalog:

  ```bash
  wanaku service catalog package \
    --routes ./routes \
    --rules ./rules.yaml \
    --output exchange-rate-catalog.zip
  ```

  ## Step 5: Deploying the Catalog

  Deploy the catalog to your running Wanaku instance:

docs: add guide for demo 4.02 Exposing Existing Camel Routes  wanaku service catalog deploy \
    --file exchange-rate-catalog.zip \
    --name exchange-rate
  ```

  ## Step 6: Starting the Camel Integration Capability

  Start the CIC to load and execute the catalog:

  ```bash
  wanaku start local --services camel
  ```

  ## Step 7: Verifying the Tool

  Confirm the tool appears in the tool list:

  ```bash
  wanaku tool list
  ```
docs/demo-4.02-exposing-existing-routes-guide  You should see `get-exchange-rate` in the output. You can now invoke it from any MCP-compatible AI agent.

  ## What's Next?

  - [4.03 Building a Resource Provider](../4.03-building-a-resource-provider/README.md) — create a custom resource provider
  - [5.01 Camel Assistant](../5.01-camel-assistant/README.md) — a full assistant example using Camel

If you find a bug, please report it. To get in touch with the community, visit the [Wanaku project](https://github.com/wanaku-ai/wanaku).
