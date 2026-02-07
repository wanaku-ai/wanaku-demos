package ai.wanaku.demos.camel.core;

import org.apache.camel.builder.RouteBuilder;

public class CatFactsRoute extends RouteBuilder {

    @Override
    public void configure() {
        from("direct:catFacts")
            .routeId("cat-facts-route")
            .log("Fetching ${header.COUNT} cat facts...")
            .toD("https://meowfacts.herokuapp.com/?count=${header.COUNT}")
            .convertBodyTo(String.class)
            .log("Response: ${body}");
    }
}
