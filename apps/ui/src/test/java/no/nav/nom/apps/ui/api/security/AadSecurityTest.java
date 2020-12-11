package no.nav.nom.apps.ui.api.security;

import com.nimbusds.jwt.JWTClaimsSet;
import lombok.SneakyThrows;
import lombok.val;
import no.nav.nom.contract.nom.ui.me.MeTo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static no.nav.nom.apps.ui.common.Matchers.hasKeyWithSingleValueThat;
import static no.nav.nom.apps.ui.common.Matchers.isOnlyRedirectTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesRegex;
import static org.hamcrest.Matchers.notNullValue;

@DisplayName("Aad Security")
class AadSecurityTest extends SecurityTestBase {
    private static final String STEP2_REDIRECT_URL_PATTERN = BASE + "oauth2\\/v2.0\\/authorize\\?.*";               // Spring Security
    private static final String STEP2_AAD_REDIRECT_URL_PATTERN = BASE + "login\\/oauth2\\/code\\/aad";              // Ligger configurert i property

    @Test
    @SneakyThrows
    @DisplayName("Authorization Code Flow")
    void scenario11() {

        // Step 1: Kall gjøres til applikasjonen der bruker ikke er logget inn.
        // Dette trigger en redirect til /oauth2/authorization/aad
        val step1 = template.getForEntity("/", String.class);
        assertThat(step1, isOnlyRedirectTo(AUTHENTICATION_ENDPOINT));


        // Step 2: Sessjon blir opprettet der tilstand lagres (blant annet original url)
        // Redirect til AzureAD blir så trigget med informasjon om flyten, scope, o.l.
        val step2 = template.getForEntity(getRedirectLocation(step1), String.class);
        assertThat(step2, isOnlyRedirectTo(STEP2_REDIRECT_URL_PATTERN));

        // https://tools.ietf.org/html/rfc6749#section-4.1.1
        val step2UrlParams = extractUrlParams(getRedirectLocation(step2));
        assertThat(step2UrlParams.size(), is(6));
        // REQUIRED, response_type HAS to be code.
        assertThat(step2UrlParams, hasKeyWithSingleValueThat("response_type", is("code")));
        // REQUIRED, client_id HAS to be the client identifier registered with AAD in this case.
        assertThat(step2UrlParams, hasKeyWithSingleValueThat("client_id", is("client-id")));
        // OPTIONAL, url to redirect back to, has to be registered in the IDP (Azure AD in this case) as a allowed redirect url.
        assertThat(step2UrlParams, hasKeyWithSingleValueThat("redirect_uri", matchesRegex(STEP2_AAD_REDIRECT_URL_PATTERN)));
        // OPTIONAL as per the OAuth2 Spec, but required for OIDC to indicate that the application intends to use OIDC to verify the users identity. See yaml file for description of scopes.
        assertThat(step2UrlParams, hasKeyWithSingleValueThat("scope", is("openid%20offline_access%20client-id/.default%20profile"))); // URL encoded
        // RECOMMENDED, used to maintain state and will be passed back to the server, can be random, can be base64 encoded.
        // SHOULD be used to mitigate cross-site request forgery.
        assertThat(step2UrlParams, hasKeyWithSingleValueThat("state", matchesRegex(".*")));
        // OIDC Spec, String value used to associate a Client session with an ID Token, and to mitigate replay attacks.
        // Nonce will be added to the OIDC token returned by the IDP.
        assertThat(step2UrlParams, hasKeyWithSingleValueThat("nonce", matchesRegex(".*")));


        // Step 3: Man har blitt redirecta til IDP, der man typisk logger inn, eventuelt allerede er logget inn.
        // Det kommer en ny redirect tilbake til vår app, der hvor state skal være samme som sendt
        // og code benyttes mot AzureAD i Spring Security for å få access-token, id-token og refresh-token (trenger client-id + client-secret ettersom KUN denne appen kan bruke code som blir returnert)
        val nonce = step2UrlParams.getFirst("nonce"); // Nonce i idToken må stemme med nonce i det som ble sendt til IDP via url.
        stubAadForAuthCode(validAccessToken(), validIdToken(nonce), refreshToken());
        stubAadUserInfo(USER_SUBJECT); // Validering gjøres på tvers av userInfo og subject i idToken, så disse må stemmer overens.
        val step3Request = transferSessionCookie(step2); // Ettersom noe informasjon blir lagret i sessjonen, må man ha samme session key i cookie.
        val step3 = template.exchange(constructUrlForStep3WithStep2State(step2), HttpMethod.GET, step3Request, String.class);
        assertThat(step3, isOnlyRedirectTo(BASE)); // Vi gikk originalt til '/', vi er nå tilbake og har blitt autentisert.

        // Validerer at vi benytter code mot AzureAD med påkrevde felter.
        val step3CodeRequest = findAll(postRequestedFor(urlEqualTo("/aad/tenant/oauth2/v2.0/token")));
        assertThat(step3CodeRequest, hasSize(1));
        assertThat(step3CodeRequest.get(0).getHeaders().getHeader(HttpHeaders.AUTHORIZATION).firstValue(), is("Basic Y2xpZW50LWlkOmNsaWVudC1zZWNyZXQ=")); // Base64 encoded client-id:client-secret fra props.
        val bodyAsUrlDecoded = URLDecoder.decode(step3CodeRequest.get(0).getBodyAsString(), Charset.defaultCharset()); // Sendes som en vanlig url query params men i body.
        assertThat(bodyAsUrlDecoded, containsString("grant_type=authorization_code"));
        assertThat(bodyAsUrlDecoded, containsString("code=codeFromAad"));
        assertThat(bodyAsUrlDecoded, matchesRegex(".*redirect_uri=http://localhost:\\d.*\\/login\\/oauth2\\/code\\/aad"));


        // Step4: Autentisert, kan nå gjøre kall til sikrede endepunkter
        val step4Request = transferSessionCookie(step3); // Samme sessjon som inneholder tokens.
        val step4 = template.exchange("/api/v1/me", HttpMethod.GET, step4Request, String.class);
        assertThat(step4.getStatusCode(), is(HttpStatus.OK));
        assertThat(step4.getBody(), is(notNullValue()));
    }

