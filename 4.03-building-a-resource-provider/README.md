# Building a Resource Provider

This guide walks you through building a custom Wanaku resource provider capability that exposes data sources as MCP resources.

## What You Will Learn

- The difference between tool invokers and resource providers
- How to implement a resource provider using the Wanaku SDK
- Packaging and registering your resource provider
- Testing resource reads through the router

## What You Will Need

- **Java 21+** and **Maven 3.8+**
- **Wanaku CLI** installed and Wanaku running locally
- Basic Java development experience

## Step 1: Understanding Resource Providers

A **resource provider** is a Wanaku capability that handles MCP resource reads. When an AI agent requests a resource like `file://config.json` or `s3://bucket/key`, the Wanaku router forwards the request to a registered resource provider that knows how to fetch that resource type.

Tool invokers handle *actions* (tool calls). Resource providers handle *data reads* (resource reads). Your custom resource provider registers a URI scheme and handles all resource reads for that scheme.

## Step 2: Create a Maven Project

```bash
mvn archetype:generate \
  -DgroupId=com.example \
    -DartifactId=my-resource-provider \
      -DarchetypeArtifactId=maven-archetype-quickstart \
        -DarchetypeVersion=1.4 \
          -DinteractiveMode=false
          ```

          Add the Wanaku SDK dependency to `pom.xml`:

          ```xml
          <dependency>
            <groupId>ai.wanaku</groupId>
              <artifactId>wanaku-provider-support</artifactId>
                <version>0.1.3</version>
                </dependency>
                ```

                ## Step 3: Implement the Resource Provider

                Create a class that implements `WanakuResourceProvider`:

                ```java
                @ApplicationScoped
                public class ConfigResourceProvider implements WanakuResourceProvider {

                    @Override
                        public String scheme() {
                                return "config";  // handles config:// URIs
                                    }

                                        @Override
                                            public ResourceContents read(String uri, Map<String, String> params) {
                                                    // Parse the URI and fetch the resource
                                                            String key = uri.replace("config://", "");
                                                                    String value = System.getenv(key.toUpperCase());
                                                                            if (value == null) {
                                                                                        throw new ResourceNotFoundException("Config key not found: " + key);
                                                                                                }
                                                                                                        return ResourceContents.text(value);
                                                                                                            }
                                                                                                            }
                                                                                                            ```
                                                                                                            
                                                                                                            ## Step 4: Add Registration Configuration
                                                                                                            
                                                                                                            In `src/main/resources/application.properties`:
                                                                                                            
                                                                                                            ```properties
                                                                                                            wanaku.provider.name=config-provider
                                                                                                            wanaku.router.host=localhost
                                                                                                            wanaku.router.port=9000
                                                                                                            ```
                                                                                                            
                                                                                                            ## Step 5: Build and Run
                                                                                                            
                                                                                                            ```bash
                                                                                                            mvn clean package -DskipTests
                                                                                                            java -jar target/my-resource-provider-runner.jar
                                                                                                            ```
                                                                                                            
                                                                                                            The provider will register itself with the Wanaku router automatically at startup.
                                                                                                            
                                                                                                            ## Step 6: Add and Test a Resource
                                                                                                            
                                                                                                            Add a resource that uses your provider:
                                                                                                            
                                                                                                            ```bash
                                                                                                            wanaku resource add \
                                                                                                              --name my-db-url \
                                                                                                                --uri config://database_url \
                                                                                                                  --description "Database connection URL from environment"
                                                                                                                  ```
                                                                                                                  
                                                                                                                  Test that an agent can read it:
                                                                                                                  
                                                                                                                  ```bash
                                                                                                                  wanaku resource read --name my-db-url
                                                                                                                  ```
                                                                                                                  
                                                                                                                  ## What's Next?
                                                                                                                  
                                                                                                                  - [5.01 Camel Assistant](../5.01-camel-assistant/README.md) — a full production example
                                                                                                                  
                                                                                                                  If you find a bug, please report it. Visit the [Wanaku project](https://github.com/wanaku-ai/wanaku).
