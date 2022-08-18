package no.nav.nom.apps.api.common;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
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
    private final Nik nik;
    @Setter
    private String appName;

    public boolean isSandbox() {
        return appName.equals("nom-sandbox-api");
    }

    @Value
    public static class Proxy {
        String baseUrl;
        String enhetPath;
        String aadClientId;
    }

    @Value
    public static class Pdl {
        String tema;
        String aadClientId;
        String baseUrl;
    }

    @Value
    public static class Nik {
        String baseUrl;
        String aadClientId;
    }
}
