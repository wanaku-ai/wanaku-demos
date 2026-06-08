# Understanding MCP and Wanaku

Welcome to the Wanaku demo series. Before writing any commands, this guide explains the concepts behind the Model Context Protocol (MCP) and how Wanaku fits into the picture.

> [!TIP]
> Prefer video? Watch [MCP in Action: Connecting AI to Enterprise Systems](https://www.youtube.com/watch?v=H82o1OcrZGg) by Zineb Bendhiba for a walkthrough of MCP in practice.

## What You Will Learn

- What MCP is and why it exists
- The three MCP primitives: tools, resources, and prompts
- How Wanaku extends MCP with routing, namespaces, and forwarding
- When to use Wanaku instead of a single MCP server

## No Setup Required

This is a conceptual guide. You do not need to install anything or run any commands. Read through it before moving on to [Demo 1.01: Getting Started](../1.01-your-first-tool/README.md).

## Step 1: What Is MCP?

The **Model Context Protocol (MCP)** is an open protocol that standardises how AI agents communicate with external tools and data sources. Think of it as a common language that lets an AI agent ask:

- *"Run this tool and give me the result"* — a **tool call**
- *"Fetch this piece of data for me"* — a **resource read**
- *"Give me a template I can fill in"* — a **prompt**

Before MCP existed, every AI application had to write its own integration layer. MCP removes that duplication by defining a single, reusable protocol.

## Step 2: The Three MCP Primitives

| Primitive | Purpose | Example |
|-----------|---------|---------|
| **Tool** | An action the agent can invoke | Call a REST API, run a shell script |
| **Resource** | A data source the agent can read | A file, a database row, an S3 object |
| **Prompt** | A reusable text template | A system prompt, a query template |

A single **MCP server** exposes one or more of these primitives. An AI agent connects to an MCP server and discovers what tools, resources, and prompts are available.

## Step 3: The Problem Wanaku Solves

A single MCP server works well for small use cases, but real deployments face three challenges:

**Multiple capability services.** You might have an HTTP capability, a database capability, and a Kafka capability each running as a separate service. Without a router, the agent would need to know about all of them.

**Namespace isolation.** In a team environment, the dev namespace and the production namespace should see different tools. Hardcoding that logic in every capability service is brittle.

**Forwarding.** An enterprise deployment might want to aggregate tools from several upstream MCP servers into a single endpoint. Without forwarding support, agents have to manage multiple connections.

## Step 4: How Wanaku Solves It

Wanaku acts as an **MCP router** that sits between your AI agent and your capability services.

The agent connects to one endpoint, the Wanaku Router. The router maintains a registry of capability services and forwards requests to the right one. Namespaces let you partition tools and resources without running separate routers. MCP Forwards let Wanaku aggregate tools from upstream MCP servers transparently.

## Step 5: When to Use Wanaku

| Scenario | Recommendation |
|----------|---------------|
| One agent, one tool | A single MCP server is enough |
| Multiple capability types (HTTP, exec, database) | Use Wanaku to route between them |
| Team with dev/staging/production isolation | Use Wanaku namespaces |
| Aggregating upstream MCP servers | Use Wanaku MCP Forwards |
| Kubernetes/OpenShift deployment | Use the Wanaku Operator |

## What's Next?

Now that you understand the concepts, move on to the hands-on guides:

- [1.01 Getting Started](../1.01-your-first-tool/README.md) — install the CLI, start Wanaku, and add your first tool
- [1.02 Basic Wanaku Operations](../1.02-basic-wanaku-operations/README.md) — day-to-day CLI operations

If you find a bug, please report it. To get in touch with the community, visit the [Wanaku project](https://github.com/wanaku-ai/wanaku).
