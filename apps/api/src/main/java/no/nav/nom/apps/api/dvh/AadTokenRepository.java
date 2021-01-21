package no.nav.nom.apps.api.dvh;

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
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AadTokenRepository {

    private final AadProperties aadProperties;
    private final RestTemplate restTemplate;

    private SignedJWT cachedToken;

    @SneakyThrows
    public String getDvhToken(){
        if(cachedToken == null || cachedToken.getJWTClaimsSet().getExpirationTime().after(Date.from(Instant.now().plusSeconds(20)))) {
            cachedToken = fetchToken();
        }

        return cachedToken.serialize();
    }

    @SneakyThrows
    private SignedJWT fetchToken(){
        val aadHeaders = new HttpHeaders();
        aadHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        val aadBodyMap = new LinkedMultiValueMap<String, String>();
        aadBodyMap.add("scope", aadProperties.getDvhScope());
        aadBodyMap.add("client_id", aadProperties.getClientId());
        aadBodyMap.add("client_assertion_type", aadProperties.getClientAssertionType());
        aadBodyMap.add("client_assertion", generateClientAssertionToken());
        aadBodyMap.add("grant_type", aadProperties.getGrantType());

        val result = restTemplate.exchange(aadProperties.getIssuerBaseUrl()+"/oauth2/v2.0/token", HttpMethod.POST, new HttpEntity<>(aadBodyMap, aadHeaders), JsonNode.class).getBody();

        return SignedJWT.parse(result.get("access_token").asText());
    }

    @SneakyThrows
    private String generateClientAssertionToken(){
        val claimSet = new JWTClaimsSet.Builder()
                .audience(aadProperties.getIssuerBaseUrl()+"/v2.0")
                .expirationTime(Date.from(Instant.now().plusSeconds(60 * 5)))
                .issuer(aadProperties.getClientId())
                .jwtID(UUID.randomUUID().toString())
                .notBeforeTime(Date.from(Instant.now()))
                .subject(aadProperties.getClientId())
                .build();

        val parsedJwk = JWK.parse(aadProperties.getJwk());
        val signer = new DefaultJWSSignerFactory().createJWSSigner(parsedJwk);
        val signedJWT = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(parsedJwk.getKeyID()).build(), claimSet);
        signedJWT.sign(signer);
        return signedJWT.serialize();
    }
}
