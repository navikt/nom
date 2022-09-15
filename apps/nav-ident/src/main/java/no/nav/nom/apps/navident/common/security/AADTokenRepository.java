package no.nav.nom.apps.navident.common.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.factories.DefaultJWSSignerFactory;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


/***
 * Shameless copy from no.nav.nom.apps.api.common.security.TokenRepository
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AADTokenRepository {

    private final AuthenticationProperties authenticationProperties;
    private final RestTemplate restTemplate;
    private final Map<String, SignedJWT> cachedAadTokensByUserAndScope = new ConcurrentHashMap<>(2);
    private final Map<String, SignedJWT> cachedAadTokensByScope = new ConcurrentHashMap<>(2);

    private static final String AAD_GRANT_TYPE_OBO = "urn:ietf:params:oauth:grant-type:jwt-bearer";
    private static final String AAD_GRANT_TYPE = "client_credentials";
    private static final String AAD_TOKEN_USE = "on_behalf_of";
    private static final String AAD_CLIENT_ASSERTION_TYPE = "urn:ietf:params:oauth:client-assertion-type:jwt-bearer";

    @SneakyThrows
    public String getOboOrClientCredToken(String scope) {
        try {
            final var auth = (JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
            if (auth != null) {
                final var token = auth.getToken().getTokenValue();
                if (isContextTokenForAnUserPrincipal(token)) {
                    return getOnbehalfofTokenFor(token, scope);
                } else {
                    return getAadToken(scope);
                }
            } else {
                log.warn("SecurityContext has no Authentication at all. Cannot crete obo token");
            }
        } catch (ClassCastException e) {
            log.error("SecurityContext Authentication is not of expected JwtAuthenticationToken type. Cannot crete obo token", e);
        }
        return null;
    }

    @SneakyThrows
    private boolean isContextTokenForAnUserPrincipal(String token) {
        SignedJWT jwt = SignedJWT.parse(token);

        String oid = jwt.getJWTClaimsSet().getStringClaim("oid");
        String sub = jwt.getJWTClaimsSet().getStringClaim("sub");

        return !oid.equals(sub);
    }


    @SneakyThrows
    public String getOnbehalfofTokenFor(String assertion, String scope) {
        log.debug("Getting obo token for scope %s".formatted(scope));
        if ( ! isValidToken(assertion) ) return null;

        val signedJwt = cachedAadTokensByUserAndScope.get(assertion + scope);
        return (isValidToken(signedJwt) ? signedJwt : getOBOTokenFor(assertion, scope)).serialize();
    }

    @SneakyThrows
    public String getAadToken(String scope) {
        log.debug("Getting service token for scope %s".formatted(scope));
        val signedJwt = cachedAadTokensByScope.get(scope);
        return (isValidToken(signedJwt) ? signedJwt : refreshAadToken(scope)).serialize();
    }

    @SneakyThrows
    private synchronized SignedJWT getOBOTokenFor(String assertion, String scope) {
        var cacheKey = assertion + scope;

        val aadHeaders = new HttpHeaders();
        aadHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        val aadBodyMap = new LinkedMultiValueMap<String, String>();
        aadBodyMap.add("scope", scope);
        aadBodyMap.add("client_id", authenticationProperties.getAad().getClientId());
        aadBodyMap.add("client_assertion_type", AAD_CLIENT_ASSERTION_TYPE);
        aadBodyMap.add("client_assertion", generateClientAssertionToken());
        aadBodyMap.add("assertion", assertion);
        aadBodyMap.add("grant_type", AAD_GRANT_TYPE_OBO);
        aadBodyMap.add("requested_token_use", AAD_TOKEN_USE);

        val result = restTemplate.exchange(authenticationProperties.getAad().getIssuerBaseUrl() + "/oauth2/v2.0/token", HttpMethod.POST, new HttpEntity<>(aadBodyMap, aadHeaders), JsonNode.class).getBody();
        val token = SignedJWT.parse(result.get("access_token").asText());

        cachedAadTokensByUserAndScope.put(cacheKey, token);
        return token;
    }

    @SneakyThrows
    private synchronized SignedJWT refreshAadToken(String scope) {
        val aadHeaders = new HttpHeaders();
        aadHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        val aadBodyMap = new LinkedMultiValueMap<String, String>();
        aadBodyMap.add("scope", scope);
        aadBodyMap.add("client_id", authenticationProperties.getAad().getClientId());
        aadBodyMap.add("client_assertion_type", AAD_CLIENT_ASSERTION_TYPE);
        aadBodyMap.add("client_assertion", generateClientAssertionToken());
        aadBodyMap.add("grant_type", AAD_GRANT_TYPE);

        val result = restTemplate.exchange(authenticationProperties.getAad().getIssuerBaseUrl() + "/oauth2/v2.0/token", HttpMethod.POST, new HttpEntity<>(aadBodyMap, aadHeaders), JsonNode.class).getBody();
        val token = SignedJWT.parse(result.get("access_token").asText());

        cachedAadTokensByScope.put(scope, token);
        return token;
    }

    @SneakyThrows
    private String generateClientAssertionToken() {
        val claimSet = new JWTClaimsSet.Builder()
                .audience(authenticationProperties.getAad().getIssuerBaseUrl() + "/v2.0")
                .expirationTime(Date.from(Instant.now().plusSeconds(60 * 5)))
                .issuer(authenticationProperties.getAad().getClientId())
                .jwtID(UUID.randomUUID().toString())
                .notBeforeTime(Date.from(Instant.now().minusSeconds(60*5)))
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

    @SneakyThrows
    private boolean isValidToken(String token) {
        return token != null && isValidToken(SignedJWT.parse(token));
    }
}
