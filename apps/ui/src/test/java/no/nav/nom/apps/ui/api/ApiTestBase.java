package no.nav.nom.apps.ui.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jwt.JWTClaimsSet;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.val;
import no.nav.nom.apps.ui.TestBase;
import no.nav.nom.apps.ui.azure.AadConsumer;
import no.nav.nom.libs.test.SecurityTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public abstract class ApiTestBase extends TestBase {

    protected static final String USER_SUBJECT = "b252dd03-6a19-4601-b749-ba4cea4008d2";
    protected static final String BASE = "http:\\/\\/localhost:\\d.*\\/";                                             // Pga Wiremock variabel port
    protected static final String AUTHENTICATION_ENDPOINT = BASE + "oauth2\\/authorization\\/aad";                    // Spring Security

    @Autowired
    protected TestRestTemplate template;

    @Autowired
    protected AadConsumer aadConsumer;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected SecurityTestUtils securityUtils;

    protected String sessionCookieValue;
    protected String xsrfCookieValue;

    @BeforeEach
    public void beforeEach() {
        sessionCookieValue = null;
        xsrfCookieValue = null;
    }

    protected void logInWith(JWTClaimsSet.Builder accessTokenClaims, JWTClaimsSet.Builder idTokenClaims, String refreshToken) {
        val step1 = template.getForEntity("/", String.class);
        val step2 = template.getForEntity(getRedirectLocation(step1), String.class);
        val step2UrlParams = extractUrlParams(getRedirectLocation(step2));

        idTokenClaims.claim("nonce", step2UrlParams.getFirst("nonce"));
        stubAadForAuthCode(accessTokenClaims, idTokenClaims, refreshToken);
        stubAadUserInfo(idTokenClaims.getClaims().get("sub").toString());

        val step3Request = transferSessionCookie(step2);
        val step3 = template.exchange(constructUrlForStep3WithStep2State(step2), HttpMethod.GET, step3Request, String.class);
        sessionCookieValue = getSessionCookieId(step3);
        xsrfCookieValue = getCookieValue(step3, "XSRF-TOKEN");
    }

    protected RequestEntity<?> requestWithCurrentSession(String url) {
        assertThat(sessionCookieValue, is(notNullValue()));
        return RequestEntity.get(URI.create(url))
                .header("Cookie", sessionCookieValue)
                .build();
    }

    protected JWTClaimsSet.Builder validIdToken() {
        return validAccessToken()
                .subject(USER_SUBJECT) // AAD gir en id på subject
                .claim("name", "Alice Wonderland");
    }

    protected JWTClaimsSet.Builder validAccessToken() {
        return securityUtils.validJwtClaims("alice", "client-id");
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
        return getCookieValue(responseEntity, "JSESSIONID_NOMUI");
    }

    protected String getCookieValue(ResponseEntity<?> entity, String key) {
        return entity.getHeaders().get("Set-Cookie").stream()
                .filter(e -> e.startsWith(key))
                .findFirst()
                .orElseThrow();
    }

    public void stubAadForAuthCode(JWTClaimsSet.Builder accessToken, JWTClaimsSet.Builder idToken, String refreshToken) {
        val tokens = tokenResponse(accessToken, idToken, refreshToken);

        stubFor(post("/aad/tenant/oauth2/v2.0/token")
                .withRequestBody(containing("authorization_code"))
                .willReturn(aResponse()
                        .withBody(tokens.getEndpointResponse())
                        .withHeader("Content-Type", "application/json; charset=utf-8")));
    }

    public void stubAadForRefreshToken(String currentRefreshToken, JWTClaimsSet.Builder accessToken, JWTClaimsSet.Builder idToken) {
        val tokens = tokenResponse(accessToken, idToken, currentRefreshToken);

        stubFor(post("/aad/tenant/oauth2/v2.0/token")
                .withRequestBody(containing("refresh_token"))
                .withRequestBody(containing(currentRefreshToken))
                .willReturn(aResponse()
                        .withBody(tokens.getEndpointResponse())
                        .withHeader("Content-Type", "application/json; charset=utf-8")));
    }

    public TokenResponse stubAadForOnBehalfOf(JWTClaimsSet.Builder accessToken, String refreshToken) {
        val tokens = tokenResponse(accessToken, null, refreshToken);

        stubFor(post("/aad/tenant/oauth2/v2.0/token")
                .withRequestBody(containing("on_behalf_of"))
                .willReturn(aResponse()
                        .withBody(tokens.getEndpointResponse())
                        .withHeader("Content-Type", "application/json; charset=utf-8")));
        return tokens;
    }

    @SneakyThrows
    private TokenResponse tokenResponse(JWTClaimsSet.Builder accessToken, JWTClaimsSet.Builder idToken, String refreshToken) {
        val iat = (Date) accessToken.getClaims().get("iat");
        val exp = (Date) accessToken.getClaims().get("exp");

        val accessTokenAsJwt = securityUtils.createToken(accessToken);
        val idTokenAsJwt = Optional.ofNullable(idToken).map(e -> securityUtils.createToken(e)).orElse(null);

        val node = objectMapper.createObjectNode();
        node.put("token_type", "Bearer");
        node.put("scope", "email openid profile, GroupMember.Read.All User.Read");
        node.put("expires_in", Duration.between(iat.toInstant(), exp.toInstant()).getSeconds());
        node.put("ext_expires_in", Duration.between(iat.toInstant(), exp.toInstant()).getSeconds());
        node.put("access_token", accessTokenAsJwt);
        Optional.ofNullable(refreshToken).ifPresent(e -> node.put("refresh_token", e));
        Optional.ofNullable(idTokenAsJwt).ifPresent(e -> node.put("id_token", e));
        val asJson = objectMapper.writeValueAsString(node);
        return new TokenResponse(accessTokenAsJwt, idTokenAsJwt, refreshToken, asJson);
    }

    @SneakyThrows
    public void stubAadUserInfo(String sub) {
        val userInfoNode = objectMapper.createObjectNode();
        userInfoNode.put("sub", sub);
        val userInfoAsJson = objectMapper.writeValueAsString(userInfoNode);
        stubFor(get("/aad/oidc/userinfo").willReturn(aResponse().withBody(userInfoAsJson).withHeader("Content-Type", "application/json")));
    }

    @Value
    public static class TokenResponse {

        String accessToken;
        String idToken;
        String refreshToken;
        String endpointResponse;
    }

    public HttpHeaders headersWithCurrentSessionAndXsrf() {
        val headers = new HttpHeaders();
        Optional.ofNullable(sessionCookieValue).ifPresent(e -> headers.add("Cookie", e));
        Optional.ofNullable(xsrfCookieValue).ifPresent(e -> headers.add("Cookie", e));
        Optional.ofNullable(xsrfCookieValue).ifPresent(e -> headers.add("X-XSRF-TOKEN", xsrfCookieValue.substring(xsrfCookieValue.indexOf("=") + 1, xsrfCookieValue.indexOf(";"))));
        return headers;
    }
}
