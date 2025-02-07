package no.nav.nom.paarorende.consumer;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class NomApiClient {

    private static final String IS_AUTHORIZED_RESSURS_SKRIV_PATH = "app/authorizedRessursRedigering/isAuthorized";

    private final WebClient nomApi;

    public NomApiClient(WebClient webClient) {
        this.nomApi = webClient;
    }

    public boolean hasAccess(String bruker, String ressurs) {
        return Boolean.TRUE.equals(nomApi.post()
            .uri(IS_AUTHORIZED_RESSURS_SKRIV_PATH)
            .bodyValue(new IsAuthorizedRessursWriteRequest(bruker, ressurs))
            .retrieve()
            .bodyToMono(Boolean.class)
            .block());
    }

    record IsAuthorizedRessursWriteRequest(
        String bruker,
        String ressurs
    ) {}
}
