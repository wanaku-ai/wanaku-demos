package ai.wanaku.demos.camel.core;

import org.apache.camel.main.Main;

public class CatFactsMain {

    public static void main(String[] args) throws Exception {
        Main main = new Main();

        main.start();
        main.getCamelContext().addRoutes(new CatFactsRoute());
        main.run(args);
    }
}
