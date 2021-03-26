package no.nav.nom.apps.api.common.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.factories.DefaultJWSSignerFactory;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class TokenRepository {

    private final AuthenticationProperties authenticationProperties;
    private final RestTemplate restTemplate;
    private final Map<String, SignedJWT> cachedAadTokensByScope = new ConcurrentHashMap<>(2);

    @SneakyThrows
    public String getAadToken(String scope) {
        val signedJwt = cachedAadTokensByScope.get(scope);
        return (isValidToken(signedJwt) ? signedJwt : refreshAadToken(scope)).serialize();
    }

    @SneakyThrows
    private synchronized SignedJWT refreshAadToken(String scope) {
        if (!isValidToken(cachedAadTokensByScope.get(scope))) {
            val aadHeaders = new HttpHeaders();
            aadHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            val aadBodyMap = new LinkedMultiValueMap<String, String>();
            aadBodyMap.add("scope", scope);
            aadBodyMap.add("client_id", authenticationProperties.getAad().getClientId());
            aadBodyMap.add("client_assertion_type", authenticationProperties.getAad().getClientAssertionType());
            aadBodyMap.add("client_assertion", generateClientAssertionToken());
            aadBodyMap.add("grant_type", authenticationProperties.getAad().getGrantType());

            val result = restTemplate.exchange(authenticationProperties.getAad().getIssuerBaseUrl() + "/oauth2/v2.0/token", HttpMethod.POST, new HttpEntity<>(aadBodyMap, aadHeaders), JsonNode.class).getBody();
            val token = SignedJWT.parse(result.get("access_token").asText());
            cachedAadTokensByScope.put(scope, token);
            return token;
        }
        return cachedAadTokensByScope.get(scope);
    }

    @SneakyThrows
    private String generateClientAssertionToken() {
        val claimSet = new JWTClaimsSet.Builder()
                .audience(authenticationProperties.getAad().getIssuerBaseUrl() + "/v2.0")
                .expirationTime(Date.from(Instant.now().plusSeconds(60 * 5)))
                .issuer(authenticationProperties.getAad().getClientId())
                .jwtID(UUID.randomUUID().toString())
                .notBeforeTime(Date.from(Instant.now()))
                .subject(authenticationProperties.getAad().getClientId())
                .build();

        val parsedJwk = JWK.parse(authenticationProperties.getAad().getJwk());
        val signer = new DefaultJWSSignerFactory().createJWSSigner(parsedJwk);
        val signedJWT = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(parsedJwk.getKeyID()).build(), claimSet);
        signedJWT.sign(signer);
        return signedJWT.serialize();
    }

    @SneakyThrows
    private boolean isValidToken(SignedJWT signedJwt) {
        return signedJwt != null && signedJwt.getJWTClaimsSet().getExpirationTime().after(Date.from(Instant.now().plusSeconds(20)));
    }
}
