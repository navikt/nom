package no.nav.nom.apps.ui.api.me;

import lombok.val;
import no.nav.nom.apps.ui.api.ApiTestBase;
import no.nav.nom.contract.nom.ui.me.MeTo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static no.nav.nom.apps.ui.common.Matchers.isOnlyRedirectTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class MeEndpointTest extends ApiTestBase {

    @Test
    @DisplayName("Call with authenticated session returns data")
    void scenario1() {
        logInWith(validIdToken());

        val result = template.exchange(requestWithCurrentSession("/api/v1/me"), MeTo.class);
        assertThat(result.getStatusCode(), is(HttpStatus.OK));
        assertThat(result.getBody(), is(notNullValue()));
        assertThat(result.getBody().getName(), is("Alice Wonderland"));
    }

    @Test
    @DisplayName("Call with no session returns no data and redirect to auth endpoint")
    void scenario2() {
        val result = template.getForEntity("/api/v1/me", MeTo.class);
        assertThat(result, isOnlyRedirectTo(AUTHENTICATION_ENDPOINT));
    }

    @Test
    @DisplayName("Call with unauthenticated session returns no data and redirect to auth endpoint")
    void scenario3() {
        val step1 = template.getForEntity("/", String.class);
        val step2 = template.getForEntity(getRedirectLocation(step1), String.class); // Ikke fulgt hele login-flyten, men fått sessjon
        sessionId = getSessionCookieId(step2);

        val result = template.exchange(requestWithCurrentSession("/api/v1/me"), MeTo.class);
        assertThat(result, isOnlyRedirectTo(AUTHENTICATION_ENDPOINT));
    }
}
