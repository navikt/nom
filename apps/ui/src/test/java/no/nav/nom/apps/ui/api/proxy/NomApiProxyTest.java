package no.nav.nom.apps.ui.api.proxy;

import lombok.val;
import no.nav.nom.apps.ui.api.ApiTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;

import java.net.URI;
import java.util.Optional;

import static no.nav.nom.apps.ui.common.Matchers.isOnlyRedirectTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@DisplayName("api/v1/nom-api/graphql")
class NomApiProxyTest extends ApiTestBase {
    private static final String URL = "/api/v1/nom-api/graphql";

    @Test
    @DisplayName("Call with authenticated session and xsrf token returns data")
    void scenario1() {
        logInWith(validAccessToken(), validIdToken(), refreshToken());

        val result = template.exchange(postWith(currentSessionAndXsrf()), String.class);
        assertThat(result.getStatusCode(), is(HttpStatus.OK));
        assertThat(result.getBody(), is(notNullValue()));
        assertThat(result.getBody(), containsString("data"));
    }

    @Test
    @DisplayName("Call with no session returns no data and redirect to auth endpoint")
    void scenario2() {
        val result = template.exchange(postWith(currentSessionAndXsrf()), String.class);
        assertThat(result, isOnlyRedirectTo(AUTHENTICATION_ENDPOINT));
    }

    @Test
    @DisplayName("Call with unauthenticated session returns no data and redirect to auth endpoint")
    void scenario3() {
        val step1 = template.getForEntity("/", String.class);
        val step2 = template.getForEntity(getRedirectLocation(step1), String.class); // Ikke fulgt hele login-flyten, men fått sessjon
        sessionCookieValue = getSessionCookieId(step2);

        val result = template.exchange(postWith(currentSessionAndXsrf()), String.class);
        assertThat(result, isOnlyRedirectTo(AUTHENTICATION_ENDPOINT));
    }

    @Test
    @DisplayName("Call with authenticated session and invalid xsrf token returns 403")
    void scenario4() {
        logInWith(validAccessToken(), validIdToken(), refreshToken());
        xsrfCookieValue = null;

        val result = template.exchange(postWith(currentSessionAndXsrf()), String.class);
        assertThat(result.getStatusCode(), is(HttpStatus.FORBIDDEN));
    }

    private RequestEntity<String> postWith(HttpHeaders headers) {
        return RequestEntity.post(URI.create(URL))
                .headers(headers)
                .body("{ \"Hello\":\"World\"}");
    }

    private HttpHeaders currentSessionAndXsrf() {
        val headers = new HttpHeaders();
        Optional.ofNullable(sessionCookieValue).ifPresent(e -> headers.add("Cookie", e));
        Optional.ofNullable(xsrfCookieValue).ifPresent(e -> headers.add("Cookie", e));
        Optional.ofNullable(xsrfCookieValue).ifPresent(e -> headers.add("X-XSRF-TOKEN", xsrfCookieValue.substring(xsrfCookieValue.indexOf("=") + 1, xsrfCookieValue.indexOf(";"))));
        return headers;
    }
}
