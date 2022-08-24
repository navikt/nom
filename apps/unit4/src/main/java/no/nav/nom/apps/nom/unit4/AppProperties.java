package no.nav.nom.apps.nom.unit4;

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

    private final Unit4 unit4;

    @Value
    public static class Unit4 {
        String baseUrl;
        String enhetPath;
        String apiUsername;
        String apiPassword;
    }
}
