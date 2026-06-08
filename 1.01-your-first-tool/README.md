# Getting Started with Wanaku

Welcome to Wanaku! In this guide you will install the Wanaku CLI, start a local instance, add a tool, and
use it from an AI agent. By the end you will have a working MCP router that connects your agent to live
data — no containers, no complex setup.

## What You Will Need

1. Java 21 or later
2. An agent that supports the Model Context Protocol (MCP). 

> [!TIP]
> If you don't know what MCP is, read our [intro to MCP and Wanaku](../1.00-understanding-mcp-and-wanaku/README.md).

## Step 1: Install the Wanaku CLI

The CLI ships as **native binaries** for Linux (x86_64) and macOS (AArch64), or as a **Java-based archive**
that runs on any OS with Java 21+. Pick the tab that matches your system:

::: code-group

```shell [Quick Install]
curl -sSL https://wanaku.ai/get-wanaku.sh | bash
```

```shell [macOS (AArch64)]
wget https://github.com/wanaku-ai/wanaku/releases/download/v0.1.3/wanaku-cli-0.1.3-osx-aarch_64.zip
unzip wanaku-cli-0.1.3-osx-aarch_64.zip
install -m 750 wanaku-cli-0.1.3-osx-aarch_64/bin/wanaku wanaku-cli-0.1.3-osx-aarch_64/bin/wanaku-cli $HOME/bin/
rm -rf wanaku-cli-0.1.3-osx-aarch_64 wanaku-cli-0.1.3-osx-aarch_64.zip
```

```shell [Linux (x86_64)]
wget https://github.com/wanaku-ai/wanaku/releases/download/v0.1.3/wanaku-cli-0.1.3-linux-x86_64.zip
unzip wanaku-cli-0.1.3-linux-x86_64.zip
install -m 750 wanaku-cli-0.1.3-linux-x86_64/bin/wanaku wanaku-cli-0.1.3-linux-x86_64/bin/wanaku-cli $HOME/bin/
rm -rf wanaku-cli-0.1.3-linux-x86_64 wanaku-cli-0.1.3-linux-x86_64.zip
```

```shell [Windows / Other]
# Requires Java 21+
wget https://github.com/wanaku-ai/wanaku/releases/download/v0.1.3/wanaku-cli-0.1.3.zip
unzip wanaku-cli-0.1.3.zip
# Add the extracted bin/ directory to your PATH
```

:::

### PATH Configuration

If you installed the CLI with `get-wanaku.sh`, it is placed in `$HOME/bin`. Some shells do not include `$HOME/bin` in the default `PATH`, so you may need to add it manually before `wanaku` is available as a command.

To add `$HOME/bin` to your `PATH` for the current session:

```shell
export PATH="$HOME/bin:$PATH"
```

To make this permanent, add the same line to your shell profile file, such as `~/.bashrc` or `~/.zshrc`, and then reload the shell.

> [!TIP]
> The **Quick Install** script auto-detects your OS and architecture, downloads the latest release,
> verifies the checksum, and installs to `$HOME/bin`. You can override the install directory with
> `WANAKU_INSTALL_DIR=/usr/local/bin`.

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
wanaku capabilities list
```

You should see output similar to:

```
service serviceType  host      port status lastSeen
http    tool-invoker 127.0.0.1 9000 active Wed, May 28, 2026 at 10:00:00
```

This means Wanaku is up and running, and the HTTP capability service is ready to handle tool calls.

## Step 4: Add a Tool

Wanaku uses *toolsets* to group related tools together. Let's import a currency conversion toolset so your
agent can look up exchange rates.

:::tabs
== Via the Web UI

1. Open <http://localhost:8080/admin/#/service-catalog>
2. Click **Toolsets Repositories Tab**
3. Expand the **wanaku-toolsets** one.
4. Import the **currency** one.

== Via the CLI

Import the toolset:

```shell
wanaku tools import https://raw.githubusercontent.com/wanaku-ai/wanaku-toolsets/refs/heads/main/toolsets/currency.json
```

Confirm the tool was added:

```shell
wanaku tools list
```

Expected output:

```
name                          namespace type uri                                                                                                       labels 
free-currency-conversion-tool default   http https://economia.awesomeapi.com.br/last/{parameter.value('fromCurrency')}-{parameter.value('toCurrency')} {}     
```

:::

## Step 5: Connect an AI Agent

Wanaku speaks the [Model Context Protocol](https://modelcontextprotocol.io) (MCP) over HTTP. Any MCP-compatible
client can connect to it. How you do that will depend on the AI agent you are using.

:::tabs
== Claude Desktop
On [Claude Desktop](https://claude.com/download) you can run Wanaku's command `wanaku configure claude`
== Claude Code
For [Claude Code](https://claude.com/product/claude-code) run `claude mcp add wanaku --transport sse http://localhost:8080/mcp/sse`
== Cursor
On [Cursor](https://cursor.com/) you can run Wanaku's command `wanaku configure cursor`
== IBM Bob
The details for [IBM Bob](https://bob.ibm.com/) are [here](https://bob.ibm.com/docs/shell/configuration/mcp/mcp-bobshell).
== Others
For any other client, `http://localhost:8080/mcp/sse` for SSE or `http://localhost:8080/mcp/` for Streamable HTTP.
:::

## Step 6: Testing

Then ask it a question like:

> How much is 100 USD in CZK?

![Claude call example](claude-call.png)

## What's Next?

You now have a working Wanaku instance with a real tool. Next, learn how to manage tools, resources, prompts, and forwards:

- [Managing Tools, Resources, Prompts, and Forwards](../1.02-basic-wanaku-operations/README.md) (demo 1.02)

If something does not work, check the logs printed by `wanaku start local` in your terminal.

If you find a bug, please [report it](https://github.com/wanaku-ai/wanaku/issues).
To get in touch with the community, visit the [Wanaku project](https://github.com/wanaku-ai/wanaku).
