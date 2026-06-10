---
title: "Basic Wanaku Operations"
description: "Manage tools, resources, prompts, and forwards — the four core MCP primitives in Wanaku."
---

# Managing Tools, Resources, Prompts, and Forwards

You have Wanaku running. You imported your first tool. Now what? This guide walks you through the day-to-day operations: adding, listing, inspecting, and removing the four core primitives that Wanaku orchestrates.

**Tools** perform actions. **Resources** expose data. **Prompts** are reusable templates. **Forwards** federate external MCP servers. Together, they give AI agents a unified catalog of capabilities to work with.

## What You Will Learn

- How to manage tools manually and generate them from OpenAPI specs
- How to expose files and data sources as resources
- How to create and edit prompt templates with typed arguments
- How to aggregate external MCP servers through forwards

## What You Will Need

You must have completed [demo 1.01](../1.01-your-first-tool/README.md). That means:

- Wanaku CLI is installed
- `wanaku start local` is running
- You have imported at least one tool

Keep Wanaku running in a terminal. The commands below assume it is available at `http://localhost:8080`.

## 1. Tools Management

Tools are callable actions — HTTP endpoints, shell commands, or custom logic that an AI agent can invoke.

### Add a Tool Manually

Create a simple weather lookup tool:

```shell
wanaku tools add -n "get-weather" \
  --description "Get current weather for a city" \
  --uri "https://api.weatherapi.com/v1/current.json" \
  --type http \
  --property "q:string,The city name" \
  --required q
```

This registers a tool with:
- **Name**: `get-weather`
- **Type**: `http` (handled by the HTTP capability service)
- **URI**: the API endpoint
- **Property**: one required string parameter `q` (the city name)

> [!NOTE]
> For this example to work with a live API, you would typically need an API key. The syntax above shows the mechanics — swap in an API you have access to, or just use it to understand the structure.

### List All Tools

See what tools are currently available:

```shell
wanaku tools list
```

Output:

```
name                          namespace type uri                                                                                                       labels 
free-currency-conversion-tool default   http https://economia.awesomeapi.com.br/last/{parameter.value('fromCurrency')}-{parameter.value('toCurrency')} {}     
get-weather                   default   http https://api.weatherapi.com/v1/current.json                                                               {}     
```

### Show Tool Details

Inspect a specific tool to see its full definition:

```shell
wanaku tools show get-weather
```

This prints the complete tool metadata, including all properties, required fields, and the URI template.

### Import a Toolset

Rather than add tools one at a time, you can import a curated collection. For example, import a cryptocurrency toolset:

```shell
wanaku tools import https://raw.githubusercontent.com/wanaku-ai/wanaku-toolsets/refs/heads/main/toolsets/crypto.json
```

