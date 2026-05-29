# Service Templates Demo

Service Templates are reusable, parameterized blueprints for creating Service Catalogs. Instead of writing Camel routes from scratch, you pick a template, fill in the parameters, and deploy. This demo shows how to use built-in templates and create your own.

> **Prerequisite:** Make sure Wanaku is running via `wanaku start local` and you are familiar with
> [Service Catalogs](../2.02-service-catalogs/README.md).

---

## Part 1: Using a Built-in Template

### Step 1: Browse Available Templates

Open the Wanaku Admin UI at <http://localhost:8080/admin/#/service-catalog> and click the
**Service Templates** tab. You should see templates like:

- `kafka-tool`
- `jms-tool`
- `github-pullrequest-source-tool`
- `mail-sink-tool`
- `rabbitmq-tool`

### Step 2: View Template Details

Click on the **kafka-tool** template to expand it. You will see its description and the list of
required properties (broker address, topics, timeout, etc.).

### Step 3: Instantiate the Template

Click **Instantiate** on the `kafka-tool` template. Fill in the properties:

| Property                       | Value                  |
|--------------------------------|------------------------|
| `kafka.brokers`                | `localhost:9092`       |
| `kafka.request.topic`         | `ai.requests`          |
| `kafka.response.topic`        | `ai.responses`         |
| `kafka.reply.timeout-ms`      | `30000`                |
| `kafka.response.group-id`     | `wanaku-demo-group`    |

Give it a name (e.g., `kafka-demo`) and click **Deploy**. The instantiated catalog is automatically
deployed.

### Step 4: Verify

```shell
wanaku tools list | grep kafka
```

You should see a `kafka_request_reply` tool (or similar, depending on the template version).

> **Note:** Actually testing the Kafka tool requires a running Kafka broker. The
> `start-local-kafka.sh` script in this directory can help you start one quickly.

---

## Part 2: Creating a Custom Template

Let's create a weather template that can be reused with different API keys and locations.

### Step 1: Create the Template Directory

```shell
mkdir -p /tmp/weather-template/weather
cd /tmp/weather-template
```

### Step 2: Create index.properties

```properties
catalog.name=weather-template
catalog.description=Weather information service template
catalog.services=weather

catalog.routes.weather=weather/weather.camel.yaml
catalog.rules.weather=weather/weather.rules.yaml
catalog.dependencies.weather=weather/weather.dependencies.txt
catalog.properties.weather=weather/service.properties
```

### Step 3: Create service.properties

This is where you define the parameterized placeholders:

```properties
weather.api.key={{weather.api.key}}
weather.location={{weather.location}}
weather.units={{weather.units}}
```

### Step 4: Create the Camel Route

Create `weather/weather.camel.yaml`:

```yaml
- route:
    id: get-weather
    description: Get current weather for the configured location
    from:
      uri: direct:wanaku
      steps:
        - setHeader:
            name: CamelHttpMethod
            constant: GET
        - log:
            message: "Fetching weather for location: {{weather.location}}"
        - to:
            uri: "https://api.openweathermap.org/data/2.5/weather?q={{weather.location}}&appid={{weather.api.key}}&units={{weather.units}}"
        - log:
            message: "Weather data: ${body}"
```

### Step 5: Add Dependencies

Create `weather/weather.dependencies.txt`:

```text
org.apache.camel:camel-http:4.18.2
org.apache.camel:camel-jackson:4.18.2
```

### Step 6: Generate Rules and Deploy

```shell
cd /tmp
wanaku service expose --path=weather-template
wanaku service package --path=weather-template -o weather-template.zip
wanaku service template deploy --file=weather-template.zip --host=http://localhost:8080
```

### Step 7: Instantiate Your Template

Open the **Service Templates** tab in the Admin UI at <http://localhost:8080/admin/#/service-catalog>.
Find `weather-template`, click **Instantiate**, and fill in the properties:

| Property              | Value                        |
|-----------------------|------------------------------|
| `weather.api.key`     | Your OpenWeatherMap API key  |
| `weather.location`    | `New York`                   |
| `weather.units`       | `metric`                     |

Give it a name (e.g., `my-weather-service`) and click **Deploy**.

> **Note:** You'll need a free API key from [OpenWeatherMap](https://openweathermap.org/api) to use this template.

---

## What's Next?

- Explore more built-in templates: `wanaku service template list`
- Read the [Service Templates Guide](https://github.com/wanaku-ai/wanaku/blob/main/docs/service-templates.md)
- Try deploying Service Catalogs on Kubernetes using the Wanaku Operator

## Troubleshooting

### Template instantiation fails with "missing property"

- Double-check property names match exactly (case-sensitive)
- Ensure all required properties are provided via `--property` flags
- Check the template's `service.properties` file for required placeholders

If you find a bug, please [report it](https://github.com/wanaku-ai/wanaku/issues).
