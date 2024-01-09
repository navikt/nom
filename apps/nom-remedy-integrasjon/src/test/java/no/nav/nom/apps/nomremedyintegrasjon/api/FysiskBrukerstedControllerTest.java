package no.nav.nom.apps.nomremedyintegrasjon.api;

import com.nimbusds.jose.JOSEObjectType;
import no.nav.nom.apps.nomremedyintegrasjon.TestBase;
import no.nav.security.mock.oauth2.MockOAuth2Server;
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FysiskBrukerstedControllerTest extends TestBase {

    @Autowired
    private MockOAuth2Server mockOAuth2Server;

    @Autowired
    private WebTestClient webTestClient;

    private final String apiPath = "/api/fysiskebrukersteder";

    @Test
    @DisplayName("Should get fysiske brukersteder")
    public void testGetFysiskeBrukerstederOk() {

        var fysiskeBrukersteder = List.of(
            new FysiskBrukersted("", "", "")
        );

        when(fysiskBrukerstedReadOnlyRepository.findAll()).thenReturn(fysiskeBrukersteder);

        webTestClient.get()
            .uri(apiPath)
            .headers(httpHeaders -> httpHeaders.setBearerAuth(
                generateToken("nom-remedy-integrasjon")
            ))
            .exchange()
            .expectStatus().isOk()
            .expectBodyList(FysiskBrukersted.class)
            .isEqualTo(fysiskeBrukersteder);
    }

    @Test
    @DisplayName("Should filter fysiske brukersteder")
    public void testFilterFysiskeBrukerstederOk() {

        var fysiskeBrukersteder = List.of(
            new FysiskBrukersted("", "", "")
        );

        var brukerstedNrsFilter = List.of("a", "b");

        when(fysiskBrukerstedReadOnlyRepository.findByBrukerstedNrIn(anyList())).thenReturn(fysiskeBrukersteder);

        webTestClient.get()
            .uri(uriBuilder -> uriBuilder
                .path(apiPath)
                .queryParam("brukerstedNrs", brukerstedNrsFilter)
                .build()
            )
            .headers(httpHeaders -> httpHeaders.setBearerAuth(
                generateToken("nom-remedy-integrasjon")
            ))
            .exchange()
            .expectStatus().isOk()
            .expectBodyList(FysiskBrukersted.class)
            .isEqualTo(fysiskeBrukersteder);

        verify(fysiskBrukerstedReadOnlyRepository, times(1))
            .findByBrukerstedNrIn(eq(brukerstedNrsFilter));
    }

    @Test
    @DisplayName("Should get 401 unauthorized for invalid audience")
    public void testGetFysiskeBrukerstederUnauthorized() {
        webTestClient.get()
            .uri(apiPath)
            .headers(httpHeaders -> httpHeaders.setBearerAuth(
                generateToken("invalid-audience")
            ))
            .exchange()
            .expectStatus().isUnauthorized();
    }

    private String generateToken(String audience) {
        return mockOAuth2Server.issueToken(
            "issuer",
            "clientId",
            new DefaultOAuth2TokenCallback(
                "aad",
                "subjectId",
                JOSEObjectType.JWT.getType(),
                List.of(audience),
                Collections.emptyMap(),
                3600
            )
        ).serialize();
    }
}
