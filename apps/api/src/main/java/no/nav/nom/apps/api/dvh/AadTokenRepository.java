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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AadTokenRepository {

    private final AadProperties aadProperties;
    private final WebClient webClient;
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
        val url =  UriComponentsBuilder
                .fromUriString(aadProperties.getIssuerBaseUrl()+"/oauth2/v2.0/token")
                .queryParam("scope", aadProperties.getDvhScope())
                .queryParam("client_id", aadProperties.getClientId())
                .queryParam("client_assertion_type", aadProperties.getClientAssertionType())
                .queryParam("client_assertion", generateClientAssertionToken())
                .queryParam("grant_type", aadProperties.getGrantType())
                .build()
                .toUriString();

        val result =  webClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

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
