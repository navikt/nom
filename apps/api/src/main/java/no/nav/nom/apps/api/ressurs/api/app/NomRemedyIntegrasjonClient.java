package no.nav.nom.apps.api.ressurs.api.app;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.common.AppProperties;
import no.nav.nom.apps.api.ressurs.model.FysiskBrukersted;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
public class NomRemedyIntegrasjonClient {

    private final RestClient restClient;

    public NomRemedyIntegrasjonClient(OAuth2AuthorizedClientManager oAuth2AuthorizedClientManager, AppProperties appProperties, RestClient.Builder builder) {
        OAuth2ClientHttpRequestInterceptor oauth2Interceptor = new OAuth2ClientHttpRequestInterceptor(oAuth2AuthorizedClientManager);
        oauth2Interceptor.setClientRegistrationIdResolver(_ -> "nom-remedy-integrasjon");

        this.restClient = builder
            .baseUrl(appProperties.clients().nomRemedyIntegrasjon().baseUrl())
            .requestInterceptor(oauth2Interceptor)
            .build();
    }

    public Set<String> getFysiskeBrukerstedNrs(List<String> fysiskeBrukerstedNrs) {
        var result = restClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/api/fysiskebrukersteder")
                .queryParam("brukerstedNrs", fysiskeBrukerstedNrs)
                .build()
            )
            .retrieve()
            .body(NomRemedyIntegrasjonFysiskBrukersted[].class);
        var body = result != null ? result : new NomRemedyIntegrasjonFysiskBrukersted[0];
        return Arrays.stream(body)
            .map(NomRemedyIntegrasjonFysiskBrukersted::brukerstedNr)
            .collect(Collectors.toSet());
    }

    public List<FysiskBrukersted> getFysiskeBrukersteder() {
        log.info("Henter fysiske brukersteder fra NomRemedyIntegrasjon");
        try {
            var brukersteder = restClient.get()
                .uri("/api/fysiskebrukersteder")
                .retrieve()
                .body(NomRemedyIntegrasjonFysiskBrukersted[].class);

            if (brukersteder == null) {
                log.error("Fikk ingen fysiske brukersteder fra NomRemedyIntegrasjon");
                return List.of();
            }

            return List.of(brukersteder)
                .stream()
                .map(fysiskBrukersted -> FysiskBrukersted.builder()
                    .nr(fysiskBrukersted.brukerstedNr())
                    .navn(fysiskBrukersted.brukerstedNavn())
                    .build())
                .toList();
        } catch (Exception e) {
            log.error("Feil ved henting av fysiske brukersteder fra NomRemedyIntegrasjon", e);
            throw e;
        }
    }

    public record NomRemedyIntegrasjonFysiskBrukersted(
        String brukerstedNr,
        String brukerstedNavn
    ) {}
}
