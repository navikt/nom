package no.nav.nom.apps.navident.common;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

@Getter
@ConstructorBinding
@AllArgsConstructor
@ConfigurationProperties("app.pdl")
public class PdlConfig {
    String tema;
    String aadClientId;
    String baseUrl;
}
