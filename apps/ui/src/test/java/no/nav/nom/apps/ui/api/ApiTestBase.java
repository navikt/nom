package no.nav.nom.apps.ui.api;

import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import lombok.SneakyThrows;
import lombok.val;
import no.nav.nom.apps.ui.TestBase;
import no.nav.nom.apps.ui.azure.AadOAuth2AccessTokenResponseClient;
import no.nav.nom.apps.ui.common.SecurityTestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames.ID_TOKEN;

public abstract class ApiTestBase extends TestBase {
    protected static final String BASE = "http:\\/\\/localhost:\\d.*\\/";                                             // Pga Wiremock variabel port
    protected static final String AUTHENTICATION_ENDPOINT = BASE + "oauth2\\/authorization\\/aad";                    // Spring Security

    @Autowired
    protected TestRestTemplate template;

    @Autowired
    protected AadOAuth2AccessTokenResponseClient tokenResponseClientMock;

    @Autowired
    protected SecurityTestUtils securityUtils;

    @Autowired
    protected RSAKey rsaKey;

    protected String sessionId;

    @BeforeEach
    public void beforeEach() {
        stubFor(get("/aad/discovery/v2.0/keys").willReturn(aResponse().withBody(String.format("{\"keys\":[%s]}", rsaKey.toPublicJWK().toString()))));
    }

    @AfterEach
    public void afterEach() {
        sessionId = null; // Start på nytt i hver test
    }

    protected void logInWith(JWTClaimsSet.Builder claimsSet) {
        val step1 = template.getForEntity("/", String.class);
        val step2 = template.getForEntity(getRedirectLocation(step1), String.class);
        val step2UrlParams = extractUrlParams(getRedirectLocation(step2));
        mockTokenResponseClientWith(validAccessToken(), claimsSet.claim("nonce", step2UrlParams.getFirst("nonce")), refreshToken());
        val step3Request = transferSessionCookie(step2);
        val step3 = template.exchange(constructUrlForStep3WithStep2State(step2), HttpMethod.GET, step3Request, String.class);
        sessionId = getSessionCookieId(step3);
    }

    protected RequestEntity<?> requestWithCurrentSession(String url) {
        assertThat(sessionId, is(notNullValue()));
        return RequestEntity.get(URI.create(url))
                .header("Cookie", sessionId)
                .build();
    }

    protected void mockTokenResponseClientWith(JWTClaimsSet.Builder accessToken, JWTClaimsSet.Builder idToken, String refreshToken) {
        val response = OAuth2AccessTokenResponse.withToken(securityUtils.createToken(accessToken))
                .refreshToken(refreshToken)
                .scopes(Set.of("derp"))
                .tokenType(OAuth2AccessToken.TokenType.BEARER)
                .additionalParameters(Map.of(ID_TOKEN, securityUtils.createToken(idToken)))
                .build();

        when(tokenResponseClientMock.getTokenResponse(any())).thenReturn(response);
    }

    protected JWTClaimsSet.Builder validIdToken() {
        return validIdToken(null);
    }

    protected JWTClaimsSet.Builder validIdToken(String nonce) {
        return securityUtils.validJwtClaims()
                .claim("nonce", nonce) // Nonce i en login context må stemme med hva som har blitt sendt inn i login flow.
                .subject("b252dd03-6a19-4601-b749-ba4cea4008d2") // AAD gir en id på subject
                .claim("name", "Alice Wonderland");
    }

    protected JWTClaimsSet.Builder validAccessToken() {
        return securityUtils.validJwtClaims();
    }

    protected String refreshToken() {
        return UUID.randomUUID().toString(); // Inneholder ikke data på lik linje som idToken og accessToken.
    }

    protected String getRedirectLocation(ResponseEntity<?> response) {
        assertThat(response.getHeaders().get("Location"), hasSize(1));
        return response.getHeaders().get("Location").get(0);
    }

    @SneakyThrows
    protected MultiValueMap<String, String> extractUrlParams(String url) {
        return UriComponentsBuilder.fromUriString(url).build().getQueryParams();
    }

    // Av en eller annen grunn, så blir state key url-encoda, dette fører til at state key ikke finnes fordi f.eks = blir til %3D.
    protected String constructUrlForStep3WithStep2State(ResponseEntity<?> step2) {
        val decoded = URLDecoder.decode(step2.getHeaders().get("Location").get(0), Charset.defaultCharset());
        val keys = extractUrlParams(decoded);

        return "/login/oauth2/code/aad?code=codeFromAad&state=" + keys.getFirst("state");
    }

    protected HttpEntity<String> transferSessionCookie(ResponseEntity<?> responseEntity) {
        val headers = new HttpHeaders();
        headers.set("Cookie", getSessionCookieId(responseEntity));
        return new HttpEntity<>(headers);
    }

    protected String getSessionCookieId(ResponseEntity<?> responseEntity) {
        return Objects.requireNonNull(responseEntity.getHeaders().get("Set-Cookie")).stream().filter(e -> e.startsWith("JSESSIONID")).findFirst().orElseThrow();

    }
}
