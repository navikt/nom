package no.nav.nom.apps.admin.nomapiclient;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import static no.nav.nom.apps.admin.nomapiclient.WebClientUtils.defaultRetry;

@Service
public class NomApiClient {

    private final WebClient nomApiAuthorizedWebClient;

    public NomApiClient(WebClient nomApiAuthorizedWebClient) {
        this.nomApiAuthorizedWebClient = nomApiAuthorizedWebClient;
    }

    public String getRessursRemedyEnhet(String navident) {
        return nomApiAuthorizedWebClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/app/ressurser/{navident}/remedyEnhet")
                .build(navident)
            )
            .retrieve()
            .bodyToMono(String.class)
            .retryWhen(defaultRetry)
            .block();
    }
}
