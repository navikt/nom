package no.nav.nom.apps.api.ressurs.api.app;

import no.nav.nom.apps.api.common.AppProperties;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class NomRemedyIntegrasjonClient {

    private final WebClient webClient;

    public NomRemedyIntegrasjonClient(OAuth2AuthorizedClientManager oAuth2AuthorizedClientManager, AppProperties appProperties) {

        ServletOAuth2AuthorizedClientExchangeFilterFunction oAuth2Filter = new ServletOAuth2AuthorizedClientExchangeFilterFunction(oAuth2AuthorizedClientManager);
        oAuth2Filter.setDefaultClientRegistrationId("nom-remedy-integrasjon");

        this.webClient = WebClient.builder()
            .filter(oAuth2Filter)
            .baseUrl(appProperties.clients().nomRemedyIntegrasjon().baseUrl())
            .build();
    }

    public Set<String> getFysiskeBrukerstedNrs(List<String> fysiskeBrukerstedNrs) {
        return webClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/api/fysiskebrukersteder")
                .queryParam("fysiskeBrukerstedNrs", fysiskeBrukerstedNrs)
                .build()
            )
            .retrieve()
            .toEntityList(FysiskBrukersted.class).block()
            .getBody().stream().map(FysiskBrukersted::brukerstedNr).collect(Collectors.toSet());

    }

    public record FysiskBrukersted (
        String brukerstedNr
    ){}

}
