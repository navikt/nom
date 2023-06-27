package no.nav.nom.apps.api.clients.ad;

import no.nav.nom.apps.api.common.AppProperties;
import no.nav.nom.apps.api.common.security.TokenRepository;
import no.nav.nom.apps.api.graphql.common.ErrorCode;
import no.nav.nom.contract.ad.AdRessurser;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Set;

@Component
public class AdClient {

    private final TokenRepository tokenRepository;
    private final AppProperties.Proxy proxyProperties;
    private final RestTemplate restTemplate;

    public AdClient(TokenRepository tokenRepository, AppProperties appProperties, RestTemplate restTemplate) {
        this.tokenRepository = tokenRepository;
        this.proxyProperties = appProperties.proxy();
        this.restTemplate = restTemplate;
    }

    public AdRessurser getAdRessurser(Set<String> navIdenter) {
        final var headers = new HttpHeaders();
        final var token = tokenRepository.getSystemAadToken(proxyProperties.aadClientId());
        headers.setBearerAuth(token.serialize());

        var result = restTemplate.exchange(
            proxyProperties.baseUrl() + "/proxy/ad",
            HttpMethod.POST,
            new HttpEntity<>(navIdenter, headers),
            AdRessurser.class
        );

        if (result.getStatusCode().is2xxSuccessful() && result.getBody() != null) {
            return result.getBody();
        } else {
            throw new ADResponseException("Error in proxy query to AD code=" + result.getStatusCode(), result, ErrorCode.SERVER_ERROR);
        }
    }
}
