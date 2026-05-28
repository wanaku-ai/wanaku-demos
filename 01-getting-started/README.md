# Getting Started with Wanaku

Welcome to Wanaku! In this guide you will install the Wanaku CLI, start a local instance, add a tool, and
use it from an AI agent. By the end you will have a working MCP router that connects your agent to live
data — no containers, no complex setup.

## What You Will Need

1. Java 21 or later
2. An LLM endpoint (can be locally hosted with [Ollama](https://ollama.ai), or a cloud provider)

## Step 1: Install the Wanaku CLI

Download the CLI from the [releases page](https://github.com/wanaku-ai/wanaku/releases/tag/v0.1.1).

The CLI ships in two flavors:

* **Native binaries** for Linux (x86_64) and macOS (AArch64) — no Java required
* **Java-based archive** that runs on any OS with Java 21+

For example, on macOS:

```shell
wget https://github.com/wanaku-ai/wanaku/releases/download/v0.1.1/wanaku-cli-0.1.1-osx-aarch_64.zip
unzip wanaku-cli-0.1.1-osx-aarch_64.zip
install -m 750 wanaku-cli-0.1.1-osx-aarch_64/bin/wanaku $HOME/bin/wanaku
rm -rf wanaku-cli-0.1.1-osx-aarch_64 wanaku-cli-0.1.1-osx-aarch_64.zip
```

> [!NOTE]
> Adjust the file name for your OS. On Linux, use the `linux-x86_64` variant. If you prefer the
> Java-based archive, download `wanaku-cli-0.1.1.zip` instead.

Verify the installation:

```shell
wanaku --version
```

## Step 2: Start Wanaku Locally

This single command downloads the router and the built-in capability services, then starts everything
on your machine:

```shell
wanaku start local
```

Authentication is disabled automatically in local mode, so there is nothing else to configure.

After a few seconds, open <http://localhost:8080> in your browser. You should see the Wanaku UI.

> [!TIP]
> You can enable additional capability services with the `--services` option. Run
> `wanaku start local --help` to see what is available.

## Step 3: Check That Everything Is Running

Use the CLI to verify that the capability services registered with the router:

```shell
wanaku targets tools list
```

You should see output similar to:

```
Service                 Target                            Configurations
http                 => localhost:9000                  =>
```

This means Wanaku is up and running, and the HTTP capability service is ready to handle tool calls.

## Step 4: Add a Tool

Wanaku uses *toolsets* to group related tools together. Let's import a currency conversion toolset so your
agent can look up exchange rates.

### Option A: Via the Web UI

1. Open <http://localhost:8080/#/tools>
2. Click **Import Toolset**
3. Paste the contents of the [currency toolset](https://raw.githubusercontent.com/wanaku-ai/wanaku-toolsets/refs/heads/main/toolsets/currency.json)

### Option B: Via the CLI

```shell
wanaku tools import https://raw.githubusercontent.com/wanaku-ai/wanaku-toolsets/refs/heads/main/toolsets/currency.json
```

Confirm the tool was added:

```shell
wanaku tools list
```

Expected output:

```
free-currency-conversion-tool => http            => https://economia.awesomeapi.com.br/last/{fromCurrency}-{toCurrency}
```

You can find more toolsets in the [wanaku-toolsets](https://github.com/wanaku-ai/wanaku-toolsets) repository.

## Step 5: Connect an AI Agent

Wanaku speaks the [Model Context Protocol](https://modelcontextprotocol.io) (MCP) over HTTP. Any MCP-compatible
client can connect to it. A few clients we have tested:

* [LibreChat](https://www.librechat.ai/)
* [HyperChat](https://github.com/BigSweetPotatoStudio/HyperChat)
* [Witsy](https://github.com/nicbarker/witsy)

Point your client's MCP server URL to:

```
http://localhost:8080/mcp/sse
```

Then ask it a question like:

> What is today's currency conversion rate from 1 euro to dollar?

Without Wanaku, a typical LLM would answer:

> I don't have real-time access to current market data. You can check online sources.

With Wanaku connected, the agent calls the currency tool and responds with live data:

> Based on the tool call response, today's conversion rate is approximately 1.0374 USD per EUR.

The exact rate will vary — that is the point. Your agent is now using real, live data.

## What's Next?

You now have a working Wanaku instance with a real tool. From here you can:

* Browse the [wanaku-toolsets](https://github.com/wanaku-ai/wanaku-toolsets) repository for more tools
* Try the [Camel JBang demo](../02-running-camel-jbang/README.md) to connect custom integrations
* Build your own capability service with the [Java Capabilities SDK](../04-plain-java-services/README.md)

If something does not work, check the logs printed by `wanaku start local` in your terminal.

If you find a bug, please [report it](https://github.com/wanaku-ai/wanaku/issues).
To get in touch with the community, visit the [Wanaku project](https://github.com/wanaku-ai/wanaku).
