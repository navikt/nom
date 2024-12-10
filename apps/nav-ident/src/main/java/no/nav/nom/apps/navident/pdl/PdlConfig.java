package no.nav.nom.apps.navident.pdl;

import lombok.AllArgsConstructor;
import lombok.Getter;
import no.nav.nom.apps.navident.common.ResourceReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;

@Getter
@AllArgsConstructor
@ConfigurationProperties("app.pdl")
public class PdlConfig {

    String tema;
    String behandlingsnummer;
    String aadClientId;
    String baseUrl;
    int timeoutInMillis;

    @Value("${app.name}")
    private String appName;

    @Bean
    public String pdlHentIdentQry() {
        return ResourceReader.readFileToString("pdl/hent-ident.graphql");
    }

}
