package no.nav.nom.apps.proxy.common;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@AllArgsConstructor
@ConfigurationProperties("app")
public class AppProperties {

    private final Unit4 unit4;
    private final int webClientMaxBytesInMemorySize;

    @Value
    public static class Unit4 {
        String unit4Username;
        String unit4Password;
        String baseUrl;
        String employeesPath;
        String apiPath;
    }
}
