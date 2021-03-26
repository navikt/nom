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
    private final Pdl pdl;

    @Value
    public static class Dvh {
        String topic;
        String tdvUsername;
        String tdvPassword;

        @Deprecated
        Ressurs ressurs;

        Stilling stilling;
        Ressurser ressurser;
        Enheter enheter;
    }


    @Value
    public static class Pdl {
        String baseUrl;
    }

    @Value
    @Deprecated()
    public static class Ressurs {
        String url;
        Long chunkSize;
    }

    @Value
    public static class Stilling {
        String url;
    }

    @Value
    public static class Ressurser {
        String url;
        Integer bucketCount;
    }

    @Value
    public static class Enheter {
        String url;
    }
}
