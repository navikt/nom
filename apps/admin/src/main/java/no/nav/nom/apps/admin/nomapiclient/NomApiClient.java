package no.nav.nom.apps.admin.nomapiclient;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import static no.nav.nom.apps.admin.nomapiclient.WebClientUtils.defaultRetry;

@Slf4j
@Service
public class NomApiClient {

    private final WebClient nomApiAuthorizedWebClient;

    public NomApiClient(WebClient nomApiAuthorizedWebClient) {
        this.nomApiAuthorizedWebClient = nomApiAuthorizedWebClient;
    }

    public String getRessursRemedyEnhet(String navident) {
        log.info("Henter remedyEnhet for ressurs {}", navident);
        return nomApiAuthorizedWebClient.get()
            .uri(uriBuilder -> {
                    var newUri = uriBuilder
                        .path("/app/ressurser/{navident}/remedyEnhet")
                        .build(navident);
                    log.info("URI for getRessursRemedyEnhet: {}", newUri);
                    return newUri;
                }
            )
            .retrieve()
            .bodyToMono(String.class)
            .retryWhen(defaultRetry)
            .block();
    }
}
