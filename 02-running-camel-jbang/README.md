# Connect Wanaku with Custom Camel JBang Tools

This guide provides instructions for integrating custom Apache Camel routes, executed via Camel JBang, as tools within the 
Wanaku MCP Router. 

This powerful feature allows the AI agent to execute complex, predefined integrations based on natural language prompts, 
effectively extending its capabilities.

The process involves setting up the Wanaku service, defining a Camel route, and registering that route as an executable tool that the AI can invoke.

### Step 1: Set Up and Run the Wanaku Service

First, the local Wanaku environment must be prepared.

1.  **Download the Wanaku CLI.**
    The CLI is available from the project's GitHub releases page. The Java binaries are suitable for this guide.

    **[Download Wanaku CLI v0.0.6](https://github.com/wanaku-ai/wanaku/releases/download/v0.0.6/cli-0.0.6.zip)**

    > [\!IMPORTANT]
    > Native executables for various operating systems are also available on the main [Releases Page](https://github.com/wanaku-ai/wanaku/releases).

2.  **Configure the System PATH.**
    Extract the archive and add the path to its `bin` directory to your system's `PATH` environment variable. This allows the `wanaku` command to be executed from any directory in the terminal.

3.  **Start the Local Service.**
    Execute the following command to initialize the Wanaku instance:

    ```bash
    wanaku start local
    ```

    This command starts the core Wanaku router along with default services for HTTP requests and file system access.

    > [\!NOTE]
    > To view all available startup services, you can use the `--list-services` flag. Additional services can be enabled via the `--services` flag to customize the instance.

#### Step 2: Prepare the Camel JBang Environment

The tool to be integrated will be executed by Camel JBang. Ensure it is installed and configured correctly in your development environment.

For installation instructions, please refer to the **[official Camel JBang documentation](https://camel.apache.org/manual/camel-jbang.html)**.

### Step 3: Define the Camel Route

The core of the new tool is an Apache Camel route. This route defines the specific integration or task the AI will be able to execute.

1.  **Design the route.** The [Kaoto](https://kaoto.io/) visual designer is a recommended tool for this purpose.
2.  **Save the route definition** to a YAML file (e.g., `laptop-order.camel.yaml`).

> [\!IMPORTANT]
> The route's output must be directed to standard output (`stdout`). The data printed to `stdout` is what will be passed back to the AI model as the result of the tool's execution.

Below is an example of a simple route that generates a JSON object and logs it. The `log` component, by default, writes to `stdout`.

```yaml
# File: ${HOME}/routes/laptop-order.camel.yaml
- route:
    id: route-3104
    from:
      id: from-4035
      uri: timer
      parameters:
        fixedRate: true
        repeatCount: "1"
        timerName: start
      steps:
        - setBody:
            expression:
              simple:
                expression: '{"number": "12354", "text": "Laptop order 12354 created successfully"}'
        - log:
            id: log-2526
            message: ${body}
```

> [\!IMPORTANT]
> To prevent extraneous logging from interfering with the model's ability to parse the result, it is critical to manage the 
> output stream. Use the `--logging-level` argument (e.g., `--logging-level=OFF`) in the execution command to suppress unwanted Camel logs.



#### Step 4: Register the Camel Route as a Wanaku Tool

With the route defined, the final step is to register it with Wanaku so the AI model becomes aware of its existence and purpose.

Use the `wanaku tools add` command with the following parameters:

* `-n`: A unique name for the tool.
* `--description`: A clear, natural language description of the tool's function. The AI uses this description to determine when to use the tool.
* `--uri`: The full command-line instruction to execute the tool.
* `--type`: The tool type, which should be `exec` for command-line executions.

> [NOTE]
> The URI represents the command that will be executed when the model decides to invoke that function.

The following command registers the example route as a tool:

```bash
wanaku tools add \
  -n "laptop-order" \
  --description "Use this tool to issue a new laptop order" \
  --uri "${HOME}/.jbang/bin/camel run --max-messages=1 --logging-level=OFF ${HOME}/routes/laptop-order.camel.yaml" \
  --type exec
```

#### Step 5: Verification

The integration is now complete. To verify its operation, issue a natural language prompt to agent using Wanaku that corresponds to the tool's description.

For example, you can now instruct the agent:

`"Please order a new laptop for me."`

The LLM will be responsible for analyzing the request, match it to the description of the `laptop-order` tool, and request 
Wanaku to execute the tool (which involves running the specified Camel JBang command). 

The output from the route will then be returned as the result of the action.

Alternatively, you can also launch the MCP Inspector and trigger that manually: 

```shell
npx @modelcontextprotocol/inspector
```