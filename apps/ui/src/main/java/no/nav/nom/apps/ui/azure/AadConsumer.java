package no.nav.nom.apps.ui.azure;

import com.fasterxml.jackson.databind.JsonNode;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.lang.String.join;
import static org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames.ID_TOKEN;

@Slf4j
@Component
@RequiredArgsConstructor
public class AadConsumer {
    private final ClientRegistrationRepository clientRegistrationRepository;
    private final RestTemplate restTemplate;

    @SneakyThrows
    @Timed("azure-ad.getAccessTokenFromRefreshToken")
    @Counted("azure-ad.getAccessTokenFromRefreshToken")
    public OAuth2AccessTokenResponse getAccessTokenFromRefreshToken(String refreshToken) {
        val aadRegistration = clientRegistrationRepository.findByRegistrationId("aad");
        val scopes = aadRegistration.getClientId() + "/.default";

        val body = "client_id=" + aadRegistration.getClientId() +
                "&scope=" + scopes +
                "&refresh_token=" + refreshToken +
                "&grant_type=refresh_token";

        val request = createPostRequest(body);

        val response = restTemplate.exchange(request, JsonNode.class);
        return convert(response.getBody());
    }

    @SneakyThrows
    @Timed("azure-ad.getOnBehalfOfTokenFor")
    @Counted("azure-ad.getOnBehalfOfTokenFor")
    public OAuth2AccessTokenResponse getOnBehalfOfTokenFor(String accessToken, Set<String> scopes) {
        val aadRegistration = clientRegistrationRepository.findByRegistrationId("aad");

        val body = "client_id=" + aadRegistration.getClientId() +
                "&scope=" + join("+", scopes) +
                "&grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer" +
                "&assertion=" + accessToken +
                "&requested_token_use=on_behalf_of";

        val request = createPostRequest(body);

        val response = restTemplate.exchange(request, JsonNode.class);
        return convert(response.getBody());
    }

    @SneakyThrows
    private RequestEntity<String> createPostRequest(String body) {
        val aadRegistration = clientRegistrationRepository.findByRegistrationId("aad");
        val headers = new HttpHeaders();
        headers.setBasicAuth(aadRegistration.getClientId(), aadRegistration.getClientSecret());
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        return RequestEntity.post(new URI(aadRegistration.getProviderDetails().getTokenUri()))
                .headers(headers)
                .body(body);
    }

    private OAuth2AccessTokenResponse convert(JsonNode node) {
        val builder = OAuth2AccessTokenResponse
                .withToken(node.get("access_token").asText())
                .refreshToken(node.get("refresh_token").asText())
                .scopes(Set.of(node.get("scope").asText().split(" ")))
                .expiresIn(node.get("expires_in").asInt())
                .tokenType(OAuth2AccessToken.TokenType.BEARER);

        Optional.ofNullable(node.get("id_token"))
                .map(JsonNode::asText)
                .ifPresent(e -> builder.additionalParameters(Map.of(ID_TOKEN, e)));

        return builder.build();
    }
}
