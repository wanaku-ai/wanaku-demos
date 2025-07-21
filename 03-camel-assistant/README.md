# Wanaku + Camel Assistant

## Pre-requisite: Ollama

We will run Ollama to serve the embedding model. Its job is to read our Apache Camel dataset and turn it into a numerical format that the AI can search through.

1. Install and run Ollama (or any other OpenAI compatible API in a host accessible from the containers).

The specific details may vary depending on your operating system. On macOS, it can be launched like any other [application after you download and install it](https://ollama.com/download/mac). On Linux, a [SystemD service may be created](https://github.com/ollama/ollama/blob/main/docs/linux.md). In any case, you should be
able to launch it with something as simple as:

```shell
ollama serve
```

> [IMPORTANT]
> On Linux, ollama may be managed by systemd. Depending on your system configuration, it may not be listening on the expected 
> port for this demo. In such case, you can stop the service (`sudo systemctl stop ollama`) and the launch it 
> using `OLLAMA_HOST=0.0.0.0:11434 ollama serve`

2. Pull the `nomic-embed-text:latest` model on the host you are running Ollama

```shell
ollama pull nomic-embed-text:latest
```

## 1\. Configure the data loader system to prepare the RAG environment


Let's get the command-line interface (CLI) you'll use to load data into the system. This tool is your starting point for getting your data where it needs to go.

[Download the Data Loader CLI (ZIP)](https://github.com/orpiske/camel-data-loader/releases/download/early-access/camel-data-loader-cli-1.0.0-SNAPSHOT.zip)

After downloading the file, unzip it to a location of your choice. You will see a new directory named camel-data-loader-cli-1.0.0-SNAPSHOT.

To ensure the tool is working correctly, open your terminal or command prompt, navigate into the unzipped directory, and run the command to view the help message.

#### **Linux & macOS**

```bash
# Navigate to the directory
cd camel-data-loader-cli-1.0.0-SNAPSHOT

# Make the script executable
./camel-data-loader-cli --help
```

-----

#### **Windows**

```powershell
# Navigate to the directory
cd camel-data-loader-cli-1.0.0-SNAPSHOT

# Run the help command to see available options
.\camel-data-loader-cli.bat --help
```

-----

## 2\. Start the Loader System

Now, let's bring the backend system online. This single command starts all the necessary services, like the database and APIs, in the background.

```shell
podman compose -f rag-database/docker-compose.yaml up -d
```

Once the command finishes, wait a moment (about 20 seconds) and the loader system will be running and ready to accept data.

## 3\. Download the Training Data

Next, let's download the [Camel Components dataset](https://huggingface.co/datasets/megacamelus/camel-components). This dataset contains specialized documentation that we'll load into our database and will provide additional context about Camel and its
components.

### Before You Begin

This step requires the **Hugging Face CLI**. If you don't have it installed, you can add it to your system by running:

```shell
pip install -U "huggingface_hub[cli]"
```

> [NOTE]
> You can find additional details about downloading datasets in these guides
> - How to install the [Hugging Face CLI](https://www.perplexity.ai/search/451721bf-3b96-4799-b488-4584c682653c)
> - [Datasets Downloading](https://huggingface.co/docs/hub/en/datasets-downloading)

### Download Command

Once the CLI is ready, run the following command to download the dataset. It will be saved to a local directory named `camel-components`.

```shell
huggingface-cli download --repo-type dataset --local-dir camel-components megacamelus/camel-components
```

> [IMPORTANT]
> HuggingFace may limit the number of requests from non-authenticated accounts, which may cause the download to be very slow 
> and/or fail at times. In this case, you can either try again after a while or authenticate.

## 4\. Load the Camel Data Set

With the system running and the dataset downloaded, it's time to load the data. We will use the `camel-data-loader-cli` tool for this step.

### Run the Loader

Execute the following command in your terminal. This command points the data loader to the dataset and starts the import process.

> **Important**: Replace `[PATH_TO_YOUR_DATASET]` with the actual path to the `camel-components` directory you downloaded earlier.

```shell
./camel-data-loader consume dataset \
  --path [PATH_TO_YOUR_DATASET]/camel-components/ \
  --source org.apache.camel
```

### Verify the Data

After the loader finishes, you can verify that the data was successfully added to the collection. The following command retrieves the first 50 records.

We pipe the output to `jq` to format the JSON response for readability.

```shell
curl --silent -X POST http://localhost:6333/collections/camel/points/scroll \
  -H "Content-Type: application/json" \
  -d '{"limit": 2}' | jq .
```

```json
{
  "result": {
    "points": [
      {
        "id": "00019b19-1297-4a70-a1c7-0c6c63bd2cf9",
        "payload": {
          "text_segment": "The description for the Salesforce component's \"reportId\" option in the common group is the Salesforce1 Analytics report Id. The default value is null, indicating it may not always be required. The data type of this option is string. The requirement status is set to false, suggesting it's optional."
        }
      },
      {
        "id": "002398c5-c8ae-4f9a-9cf9-8fde2e605505",
        "payload": {
          "text_segment": "The \"inheritExchangePropertiesForReplies\" option in the Apache Camel gRPC component is designed to copy exchange properties from the original exchange to all exchanges created for the route defined by streamRepliesTo. This means that any additional properties set on the original exchange will be propagated to replies. Its default value is false, indicating that exchange properties are not inherited unless explicitly specified. It's important to note that this option is not required when using the gRPC component as a producer; its usage depends on the specific use case and whether there's a need to propagate additional information in the reply exchanges."
        }
      }
    ],
    "next_page_offset": "003003bf-61f8-4c7c-af8f-cbf22fa3961f"
  },
  "status": "ok",
  "time": 0.00043896
}
```

If successful, you will see a JSON object containing the `points` you just loaded into the system.## Part 2: Run Wanaku


# 5\. Launch Wanaku:

With the data loaded, the next step is to launch the Wanaku MCP Router. This is a critical integration service that acts as a secure gateway between AI agents and your enterprise systems.

By managing how AI agents access data and tools, Wanaku ensures that all interactions are governed and secure. The following command starts the router services in the background.

```shell
podman compose -f wanaku/docker-compose.yaml up -d
```

> [IMPORTANT] 
> Please make sure to download the latest [Wanaku CLI from the releases page](https://github.com/wanaku-ai/wanaku/releases/). 

## 6\. Add Tools to Wanaku

Now, let's extend Wanaku's capabilities by adding some tools. The demo comes pre-configured for two tools: one for making HTTP calls and another for searching with DuckDuckGo. We'll start by adding the search tool.

### Add the DuckDuckGo Search Tool

This command registers a new tool with Wanaku that allows it to perform internet searches using DuckDuckGo.

```shell
wanaku tools add --name "duckduckgo-search" --description "Search on the internet using DuckDuckGo" --uri "duckduckgo://search" --type duckduckgo
```

-----

### Verify the Tool was Added

You can confirm the tool was successfully added by listing all available tools in the command line.

```shell
wanaku tools list
```

You should see the `duckduckgo-search` tool in the output:

```
name                type        uri
duckduckgo-search   duckduckgo  duckduckgo://search
```

Alternatively, you can view all registered tools in the Wanaku UI by navigating to **http://localhost:8080/\#/tools**.


# 7\. Launch MCP forward

Run the Camel Catalog MCP. This is an MCP (Model Context Protocol) server that provides tools for querying a Camel Catalog. It can be used to retrieve information about Apache Camel components, data formats, languages, and more.

To run this application, launch the container:

```
podman compose -f mcp-servers/docker-compose.yaml up -d
```

## 8\. Connect the Camel Catalog Tools

Now, let's make the tools from the **Camel Catalog MCP** server available within Wanaku. This command creates a "forward," allowing Wanaku to act as a proxy and expose all the catalog's tools as if they were its own. This centralizes all capabilities for the AI agents.

```shell
wanaku forwards add --name camel-catalog-mcp --service=http://host.docker.internal:8010/mcp/sse
```

-----

### Verify All Tools

To see the result, list the tools again. The output will now include both the DuckDuckGo tool you added manually and all the new remote tools from the Camel Catalog, which are essential for querying component information.

```shell
wanaku tools list
```

You should see an expanded list similar to this:

```
name                                  type              uri
duckduckgo-search                     duckduckgo        duckduckgo://search
getComponentURL                       mcp-remote-tool   <remote>
getDependency                         mcp-remote-tool   <remote>
getInformationAboutComponent          mcp-remote-tool   <remote>
getInformationAboutComponentOptions   mcp-remote-tool   <remote>
...and several others
```

## 8\. Launch the LangFlow Visual UI 🎨

Next, we'll start [LangFlow](https://www.langflow.org/), a visual platform for building AI applications. Its drag-and-drop interface simplifies creating and managing complex AI workflows without extensive coding. This is where you'll connect the tools and services you've already configured.

-----

### Start the LangFlow Service

This command launches the LangFlow application in the background.

```shell
podman compose -f langflow/docker-compose.yaml up -d
```

-----

### Create Your First Workflow

1.  Open your web browser and navigate to **http://localhost:7860/**.
2.  Click the button to **create your first workflow**.
3.  In the dialog that appears, select **"Blank Flow"** to get started.

## 9\. Configure the Camel Assistant Agent

It's time to bring everything together in **LangFlow**. In this section, we will upload and configure a pre-built AI agent designed to use the Wanaku router and the tools you've just set up.

### Step 9.1: Upload the Agent Workflow

First, let's load the pre-configured agent into your LangFlow workspace.

1.  Navigate to your LangFlow flows page at **http://localhost:7860/flows**. You can also click the LangFlow icon in the top-left corner.
2.  Click the **Upload** button, located to the right of the "Projects" label.
3.  Select the `camel-assistant.json` file located in the `langflow` directory of this demo.

After uploading, the agent workflow will load onto the canvas, looking like this:

### Step 9.2: Configure Global Settings

Next, we need to configure LangFlow's global settings to connect it to our running services.

1.  **Add the Wanaku MCP Server**

      * Navigate to the MCP Servers settings: **http://localhost:7860/settings/mcp-servers**.
      * Add a new server with the following parameters:
          * **Name**: `Wanaku MCP Router`
          * **SSE URL**: `http://host.docker.internal:8080/mcp/sse`

2.  **Set Global Variables and Credentials**

      * Navigate to the Global Variables settings: **http://localhost:7860/settings/global-variables**.
      * Add the following variables. These tell the agent where to find the vector database and the local AI model server.
          * **QDRANT\_URL**: `http://host.docker.internal`
          * **OLLAMA\_BASE\_URL**: `http://host.docker.internal:11434`
              * For this variable, set the **Apply to fields** dropdown to **"Ollama Base URL"**.
      * Add your Gemini API key as a credential.
          * **GEMINI\_API\_KEY**: `<your-google-api-key>`
              * For this credential, set the **Apply to fields** dropdown to **"Google API Key"**.

    > **Note**: This agent is pre-configured for Google Gemini. However, the workflow can be easily adapted to use other model APIs like Ollama or OpenAI.

### Step 9.3: Configure the Workflow Components

Now, return to the agent workflow canvas to connect the settings to the visual components.

1.  **Connect the MCP Tools**

      * Find the **MCP Tools** component on the canvas.
      * In its settings, select `wanaku_mcp_router` from the dropdown.
      * Ensure that **"Tool Mode"** is enabled.

      * **Troubleshooting**: If the actions appear as "Loading actions...", toggle the "Tool Mode" switch off and on again. This will force a refresh. You may need to re-link the tools to the agent component.

2.  **Configure the Qdrant Component**

      * Find the **Qdrant** component on the canvas.
      * In its settings, set the **URL** field to use the `QDRANT_URL` variable.

    > **Important**: Make sure you set the **URL** field, not the `Host` field.

3.  **Configure the Agent Model**

      * Find the main **Agent** component.
      * If you are using Google Gemini, ensure the **Model** is set to `gemini-1.5-flash` or another model with Function Calling capabilities (like `gemini-1.5-pro`).

### Step 9.4: Test the Agent

With the configuration complete, it's time to test the agent in the Playground.

1.  Click the **"Playground"** icon in the top right of the LangFlow interface. A chat window will appear.

2.  **First Test (Internal Knowledge)**

      * The playground includes a default question: "What is the default value for the secured option for jt400 for apache camel?"
      * Click **Send**. The agent will answer using its pre-trained knowledge and the data from the Qdrant vector store.

3.  **Second Test (Tool Usage)**

      * This time, ask the agent a question that requires an external tool, like an internet search. Type: `Search on the internet about the Apache Software Foundation`.
      * Click **Send**.
      * You should see the agent acknowledge its use of the `duckduckgo-search` tool and provide a summarized result from the web search.

4.  **Third Test (Forward Tool Usage)**

      * Now, ask the agent a question that requires a forwarded tool, such as something that can be provided by the [Camel Catalog MCP](http://github.com/orpiske/camel-catalog-mcp). Type: `What is the URL for the Kafka component for Apache Camel? `.
      * Click **Send**.
      * You should see the agent acknowledge its use of the `getComponentURL` tool and provide a response pointing to the [Kafka component page](https://camel.apache.org/components/4.10.x/kafka-component.html).


## Shutdown

After you have finished playing with the system, you can shutdown everything by running the following commands:

```shell
wanaku tools remove --name duckduckgo-search
wanaku forwards remove --name camel-catalog-mcp
podman compose -f langflow/docker-compose.yaml down
podman compose -f mcp-servers/docker-compose.yaml down
podman compose -f wanaku/docker-compose.yaml down
podman compose -f rag-database/docker-compose.yaml down
```


## Congratulations!

You've successfully reached the end of the demo. Well done!

You have now experienced the full workflow of setting up a sophisticated AI system, from launching backend services and loading data to configuring and testing a powerful, tool-enabled agent. You've seen how Wanaku can connect an AI to external tools and how LangFlow can be used to visually orchestrate complex tasks.

This is just the beginning. We encourage you to continue exploring. Try asking the agent more complex questions, adding new tools to Wanaku, or customizing the agent's behavior in the LangFlow UI.

Thank you for following along, and we hope you found this guide helpful.
