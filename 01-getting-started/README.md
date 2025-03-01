
# Getting Started with Wanaku

Requirements: 

1. Podman or Docker 
2. An LLM endpoint (can be locally hosted, like Ollama)

## Download Wanaku CLI (UI)

## Download Wanaku CLI (CLI)

This is optional.

```shell
wget https://github.com/wanaku-ai/wanaku/releases/download/v0.0.1/cli-0.0.1-osx-aarch_64.zip
unzip cli-0.0.1-osx-aarch_64.zip
install -m 750 cli-0.0.1-osx-aarch_64/bin/cli $HOME/bin/wanaku
rm -rf cli-0.0.1-osx-aarch_64 cli-0.0.1-osx-aarch_64.zip
```

```shell
wanaku --help
```

## Checking the Environment

Check if podman is available (adjust the command if you use docker).

```shell
podman ps
```

## Launching Wanaku MCP Router from the Docker Compose file

```shell
wget -c https://raw.githubusercontent.com/wanaku-ai/wanaku/refs/tags/wanaku-0.0.1/docker-compose.yml
```

Check what is being launched.

```shell
cat docker-compose.yml
```

Launch the containers.

```shell
docker-compose up -d
```

Check if the containers have launched.

```
podman ps
```

Open http://localhost:8080/

Add the currency toolset from
https://github.com/wanaku-ai/wanaku-toolsets/blob/main/toolsets/currency.json

```shell
wanaku targets tools link --service=http --target=host.docker.internal:9000
```

Wanaku MCP server is available on http://localhost:8080/mcp/sse. Any agent with MCP capabilities configured to access that server, will be able to list and use tools registered on Wanaku.

At this point, you need to configure a client that has MCP capabilities. On this demo, I am going to use a local LibreChat instance configure to access a LLM hosted on my server. 

First, let's ask the model without configuring an agent: 

```What is today's currency conversion rate from 1 euro to dollar?```

Without any configuration, a model would provide you a response similar to this: 

```
However, I'm a large language model, I don't have real-time access to current market data. But I can suggest some ways for you to find the latest exchange rates.

You can check online sources.
```

Now, let's try using it with an agent configured

```What is today's currency conversion rate from 1 euro to dollar?```

The response will, of course, vary as the currency rates vary with time, but you should receive a response similar to this:

```
Based on the tool call response, today's currency conversion rate from 1 euro to dollar is approximately 1.03738 USD per EUR.

Please note that this rate may fluctuate constantly due to various market and economic factors, so it's always best to check the current rate for accuracy.
```