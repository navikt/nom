package no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon;

import no.nav.nom.apps.nomremedyintegrasjon.TestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.LocalDate;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class AdminControllerTest extends TestBase {

    @MockBean
    NomIntegrasjonService nomIntegrasjonService;

    @Test
    @DisplayName("POST /admin/slettFraRemedy should delete inactive ressurser from remedy")
    void testDeleteRessurserFromRemedy() {

        var sluttdato = LocalDate.now().minusDays(1);

        webTestClient.post()
            .uri(uriBuilder -> uriBuilder
                .path("/admin/slettFraRemedy")
                .queryParam("sluttdato", sluttdato.toString())
                .build()
            )
            .headers(httpHeaders -> httpHeaders.setBearerAuth(
                nomAdminUserToken()
            ))
            .exchange()
            .expectStatus().isOk();

        verify(nomIntegrasjonService).findRessurserWithSluttdatoAndMarkForDeletion(
            eq(sluttdato), eq(LOGGED_IN_USER_NAVIDENT)
        );
    }

    @ParameterizedTest(name = "sluttdato: {0}")
    @DisplayName("POST /admin/slettFraRemedy should NOT delete active ressurser from remedy")
    @MethodSource("testNotDeleteActiveRessurserFromRemedyInput")
    void testNotDeleteActiveRessurserFromRemedy(LocalDate sluttdato) {

        webTestClient.post()
            .uri(uriBuilder -> uriBuilder
                .path("/admin/slettFraRemedy")
                .queryParam("sluttdato", sluttdato.toString())
                .build()
            )
            .headers(httpHeaders -> httpHeaders.setBearerAuth(
                nomAdminUserToken()
            ))
            .exchange()
            .expectStatus().isBadRequest();

        verify(nomIntegrasjonService, times(0)).findRessurserWithSluttdatoAndMarkForDeletion(any(), any());
    }

    private static Stream<Arguments> testNotDeleteActiveRessurserFromRemedyInput() {
        return Stream.of(
            Arguments.of(LocalDate.now()),
            Arguments.of(LocalDate.now().plusDays(1))
        );
    }

    @Test
    @DisplayName("POST /admin/slettFraRemedy should return 403 FORBIDDEN for non admin users")
    void testDeleteRessurserFromRemedyForbidden() {

        webTestClient.post()
            .uri(uriBuilder -> uriBuilder
                .path("/admin/slettFraRemedy")
                .queryParam("sluttdato", LocalDate.now().minusDays(1))
                .build()
            )
            .headers(httpHeaders -> httpHeaders.setBearerAuth(
                noScopesUserToken()
            ))
            .exchange()
            .expectStatus().isForbidden();
    }
}
