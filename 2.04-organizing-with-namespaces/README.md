# Organizing with Namespaces

This guide explains how Wanaku namespaces work and how to use them to isolate tools and resources across environments or teams.

## What You Will Learn

- What namespaces are and when to use them
- Creating and switching between namespaces
- Isolating tools and resources per namespace
- Listing and managing resources within a namespace

## What You Will Need

- **Wanaku CLI** installed (download from [releases page](https://github.com/wanaku-ai/wanaku/releases))
- **Wanaku running** via `wanaku start local` (see [Getting Started](../1.01-your-first-tool/README.md))

## Step 1: Understanding Namespaces

A **namespace** in Wanaku is a logical partition that isolates tools and resources. By default, every tool and resource you add belongs to the `default` namespace. Using namespaces you can:

- Keep development tools separate from production tools
- Allow different teams to manage their own tool sets without conflicts
- Switch context quickly without restarting Wanaku

Think of namespaces like separate workspaces — the router is the same, but each namespace has its own catalog of tools and resources.

## Step 2: Creating a Namespace

To create a new namespace, use the `wanaku namespace create` command:

```bash
wanaku namespace create --name dev
```

You should see output confirming the namespace was created:

```
Namespace 'dev' created successfully.
```

Create a second namespace for staging:

```bash
wanaku namespace create --name staging
```

## Step 3: Listing Namespaces

To see all available namespaces:

```bash
wanaku namespace list
```

Expected output:

```
NAME       CREATED
default    ...
dev        ...
staging    ...
```

## Step 4: Adding Tools to a Specific Namespace

When adding tools or resources, use the `--namespace` flag to target a specific namespace:

```bash
wanaku tool add \
  --name get-dev-config \
    --uri https://config.internal/dev \
      --description "Get dev environment configuration" \
        --namespace dev
        ```

        Add a different tool to the staging namespace:

        ```bash
        wanaku tool add \
          --name get-staging-config \
            --uri https://config.internal/staging \
              --description "Get staging environment configuration" \
                --namespace staging
                ```

                ## Step 5: Listing Resources Within a Namespace

                To list tools in a specific namespace:

                ```bash
                wanaku tool list --namespace dev
                ```

                Compare this to listing tools in staging:

                ```bash
                wanaku tool list --namespace staging
                ```

                Each namespace shows only the tools that belong to it.

                ## Step 6: Switching the Active Namespace

                You can set a default namespace for your CLI session so you don't have to specify `--namespace` on every command:

                ```bash
                wanaku namespace use dev
                ```

                After switching, commands like `wanaku tool list` and `wanaku tool add` will automatically target the `dev` namespace.

                To switch back to the default namespace:

                ```bash
                wanaku namespace use default
                ```

                ## When to Use Namespaces

                | Use Case | Recommendation |
                |----------|---------------|
                | Single developer, single project | Use the default namespace |
                | Multiple environments (dev/staging/prod) | Create one namespace per environment |
                | Multiple teams sharing one Wanaku instance | Create one namespace per team |
                | Temporary experiments | Create a throwaway namespace and delete it when done |

                ## What's Next?

                - [2.05 Aggregating MCP Servers with Forwards](../2.05-aggregating-mcp-servers-with-forwards/README.md) — combine multiple MCP servers into one endpoint
                - [3.01 Wanaku on the Cloud](../3.01-wanaku-on-the-cloud/README.md) — deploy Wanaku to Kubernetes

                If you find a bug, please report it. To get in touch with the community, visit the [Wanaku project](https://github.com/wanaku-ai/wanaku).
