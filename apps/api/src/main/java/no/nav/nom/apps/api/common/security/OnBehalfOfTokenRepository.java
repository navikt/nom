package no.nav.nom.apps.api.common.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.factories.DefaultJWSSignerFactory;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.SneakyThrows;
import lombok.val;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


@Component
public class OnBehalfOfTokenRepository {

    private static final String AAD_GRANT_TYPE_OBO = "urn:ietf:params:oauth:grant-type:jwt-bearer";
    private static final String AAD_TOKEN_USE = "on_behalf_of";
    private static final String AAD_CLIENT_ASSERTION_TYPE = "urn:ietf:params:oauth:client-assertion-type:jwt-bearer";
    private final AuthenticationProperties.Aad aadAuthenticationProperties;
    private final WebClient webClient;
    private final JWSSigner jwsSigner;
    private final Map<String, SignedJWT> cachedAadTokensByUserAndScope = new ConcurrentHashMap<>(2);

    public OnBehalfOfTokenRepository(AuthenticationProperties authenticationProperties) throws Exception {
        this.aadAuthenticationProperties = authenticationProperties.aad();
        this.webClient = WebClient.builder()
            .baseUrl(aadAuthenticationProperties.issuerBaseUrl())
            .build();
        this.jwsSigner = new DefaultJWSSignerFactory().createJWSSigner(aadAuthenticationProperties.jwk());
    }

    @SneakyThrows
    public SignedJWT getOnbehalfofTokenFor(String scope) {
        String assertion = ((JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication()).getToken().getTokenValue();
        val signedJwt = cachedAadTokensByUserAndScope.get(assertion + scope);
        return isValidToken(signedJwt) ? signedJwt : refreshOnbehalfofTokenFor(assertion, scope);
    }

    @SneakyThrows
    private synchronized SignedJWT refreshOnbehalfofTokenFor(String assertion, String scope) {
        var cacheKey = assertion + scope;

        if (isValidToken(cachedAadTokensByUserAndScope.get(cacheKey))) {
            return cachedAadTokensByUserAndScope.get(cacheKey);
        }

        val aadHeaders = new HttpHeaders();
        aadHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        val aadBodyMap = new LinkedMultiValueMap<String, String>();
        aadBodyMap.add("scope", scope);
        aadBodyMap.add("client_id", aadAuthenticationProperties.clientId());
        aadBodyMap.add("client_assertion_type", AAD_CLIENT_ASSERTION_TYPE);
        aadBodyMap.add("client_assertion", generateClientAssertionToken());
        aadBodyMap.add("assertion", assertion);
        aadBodyMap.add("grant_type", AAD_GRANT_TYPE_OBO);
        aadBodyMap.add("requested_token_use", AAD_TOKEN_USE);

        var createTokenResponseBody = webClient.post()
            .uri("/oauth2/v2.0/token")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .bodyValue(aadBodyMap)
            .retrieve()
            .bodyToMono(JsonNode.class)
            .block();

        val accessToken = SignedJWT.parse(createTokenResponseBody.get("access_token").asText());
        cachedAadTokensByUserAndScope.put(cacheKey, accessToken);
        return accessToken;
    }

    @SneakyThrows
    private String generateClientAssertionToken() {
        final var claimSet = new JWTClaimsSet.Builder().audience(aadAuthenticationProperties.issuerBaseUrl() + "/v2.0")
            .expirationTime(Date.from(Instant.now().plusSeconds(60 * 5)))
            .issuer(aadAuthenticationProperties.clientId())
            .jwtID(UUID.randomUUID().toString())
            .notBeforeTime(Date.from(Instant.now()))
            .subject(aadAuthenticationProperties.clientId())
            .build();

        final var signedJWT = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(aadAuthenticationProperties.jwk().getKeyID()).build(), claimSet);
        signedJWT.sign(jwsSigner);
        return signedJWT.serialize();
    }

    @SneakyThrows
    private boolean isValidToken(SignedJWT signedJwt) {
        return signedJwt != null && signedJwt.getJWTClaimsSet().getExpirationTime().after(Date.from(Instant.now().plusSeconds(20)));
    }
}
