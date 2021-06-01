package no.nav.nom.apps.api.common;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

@Getter
@ConstructorBinding
@AllArgsConstructor
@ConfigurationProperties("app")
public class AppProperties {

    private final Proxy proxy;
    private final Pdl pdl;
    private final Hendelse hendelse;

    @Value
    public static class Proxy {
        String baseUrl;
        String aadClientId;
    }

    @Value
    public static class Pdl {
        String tema;
        String aadClientId;
        String baseUrl;
    }

    @Value
    public static class Hendelse {

        String topic;
        String aadClientId;

    }
}
