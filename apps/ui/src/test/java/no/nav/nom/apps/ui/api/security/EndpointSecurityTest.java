package no.nav.nom.apps.ui.api.security;

import lombok.SneakyThrows;
import lombok.Value;
import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;

import java.net.URI;
import java.util.List;

import static no.nav.nom.apps.ui.common.Matchers.isOnlyRedirectTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@DisplayName("Endpoint Security")
class EndpointSecurityTest extends SecurityTestBase {

    @ParameterizedTest
    @DisplayName("Endpoints")
    @MethodSource("allEndpoints")
    void scenario1(Endpoint endpoint) {
        if (endpoint.isSecured() && HttpMethod.GET == endpoint.getMethod()) {
            callWithNoSession(endpoint);
            callWithUnauthenticatedSession(endpoint);
        } else {
            callWithNoSession(endpoint);
            callWithUnauthenticatedSession(endpoint);
            callWithAuthenticatedSessionButMissingXsrf(endpoint);
            callWithAuthenticatedSessionButInvalidXsrf(endpoint);
        }
    }

    private void callWithNoSession(Endpoint endpoint) {
        val result = execute(endpoint, new HttpHeaders());
        assertThat(result, isOnlyRedirectTo(AUTHENTICATION_ENDPOINT));
    }

    private void callWithUnauthenticatedSession(Endpoint endpoint) {
        val step1 = template.getForEntity("/", String.class);
        val step2 = template.getForEntity(getRedirectLocation(step1), String.class); // Ikke fulgt hele login-flyten, men fått sessjon
        sessionCookieValue = getSessionCookieId(step2);

        val headers = new HttpHeaders();
        headers.add("Cookie", sessionCookieValue);
        val result = execute(endpoint, headers);
        assertThat(result, isOnlyRedirectTo(AUTHENTICATION_ENDPOINT));
    }

    private void callWithAuthenticatedSessionButMissingXsrf(Endpoint endpoint) {
        logInWith(validAccessToken(), validIdToken(), refreshToken());

        val headers = new HttpHeaders();
        headers.add("Cookie", sessionCookieValue);
        val result = execute(endpoint, headers);
        assertThat(result.getStatusCode(), is(HttpStatus.FORBIDDEN));
    }

    private void callWithAuthenticatedSessionButInvalidXsrf(Endpoint endpoint) {
        logInWith(validAccessToken(), validIdToken(), refreshToken());

        val headers = new HttpHeaders();
        headers.add("Cookie", sessionCookieValue);
        headers.add("Cookie", xsrfCookieValue);
        headers.add("X-XSRF-TOKEN", "somethingElse");
        val result = execute(endpoint, headers);
        assertThat(result.getStatusCode(), is(HttpStatus.FORBIDDEN));
    }

    @SneakyThrows
    private ResponseEntity<?> execute(Endpoint endpoint, HttpHeaders headers) {
        if (endpoint.getMethod() == HttpMethod.GET) {
            val req = RequestEntity.get(new URI(endpoint.getUrl()))
                    .headers(headers)
                    .build();
            return template.exchange(req, String.class);
        } else if (endpoint.getMethod() == HttpMethod.POST) {
            val req = RequestEntity.post(new URI(endpoint.getUrl()))
                    .headers(headers)
                    .body(""); // Ikke så viktig med Body, skal uansett aldri komme så langt som koden.
            return template.exchange(req, String.class);
        }
        return null;
    }

    @Value
    private static class Endpoint {
        String url;
        HttpMethod method;
        boolean secured;
    }

    public static List<Endpoint> allEndpoints() {
        return List.of(
                new Endpoint("/api/v1/me", HttpMethod.GET, true),
                new Endpoint("/api/v1/nom-api/graphql", HttpMethod.POST, true)
        );
    }
}
