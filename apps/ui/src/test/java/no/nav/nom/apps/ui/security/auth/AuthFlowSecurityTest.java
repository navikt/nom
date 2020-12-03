package no.nav.nom.apps.ui.security.auth;

import lombok.SneakyThrows;
import lombok.val;
import no.nav.nom.apps.ui.security.SecurityTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLDecoder;
import java.nio.charset.Charset;

import static no.nav.nom.apps.ui.common.Matchers.hasKeyWithSingleValueThat;
import static no.nav.nom.apps.ui.common.Matchers.isOnlyRedirectTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesRegex;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

class AuthFlowSecurityTest extends SecurityTestBase {
    private static final String BASE = "http:\\/\\/localhost:\\d.*\\/";                                             // Pga Wiremock variabel port
    private static final String STEP1_REDIRECT_URL_PATTERN = BASE + "oauth2\\/authorization\\/aad";                 // Spring Security
    private static final String STEP2_REDIRECT_URL_PATTERN = BASE + "oauth2\\/v2.0\\/authorize\\?.*";               // Spring Security
    private static final String STEP2_AAD_REDIRECT_URL_PATTERN = BASE + "login\\/oauth2\\/code\\/aad";              // Ligger configurert i property

    @Test
    @DisplayName("Authorization Flow")
    void scenario1() {

        // Step 1: Kall gjøres til applikasjonen der bruker ikke er logget inn.
        // Dette trigger en redirect til /oauth2/authorization/aad
        val step1 = template.getForEntity("/", String.class);
        assertThat(step1, isOnlyRedirectTo(STEP1_REDIRECT_URL_PATTERN));


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
        // OPTIONAL as per the OAuth2 Spec, but required for OIDC to indicate that the application intends to use OIDC to verify the users identity.
        assertThat(step2UrlParams, hasKeyWithSingleValueThat("scope", is("openid")));
        // RECOMMENDED, used to maintain state and will be passed back to the server, can be random, can be base64 encoded.
        // SHOULD be used to mitigate cross-site request forgery.
        assertThat(step2UrlParams, hasKeyWithSingleValueThat("state", matchesRegex(".*")));
        // OIDC Spec, String value used to associate a Client session with an ID Token, and to mitigate replay attacks.
        // Nonce will be added to the OIDC token returned by the IDP.
        assertThat(step2UrlParams, hasKeyWithSingleValueThat("nonce", matchesRegex(".*")));


        // Step 3: Man har blitt redirecta til IDP, der man typisk logger inn, eventuelt allerede er logget inn.
        // Det kommer en ny redirect tilbake til vår app, der hvor state skal være samme som sendt
        // og code benyttes mot AzureAD i Spring Security for å få access-token, id-token og refresh-token (trenger client-id + client-secret ettersom KUN denne appen kan bruke code som blir returnert)
        // Dette steget er litt syntetisk den forstand at det ikke er applikasjonen i seg selv som gjør dette, men Azure som sender denne redirecten tilbake til browser, slik at fra applikasjonen sitt perspektiv,
        // har ikke den gjort noe bortsett ifra at client nå kommer tilbake.
        mockTokenResponseClientWith(validAccessToken(), validIdToken(step2UrlParams.getFirst("nonce")), refreshToken()); // Nonce i idToken må stemme med nonce i det som ble sendt til IDP via url.
        val step3Request = transferSessionCookie(step2); // Ettersom noe informasjon blir lagret i sessjonen, må man ha samme session key i cookie.
        val step3 = template.exchange(constructUrlForStep3WithStep2State(step2), HttpMethod.GET, step3Request, String.class);
        assertThat(step3, isOnlyRedirectTo(BASE)); // Vi gikk originalt til '/', vi er nå tilbake og har blitt autentisert.


        // Step4: Autentisert, kan nå gjøre kall til sikrede endepunkter
        val step4Request = transferSessionCookie(step3); // Samme sessjon som inneholder tokens.
        val step4 = template.exchange("/api/v1/me", HttpMethod.GET, step4Request, String.class);
        assertThat(step4.getStatusCode(), is(HttpStatus.OK));
        assertThat(step4.getBody(), is(notNullValue()));
    }

    @Test
    @DisplayName("Authorization Flow Error Codes - Nonce")
    void scenario2() {
        val step1 = template.getForEntity("/", String.class);
        val step2 = template.getForEntity(getRedirectLocation(step1), String.class);
        mockTokenResponseClientWith(validAccessToken(), validIdToken("invalid nonce"), refreshToken());
        val step3Request = transferSessionCookie(step2);
        val step3 = template.exchange(constructUrlForStep3WithStep2State(step2), HttpMethod.GET, step3Request, String.class);
        assertThat(step3, isOnlyRedirectTo(BASE + "login\\?error"));
    }

    @Test
    @DisplayName("Authorization Flow Error Codes - Audience")
    void scenario3() {
        val step1 = template.getForEntity("/", String.class);
        val step2 = template.getForEntity(getRedirectLocation(step1), String.class);
        val step2UrlParams = extractUrlParams(getRedirectLocation(step2));
        mockTokenResponseClientWith(validAccessToken(), validIdToken(step2UrlParams.getFirst("nonce")).audience("invalid aud"), refreshToken());
        val step3Request = transferSessionCookie(step2);
        val step3 = template.exchange(constructUrlForStep3WithStep2State(step2), HttpMethod.GET, step3Request, String.class);
        assertThat(step3, isOnlyRedirectTo(BASE + "login\\?error"));
    }

    private String getRedirectLocation(ResponseEntity<?> response) {
        assertThat(response.getHeaders().get("Location"), hasSize(1));
        return response.getHeaders().get("Location").get(0);
    }

    @SneakyThrows
    private MultiValueMap<String, String> extractUrlParams(String url) {
        return UriComponentsBuilder.fromUriString(url).build().getQueryParams();
    }

    // Av en eller annen grunn, så blir state key url-encoda, dette fører til at state key ikke finnes fordi f.eks = blir til %3D.
    private String constructUrlForStep3WithStep2State(ResponseEntity<?> step2) {
        val decoded = URLDecoder.decode(step2.getHeaders().get("Location").get(0), Charset.defaultCharset());
        val keys = extractUrlParams(decoded);

        return "/login/oauth2/code/aad?code=codeFromAad&state=" + keys.getFirst("state");
    }

    private HttpEntity<String> transferSessionCookie(ResponseEntity<?> responseEntity) {
        val headers = new HttpHeaders();
        headers.set("Cookie", responseEntity.getHeaders().get("Set-Cookie").stream().filter(e -> e.startsWith("JSESSIONID")).findFirst().orElseThrow());
        return new HttpEntity<>(headers);
    }
}