    @Test
    @DisplayName("Authorization Flow Error Codes - Invalid Nonce")
    void scenario12() {
        val step1 = template.getForEntity("/", String.class);
        val step2 = template.getForEntity(getRedirectLocation(step1), String.class);
        stubAadForAuthCode(validAccessToken(), validIdToken("invalid_nonce"), refreshToken());
        val step3Request = transferSessionCookie(step2);
        val step3 = template.exchange(constructUrlForStep3WithStep2State(step2), HttpMethod.GET, step3Request, String.class);
        assertThat(step3, isOnlyRedirectTo(BASE + "login\\?error"));
    }

    @Test
    @DisplayName("Authorization Flow Error Codes - Invalid Audience")
    void scenario13() {
        val step1 = template.getForEntity("/", String.class);
        val step2 = template.getForEntity(getRedirectLocation(step1), String.class);
        val step2UrlParams = extractUrlParams(getRedirectLocation(step2));
        stubAadForAuthCode(validAccessToken(), validIdToken(step2UrlParams.getFirst("nonce")).audience("invalid aud"), refreshToken());
        val step3Request = transferSessionCookie(step2);
        val step3 = template.exchange(constructUrlForStep3WithStep2State(step2), HttpMethod.GET, step3Request, String.class);
        assertThat(step3, isOnlyRedirectTo(BASE + "login\\?error"));
    }

    @Test
    @DisplayName("Authorization Flow Error Codes - Invalid/Unkwown State")
    void scenario14() {
        val step1 = template.getForEntity("/", String.class);
        val step2 = template.getForEntity(getRedirectLocation(step1), String.class);
        val step2UrlParams = extractUrlParams(getRedirectLocation(step2));
        stubAadForAuthCode(validAccessToken(), validIdToken(step2UrlParams.getFirst("nonce")), refreshToken());
        val step3Request = transferSessionCookie(step2);
        val step2RedirectUrl = constructUrlForStep3WithStep2State(step2);
        val modifiedUrlWithInvalidState = step2RedirectUrl.replaceFirst("&state=.*=", "&state=invalid");
        val step3 = template.exchange(modifiedUrlWithInvalidState, HttpMethod.GET, step3Request, String.class);
        assertThat(step3, isOnlyRedirectTo(BASE + "login\\?error"));
    }

    @Test
    @DisplayName("Authorization Flow Error Codes - Expired")
    void scenario15() {
        val step1 = template.getForEntity("/", String.class);
        val step2 = template.getForEntity(getRedirectLocation(step1), String.class);
        val step2UrlParams = extractUrlParams(getRedirectLocation(step2));
        stubAadForAuthCode(validAccessToken(), validIdToken(step2UrlParams.getFirst("nonce")).expirationTime(Date.from(LocalDateTime.now().minusHours(1).toInstant(ZoneOffset.UTC))), refreshToken());
        val step3Request = transferSessionCookie(step2);
        val step3 = template.exchange(constructUrlForStep3WithStep2State(step2), HttpMethod.GET, step3Request, String.class);
        assertThat(step3, isOnlyRedirectTo(BASE + "login\\?error"));
    }

    @Test
    @DisplayName("Refresh Flow")
    void scenario21() {
        // Når bruker gjør kall til NOM-Ui på et sikret endepunkt som støtter access token rotation, så skal det trigge et kall til AzureAD for å rotere access token via refresh token.

        val refreshToken = refreshToken();
        logInWith(validAccessToken().expirationTime(Date.from(Instant.now().minus(10, ChronoUnit.DAYS))), validIdToken(), refreshToken);
        stubAadForRefreshToken(refreshToken, validAccessToken(), validIdToken().claim("name", "Test"));

        val result = template.exchange(requestWithCurrentSession("/api/v1/me"), MeTo.class);
        assertThat(result.getStatusCode(), is(HttpStatus.OK));

        val aadRefreshRequest = findAll(postRequestedFor(urlEqualTo("/aad/tenant/oauth2/v2.0/token")).withRequestBody(containing("refresh_token")));
        assertThat(aadRefreshRequest, hasSize(1));
        assertThat(aadRefreshRequest.get(0).getHeaders().getHeader(HttpHeaders.AUTHORIZATION).firstValue(), is("Basic Y2xpZW50LWlkOmNsaWVudC1zZWNyZXQ=")); // Base64 encoded client-id:client-secret fra props.
        val bodyAsUrlDecoded = URLDecoder.decode(aadRefreshRequest.get(0).getBodyAsString(), Charset.defaultCharset()); // Sendes som en vanlig url query params men i body.
        assertThat(bodyAsUrlDecoded, containsString("client_id=client-id"));
        assertThat(bodyAsUrlDecoded, containsString("scope=client-id/.default"));
        assertThat(bodyAsUrlDecoded, containsString("grant_type=refresh_token"));
        assertThat(bodyAsUrlDecoded, containsString("refresh_token=" + refreshToken));
    }

    protected JWTClaimsSet.Builder validIdToken(String nonce) {
        return validIdToken()
                .claim("nonce", nonce); // Nonce i en login context må stemme med hva som har blitt sendt inn i login flow.
    }
}
