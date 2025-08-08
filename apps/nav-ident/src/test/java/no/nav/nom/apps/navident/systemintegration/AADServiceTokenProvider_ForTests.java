package no.nav.nom.apps.navident.systemintegration;

import com.fasterxml.jackson.databind.JsonNode;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.factories.DefaultJWSSignerFactory;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.nom.apps.navident.common.security.AuthenticationProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Component
@Slf4j
public class AADServiceTokenProvider_ForTests {
    private static final String AAD_GRANT_TYPE = "client_credentials";
    private static final String AAD_CLIENT_ASSERTION_TYPE = "urn:ietf:params:oauth:client-assertion-type:jwt-bearer";

    private final AuthenticationProperties authenticationProperties;
    private final WebClient webClient;

    public AADServiceTokenProvider_ForTests(AuthenticationProperties authenticationProperties, WebClient.Builder builder) {
        this.authenticationProperties = authenticationProperties;
        this.webClient = builder.clone().baseUrl(authenticationProperties.getAad().getIssuerBaseUrl()).build();
    }

    @SneakyThrows
    public synchronized SignedJWT getAadToken(String clientId, String jwk) {
        val aadHeaders = new HttpHeaders();
        aadHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        val aadBodyMap = new LinkedMultiValueMap<String, String>();
        aadBodyMap.add("scope", "api://dev-gcp.org.nav-ident/.default");
        aadBodyMap.add("client_id", clientId);
        aadBodyMap.add("client_assertion_type", AAD_CLIENT_ASSERTION_TYPE);
        aadBodyMap.add("client_assertion", generateClientAssertionToken(clientId, jwk));
        aadBodyMap.add("grant_type", AAD_GRANT_TYPE);

        val result = webClient.post()
            .uri("/oauth2/v2.0/token")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .bodyValue(aadBodyMap)
            .retrieve()
            .bodyToMono(JsonNode.class)
            .block();

        val token = SignedJWT.parse(result.get("access_token").asText());

        return token;
    }

    @SneakyThrows
    private String generateClientAssertionToken(String clientId, String jwk) {
        val claimSet = new JWTClaimsSet.Builder()
                .audience(authenticationProperties.getAad().getIssuerBaseUrl() + "/v2.0")
                .expirationTime(Date.from(Instant.now().plusSeconds(60 * 5)))
                .issuer(clientId)
                .jwtID(UUID.randomUUID().toString())
                .notBeforeTime(Date.from(Instant.now().minusSeconds(60*5)))
                .subject(clientId)
                .build();

        val parsedJwk = JWK.parse(jwk);
        val signer = new DefaultJWSSignerFactory().createJWSSigner(parsedJwk);
        val signedJWT = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(parsedJwk.getKeyID()).build(), claimSet);
        signedJWT.sign(signer);
        return signedJWT.serialize();
    }
}
