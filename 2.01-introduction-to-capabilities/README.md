---
title: "Introduction to Capabilities"
description: "Understand how capabilities work in Wanaku — built-in HTTP, Exec, and the Camel Integration Capability."
---

# Introduction to Capabilities

This guide explains what capabilities are in Wanaku, how they work, and what ships out-of-the-box versus what you can extend.

## What You Will Learn

- What capabilities are and how they differ from the router
- The two types of capabilities: tool invokers and resource providers
- What built-in capabilities Wanaku ships with
- How the Camel Integration Capability extends Wanaku for enterprise integration
- Where to find ready-made example capabilities

## What You Will Need

- **Wanaku CLI** installed (download from [releases page](https://github.com/wanaku-ai/wanaku/releases/tag/v0.1.3))
- **Wanaku running** via `wanaku start local` (see [Getting Started](../1.01-your-first-tool/README.md))

## Step 1: Understanding Capabilities

In Wanaku, **capabilities** are standalone services that perform the actual work behind MCP tools and resources. The Wanaku Router itself doesn't execute anything — it routes requests to capability services over gRPC.

When an AI agent invokes a tool or reads a resource through the router, here's what happens:

1. The router receives the MCP request
2. The router looks up which capability can handle that tool or resource
3. The router forwards the request to that capability service via gRPC
4. The capability executes the work (makes an HTTP call, runs a shell command, queries a database, etc.)
5. The capability sends the response back to the router
6. The router returns the response to the AI agent

Each capability service registers itself with the router at startup and advertises what it can handle. Multiple capabilities can run simultaneously, each handling a different type of work.

### Two Types of Capabilities

Wanaku defines two capability types:

**Tool invokers**: Handle MCP tool calls. When you define a tool with a URI like `http://api.example.com/endpoint` or `exec://run-script.sh`, the router forwards the invocation to a tool invoker capability that knows how to execute that URI scheme.

**Resource providers**: Handle MCP resource reads. When an AI agent requests a resource like `file://config.json` or `s3://bucket/key`, the router forwards the request to a resource provider capability that knows how to fetch that resource.

## Step 2: Built-in Capabilities

Wanaku ships with two built-in tool capabilities. The HTTP capability starts automatically when you run `wanaku start local`; the Exec capability must be enabled explicitly:

### HTTP Capability

URI scheme: `http://` or `https://`

The HTTP capability makes HTTP requests to external APIs. When you add a tool with an `http://` or `https://` URI, the router forwards the call to this capability, which performs the HTTP request and returns the response. This is what powered the currency converter tool from the Getting Started guide (1.01).

### Exec Capability

URI scheme: `exec://`

The Exec capability executes local shell commands. Tools with the `exec://` URI scheme are routed here. Useful for running scripts, CLI tools, or system commands that an AI agent needs to invoke.

> [!NOTE]
> By default, `wanaku start local` only starts the HTTP capability. To also start the Exec capability, use:
> ```shell
> wanaku start local --services exec
> ```

### Checking Which Capabilities Are Running

You can see which capabilities are currently registered with the router:

```shell
wanaku capabilities list
```

If you started Wanaku with the default settings, you'll see:

```
service serviceType  host      port status lastSeen
http    tool-invoker 127.0.0.1 9000 active Wed, May 28, 2026 at 10:00:00
```

If you started with `--services exec`, you'll see both:

```
service serviceType  host      port  status lastSeen
exec    tool-invoker 127.0.0.1 9001  active Wed, May 28, 2026 at 10:00:00
http    tool-invoker 127.0.0.1 9000  active Wed, May 28, 2026 at 10:00:00
```

## Step 3: The Camel Integration Capability

The built-in HTTP and Exec capabilities cover simple use cases, but enterprise integration requires more: databases, message queues, cloud services, file systems, REST APIs, and everything in between. That's where the **Camel Integration Capability (CIC)** comes in.

The CIC lets you define integrations as Apache Camel routes (written in YAML) and expose them as MCP tools. It leverages Camel's ecosystem of 300+ connectors, so you can integrate with Kafka, PostgreSQL, AWS S3, Salesforce, SAP, and just about any other system without writing custom Java code.

Here's how it works:

1. You write Camel routes in YAML and define a rules file that exposes those routes as MCP tools
2. You package the routes and rules into a **Service Catalog** and deploy it to the Wanaku router
3. You run the CIC as a capability service (either locally or on Kubernetes/OpenShift)
4. The CIC registers with the router, downloads the Service Catalog, and starts executing your Camel routes
5. AI agents can now invoke your routes as tools through the router

### When to Use the CIC

Use the Camel Integration Capability when you need to:

- Connect to databases, message queues, or cloud services
- Transform or enrich data before sending it somewhere
- Orchestrate multi-step workflows (call API A, process the result, write to database B)
- Integrate with enterprise systems (SAP, Salesforce, Oracle, etc.)
- Build reusable integration templates that multiple AI agents can share

### Where to Learn More

The CIC is covered in depth in two guides:

- [Service Catalogs](../2.02-service-catalogs/README.md) (next guide in this chapter) — hands-on walkthrough of creating and deploying Camel routes locally using `wanaku start local`
- [Camel Integration Capability](../3.02-camel-integration-capability/README.md) — deploying the CIC on Kubernetes/OpenShift for production

## Step 4: Custom Capabilities and Wanaku Examples

Beyond the built-in capabilities and the CIC, you can build your own capability services from scratch using the **Wanaku Capabilities SDK**. This is useful when you need custom logic that doesn't fit into a Camel route, or when you want to wrap an existing Java library or service as an MCP tool or resource provider.

The SDK lets you:

- Build tool invoker capabilities that handle custom URI schemes
- Build resource provider capabilities that fetch data from custom sources
- Expose existing Java services as MCP capabilities without rewriting them

### Ready-Made Examples

The [wanaku-examples](https://github.com/wanaku-ai/wanaku-examples) repository contains reference implementations built with the SDK:

**Tool examples:**
- DuckDuckGo search
- Jira integration
- Kafka producer/consumer
- AWS SQS integration
- Telegram bot

**Resource provider examples:**
- File system reader
- FTP client
- AWS S3 reader

> [!TIP]
> These examples are aimed at developers who want to extend Wanaku with custom capabilities. They are reference implementations, not production-ready services. Use them to understand the SDK patterns, then build your own.

### Where to Learn More

Chapter 4 covers building custom capabilities from scratch:

- [Building a Java Capability](../4.01-plain-java-capability/README.md) — step-by-step guide to building a tool invoker capability
- [Exposing Existing Camel Routes](../4.02-exposing-existing-routes/README.md) — wrapping existing Camel routes as capabilities

## What's Next?

- [Service Catalogs](../2.02-service-catalogs/README.md) (demo 2.02) — next guide in this chapter; hands-on with creating and deploying Camel routes locally

If you find a bug, please [report it](https://github.com/wanaku-ai/wanaku/issues).
To get in touch with the community, visit the [Wanaku project](https://github.com/wanaku-ai/wanaku).
