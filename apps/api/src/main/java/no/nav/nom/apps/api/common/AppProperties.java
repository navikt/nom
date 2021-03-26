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
    private final Dvh dvh;
    private final Pdl pdl;

    @Value
    public static class Dvh {
        String baseUrl;
        String topic;
        String aadClientId;
    }

    @Value
    public static class Pdl {
        String tema;
    }
}
