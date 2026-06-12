# Testing Wanaku Capabilities

This guide shows you how to test your Wanaku tool and resource capabilities to ensure they work correctly before connecting MCP clients.

## What You Will Learn

- How to manually test tool capabilities using curl and the Wanaku CLI
- How to verify resource providers return expected data
- How to use the Wanaku health endpoints to check system status
- How to write integration tests for custom capabilities
- How to debug capability failures

## What You Will Need

- A running Wanaku instance (`wanaku server start`)
- The Wanaku CLI installed and on your PATH
- `curl` or any HTTP client for ad-hoc testing
- At least one registered tool or resource capability

## Step 1: Check System Health

Before testing capabilities, confirm Wanaku is healthy:

```bash
curl http://localhost:8080/q/health
```

You should see `"status": "UP"` in the response. If not, check the server logs.

Also verify your capabilities are registered:

```bash
wanaku tools list
wanaku resources list
```

## Step 2: Test a Tool Capability with curl

Tool capabilities are invoked via the MCP protocol. You can simulate a tool call using curl:

```bash
curl -X POST http://localhost:8080/mcp/tools/call \
  -H "Content-Type: application/json" \
    -d '{
        "name": "your-tool-name",
            "arguments": {
                  "param1": "value1"
                      }
                        }'
                        ```

                        - Replace `your-tool-name` with the registered tool name
                        - Replace `param1`/`value1` with the actual arguments expected by your tool
                        - A successful call returns the tool output as a JSON response

                        ## Step 3: Test a Resource Capability

                        Resource capabilities serve data to MCP clients. To retrieve a resource:

                        ```bash
                        curl http://localhost:8080/mcp/resources/read \
                          -H "Content-Type: application/json" \
                            -d '{"uri": "your-resource-uri"}'
                            ```

                            - Replace `your-resource-uri` with the URI of your registered resource
                            - The response should contain the resource contents

                            ## Step 4: Inspect Capability Logs

                            If a capability call fails, inspect the server logs for details:

                            ```bash
                            wanaku server logs --tail 50
                            ```

                            Common log messages to look for:

                            - `ToolNotFoundException` — the tool name is misspelled or not registered
                            - `ResourceNotFoundException` — the resource URI does not match any registered resource
                            - `CapabilityInvocationException` — the underlying provider threw an error; check the provider logs
                            - `TimeoutException` — the provider took too long to respond; increase the timeout if needed

                            ## Step 5: Write Integration Tests

                            For custom capabilities, write integration tests using your preferred test framework. A typical Java/Quarkus test looks like this:

                            ```java
                            @QuarkusIntegrationTest
                            class MyCapabilityTest {

                                @Test
                                    void testToolResponse() {
                                            given()
                                                        .contentType(ContentType.JSON)
                                                                    .body("""
                                                                                    {"name": "my-tool", "arguments": {"input": "hello"}}
                                                                                                    """)
                                                                                                            .when()
                                                                                                                        .post("/mcp/tools/call")
                                                                                                                                .then()
                                                                                                                                            .statusCode(200)
                                                                                                                                                        .body("content[0].text", containsString("expected output"));
                                                                                                                                                            }
                                                                                                                                                            }
                                                                                                                                                            ```
                                                                                                                                                            
                                                                                                                                                            - Use `@QuarkusIntegrationTest` to run tests against the packaged application
                                                                                                                                                            - Use RestAssured for HTTP assertions
                                                                                                                                                            - Verify both success and error paths
                                                                                                                                                            
                                                                                                                                                            ## Step 6: Automate Capability Testing in CI
                                                                                                                                                            
                                                                                                                                                            Add a test stage to your CI pipeline:
                                                                                                                                                            
                                                                                                                                                            ```yaml
                                                                                                                                                            # .github/workflows/ci.yml
                                                                                                                                                            - name: Run capability integration tests
                                                                                                                                                              run: mvn verify -Dquarkus.test.integration-test-profile=test
                                                                                                                                                              ```
                                                                                                                                                              
                                                                                                                                                              - Run integration tests on every pull request
                                                                                                                                                              - Include both positive and negative test cases
                                                                                                                                                              - Test with representative inputs to catch edge cases
                                                                                                                                                              
                                                                                                                                                              ## What's Next?
                                                                                                                                                              
                                                                                                                                                              - Review the [Troubleshooting Guide](../troubleshooting/README.md) if your tests reveal issues
                                                                                                                                                              - See the [Monitoring Wanaku in Production](../5.02-monitoring-wanaku-in-production/README.md) guide for production observability
                                                                                                                                                              - Explore [Exposing Existing Camel Routes](../4.02-exposing-existing-routes/README.md) for building custom capabilities
