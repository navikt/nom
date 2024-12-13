package no.nav.nom.apps.api.common;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import no.nav.nom.apps.api.clients.nomproxy.NomProxyClient;
import no.nav.nom.apps.api.orgenhet.implementations.dvh.v2.DvhOrgenhetV2;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
public class NomProxyRestClient {
    private final AppProperties appProperties;
    private final NomProxyClient nomProxyClient;

    public List<DvhOrgenhetV2> getTdvOrgenheter() {
        return StreamSupport.stream(getEnheterResponse().spliterator(), false)
            .map(DvhOrgenhetV2::new)
            .toList();
    }

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
