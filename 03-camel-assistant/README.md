# Wanaku + Camel Assistant on OpenShift

Build a powerful AI Agent that knows everything about Apache Camel! In this comprehensive tutorial,
we'll show you how to go beyond generic chatbots and create a specialized assistant using Retrieval-Augmented Generation (RAG),
Wanaku MCP Router as the tool provider and LangFlow as the Visual AI Designer.

## Prerequisites

Before starting this tutorial, ensure you have:

- **OpenShift cluster access** with permissions to create namespaces and deploy applications
- **`oc` CLI** installed and configured to access your cluster
- **`helm` CLI** installed (v3.x or later)
- **[Wanaku CLI](https://github.com/wanaku-ai/wanaku/releases/)** installed
- **[Hugging Face CLI](https://www.perplexity.ai/search/451721bf-3b96-4799-b488-4584c682653c)** for downloading training data

## 1\. Create the Namespace

First, create a dedicated namespace for this demo:

```shell
oc new-project camel-assistant
```

## 2\. Deploy Ollama on OpenShift

We will run Ollama to serve the embedding model. Its job is to read our Apache Camel dataset and turn it into a numerical format that the AI can search through.

### Deploy Ollama

```shell
oc apply -f ollama/ollama.yaml -n camel-assistant
```

### Wait for Ollama to be Ready

```shell
oc wait --for=condition=ready pod -l app=ollama -n camel-assistant --timeout=300s
```

### Pull the Embedding Model

Once the pod is running, pull the `nomic-embed-text:latest` model:

```shell
oc exec deploy/ollama -n camel-assistant -- ollama pull nomic-embed-text:latest
```

## 3\. Configure the Data Loader System

Let's get the command-line interface (CLI) you'll use to load data into the system. This tool is your starting point for getting your data where it needs to go.

[Download the Data Loader CLI (ZIP)](https://github.com/orpiske/camel-data-loader/releases/download/v1.1.0/camel-data-loader-cli-1.1.0.zip)

After downloading the file, unzip it to a location of your choice. You will see a new directory named camel-data-loader-cli-1.1.0.

To ensure the tool is working correctly, open your terminal or command prompt, navigate into the unzipped directory, and run the command to view the help message.

#### **Linux & macOS**

```bash
# Navigate to the directory
cd camel-data-loader-cli-1.1.0

# Make the script executable
./camel-data-loader-cli --help
```

-----

#### **Windows**

```powershell
# Navigate to the directory
cd camel-data-loader-cli-1.1.0

# Run the help command to see available options
.\camel-data-loader-cli.bat --help
```

-----

## 4\. Deploy Qdrant (Vector Database)

Now, let's deploy the Qdrant vector database to store our embeddings.

```shell
oc apply -f rag-database/qdrant.yaml -n camel-assistant
```

Wait for Qdrant to be ready:

```shell
oc wait --for=condition=ready pod -l app=qdrant -n camel-assistant --timeout=300s
```

Get the Qdrant route URL for external access:

```shell
QDRANT_ROUTE=$(oc get route qdrant -n camel-assistant -o jsonpath='{.spec.host}')
echo "Qdrant URL: http://${QDRANT_ROUTE}"
```

## 5\. Download the Training Data

Next, let's download the [Camel Components dataset](https://huggingface.co/datasets/megacamelus/camel-components). This dataset contains specialized documentation that we'll load into our database and will provide additional context about Camel and its components.

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

## 6\. Load the Camel Data Set

With the system running and the dataset downloaded, it's time to load the data. We will use the `camel-data-loader-cli` tool for this step.

### Run the Loader

First, get the Qdrant ingestion URL from the OpenShift route:

```shell
QDRANT_ROUTE=$(oc get route qdrant -n camel-assistant -o jsonpath='{.spec.host}')
export INGESTION_ADDRESS="http://${QDRANT_ROUTE}"
echo "Ingestion address: ${INGESTION_ADDRESS}"
```

Execute the following command in your terminal. This command points the data loader to the dataset and the Qdrant instance on OpenShift.

> **Important**: Replace `[PATH_TO_YOUR_DATASET]` with the actual path to the `camel-components` directory you downloaded earlier.

```shell
./camel-data-loader consume dataset \
  --address ${INGESTION_ADDRESS} \
  --path [PATH_TO_YOUR_DATASET]/camel-components/ \
  --source org.apache.camel
```

### Verify the Data

After the loader finishes, you can verify that the data was successfully added to the collection. Use the Qdrant route URL to access it externally:

```shell
QDRANT_ROUTE=$(oc get route qdrant -n camel-assistant -o jsonpath='{.spec.host}')
curl --silent -X POST http://${QDRANT_ROUTE}/collections/camel/points/scroll \
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

If successful, you will see a JSON object containing the `points` you just loaded into the system.

## 7\. Launch Wanaku

With the data loaded, the next step is to launch the Wanaku MCP Router. This is a critical integration service that acts as a secure gateway between AI agents and your enterprise systems.

### Deploy Keycloak (Optional - for Authentication)

If you need authentication support, deploy Keycloak first:

```shell
cd wanaku/keycloak
./deploy.sh
```

To configure authentication (optional):

```shell
./configure-auth.sh
```

### Deploy Wanaku Operator

Deploy the Wanaku operator using the provided script:

```shell
cd wanaku
./deploy.sh
```

> [IMPORTANT]
> Please make sure to download the latest [Wanaku CLI from the releases page](https://github.com/wanaku-ai/wanaku/releases/).

## 8\. Add Tools to Wanaku

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

Alternatively, you can view all registered tools in the Wanaku UI by getting the route:

```shell
WANAKU_ROUTE=$(oc get route wanaku -n camel-assistant -o jsonpath='{.spec.host}')
echo "Wanaku UI: http://${WANAKU_ROUTE}/#/tools"
```


## 9\. Launch MCP Servers (Camel Catalog)

Run the Camel Catalog MCP. This is an MCP (Model Context Protocol) server that provides tools for querying a Camel Catalog. It can be used to retrieve information about Apache Camel components, data formats, languages, and more.

Deploy the Camel Catalog MCP server:

```shell
cd mcp-servers
./deploy.sh
```

Or apply the deployment directly:

```shell
oc apply -f mcp-servers/camel-catalog-deployment.yaml -n camel-assistant
```

## 10\. Connect the Camel Catalog Tools

Now, let's make the tools from the **Camel Catalog MCP** server available within Wanaku. This command creates a "forward," allowing Wanaku to act as a proxy and expose all the catalog's tools as if they were its own. This centralizes all capabilities for the AI agents.

```shell
wanaku forwards add --name camel-catalog-mcp --service=http://camel-catalog-mcp.camel-assistant.svc.cluster.local:8010/mcp/sse
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

## 11\. Launch the LangFlow Visual UI

Next, we'll start [LangFlow](https://www.langflow.org/), a visual platform for building AI applications. Its drag-and-drop interface simplifies creating and managing complex AI workflows without extensive coding. This is where you'll connect the tools and services you've already configured.

-----

### Start the LangFlow Service

Deploy LangFlow using the provided script:

```shell
cd langflow
./deploy.sh
```

-----

### Get the LangFlow URL

```shell
LANGFLOW_ROUTE=$(oc get route langflow -n camel-assistant -o jsonpath='{.spec.host}')
echo "LangFlow URL: http://${LANGFLOW_ROUTE}"
```

### Create Your First Workflow

1.  Open your web browser and navigate to the LangFlow URL displayed above.
2.  Click the button to **create your first workflow**.
3.  In the dialog that appears, select **"Blank Flow"** to get started.

## 12\. Configure the Camel Assistant Agent

It's time to bring everything together in **LangFlow**. In this section, we will upload and configure a pre-built AI agent designed to use the Wanaku router and the tools you've just set up.

### Step 12.1: Upload the Agent Workflow

First, let's load the pre-configured agent into your LangFlow workspace.

1.  Navigate to your LangFlow flows page (LangFlow URL + `/flows`). You can also click the LangFlow icon in the top-left corner.
2.  Click the **Upload** button, located to the right of the "Projects" label.
3.  Select the `camel-assistant.json` file located in the `langflow` directory of this demo.

After uploading, the agent workflow will load onto the canvas.

### Step 12.2: Configure Global Settings

Next, we need to configure LangFlow's global settings to connect it to our running services on OpenShift.

1.  **Set Global Variables and Credentials**

      * Navigate to the Global Variables settings (LangFlow URL + `/settings/global-variables`).
      * Add the following variables. These tell the agent where to find the vector database and the local AI model server.
          * **QDRANT\_URL**: `http://qdrant.camel-assistant.svc.cluster.local:6333`
          * **OLLAMA\_BASE\_URL**: `http://ollama.camel-assistant.svc.cluster.local:11434`
              * For this variable, set the **Apply to fields** dropdown to **"Ollama Base URL"**.
      * Add your Gemini API key as a credential.
          * **GEMINI\_API\_KEY**: `<your-google-api-key>`
              * For this credential, set the **Apply to fields** dropdown to **"Google API Key"**.

    > **Note**: This agent is pre-configured for Google Gemini. However, the workflow can be easily adapted to use other model APIs like Ollama or OpenAI.

### Step 12.3: Configure the MCP Server

Then, we need to make sure we have a global MCP server added.

1.  **Add the Wanaku MCP Server**

    * Navigate to the MCP Servers settings (LangFlow URL + `/settings/mcp-servers`).
    * Get the Wanaku route first:
      ```shell
      WANAKU_ROUTE=$(oc get route wanaku -n camel-assistant -o jsonpath='{.spec.host}')
      echo "Wanaku SSE URL: http://${WANAKU_ROUTE}/mcp/sse"
      ```
    * Add a new server with the following parameters:
        * **Name**: `Wanaku MCP Router`
        * **SSE URL**: Use the URL displayed above (e.g., `http://wanaku-camel-assistant.apps.<cluster-domain>/mcp/sse`)


### Step 12.4: Configure the Workflow Components

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
      * If you are using Google Gemini, ensure the **Model** is set to `gemini-2.5-flash` or another model with Function Calling capabilities (like `gemini-2.5-pro`).

### Step 12.5: Test the Agent

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
# Remove tools and forwards from Wanaku
wanaku tools remove --name duckduckgo-search
wanaku forwards remove --name camel-catalog-mcp

# Delete LangFlow
oc delete -f langflow/langflow-service.yaml -n camel-assistant
oc delete -f langflow/langflow-deployment.yaml -n camel-assistant
oc delete -f langflow/postgres-deployment.yaml -n camel-assistant
oc delete -f langflow/secrets.yaml -n camel-assistant
oc delete -f langflow/persistent-volumes.yaml -n camel-assistant

# Delete MCP Servers
oc delete -f mcp-servers/camel-catalog-deployment.yaml -n camel-assistant

# Undeploy Wanaku
cd wanaku
./undeploy.sh

# Delete Qdrant
oc delete -f rag-database/qdrant.yaml -n camel-assistant

# Delete Ollama
oc delete -f ollama/ollama.yaml -n camel-assistant

# Optionally delete the namespace
oc delete project camel-assistant
```


## Congratulations!

You've successfully reached the end of the demo. Well done!

You have now experienced the full workflow of setting up a sophisticated AI system on OpenShift, from deploying services and loading data to configuring and testing a powerful, tool-enabled agent. You've seen how Wanaku can connect an AI to external tools and how LangFlow can be used to visually orchestrate complex tasks.

This is just the beginning. We encourage you to continue exploring. Try asking the agent more complex questions, adding new tools to Wanaku, or customizing the agent's behavior in the LangFlow UI.

Thank you for following along, and we hope you found this guide helpful.
