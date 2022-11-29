package no.nav.nom.apps.api.common;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.val;
import no.nav.nom.apps.api.common.security.TokenRepository;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class NomProxyRestClient {
    private final TokenRepository tokenRepository;
    private final RestTemplate restTemplate;
    private final AppProperties appProperties;

    public JsonNode getEnheterResponse() {
        return dvhExchange(appProperties.proxy().enhetPath()).get("NOM_HRORG_ORGENHETResponse").get("NOM_HRORG_ORGENHETResult");
    }

    private JsonNode dvhExchange(String path) {
        val headers = new HttpHeaders();
        headers.setBearerAuth(tokenRepository.getSystemAadToken(appProperties.proxy().aadClientId()).serialize());

        return restTemplate.exchange(appProperties.proxy().baseUrl() + path, HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class).getBody();
    }
}
