package no.nav.nom.apps.proxy.common;


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
    private final Tel tel;
    private final Unit4 unit4;

    @Value
    public static class Tel {
        String telUsername;
        String telPassword;
        String url;
    }

    @Value
    public static class Dvh {
        String tdvUsername;
        String tdvPassword;
        String baseUrl;

        Stilling stilling;
        Ressurser ressurser;
        Enheter enheter;
        TelDvh telDvh;

    }

    @Value
    public static class Unit4 {
        String unit4Username;
        String unit4Password;
        String baseUrl;
        String employeesPath;
        String apiPath;
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
        String urlV2;
    }

    @Value
    public static class TelDvh {
        String url;
    }
}
