package no.nav.nom.skjermede.personer.pip.config;

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
    private final SkjermedePersoner skjermedePersoner;

    @Value
    public static class SkjermedePersoner {
        String topicName;
    }
}