Toolsets group related tools under a common theme. Browse the [wanaku-toolsets](https://github.com/wanaku-ai/wanaku-toolsets) repository for more.

### Generate Tools from OpenAPI Specs

If you have an OpenAPI specification (local file or URL), Wanaku can generate tools from it automatically:

```shell
wanaku tools generate http://petstore3.swagger.io/api/v3/openapi.yaml
```

This scans the spec and creates one tool per endpoint. The generated tools respect the parameter schemas, descriptions, and required fields defined in the spec. It is the fastest way to make an entire REST API available to AI agents.

### Remove a Tool

When you no longer need a tool:

```shell
wanaku tools remove --name "get-weather"
```

The tool is removed from the catalog and will no longer appear to connected agents.

> [!TIP]
> Tools added manually or via import are stored in Wanaku's internal catalog. If you restart Wanaku in local mode, tools persist in the embedded database. To reset everything, stop Wanaku and delete the data directory (printed in the startup logs).

## 2. Resources Management

**Resources** are data sources that AI agents can read but not modify. Think of them as read-only endpoints: files, database query results, or API payloads. Unlike tools, which perform actions, resources simply return data.

### Add a Resource

Expose a local file as a resource:

```shell
wanaku resources add --name "project-readme" \
  --description "Project README file" \
  --type file \
  --location "/path/to/your/project/README.md" \
  --mime-type "text/markdown"
```

Replace `/path/to/your/project/README.md` with an actual file path on your system. This makes the file content available to AI agents through the MCP resource protocol.

Why does this matter? An agent can now say:

> Read the project README and summarize the setup steps.

And Wanaku hands back the file content without you having to paste it into the chat.

### List Resources

See all registered resources:

```shell
wanaku resources list
```

Output:

```
name            description           type uri                                   mimeType      
project-readme  Project README file   file /path/to/your/project/README.md      text/markdown 
```

### Show Resource Details

Inspect a specific resource:

```shell
wanaku resources show project-readme
```

This prints metadata including the resource type, location, and MIME type.

### Remove a Resource

When the resource is no longer needed:

```shell
wanaku resources remove --name "project-readme"
```

The resource is removed from the catalog.

> [!NOTE]
> Resource management is useful when you want AI agents to have on-demand access to documentation, configuration files, or query results without manually copying them into prompts.

## 3. Prompts Management

**Prompts** are reusable templates with typed arguments. Instead of writing the same instructions over and over, you define a prompt once, declare its inputs, and let the agent invoke it with different values.

### Add a Prompt

Create a code review prompt:

```shell
wanaku prompts add \
  --name "code-review" \
  --description "Review code for quality and best practices" \
  --message "user:text:Please review the following code for quality, security, and best practices: {{code}}" \
  --argument "code:The code snippet to review:true"
```

Breaking this down:

- **Name**: `code-review`
- **Description**: What the prompt does
- **Message**: The prompt template. `{{code}}` is a placeholder that gets replaced with the actual argument value.
- **Argument**: Declares a required string argument called `code`

The syntax for `--argument` is: `<name>:<description>:<required>`.  
The syntax for `--message` is: `<role>:<content-type>:<text>`.

Supported roles are `user`, `assistant`, and `system`. Content type is usually `text`.

### List Prompts

See all registered prompts:

```shell
wanaku prompts list
```

Output:

```
name        description                                        
code-review Review code for quality and best practices         
```

### Edit a Prompt

Update a prompt's description (or any other field):

```shell
wanaku prompts edit --name "code-review" \
  --description "Detailed code review focusing on security, performance, and maintainability"
```

This updates the existing prompt without recreating it.

### Remove a Prompt

When a prompt is no longer useful:

```shell
wanaku prompts remove --name "code-review"
```

> [!TIP]
> Prompts shine when you have standardized workflows. For example, a team might define prompts for "draft release notes," "analyze test coverage," or "explain this error message." Agents discover these prompts automatically and can invoke them with context-specific arguments.

## 4. Forwards (MCP Bridges)

**Forwards** let Wanaku aggregate multiple MCP servers. Instead of connecting your AI agent to five different MCP endpoints, you connect it to Wanaku, and Wanaku forwards requests to the right upstream server.

When you add a forward, all tools, resources, and prompts from the external MCP server appear in Wanaku's catalog alongside local ones. The agent sees a single, unified view.

### Add a Forward

Connect an external MCP server:

```shell
wanaku forwards add \
  --service="http://another-mcp-server:8080/mcp/sse" \
  --name my-external-server
```

Replace `http://another-mcp-server:8080/mcp/sse` with the actual URL of the external MCP server. The `--name` flag gives it a recognizable label.

After adding the forward, any tools or resources exposed by that server become available to agents connected to Wanaku.

### List Forwards

See all registered forward connections:

```shell
wanaku forwards list
```

Output:

```
name                service                                  
my-external-server  http://another-mcp-server:8080/mcp/sse   
```

### Remove a Forward

When you no longer need the external server:

```shell
wanaku forwards remove --name my-external-server
```

Tools and resources from that server are removed from the catalog immediately.

> [!NOTE]
> Forwards are powerful when you have multiple teams or services exposing MCP-compatible capabilities. Instead of managing N different MCP connections in your agent config, you manage one connection to Wanaku, and Wanaku handles the rest.

## What You Have Learned

You now know how to:

- Add, list, inspect, and remove **tools** — both manually and from OpenAPI specs
- Expose files and data as **resources** for AI agents to read
- Create reusable **prompts** with typed arguments
- Aggregate external MCP servers using **forwards**

These four primitives — tools, resources, prompts, forwards — are the building blocks of Wanaku. Mastering them means you can quickly assemble a rich capability catalog for any AI agent.

## What's Next?

Now that you can manage Wanaku's core primitives, it is time to learn about capabilities:

- [Introduction to Capabilities](../2.01-introduction-to-capabilities/README.md) (demo 2.01)

If you find a bug, please [report it](https://github.com/wanaku-ai/wanaku/issues).
To get in touch with the community, visit the [Wanaku project](https://github.com/wanaku-ai/wanaku).
