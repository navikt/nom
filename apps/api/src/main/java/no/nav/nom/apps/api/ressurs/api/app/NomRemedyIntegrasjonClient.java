package no.nav.nom.apps.api.ressurs.api.app;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.common.AppProperties;
import no.nav.nom.apps.api.ressurs.model.FysiskBrukersted;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
public class NomRemedyIntegrasjonClient {

    private final WebClient webClient;

    public NomRemedyIntegrasjonClient(OAuth2AuthorizedClientManager oAuth2AuthorizedClientManager, AppProperties appProperties, WebClient.Builder builder) {

        ServletOAuth2AuthorizedClientExchangeFilterFunction oAuth2Filter = new ServletOAuth2AuthorizedClientExchangeFilterFunction(oAuth2AuthorizedClientManager);
        oAuth2Filter.setDefaultClientRegistrationId("nom-remedy-integrasjon");

        this.webClient = builder.clone()
            .filter(oAuth2Filter)
            .baseUrl(appProperties.clients().nomRemedyIntegrasjon().baseUrl())
            .build();
    }

    public Set<String> getFysiskeBrukerstedNrs(List<String> fysiskeBrukerstedNrs) {
        return webClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/api/fysiskebrukersteder")
                .queryParam("brukerstedNrs", fysiskeBrukerstedNrs)
                .build()
            )
            .retrieve()
            .toEntityList(NomRemedyIntegrasjonFysiskBrukersted.class).block()
            .getBody().stream().map(NomRemedyIntegrasjonFysiskBrukersted::brukerstedNr).collect(Collectors.toSet());
    }

    public List<FysiskBrukersted> getFysiskeBrukersteder() {
        log.info("Henter fysiske brukersteder fra NomRemedyIntegrasjon");
        try {
            var nomRemedyIntegrasjonFysiskBrukersteder = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/api/fysiskebrukersteder")
                    .build()
                )
                .retrieve()
                .toEntityList(NomRemedyIntegrasjonFysiskBrukersted.class)
                .block()
                .getBody();

            if (nomRemedyIntegrasjonFysiskBrukersteder == null) {
                log.error("Fikk ingen fysiske brukersteder fra NomRemedyIntegrasjon");
                return List.of();
            }

            return nomRemedyIntegrasjonFysiskBrukersteder
                .stream()
                .map(fysiskBrukersted -> new FysiskBrukersted(fysiskBrukersted.brukerstedNr(), fysiskBrukersted.brukerstedNavn()))
                .toList();
        } catch (Exception e) {
            log.error("ALERT: Feil ved henting av fysiske brukersteder fra NomRemedyIntegrasjon", e);
            throw e;
        }
    }

    public record NomRemedyIntegrasjonFysiskBrukersted (
        String brukerstedNr,
        String brukerstedNavn
    ){}
}
