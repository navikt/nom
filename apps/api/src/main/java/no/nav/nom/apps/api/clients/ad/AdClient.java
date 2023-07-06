package no.nav.nom.apps.api.clients.ad;

import no.nav.nom.apps.api.clients.nomproxy.NomProxyClient;
import no.nav.nom.apps.api.graphql.common.ErrorCode;
import no.nav.nom.contract.ad.AdRessurser;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Set;

@Component
public class AdClient {

    private final NomProxyClient nomProxyClient;

    public AdClient(NomProxyClient nomProxyClient) {
        this.nomProxyClient = nomProxyClient;
    }

    public AdRessurser getAdRessurser(Set<String> navIdenter) {

        return nomProxyClient.getWebClient().post()
            .uri("/proxy/ad")
            .bodyValue(navIdenter)
            .retrieve()
            .bodyToMono(AdRessurser.class)
            .doOnError(WebClientResponseException.class, (webClientResponseException) -> {
                throw new ADResponseException(
                    "Error in proxy query to AD code=" + webClientResponseException.getStatusCode(),
                    ErrorCode.SERVER_ERROR
                );
            })
            .block();
    }
}
