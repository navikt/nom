package no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon;

import no.nav.nom.apps.nomremedyintegrasjon.TestBase;
import no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.repository.errorstate.ErrorState;
import no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.repository.remedy.NomIntegrasjonEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;


public class AdminControllerTest extends TestBase {

    @MockitoBean
    NomRemedyIntegrasjonService nomRemedyIntegrasjonService;

    @Test
    @DisplayName("GET /admin/bestillinger burde returnere alle bestillinger til remedy")
    void test_001() {
        when(nomRemedyIntegrasjonService.getNomIntegrasjonEntities()).thenReturn(List.of(nomIntegrasjonEntity()));

        var nomIntegrasjonEnitites = webTestClient.get().uri("/admin/bestillinger")
            .headers(httpHeaders -> httpHeaders.setBearerAuth(nomAdminUserToken()))
            .exchange()
            .expectStatus().isOk()
            .expectBodyList(NomIntegrasjonEntity.class)
            .returnResult().getResponseBody();

        assertThat(nomIntegrasjonEnitites)
            .hasSize(1)
            .allMatch(nomIntegrasjonEntity -> {
                assertThat(nomIntegrasjonEntity.getEntryId()).isEqualTo("1");
                assertThat(nomIntegrasjonEntity.getEntryStatus()).isEqualTo("Ny");
                assertThat(nomIntegrasjonEntity.getNavId()).isEqualTo("N123456");
                assertThat(nomIntegrasjonEntity.getFodselsnummer()).isEqualTo("1234567812345");
                return true;
            });

    }

    @Test
    @DisplayName("GET /admin/bestillinger should return 403 FORBIDDEN for non admin users")
    void test_002() {

        webTestClient.get()
            .uri("/admin/bestillinger")
            .headers(httpHeaders -> httpHeaders.setBearerAuth(
                noScopesUserToken()
            ))
            .exchange()
            .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("POST /admin/lesOgEksekverRessursState/{navident} test at ny kjører uten feil")
    void test_003() {
        webTestClient.post()
            .uri("/admin/lesOgEksekverRessursState/S123456")
            .headers(httpHeaders -> httpHeaders.setBearerAuth(nomAdminUserToken()))
            .exchange()
            .expectStatus().isOk();
    }

    @Test
    @DisplayName("POST /admin/lesOgEksekverRessursState/{navident} burde returnere 403 FORBIDDEN for ikke admin brukere")
    void test_004() {
        webTestClient.post()
            .uri("/admin/lesOgEksekverRessursState/S123456")
            .headers(httpHeaders -> httpHeaders.setBearerAuth(noScopesUserToken()))
            .exchange()
            .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("GET /admin/getAllErrorState at du får alle errorStates i errorStateTabellen")
    void test_005() {
        when(nomRemedyIntegrasjonService.getAllErrorStates()).thenReturn(List.of(
            errorState()
        ));

        var errorStates = webTestClient.get()
            .uri("/admin/getAllErrorState")
            .headers(httpHeaders -> httpHeaders.setBearerAuth(nomAdminUserToken()))
            .exchange()
            .expectStatus().isOk()
            .expectBodyList(ErrorState.class)
            .returnResult().getResponseBody();

        assertThat(errorStates)
            .hasSize(1)
            .allMatch(errorState -> {
                assertThat(errorState.getErrorStateFid()).isEqualTo("fid-1");
                assertThat(errorState.getNavident()).isEqualTo("S123456");
                return true;
            });
    }

    @Test
    @DisplayName("GET /admin/getAllErrorState burde returnere 403 FORBIDDEN for ikke admin brukere")
    void test_006() {
        webTestClient.get()
            .uri("/admin/getAllErrorState")
            .headers(httpHeaders -> httpHeaders.setBearerAuth(noScopesUserToken()))
            .exchange()
            .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("DELETE /admin/deleteAllErrorStates sletter alle uten feil")
    void test_007() {
        webTestClient.delete()
            .uri("/admin/deleteAllErrorStates")
            .headers(httpHeaders -> httpHeaders.setBearerAuth(nomAdminUserToken()))
            .exchange()
            .expectStatus().isOk();
    }

    @Test
    @DisplayName("DELETE /admin/deleteAllErrorStates burde returnere 403 FORBIDDEN for ikke admin brukere")
    void test_008() {
        webTestClient.delete()
            .uri("/admin/deleteAllErrorStates")
            .headers(httpHeaders -> httpHeaders.setBearerAuth(noScopesUserToken()))
            .exchange()
            .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("DELETE /admin/deleteErrorState/{fid} slett en enkel error uten feil")
    void test_009() {
        webTestClient.delete()
            .uri("/admin/deleteErrorState/fid-1")
            .headers(httpHeaders -> httpHeaders.setBearerAuth(nomAdminUserToken()))
            .exchange()
            .expectStatus().isOk();
    }

    @Test
    @DisplayName("DELETE /admin/deleteErrorState/{fid} burde returnere 403 FORBIDDEN for ikke admin brukere")
    void test_0010() {
        webTestClient.delete()
            .uri("/admin/deleteErrorState/fid-1")
            .headers(httpHeaders -> httpHeaders.setBearerAuth(noScopesUserToken()))
            .exchange()
            .expectStatus().isForbidden();
    }

    private static ErrorState errorState() {
        return ErrorState.builder()
            .errorId(UUID.randomUUID())
            .errorStateFid("fid-1")
            .errorStateType("FID")
            .errorMessage("error message")
            .createdAt(LocalDateTime.now())
            .navident("S123456")
            .build();
    }


    private static NomIntegrasjonEntity nomIntegrasjonEntity() {
        return NomIntegrasjonEntity.builder()
            .entryId("1")
            .entryStatus("Ny")
            .entryOperasjon("Move")
            .fornavn("Nav")
            .etternavn("Navesen")
            .navId("N123456")
            .fodselsnummer("1234567812345")
            .sektor(0)
            .enhetsId("2990")
            .fysiskBrukerstedId("2700")
            .tiltredelsesdato(12L)
            .utlopsdato(null)
            .mobil("48484848")
            .direkteTelefon(null)
            .masseflytting(null)
            .build();
    }
}
