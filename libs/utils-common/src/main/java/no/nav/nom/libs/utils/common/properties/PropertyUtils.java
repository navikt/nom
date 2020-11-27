package no.nav.nom.libs.utils.common.properties;

public class PropertyUtils {

    public static void initDefaultProperties(String appName) {
        // Får ikke satt disse i common-app, mest sannsyneligvis pga rekkefølge de blir lastet.
        System.setProperty("spring.main.banner-mode", "off");
        System.setProperty("spring.main.cloud-platform", "kubernetes");

        // Vente på at alle requests skal bli ferdig
        System.setProperty("server.shutdown", "graceful");
        System.setProperty("app.name", appName);
        System.setProperty("spring.application.name", appName);
    }
}
