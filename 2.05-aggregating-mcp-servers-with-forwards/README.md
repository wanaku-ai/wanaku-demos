# Aggregating MCP Servers with Forwards

This guide explains how Wanaku MCP Forwards work and how to use them to expose tools from upstream MCP servers through a single Wanaku router endpoint.

## What You Will Learn

- What MCP Forwards are and why they are useful
- Configuring a forward to an upstream MCP server
- Verifying that forwarded tools appear in the router
- Managing and removing forwards

## What You Will Need

- **Wanaku CLI** installed (download from [releases page](https://github.com/wanaku-ai/wanaku/releases))
- **Wanaku running** via `wanaku start local` (see [Getting Started](../1.01-your-first-tool/README.md))
- An upstream MCP server to forward to (for this demo we use a public test server)

## Step 1: Understanding MCP Forwards

A **forward** tells the Wanaku router to proxy MCP requests to another MCP server. From an AI agent's perspective, the forwarded tools look identical to locally registered tools — the agent connects to Wanaku and sees a unified list of tools, regardless of how many upstream servers there are.

This is useful when you:

- Want to aggregate tools from multiple team-owned MCP servers into a single endpoint
- Need to expose a third-party MCP server to your agents without giving them direct access
- Are migrating from multiple MCP servers to a Wanaku-managed deployment gradually

## Step 2: Adding a Forward

To add a forward to an upstream MCP server, use `wanaku forward add`:

```bash
wanaku forward add \
  --name upstream-weather \
    --uri http://weather-mcp-server.example.com:8080
    ```

    The `--name` is a human-readable label for this forward. The `--uri` is the address of the upstream MCP server.

    You should see:

    ```
    Forward 'upstream-weather' added successfully.
    ```

    ## Step 3: Verifying the Forward

    List the current forwards to confirm it was added:

    ```bash
    wanaku forward list
    ```

    Expected output:

    ```
    NAME               URI                                        STATUS
    upstream-weather   http://weather-mcp-server.example.com:8080  active
    ```

    ## Step 4: Checking Forwarded Tools

    When a forward is active, the tools from the upstream server appear alongside locally registered tools:

    ```bash
    wanaku tool list
    ```

    Tools from the upstream MCP server will appear with a `[forwarded]` indicator in the output, showing they originate from the `upstream-weather` forward.

    ## Step 5: Removing a Forward

    To remove a forward:

    ```bash
    wanaku forward remove --name upstream-weather
    ```

    After removal, the forwarded tools will no longer appear in `wanaku tool list`.

    ## When to Use Forwards

    | Use Case | Recommendation |
    |----------|---------------|
    | Single MCP server with all tools | No forwards needed |
    | Multiple team-owned MCP servers | Add a forward per upstream server |
    | Third-party MCP servers | Use forwards to proxy and control access |
    | Gradual migration from many servers to one | Use forwards during the transition |

    ## What's Next?

    - [3.01 Wanaku on the Cloud](../3.01-wanaku-on-the-cloud/README.md) — deploy Wanaku to Kubernetes or OpenShift
    - [3.02 Camel Integration Capability](../3.02-camel-integration-capability/README.md) — advanced enterprise integrations

    If you find a bug, please report it. To get in touch with the community, visit the [Wanaku project](https://github.com/wanaku-ai/wanaku).
