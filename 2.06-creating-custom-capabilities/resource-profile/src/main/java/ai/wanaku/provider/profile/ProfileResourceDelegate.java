package ai.wanaku.provider.profile;

import java.util.List;
import java.util.Map;

import ai.wanaku.capabilities.sdk.api.exceptions.InvalidResponseTypeException;
import ai.wanaku.capabilities.sdk.api.exceptions.NonConvertableResponseException;
import ai.wanaku.capabilities.sdk.config.provider.api.ConfigResource;
import ai.wanaku.core.capabilities.provider.AbstractResourceDelegate;
import ai.wanaku.core.capabilities.config.WanakuServiceConfig;
import ai.wanaku.core.exchange.v1.ResourceRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import static ai.wanaku.core.uri.URIHelper.buildUri;

@ApplicationScoped
public class ProfileResourceDelegate extends AbstractResourceDelegate {
    @Inject
    WanakuServiceConfig config;

    @Override
    protected String getEndpointUri(ResourceRequest request, ConfigResource configResource) {
        return buildUri(request.getLocation(), Map.of());
    }

    @Override
    protected List<String> coerceResponse(Object response)
            throws InvalidResponseTypeException, NonConvertableResponseException {
        if (response == null) {
            throw new InvalidResponseTypeException("Invalid response type from the consumer: null");
        }

        return List.of(response.toString());
    }
}
