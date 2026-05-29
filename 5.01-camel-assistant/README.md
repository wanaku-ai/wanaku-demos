# Wanaku + Camel Assistant on OpenShift

This guide walks you through deploying a specialized Apache Camel assistant on OpenShift. It uses Retrieval-Augmented Generation (RAG) backed by a Camel dataset, Wanaku MCP Router as the tool provider, and LangFlow as the visual AI designer.

## What You Will Need

Ensure you have:

- **OpenShift cluster access** with permissions to create namespaces and deploy applications
- **`oc` CLI** installed and configured to access your cluster
- **`helm` CLI** installed (v3.x or later)
- **[Wanaku CLI](https://github.com/wanaku-ai/wanaku/releases/)** installed
- **[Hugging Face CLI](https://www.perplexity.ai/search/451721bf-3b96-4799-b488-4584c682653c)** for downloading training data

## Step 1: Create the Namespace

Create a dedicated namespace for this demo:

```shell
oc new-project camel-assistant
```

## Step 2: Deploy Ollama on OpenShift

Ollama serves the embedding model that turns the Apache Camel dataset into vectors the AI can search.

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

## Step 3: Configure the Data Loader System

Download and extract the data loader CLI:

[Download the Data Loader CLI (ZIP)](https://github.com/orpiske/camel-data-loader/releases/download/v1.1.0/camel-data-loader-cli-1.1.0.zip)

Verify the tool is working:

#### **Linux & macOS**

```shell
# Navigate to the directory
cd camel-data-loader-cli-1.1.0

# Make the script executable
./camel-data-loader-cli --help
```

#### **Windows**

```powershell
# Navigate to the directory
cd camel-data-loader-cli-1.1.0

# Run the help command to see available options
.\camel-data-loader-cli.bat --help
```

## Step 4: Deploy Qdrant (Vector Database)

Deploy the Qdrant vector database to store the embeddings.

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

## Step 5: Download the Training Data

Download the [Camel Components dataset](https://huggingface.co/datasets/megacamelus/camel-components). This dataset contains Apache Camel documentation that will be loaded into Qdrant to provide additional context to the AI agent.

### Before You Begin

This step requires the **Hugging Face CLI**. If you don't have it installed, you can add it to your system by running:

```shell
pip install -U "huggingface_hub[cli]"
```

> [!NOTE]
> You can find additional details about downloading datasets in these guides
> - How to install the [Hugging Face CLI](https://www.perplexity.ai/search/451721bf-3b96-4799-b488-4584c682653c)
> - [Datasets Downloading](https://huggingface.co/docs/hub/en/datasets-downloading)

### Download Command

Once the CLI is ready, run the following command to download the dataset. It will be saved to a local directory named `camel-components`.

```shell
huggingface-cli download --repo-type dataset --local-dir camel-components megacamelus/camel-components
```

> [!IMPORTANT]
> HuggingFace may limit the number of requests from non-authenticated accounts, which may cause the download to be very slow
> and/or fail at times. In this case, you can either try again after a while or authenticate.

## Step 6: Load the Camel Data Set

Load the downloaded dataset into Qdrant using the `camel-data-loader-cli`.

Get the Qdrant ingestion URL from the OpenShift route:

```shell
QDRANT_ROUTE=$(oc get route qdrant -n camel-assistant -o jsonpath='{.spec.host}')
export INGESTION_ADDRESS="http://${QDRANT_ROUTE}"
echo "Ingestion address: ${INGESTION_ADDRESS}"
```

Run the loader, replacing `[PATH_TO_YOUR_DATASET]` with the path to the `camel-components` directory you downloaded:

```shell
./camel-data-loader consume dataset \
  --address ${INGESTION_ADDRESS} \
  --path [PATH_TO_YOUR_DATASET]/camel-components/ \
  --source org.apache.camel
```

### Verify the Data

Verify that the data was loaded into the collection:

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

## Step 7: Launch Wanaku

With the data loaded, the next step is to launch the Wanaku MCP Router. This is a critical integration service that acts as a secure gateway between AI agents and your enterprise systems.

### Deploy Keycloak (Optional - for Authentication)

If you need authentication support, deploy Keycloak first:

```shell
oc apply -f wanaku/keycloak/keycloak.yaml -n camel-assistant
oc wait --for=condition=ready pod -l app=keycloak -n camel-assistant --timeout=300s
```

Once Keycloak is ready, download the Wanaku realm configuration and create the realm:

```shell
curl -sLO https://raw.githubusercontent.com/wanaku-ai/wanaku/wanaku-0.1.1/deploy/auth/wanaku-config.json

KEYCLOAK_HOST=$(oc get route keycloak -n camel-assistant -o jsonpath='{.spec.host}')

wanaku admin realm create \
  --keycloak-url "http://${KEYCLOAK_HOST}" \
  --admin-username admin \
  --admin-password \
  --config wanaku-config.json \
  --plain
```

Then, retrieve the OIDC client secret:

```shell
OIDC_SECRET=$(WANAKU_ADMIN_USERNAME=admin WANAKU_ADMIN_PASSWORD=admin \
  wanaku admin credentials show \
  --keycloak-url "http://${KEYCLOAK_HOST}" \
  --client-id wanaku-service \
  --show-secret --plain | cut -d ' ' -f 3)

echo "OIDC Secret: ${OIDC_SECRET}"
```

### Deploy the Wanaku Operator

Download the Helm chart from the Wanaku repository:

```shell
curl -sL https://github.com/wanaku-ai/wanaku/archive/refs/tags/wanaku-0.1.1.tar.gz | \
  tar xz --strip-components=4 wanaku-wanaku-0.1.1/apps/wanaku-operator/deploy/helm/wanaku-operator
```

Install the operator:

```shell
helm install wanaku-operator \
  ./wanaku-operator \
  --namespace camel-assistant \
  --set operatorNamespace=camel-assistant
```

Wait for it to become available:

```shell
oc wait deployment/wanaku-operator \
  --for=condition=Available \
  --timeout=120s \
  --namespace camel-assistant
```

## Step 8: Add Tools to Wanaku

Add a DuckDuckGo search tool so the agent can look up information on the internet.

```shell
wanaku tools add --name "duckduckgo-search" --description "Search on the internet using DuckDuckGo" --uri "duckduckgo://search" --type duckduckgo
```

### Verify the Tool was Added

Confirm the tool was added:

```shell
wanaku tools list
```

You should see the `duckduckgo-search` tool in the output:

```
name                type        uri
duckduckgo-search   duckduckgo  duckduckgo://search
```

You can also view all registered tools in the Wanaku UI:

```shell
WANAKU_ROUTE=$(oc get route wanaku -n camel-assistant -o jsonpath='{.spec.host}')
echo "Wanaku UI: http://${WANAKU_ROUTE}/#/tools"
```


## Step 9: Launch MCP Servers (Camel Catalog)

The Camel Catalog MCP server provides tools for querying Apache Camel components, data formats, and languages.

Deploy the Camel Catalog MCP server:

```shell
oc apply -f mcp-servers/camel-catalog-deployment.yaml -n camel-assistant
oc wait --for=condition=ready pod -l app=camel-catalog-mcp -n camel-assistant --timeout=300s
```

## Step 10: Connect the Camel Catalog Tools

Forward the Camel Catalog tools into Wanaku so it can expose them to AI agents:

```shell
wanaku forwards add --name camel-catalog-mcp --service=http://camel-catalog-mcp.camel-assistant.svc.cluster.local:8010/mcp/sse
```

### Verify All Tools

List the tools again to see both the DuckDuckGo tool and the forwarded Camel Catalog tools:

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

## Step 11: Launch the LangFlow Visual UI

[LangFlow](https://www.langflow.org/) is a visual platform for building AI workflows. This is where you'll connect the tools and services you've configured.

### Start the LangFlow Service

Deploy LangFlow and its PostgreSQL database:

```shell
oc apply -f langflow/persistent-volumes.yaml -n camel-assistant
oc apply -f langflow/secrets.yaml -n camel-assistant
oc apply -f langflow/postgres-deployment.yaml -n camel-assistant
oc wait --for=condition=ready pod -l app=postgres -n camel-assistant --timeout=300s
oc apply -f langflow/langflow-deployment.yaml -n camel-assistant
oc apply -f langflow/langflow-service.yaml -n camel-assistant
oc wait --for=condition=ready pod -l app=langflow -n camel-assistant --timeout=300s
```

### Get the LangFlow URL

```shell
LANGFLOW_ROUTE=$(oc get route langflow -n camel-assistant -o jsonpath='{.spec.host}')
echo "LangFlow URL: http://${LANGFLOW_ROUTE}"
```

### Create Your First Workflow

1.  Open your web browser and navigate to the LangFlow URL displayed above.
2.  Click the button to **create your first workflow**.
3.  In the dialog that appears, select **"Blank Flow"** to get started.

## Step 12: Configure the Camel Assistant Agent

Upload and configure the pre-built AI agent that uses the Wanaku router and the tools you've set up.

### Step 12.1: Upload the Agent Workflow

Load the pre-configured agent into your LangFlow workspace.

1.  Navigate to your LangFlow flows page (LangFlow URL + `/flows`). You can also click the LangFlow icon in the top-left corner.
2.  Click the **Upload** button, located to the right of the "Projects" label.
3.  Select the `camel-assistant.json` file located in the `langflow` directory of this demo.

After uploading, the agent workflow will load onto the canvas.

### Step 12.2: Configure Global Settings

Configure LangFlow's global settings to connect it to the running services on OpenShift.

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

Add the Wanaku MCP Router as a global MCP server in LangFlow.

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

Return to the agent workflow canvas to connect the settings to the visual components.

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

Test the agent in the Playground.

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

Remove all deployed resources:

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
helm uninstall wanaku-operator --namespace camel-assistant
oc delete -f wanaku/keycloak/keycloak.yaml -n camel-assistant

# Delete Qdrant
oc delete -f rag-database/qdrant.yaml -n camel-assistant

# Delete Ollama
oc delete -f ollama/ollama.yaml -n camel-assistant

# Optionally delete the namespace
oc delete project camel-assistant
```


## What's Next?

You now have a working AI assistant backed by Apache Camel documentation, internet search, and Camel Catalog tools. From here you can:

- Add more tools to Wanaku and see the agent pick them up automatically
- Customize the agent's behavior in the LangFlow UI
- Review the [Wanaku on the Cloud](../3.01-wanaku-on-the-cloud/README.md) guide for a production deployment with authentication

If you find a bug, please [report it](https://github.com/wanaku-ai/wanaku/issues).
To get in touch with the community, visit the [Wanaku project](https://github.com/wanaku-ai/wanaku).
