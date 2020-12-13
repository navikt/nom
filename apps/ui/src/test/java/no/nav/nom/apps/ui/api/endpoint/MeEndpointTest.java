package no.nav.nom.apps.ui.api.endpoint;

import lombok.val;
import no.nav.nom.apps.ui.api.ApiTestBase;
import no.nav.nom.contract.nom.ui.me.MeTo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@DisplayName("api/v1/me")
class MeEndpointTest extends ApiTestBase {

    @Test
    @DisplayName("Call with authenticated session returns data")
    void scenario1() {
        logInWith(validAccessToken(), validIdToken(), refreshToken());

        val result = template.exchange(requestWithCurrentSession("/api/v1/me"), MeTo.class);
        assertThat(result.getStatusCode(), is(HttpStatus.OK));
        assertThat(result.getBody(), is(notNullValue()));
        assertThat(result.getBody().getName(), is("Alice Wonderland"));
    }
}
