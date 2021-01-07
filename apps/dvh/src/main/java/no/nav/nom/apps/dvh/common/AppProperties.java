package no.nav.nom.apps.dvh.common;


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

    @Value
    public static class Dvh {
        String topic;
        String tdvUsername;
        String tdvPassword;
        Enhet enhet;
        Ressurs ressurs;
        Stilling stilling;
    }

    @Value
    public static class Enhet {
        String url;
    }

    @Value
    public static class Ressurs {
        String url;
        Long chunkSize;
    }

    @Value
    public static class Stilling {
        String url;
    }
}
