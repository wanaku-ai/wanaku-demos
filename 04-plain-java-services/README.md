# Building a Java Capability for Wanaku

This guide will walk you through creating a simple "echo" capability for Wanaku using Java. 

This service will read the value of an environment variable and return it, demonstrating the basic principles of the 
[Wanaku Capabilities SDK](https://github.com/wanaku-ai/wanaku-capabilities-java-sdk).

### Prerequisites

* Java Development Kit (JDK)
* Apache Maven
* An IDE of your choice (e.g., VS Code, IntelliJ IDEA)

-----

## 🛠️ 1. Project Setup

First, generate a new capability project using the Wanaku SDK's Maven archetype. This command creates a complete project structure for you.

```shell
mvn -B archetype:generate \
    -DarchetypeGroupId=ai.wanaku.sdk \
    -DarchetypeArtifactId=capabilities-archetypes-java-tool \
    -DarchetypeVersion=0.0.7 \
    -DgroupId=net.orpiske \
    -Dpackage=net.orpiske \
    -DartifactId=echo \
    -Dname=EchoService \
    -Dwanaku-sdk-version=0.0.7
```

> [IMPORTANT]
> Make sure the `DarchetypeVersion` and `Dwanaku-sdk-version` match the version of Wanaku you are using.

Once the project is created, run an initial build to compile the code and download dependencies.

```shell
mvn clean package
```

-----

## 💻 2. Implementing the Capability Logic

Now, let's write the Java code for our echo service. We only need to modify two files.

#### Configure Properties in `ProvisionBase`

Since our service doesn't require any arguments from Wanaku, we can simplify the `properties` method in the `ProvisionBase.java` class to return an empty `Map`.

```java
public Map<String, PropertySchema> properties() {
    // This service has no server-sent properties, so an empty map is sufficient.
    return Map.of();
}
```

#### Implement the Tool Logic in `AppTool`

Next, we'll implement the core logic in `AppTool.java`. The goal is to read an environment variable named `MCP_ECHO` and send its value back to Wanaku. The boilerplate code is already there; you just need to add the line that gets the environment variable.

```java
public void invokeTool(ToolInvokeRequest request, StreamObserver<ToolInvokeReply> responseObserver) {
    try {
        // Get the value of the environment variable.
        String response = System.getenv("MCP_ECHO");

        // Build the response for Wanaku.
        responseObserver.onNext(
                ToolInvokeReply.newBuilder()
                        .setIsError(false)
                        .addAllContent(List.of(response)).build());

        responseObserver.onCompleted();
    } finally {
        // Perform any cleanup if necessary.
    }
}
```

-----

## 📦 3. Packaging the Application

To package our application into a single, executable JAR file, we need to add the `maven-assembly-plugin` to the `pom.xml`.

Add the following `<build>` section inside the `<project>` tag in your `pom.xml`:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-assembly-plugin</artifactId>
            <version>3.7.1</version>
            <configuration>
                <descriptorRefs>
                    <descriptorRef>jar-with-dependencies</descriptorRef>
                </descriptorRefs>
                <archive>
                    <manifest>
                        <mainClass>net.orpiske.App</mainClass>
                    </manifest>
                </archive>
                <finalName>echo-app</finalName>
                <appendAssemblyId>false</appendAssemblyId>
            </configuration>
            <executions>
                <execution>
                    <id>make-assembly</id>
                    <phase>package</phase>
                    <goals>
                        <goal>single</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

**Note:** After Wanaku version `0.0.8`, this plugin configuration will be included in the archetype by default, so this step will no longer be necessary.

Now, build the project again to create the executable JAR.

```shell
mvn clean package
```

You should now find the final artifact at `target/echo-app.jar`.

-----

## 🚀 4. Running and Verifying the Capability

Let's launch the service and test it with Wanaku.

1.  **Test the JAR** Run the JAR with the `--help` flag to ensure it's executable.

    ```shell
    java -jar target/echo-app.jar --help
    ```

2.  **Start Wanaku** In a separate terminal, start a local Wanaku instance. You can use the `wanaku` CLI or a container.

    ```shell
    # Example using Podman
    podman run -d -p 8080:8080 quay.io/wanaku/wanaku-router:wanaku-0.0.7
    ```

3.  **Set the Environment Variable** Export the `MCP_ECHO` variable with a test value.

    ```shell
    export MCP_ECHO="Hello from the Echo Service!"
    ```

4.  **Launch the Capability Service** Run your new capability, telling it how to connect to Wanaku.

    ```shell
    java -jar target/echo-app.jar --registration-url http://localhost:8080 --registration-announce-address localhost --grpc-port 9191 --name echo-service
    ```

    **Note:** If Wanaku is running in a container, set `--registration-announce-address` to an address reachable from the container, like `host.docker.internal` or your machine's IP.

5.  **Check Registration** ✅  
    After a few seconds, use the `wanaku` CLI to see if your capability has registered successfully.

    ```shell
    wanaku capabilities list
    ```

6.  **Add a Tool** Create a tool in Wanaku that uses your new capability.

    ```shell
    wanaku tools add --name "echo-tool" \
      --description "This tool echoes a value set in an environment variable." \
      --uri "echo://my-echo" \
      --type echo-service
    ```

7.  **Final Test** Finally, use the MCP Inspector or another client to invoke your `echo-tool`. It should reply with "Hello from the Echo Service\!"

    ```shell
    npx @modelcontextprotocol/inspector
    ```

You can find the complete sample code for this example in the [wanaku-echo-capability-example repository](https://github.com/orpiske/wanaku-echo-capability-example).

Happy coding\!