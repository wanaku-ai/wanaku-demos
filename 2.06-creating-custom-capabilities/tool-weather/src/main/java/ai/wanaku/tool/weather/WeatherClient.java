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
        String body = parsedRequest.body();
        String city = body == null || body.isBlank() ? "Prague" : body.trim();

        LOG.infof("Generating demo weather response for %s", city);
        return "Weather for " + city + ": 22C and clear skies";
    }
}
