# Getting Started with Wanaku

Requirements: 

1. Podman or Docker 
2. An LLM endpoint (can be locally hosted, like Ollama)

## Download Wanaku CLI (CLI) - Optional

The CLI can be used to manage the Wanaku router. You can download the CLI from the [releases page](https://github.com/wanaku-ai/wanaku/releases).

The CLI is distributed in two flavors: 

* Native binaries for Linux and macOS
* Java-based binaries for all operating systems

For instance, to get the Wanaku CLI for macOS:

```shell
wget https://github.com/wanaku-ai/wanaku/releases/download/v0.0.4/cli-0.0.4-osx-aarch_64.zip
unzip cli-0.0.4-osx-aarch_64.zip
install -m 750 cli-0.0.4-osx-aarch_64/bin/cli $HOME/bin/wanaku
rm -rf cli-0.0.4-osx-aarch_64 cli-0.0.4-osx-aarch_64.zip
```

> [!NOTE]
> Make sure to adjust this according to your OS. 

Check if it was installed successfully:

```shell
wanaku --version
```

Expected result:
```
Wanaku CLI version 0.0.4
Usage: wanaku [-hv] [COMMAND]
  -h, --help      Display the help and sub-commands
  -v, --version   Display the current version of Wanaku CLI
Commands:
  forwards   Manage forwards
  resources  Manage resources
  start      Start Wanaku
  services   Manage services
  targets    Manage targets
  tools      Manage tools
  toolset    Manage toolsets
```

## Checking the Environment

Check if podman is available (adjust the command if you use docker).

```shell
podman ps
```

Check if docker compose is available. 

```shell
docker-compose --version 
```

Expected result: 

```
Docker Compose version v2.19.1
```

**NOTE**: the docker-compose version may differ, but that is OK.

## Launching Wanaku MCP Router from the Docker Compose file

Download the [`docker-compose.yml`](./docker-compose.yml) file for this getting started and save it to any directory on your computer.

Launch the containers.

```shell
docker-compose up -d
```

**NOTE**: Wanaku is composed of several different services that will automatically register themselves, expose configurations and perform other initializations tasks, therefore, it may take a couple of seconds for it to start.

Check if the containers have launched.

```
podman ps
```

Expected result: 

```shell
CONTAINER ID  IMAGE                                                       COMMAND     CREATED         STATUS         PORTS                   NAMES
ada51fc96d6b  quay.io/wanaku/wanaku-provider-ftp:wanaku-0.0.4                         50 seconds ago  Up 50 seconds  0.0.0.0:9004->9000/tcp  01-getting-started-wanaku-provider-ftp-1
bcb57c4665ad  quay.io/wanaku/wanaku-tool-service-http:wanaku-0.0.4                    50 seconds ago  Up 50 seconds  0.0.0.0:9000->9000/tcp  01-getting-started-wanaku-tool-service-http-1
bc649001189a  quay.io/wanaku/wanaku-provider-s3:wanaku-0.0.4                          50 seconds ago  Up 50 seconds  0.0.0.0:9005->9000/tcp  01-getting-started-wanaku-provider-s3-1
86b7649c0d92  quay.io/wanaku/wanaku-tool-service-tavily:wanaku-0.0.4                  50 seconds ago  Up 50 seconds  0.0.0.0:9006->9000/tcp  01-getting-started-wanaku-tool-service-tavily-1
9ff41b970f1b  quay.io/wanaku/wanaku-tool-service-kafka:wanaku-0.0.4                   50 seconds ago  Up 50 seconds  0.0.0.0:9003->9000/tcp  01-getting-started-wanaku-tool-service-kafka-1
ff1bc724b8ea  quay.io/wanaku/wanaku-provider-file:wanaku-0.0.4                        50 seconds ago  Up 50 seconds  0.0.0.0:9002->9000/tcp  01-getting-started-wanaku-provider-file-1
899320498a0e  quay.io/wanaku/wanaku-tool-service-yaml-route:wanaku-0.0.4              50 seconds ago  Up 50 seconds  0.0.0.0:9001->9000/tcp  01-getting-started-wanaku-tool-service-yaml-route-1
068e6fdcdcd3  quay.io/wanaku/wanaku-router:wanaku-0.0.4                               50 seconds ago  Up 50 seconds  0.0.0.0:8080->8080/tcp  01-getting-started-wanaku-router-1
```

Now, check if the services registered themselves, so that Wanaku can find them: 

```shell
wanaku targets tools list
```

Expected result: 

```
Service                 Target                            Configurations
camel-yaml           => 10.89.5.49:9000                =>
kafka                => 10.89.5.53:9000                => bootstrapHost, replyToTopic
http                 => 10.89.5.48:9000                =>
tavily               => 10.89.5.54:9000                =>
```

This means that Wanaku is fully up and running, and the downstream services registered themselves with the router.

At this point, you can open the UI in your browser. Wanaku listens at http://localhost:8080/ by default. 

## Importing a ToolSet

### Importing a ToolSet on the Web Interface

Copy the contents of the [currency](https://raw.githubusercontent.com/wanaku-ai/wanaku-toolsets/refs/tags/wanaku-0.0.4/toolsets/currency.json) toolset. 

Then, on the Wanaku UI, navigate to the tools (i.e.; http://localhost:8080/#/tools) page. There, click on Import Toolset 
and paste the contents on the form. You should see the new tools added to Wanaku.

You can visit the [wanaku-toolsets](https://github.com/wanaku-ai/wanaku-toolsets) repository
for more toolsets.

## Importing a ToolSet via Command Line Interface

Add the currency toolset from the [wanaku-toolsets](https://github.com/wanaku-ai/wanaku-toolsets) repository:

```shell
wanaku tools import https://raw.githubusercontent.com/wanaku-ai/wanaku-toolsets/refs/tags/wanaku-0.0.4/toolsets/currency.json
```

To check if it worked, you can run `wanaku tools list` to list the tools added to the router:

```shell
free-currency-conversion-tool => http            => https://economia.awesomeapi.com.br/last/{fromCurrency}-{toCurrency}
```

## Using Wanaku with an Agent

You can use Wanaku with any tool that supports the Model Context Protocol (MCP) via the HTTP SSE protocol. Any agent with MCP capabilities configured to access that server, will be able to list and use tools registered on Wanaku. You can find many MCP clients [here](https://github.com/punkpeye/awesome-mcp-clients). We have tested Wanaku with: 
* LibreChat
* HyperChat
* Witsy

At this point, you need to configure the client of your choice so that it points to where Wanaku is listening to on. By 
default, the MCP server is available on http://localhost:8080/mcp/sse.

After that, check if the client is correctly configured and capable of calling MCP tools. To do so, ask it a question 
such as: 

```What is today's currency conversion rate from 1 euro to dollar?```

Without any configuration, a model would provide you a response similar to this: 

```
However, I'm a large language model, I don't have real-time access to current market data. But I can suggest some ways for you to find the latest exchange rates.

You can check online sources.
```

However, if everything is working well, then the response should look like this:

```
Based on the tool call response, today's currency conversion rate from 1 euro to dollar is approximately 1.03738 USD per EUR.

Please note that this rate may fluctuate constantly due to various market and economic factors, so it's always best to check the current rate for accuracy.
```

**NOTE**: the response will, of course, vary as the currency rates vary with time.

If something doesn't work, try inspecting the container logs by running commands such as `podman logs -f demo-wanaku-router-1`.

If you find a bug, don't hesitate to [report it](https://github.com/wanaku-ai/wanaku/issues).
If you would like to improve something, [reach out to the community](https://github.com/wanaku-ai/wanaku).