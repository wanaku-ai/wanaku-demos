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
