---
title: "Organizing with Namespaces"
description: "Use Wanaku namespaces to isolate tools and resources across environments or teams."
---

# Organizing with Namespaces

This guide explains how Wanaku namespaces work and how to use them to isolate tools and resources across environments or teams.

## What You Will Learn

- What namespaces are and when to use them
- Creating and managing namespaces
- Isolating tools and resources per namespace
- Listing and managing resources within a namespace

## What You Will Need

- **Wanaku CLI** installed (download from [releases page](https://github.com/wanaku-ai/wanaku/releases))
- **Wanaku running** via `wanaku start local` (see [Getting Started](../1.01-your-first-tool/README.md))

## Step 1: Understanding namespaces

A **namespace** in Wanaku is a logical partition that isolates tools and resources. By default, every tool and resource you add belongs to the `default` namespace. Using namespaces you can:

- Keep development tools separate from production tools
- Allow different teams to manage their own tool sets without conflicts
- Organize tools by environment or team without restarting Wanaku

Think of namespaces like separate workspaces — the router is the same, but each namespace has its own catalog of tools and resources.

## Step 2: Creating a namespace

To create a new namespace, use the `wanaku namespaces create` command:

```shell
wanaku namespaces create --path /dev --name dev
```

You should see output confirming the namespace was created.

Create a second namespace for staging:

```shell
wanaku namespaces create --path /staging --name staging
```

## Step 3: Listing namespaces

To see all available namespaces:

```shell
wanaku namespaces list
```

Expected output:

```text
NAME       CREATED
default    ...
dev        ...
staging    ...
```

## Step 4: Adding tools to a specific namespace

When adding tools, use the `-N` / `--namespace` flag to target a specific namespace:

```shell
wanaku tools add \
  --name get-dev-config \
  --uri https://config.internal/dev \
  --description "Get dev environment configuration" \
  --type http \
  --namespace dev
```

Add a different tool to the staging namespace:

```shell
wanaku tools add \
  --name get-staging-config \
  --uri https://config.internal/staging \
  --description "Get staging environment configuration" \
  --type http \
  --namespace staging
```

## Step 5: Listing tools and their namespaces

To see all registered tools along with the namespace each one belongs to:

```shell
wanaku tools list
```

The output includes a `namespace` column so you can identify which tools belong to which namespace.

## When to use namespaces

| Use Case | Recommendation |
|----------|---------------|
| Single developer, single project | Use the default namespace |
| Multiple environments (dev/staging/prod) | Create one namespace per environment |
| Multiple teams sharing one Wanaku instance | Create one namespace per team |
| Temporary experiments | Create a throwaway namespace and delete it when done |

## What's Next?

- [2.05 Aggregating MCP Servers with Forwards](../2.05-aggregating-mcp-servers-with-forwards/README.md) — combine multiple MCP servers into one endpoint
- [3.01 Wanaku on the Cloud](../3.01-wanaku-on-the-cloud/README.md) — deploy Wanaku to Kubernetes

If you find a bug, please [report it](https://github.com/wanaku-ai/wanaku/issues).
To get in touch with the community, visit the [Wanaku project](https://github.com/wanaku-ai/wanaku).
