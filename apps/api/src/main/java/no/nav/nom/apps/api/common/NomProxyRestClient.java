package no.nav.nom.apps.api.common;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import no.nav.nom.apps.api.clients.nomproxy.NomProxyClient;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NomProxyRestClient {
    private final AppProperties appProperties;
    private final NomProxyClient nomProxyClient;

    public JsonNode getEnheterResponse() {
        return dvhExchange(appProperties.clients().nomProxy().enhetPath()).get("NOM_HRORG_ORGENHETResponse").get("NOM_HRORG_ORGENHETResult");
    }

    private JsonNode dvhExchange(String path) {
        return nomProxyClient.getWebClient().get()
            .uri(path)
            .retrieve()
            .bodyToMono(JsonNode.class)
            .block();
    }
}
