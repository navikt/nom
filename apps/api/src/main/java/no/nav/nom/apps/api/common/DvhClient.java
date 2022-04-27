package no.nav.nom.apps.api.common;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.val;
import no.nav.nom.apps.api.common.security.TokenRepository;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

@RequiredArgsConstructor
public class DvhClient {
    private final TokenRepository tokenRepository;
    private final RestTemplate restTemplate;
    private final AppProperties appProperties;

    private JsonNode dvhExchange(String urlPath) {
        val headers = new HttpHeaders();
        headers.setBearerAuth(tokenRepository.getAadToken(appProperties.getProxy().getAadClientId()).serialize());

        return restTemplate.exchange(appProperties.getProxy().getBaseUrl() + urlPath, HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class).getBody();
    }
}
