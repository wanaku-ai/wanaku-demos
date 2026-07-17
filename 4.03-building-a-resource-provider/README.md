---
title: "Building a Resource Provider"
description: "Create a custom file resource provider for the Wanaku MCP Router using the Java Capabilities SDK."
---

# Building a Resource Provider for Wanaku

This guide walks you through creating a resource provider for Wanaku. Like tools, resources are registered as a service that Wanaku can interact with — but the role is different. A tool is something an AI agent _invokes_ to perform an action. A resource is something the agent _reads_ to get information during a conversation.

This demo builds a simple file resource provider: when Wanaku requests a resource, the provider reads a file from disk and returns its content, demonstrating how to implement the resource side of the Wanaku Capabilities SDK.

## What You Will Learn

- The difference between tool invokers and resource providers in Wanaku
- How to build a resource provider using the Java Capabilities SDK
- How the gRPC resource protocol works (request/response patterns)
- How to register and test a resource provider locally

## What You Will Need

- **Wanaku CLI** installed (download from [releases page](https://github.com/wanaku-ai/wanaku/releases/tag/v0.2.0))
- **Wanaku running locally** via `wanaku start local`
- **Java 21+**
- **Apache Maven 3.8+**
- **An IDE** of your choice (e.g., VS Code, IntelliJ IDEA)

## Step 1: Generate the Project

Use the `wanaku capabilities create resource` command to scaffold a new resource provider project. This generates the correct structure out of the box — no need to start from a tool archetype and adapt it manually.

```shell
wanaku capabilities create resource \
  --name file-resource \
  --type quarkus \
  --path .
```

> [!IMPORTANT]
> The `--type quarkus` option produces the smallest scaffold. If you prefer a Camel-backed resource provider, pass `--type camel` instead.

This creates a project named `wanaku-provider-file-resource` in the current directory. The generated project includes:

- `pom.xml` with the Wanaku resource provider dependencies
- `FileResourceConsumer.java` — where you implement the resource logic
- `FileResourceDelegate.java` — handles URI construction and response coercion

## Step 2: Implementing the Resource Provider

The generated `FileResourceConsumer` is the only file you need to customize. It implements the `ResourceConsumer` interface, which has a single method: `consume(uri, request)`. The `uri` parameter carries the resource URI from Wanaku, and `request` provides the full `ResourceRequest` message.

### Customize `FileResourceConsumer`

In `src/main/java/<your-package>/FileResourceConsumer.java`, replace the placeholder logic with a simple file reader:

```java
package ai.wanaku.provider.file;

import ai.wanaku.core.capabilities.provider.ResourceConsumer;
import ai.wanaku.core.exchange.v1.ResourceRequest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class FileResourceConsumer implements ResourceConsumer {

  @Override
  public Object consume(String uri, ResourceRequest request) {
    try {
      return Files.readString(Path.of(uri.substring("file://".length())));
    } catch (IOException e) {
      return "Error reading resource: " + e.getMessage();
    }
  }
}
```

> [!NOTE]
> The `ResourceConsumer.consume` method receives the resource URI as a plain string (e.g., `file:///tmp/wanaku-resource.txt`), not as a `ToolInvokeRequest` with `wanaku_body` and `wanaku_meta_*` arguments. This is the fundamental difference between the tool and resource protocols in Wanaku.

> [!TIP]
> The generated `FileResourceDelegate.java` does not need changes for this demo. It already constructs the target URI from the incoming request and coerces the response returned by the consumer into the gRPC wire format.

> [!NOTE]
> This implementation reads files using a `file://` URI scheme that is stripped and passed directly to `Path.of()`. Only valid filesystem paths reachable from the service will work. In production, you would add input validation and access controls.

## Step 3: Packaging the Application

Build the project to create the executable JAR:

```shell
mvn clean package
```

You should now find the final artifact at `target/quarkus-app/quarkus-run.jar`.

## Step 4: Running Against a Local No-Auth Router

The generated project enables OIDC authentication by default. Because `wanaku start local` runs without authentication (no Keycloak), the capability will fail to start until you disable OIDC.

> [!IMPORTANT]
> The Quarkus augmentation change below persists until the next `mvn clean package`, so you only need to repeat it after a fresh build.

1. **Disable OIDC** Re-augment the project to turn off the OIDC client.

```shell
java -Dquarkus.launch.rebuild=true -Dquarkus.oidc-client.enabled=false \
  -jar target/quarkus-app/quarkus-run.jar
```

2. **Create a test resource file** Create the file that your resource provider will serve.

```shell
echo "Hello from the File Resource Provider!" > /tmp/wanaku-resource.txt
```

3. **Launch the resource provider** Run the re-augmented JAR, pointing it at the local Wanaku router (already running via `wanaku start local`).

```shell
java \
  -Dwanaku.service.registration.uri=http://localhost:8080 \
  -Dquarkus.grpc.server.port=9191 \
  -jar target/quarkus-app/quarkus-run.jar
```

## Step 5: Verifying the Resource Provider

With the provider running, verify it end to end with the Wanaku CLI.

1. **Check registration** After a few seconds, verify that your capability has registered successfully.

```shell
wanaku capabilities list
```

2. **Expose a resource** Create a resource in Wanaku that uses your new capability.

```shell
wanaku resources expose \
  --name "file-resource" \
  --description "Reads content from a local file." \
  --location "file:///tmp/wanaku-resource.txt" \
  --mimeType "text/plain" \
  --type file-resource
```

> [!IMPORTANT]
> The `--type` value must match the capability service name you chose when you generated the project (the value of `--name` in Step 1).

3. **Verify the resource registration** Use `wanaku resources list` to confirm that the resource is registered.

```shell
wanaku resources list
```

4. **Read the resource** Resources are consumed via an MCP client. Open the MCP Inspector to connect to your Wanaku router and request the `file-resource` resource. It should return the text you stored in the file.

```shell
npx @modelcontextprotocol/inspector
```

## What's Next?

- [Camel Assistant](../5.01-camel-assistant/README.md) (demo 5.01) — build an AI assistant backed by Apache Camel documentation and Wanaku tools

If you find a bug, please [report it](https://github.com/wanaku-ai/wanaku/issues).
To get in touch with the community, visit the [Wanaku project](https://github.com/wanaku-ai/wanaku).
