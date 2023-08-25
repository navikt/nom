package no.nav.nom.apps.nom.unit4;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("app")
public record AppProperties(
        Unit4 unit4,
        Proxy proxy,
        Kafka kafka,
        Ressurs ressurs,
        String name
) {

    public record Unit4(
            String baseUrl,
            String employeesPath,
            String apiUsername,
            String apiPassword
    ) {}

    public record Proxy(
            String baseUrl,
            String aadClientId
    ) {}

    public record Kafka(
            Listener listener
    ) {
        public record Listener(
                Duration blockSecondsOnBail
        ) {}
    }

    public record Ressurs(
            String topic
    ) {}
}
