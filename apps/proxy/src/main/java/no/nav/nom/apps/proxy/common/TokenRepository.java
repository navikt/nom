package no.nav.nom.apps.proxy.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class TokenRepository {

    private final RestTemplate restTemplate;
    private final AuthenticationProperties authenticationProperties;


    private SignedJWT cachedPdlStsJwt;

    @SneakyThrows
    public String getStsToken() {
        return (isValidToken(cachedPdlStsJwt) ? cachedPdlStsJwt : refreshStsToken()).serialize();
    }

    @SneakyThrows
    private synchronized SignedJWT refreshStsToken() {
        if (!isValidToken(cachedPdlStsJwt)) {
            val url = UriComponentsBuilder
                    .fromUriString(authenticationProperties.getSts().getIssuerBaseUrl() + "/rest/v1/sts/token")
                    .queryParam("grant_type", authenticationProperties.getSts().getGrantType())
                    .queryParam("scope", authenticationProperties.getSts().getScope())
                    .build()
                    .toUriString();

            val headers = new HttpHeaders();
            headers.setBasicAuth(authenticationProperties.getSts().getUsername(), authenticationProperties.getSts().getPassword());
            headers.setContentType(MediaType.APPLICATION_JSON);

            val result = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class).getBody();
            cachedPdlStsJwt = SignedJWT.parse(result.get("access_token").asText());
        }
        return cachedPdlStsJwt;
    }

    @SneakyThrows
    private boolean isValidToken(SignedJWT signedJwt) {
        return signedJwt != null && signedJwt.getJWTClaimsSet().getExpirationTime().after(Date.from(Instant.now().plusSeconds(20)));
    }
}
