package no.nav.nom.apps.ui.api.endpoint;

import lombok.val;
import no.nav.nom.apps.ui.api.ApiTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;

import java.net.URI;

import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@DisplayName("api/v1/nom-api/graphql")
class NomApiProxyTest extends ApiTestBase {
    private static final String URL = "/api/v1/nom-api/graphql";

    @Test
    @DisplayName("Call with authenticated session and xsrf token returns data")
    void scenario1() {
        logInWith(validAccessToken(), validIdToken(), refreshToken());
        stubAadForOnBehalfOf(validAccessToken(), refreshToken());

        val result = template.exchange(postWith(headersWithCurrentSessionAndXsrf()), String.class);
        assertThat(result.getStatusCode(), is(HttpStatus.OK));
        assertThat(result.getBody(), is(notNullValue()));
        assertThat(result.getBody(), containsString("data"));
    }

    @Test
    @DisplayName("Call is made with On-Behalf-Of token")
    void scenario2() {
        logInWith(validAccessToken(), validIdToken(), refreshToken());
        val tokens = stubAadForOnBehalfOf(validAccessToken(), refreshToken());

        template.exchange(postWith(headersWithCurrentSessionAndXsrf()), String.class);

        val nomApiRequest = findAll(postRequestedFor(urlEqualTo("/nom-api/graphql")));
        assertThat(nomApiRequest, hasSize(1));
        assertThat(nomApiRequest.get(0).getHeaders().getHeader(HttpHeaders.AUTHORIZATION).firstValue(), is("Bearer " + tokens.getAccessToken()));
    }

    private RequestEntity<String> postWith(HttpHeaders headers) {
        return RequestEntity.post(URI.create(URL))
                .headers(headers)
                .body("{ \"Hello\":\"World\"}");
    }
}
