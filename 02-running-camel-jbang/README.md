# Trying Camel JBang with Wanaku

1. Download the CLI:
   https://github.com/wanaku-ai/wanaku/releases/download/v0.0.6/cli-0.0.6.zip

> [!IMPORTANT]
> This is using the Java binaries. You can also find native executables on the [releases page](https://github.com/wanaku-ai/wanaku/releases).

2. Extract it and make sure that its directory is in the PATH.

3. Run this command to get a local instance of Wanaku:

`wanaku start local`

The command above starts the router + 2 additional services (one for accessing HTTP/REST APIs and another to read files).

> [!NOTE]
> You can use the option `--list-services` to list the available services and then use the `--services` flag to enable additional
> ones and customize your getting started experience.

4. Install [Camel JBang](https://camel.apache.org/manual/camel-jbang.html) if you haven't done that already.

5. Now, design route in [Kaoto](https://kaoto.io/). After done, copy the path of the file your created. 

> [!IMPORTANT] 
> Make sure that any data that the route outputs is saved to the standard output (stdout).

6. Open another terminal and add the newly designed route as a tool in Wanaku. 

For instance, let's suppose that you have designed a route to order laptops:


```yaml
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
                expression: '{"number": "12354", "text": "Laptop order 12354 created
                  successfully"}'
        - log:
            id: log-2526
            message: ${body}

```
> [!IMPORTANT]
> Make sure that any data that needs to be passed to the model is printed on the standard output. You can use `--logging-level` 
> to fine-tune the output and prevent the model to be confused.

7. Now wou can add that as a tool using (supposing you have saved the file to `${HOME}/routes/laptop-order.camel.yaml`):

```
wanaku tools add -n "laptop-order" --description "Issue a new laptop order" --uri "${HOME}/.jbang/bin/camel run --max-messages=1 ${HOME}/routes/laptop-order.camel.yaml" --type exec
```

The URI represents the command that will be executed when the model decides to invoke that function. 