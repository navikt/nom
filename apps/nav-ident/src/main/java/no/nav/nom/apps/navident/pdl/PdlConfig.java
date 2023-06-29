package no.nav.nom.apps.navident.pdl;

import lombok.AllArgsConstructor;
import lombok.Getter;
import no.nav.nom.apps.navident.common.ResourceReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import static no.nav.nom.libs.utils.common.mdc.MdcConfig.HEADER_CONSUMER_ID_KEY;

@Getter
@ConstructorBinding
@AllArgsConstructor
@ConfigurationProperties("app.pdl")
public class PdlConfig {

    String tema;
    String behandlingsnummer;
    String aadClientId;
    String baseUrl;
    int timeoutInMillis;

    @Value("${app.name}") private String appName;

    @Bean
    public WebClient pdlWebClient() {
        return WebClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HEADER_CONSUMER_ID_KEY, appName)
                .defaultHeader("Tema", tema)
                .defaultHeader("behandlingsnummer", behandlingsnummer)
                .build();
    }

    @Bean
    public String pdlHentIdentQry() {
        return ResourceReader.readFileToString("pdl/hent-ident.graphql");
    }

}
