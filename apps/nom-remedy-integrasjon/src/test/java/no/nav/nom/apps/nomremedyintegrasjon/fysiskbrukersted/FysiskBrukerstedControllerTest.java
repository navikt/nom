package no.nav.nom.apps.nomremedyintegrasjon.fysiskbrukersted;

import no.nav.nom.apps.nomremedyintegrasjon.TestBase;
import no.nav.nom.apps.nomremedyintegrasjon.fysiskbrukersted.repository.FysiskBrukersted;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FysiskBrukerstedControllerTest extends TestBase {

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
                noScopesUserToken()
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
                noScopesUserToken()
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
                invalidAudienceToken()
            ))
            .exchange()
            .expectStatus().isUnauthorized();
    }

}
