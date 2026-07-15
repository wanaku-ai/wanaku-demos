---
title: "Creating Custom Capabilities"
description: "Use `wanaku capabilities create` to scaffold new tool and resource capability services."
---

# Creating Custom Capabilities

This guide shows how to generate new Wanaku capability services from the built-in archetypes and where to customize the
generated code. It covers both tool invokers and resource providers.

## What You Will Learn

- How `wanaku capabilities create` scaffolds a new capability project
- How the generated tool and resource projects differ
- Which generated classes you usually customize first
- How to build and register the new capability with a local Wanaku router

## What You Will Need

- **Wanaku CLI** installed
- **Wanaku running locally** via `wanaku start local`
- **Maven** and a JDK compatible with the generated project

## Step 1: Generate a tool capability

Create a new tool capability using the `tool` subcommand:

```shell
wanaku capabilities create tool \
  --name weather \
  --type quarkus \
  --path ./demo-capabilities
```

This creates a project named `wanaku-tool-service-weather` inside `./demo-capabilities/`.

This demo uses `--type quarkus` because it produces the smallest scaffold. If you want the Camel-backed variant, pass
`--type camel` instead.

The generated project includes:

- `pom.xml` with the Wanaku capability SDK dependencies
- `WeatherDelegate.java` for response coercion
- `WeatherClient.java` for the tool implementation
- `src/main/resources/application.properties` for capability configuration

For this demo, the generated `WeatherClient` is the only file we need to customize.
The example implementation in this chapter lives in
[tool-weather/src/main/java/ai/wanaku/tool/weather/WeatherClient.java](tool-weather/src/main/java/ai/wanaku/tool/weather/WeatherClient.java).

## Step 2: Customize the tool logic

The generated client already parses the request for you. Replace the `TODO` logic with a simple response so you can
verify the end-to-end flow without any external dependency.

```java
package ai.wanaku.tool.weather;

import ai.wanaku.capabilities.sdk.config.provider.api.ConfigResource;
import ai.wanaku.core.capabilities.common.ParsedToolInvokeRequest;
import ai.wanaku.core.capabilities.tool.Client;
import ai.wanaku.core.exchange.v1.ToolInvokeRequest;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

@ApplicationScoped
public class WeatherClient implements Client {
    private static final Logger LOG = Logger.getLogger(WeatherClient.class);

    @Override
    public Object exchange(ToolInvokeRequest request, ConfigResource configResource) {
        ParsedToolInvokeRequest parsedRequest = ParsedToolInvokeRequest.parseRequest(request, configResource);
        String city = parsedRequest.body().isBlank() ? "Prague" : parsedRequest.body().trim();

        LOG.infof("Generating demo weather response for %s", city);
        return "Weather for " + city + ": 22C and clear skies";
    }
}
```

The companion `WeatherDelegate` can stay as generated for this simple demo because it already converts the response to
strings.

## Step 3: Generate a resource capability

Create a new resource provider using the `resource` subcommand:

```shell
wanaku capabilities create resource \
  --name profile \
  --type quarkus \
  --path ./demo-capabilities
```

This creates a second project named `wanaku-provider-profile` in the same working folder.

The generated project includes:

- `pom.xml` with the resource provider dependencies
- `ProfileResourceDelegate.java` for URI construction and response coercion
- `ProfileResourceConsumer.java` for fetching the resource data

## Step 4: Customize the resource logic

For the provider, implement the consumer with a small deterministic payload so you can demonstrate the feature quickly.
The example implementation in this chapter lives in
[resource-profile/src/main/java/ai/wanaku/provider/profile/ProfileResourceConsumer.java](resource-profile/src/main/java/ai/wanaku/provider/profile/ProfileResourceConsumer.java).

```java
package ai.wanaku.provider.profile;

import ai.wanaku.core.capabilities.provider.ResourceConsumer;
import ai.wanaku.core.exchange.v1.ResourceRequest;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ProfileResourceConsumer implements ResourceConsumer {

    @Override
    public Object consume(String uri, ResourceRequest request) {
        return """
                {
                  "name": "Ada Lovelace",
                  "role": "platform engineer",
                  "source": "%s"
                }
                """.formatted(uri);
    }
}
```

The generated `ProfileResourceDelegate` can also stay as-is for this demo. It already builds the target URI from the
incoming request.

## Step 5: Build and run

Build each generated project from its own directory:

```shell
cd demo-capabilities/wanaku-tool-service-weather
mvn clean package

cd ../wanaku-provider-profile
mvn clean package
```

Launch each service in a separate terminal, pointing it at the local router:

```shell
java \
  -Dwanaku.service.registration.uri=http://localhost:8080 \
  -Dquarkus.grpc.server.port=9900 \
  -jar target/quarkus-app/quarkus-run.jar
```

For the resource provider, use a different gRPC port if both services run at the same time.

### Running against a local no-auth router

The generated capability projects include OIDC authentication by default. If you are running the router locally in
`noauth` mode, the capability will fail to start until you re-augment it to disable OIDC.

Do this once per build:

```shell
java -Dquarkus.launch.rebuild=true -Dquarkus.oidc-client.enabled=false -jar target/quarkus-app/quarkus-run.jar
```

Then start it normally, adjusting the port and router URL as needed:

```shell
java \
  -Dquarkus.http.port=9010 \
  -Dwanaku.service.registration.uri=http://localhost:8080 \
  -jar target/quarkus-app/quarkus-run.jar
```

The augmentation change persists until the next `mvn clean package`, so you only need to repeat this step after a fresh
build.

## Step 6: Verify the capability registration

Check that both services registered with Wanaku:

```shell
wanaku capabilities list
```

You should see entries for both the `weather` tool capability and the `profile` resource capability.

## Step 7: Wire them into the router

Add a tool that uses the generated tool capability:

```shell
wanaku tools add \
  --name weather-now \
  --description "Return a simple demo weather response" \
  --uri "weather://city" \
  --type weather
```

Use the capability service name you chose when you generated the project as the `--type` value.

Expose a resource that uses the generated resource provider:

```shell
wanaku resources expose \
  --name team-profile \
  --description "Demo team profile resource" \
  --location "profile://team" \
  --type profile \
  --mimeType application/json
```

Again, `--type` must match the resource provider service name.

## What's Next?

- [3.01 Wanaku on the Cloud](../3.01-wanaku-on-the-cloud/README.md) - deploy Wanaku on Kubernetes or OpenShift
- [4.01 Building a Java Capability](../4.01-plain-java-capability/README.md) - build a capability from scratch using the SDK

If you find a bug, please [report it](https://github.com/wanaku-ai/wanaku/issues).
To get in touch with the community, visit the [Wanaku project](https://github.com/wanaku-ai/wanaku).
