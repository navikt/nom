package no.nav.nom.apps.api.orgtilknytning.api.app;

import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.OrgtilknytningCommandService;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.EndreOrgtilknytningDato;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.EndreOrgtilknytningType;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.OpprettOrgtilknytning;
import no.nav.nom.apps.api.ressurs.eventsourcing.state.RessursStateListener;
import no.nav.nom.apps.api.rest.RestTestBase;
import no.nav.nom.contract.nom.Tilknytningtype;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static no.nav.nom.apps.api.common.TestData.orgEnhetState;
import static no.nav.nom.apps.api.common.TestData.orgtilknytningState;
import static no.nav.nom.apps.api.common.TestData.ressursState;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class OrgtilknytningControllerTest extends RestTestBase {

    @MockBean
    private OrgtilknytningCommandService orgtilknytningCommandService;

    @MockBean
    private RessursStateListener ressursStateListener;

    @Captor
    ArgumentCaptor<String> stringArgumentCaptor;

    @Captor
    ArgumentCaptor<EndreOrgtilknytningDato> endreOrgtilknytningDatoArgumentCaptor;

    @Captor
    ArgumentCaptor<EndreOrgtilknytningType> endreOrgtilknytningTypeArgumentCaptor;

    @Captor
    ArgumentCaptor<OpprettOrgtilknytning> opprettOrgtilknytningArgumentCaptor;

    @BeforeEach
    void setInitialLoadCompleteAndEnableUnleashAndClearData() {
        when(ressursStateListener.initloadComplete()).thenReturn(true);
        when(unleash.isEnabled(any())).thenReturn(true);
        clearOrgtilknytninger();
        ressursStateRepository.clear();
    }

    @Test
    @DisplayName("Should get all orgtilknytninger when no query params specified")
    void test_get_all_orgtilknytninger() {
        var existingOrgtilknytninger = List.of(
            orgtilknytningState(),
            orgtilknytningState()
        );

        saveOrgtilknytninger(existingOrgtilknytninger);

        var responseBody =  webTestClient
            .get()
            .uri("/app/orgtilknytninger")
            .headers(httpHeaders -> {
                httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                httpHeaders.setBearerAuth(noScopesUserToken());
            })
            .exchange()
            .expectStatus().is2xxSuccessful()
            .expectBodyList(OrgtilknytningState.class)
            .returnResult().getResponseBody();

        assertThat(responseBody)
            .hasSameElementsAs(existingOrgtilknytninger);
    }

    @Test
    @DisplayName("Should get all orgtilknytninger for navident")
    void test_get_all_orgtilknytninger_for_navident() {
        var navident = "Y654321";

        var orgtilknytningerForNavident = List.of(
            orgtilknytningState().toBuilder().navident(navident).build(),
            orgtilknytningState().toBuilder().navident(navident).build()
        );
        saveOrgtilknytninger(orgtilknytningerForNavident);

        var otherOrgtilknytninger = List.of(orgtilknytningState(), orgtilknytningState());
        saveOrgtilknytninger(otherOrgtilknytninger);

        var responseBody = webTestClient
            .get()
            .uri(uriBuilder -> uriBuilder
                .path("/app/orgtilknytninger")
                .queryParam("navident", navident)
                .build()
            )
            .headers(httpHeaders -> {
                httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                httpHeaders.setBearerAuth(noScopesUserToken());
            })
            .exchange()
            .expectStatus().is2xxSuccessful()
            .expectBodyList(OrgtilknytningState.class)
            .returnResult().getResponseBody();

        assertThat(responseBody)
            .hasSameElementsAs(orgtilknytningerForNavident);
    }

    @Test
    @DisplayName("Should update dato for orgtilknytning")
    void test_update_orgtilknytning_date() {

        var orgtilknytningToUpdate = orgtilknytningState();
        saveOrgtilknytninger(List.of(orgtilknytningToUpdate));

        saveRessurser(List.of(
            ressursState().toBuilder()
                .navident(orgtilknytningToUpdate.navident())
                .sektor(Sektor.EKSTERN)
                .build()
        ));

        var endreOrgtilknytningDatoRequest = new EndreOrgtilknytningDatoRequest(
            LocalDate.now(),
            Optional.of(LocalDate.now().plusYears(1))
        );


        webTestClient
            .post()
            .uri(uriBuilder -> uriBuilder
                .path("/app/orgtilknytninger/{fid}/dato")
                .build(orgtilknytningToUpdate.fid())
            )
            .headers(httpHeaders -> {
                httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                httpHeaders.setBearerAuth(nomWriteUserToken());
            })
            .bodyValue(endreOrgtilknytningDatoRequest)
            .exchange()
            .expectStatus().is2xxSuccessful();

        verify(
            orgtilknytningCommandService,
            times(1)
        ).endreOrgtilknytningDato(
            stringArgumentCaptor.capture(),
            endreOrgtilknytningDatoArgumentCaptor.capture()
        );

        assertThat(stringArgumentCaptor.getValue()).isEqualTo(orgtilknytningToUpdate.fid());
        assertThat(endreOrgtilknytningDatoArgumentCaptor.getValue())
            .matches(endreOrgtilknytningDato ->
                endreOrgtilknytningDato.gyldigFom().equals(endreOrgtilknytningDatoRequest.gyldigFom())
            )
            .matches(endreOrgtilknytningDato ->
                endreOrgtilknytningDato.gyldigTom().equals(endreOrgtilknytningDatoRequest.gyldigTom())
            );
    }

    @Test
    @DisplayName("Should not be able to update dato if it leads to no active orgtilknytninger")
    void test_disallow_update_if_no_remaining_active() {

        var orgtilknytningToUpdate = orgtilknytningState();
        saveOrgtilknytninger(List.of(orgtilknytningToUpdate));

        saveRessurser(List.of(
            ressursState().toBuilder()
                .navident(orgtilknytningToUpdate.navident())
                .sektor(Sektor.EKSTERN)
                .build()
        ));

        var endreOrgtilknytningDatoRequest = new EndreOrgtilknytningDatoRequest(
            orgtilknytningToUpdate.gyldigFom(),
            Optional.of(LocalDate.now().minusDays(1))
        );


        webTestClient
            .post()
            .uri(uriBuilder -> uriBuilder
                .path("/app/orgtilknytninger/{fid}/dato")
                .build(orgtilknytningToUpdate.fid())
            )
            .headers(httpHeaders -> {
                httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                httpHeaders.setBearerAuth(nomWriteUserToken());
            })
            .bodyValue(endreOrgtilknytningDatoRequest)
            .exchange()
            .expectStatus().isBadRequest();

        verify(
            orgtilknytningCommandService,
            times(0)
        ).endreOrgtilknytningDato(
            stringArgumentCaptor.capture(),
            endreOrgtilknytningDatoArgumentCaptor.capture()
        );
    }

    @Test
    @DisplayName("Should block update dato for orgtilknytning if tilknyttet ressurs is not EKSTERN")
    void test_update_orgtilknytning_for_ekstern_only_date() {
        var orgtilknytningToUpdate = orgtilknytningState();
        saveOrgtilknytninger(List.of(orgtilknytningToUpdate));

        saveRessurser(List.of(
            ressursState().toBuilder()
                .navident(orgtilknytningToUpdate.navident())
                .sektor(Sektor.NAV_STATLIG)
                .build()
        ));

        webTestClient
            .post()
            .uri(uriBuilder -> uriBuilder
                .path("/app/orgtilknytninger/{fid}/dato")
                .build(orgtilknytningToUpdate.fid())
            )
            .headers(httpHeaders -> {
                httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                httpHeaders.setBearerAuth(nomWriteUserToken());
            })
            .bodyValue(
                new EndreOrgtilknytningDatoRequest(
                    LocalDate.now(),
                    Optional.of(LocalDate.now().plusYears(1))
                )
            )
            .exchange()
            .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("Should forbid update dato if user does not have granted authority 'MUTATE_RESSURS'")
    void test_update_orgtilknytning_forbidden() {

        webTestClient
            .post()
            .uri(uriBuilder -> uriBuilder
                .path("/app/orgtilknytninger/{fid}/dato")
                .build("some-fid")
            )
            .headers(httpHeaders -> {
                httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                httpHeaders.setBearerAuth(noScopesUserToken());
            })
            .bodyValue(
                new EndreOrgtilknytningDatoRequest(
                    LocalDate.now(),
                    Optional.of(LocalDate.now().plusYears(1))
                )
            )
            .exchange()
            .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("Should update orgtilknytningType for orgtilknytning")
    void test_update_orgtilknytning_type() {

        var orgtilknytningToUpdate = orgtilknytningState();
        saveOrgtilknytninger(List.of(orgtilknytningToUpdate));

        saveRessurser(List.of(
            ressursState().toBuilder()
                .navident(orgtilknytningToUpdate.navident())
                .sektor(Sektor.EKSTERN)
                .build()
        ));

        var endreOrgtilknytningTypeRequest = new EndreTilknytningtypeRequest(
            Optional.of(Tilknytningtype.KONSULENT)
        );


        webTestClient
            .post()
            .uri(uriBuilder -> uriBuilder
                .path("/app/orgtilknytninger/{fid}/orgtilknytningType")
                .build(orgtilknytningToUpdate.fid())
            )
            .headers(httpHeaders -> {
                httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                httpHeaders.setBearerAuth(nomWriteUserToken());
            })
            .bodyValue(endreOrgtilknytningTypeRequest)
            .exchange()
            .expectStatus().is2xxSuccessful();

        verify(
            orgtilknytningCommandService,
            times(1)
        ).endreOrgtilknytningType(
            stringArgumentCaptor.capture(),
            endreOrgtilknytningTypeArgumentCaptor.capture()
        );

        assertThat(stringArgumentCaptor.getValue()).isEqualTo(orgtilknytningToUpdate.fid());
        assertThat(endreOrgtilknytningTypeArgumentCaptor.getValue())
            .matches(endreOrgtilknytningType ->
                endreOrgtilknytningType.orgtilknytningType().equals(endreOrgtilknytningTypeRequest.tilknytningtype())
            );
    }

    @Test
    @DisplayName("Should block update orgtilknytningType for orgtilknytning if tilknyttet ressurs is not EKSTERN")
    void test_update_orgtilknytning_type_for_ekstern_only() {
        var orgtilknytningToUpdate = orgtilknytningState();
        saveOrgtilknytninger(List.of(orgtilknytningToUpdate));

        saveRessurser(List.of(
            ressursState().toBuilder()
                .navident(orgtilknytningToUpdate.navident())
                .sektor(Sektor.NAV_STATLIG)
                .build()
        ));

        webTestClient
            .post()
            .uri(uriBuilder -> uriBuilder
                .path("/app/orgtilknytninger/{fid}/orgtilknytningType")
                .build(orgtilknytningToUpdate.fid())
            )
            .headers(httpHeaders -> {
                httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                httpHeaders.setBearerAuth(nomWriteUserToken());
            })
            .bodyValue(
                new EndreTilknytningtypeRequest(
                    Optional.empty()
                )
            )
            .exchange()
            .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("Should forbid update orgtilknytning type if user does not have granted authority 'MUTATE_RESSURS'")
    void test_update_orgtilknytning_type_forbidden() {

        webTestClient
            .post()
            .uri("/app/orgtilknytninger/some-fid/orgtilknytningType")
            .headers(httpHeaders -> {
                httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                httpHeaders.setBearerAuth(noScopesUserToken());
            })
            .bodyValue(
                new EndreTilknytningtypeRequest(Optional.empty())
            )
            .exchange()
            .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("Should add new orgtilknytning")
    void test_add_orgtilknytning() {
        var navident = "Y654321";
        var orgEnhet = orgEnhetState();

        saveOrgenheter(List.of(orgEnhet));
        saveRessurser(List.of(
            ressursState().toBuilder()
                .navident(navident)
                .sektor(Sektor.EKSTERN)
                .build()
        ));

        var existingOrgtilknytning = orgtilknytningState().toBuilder()
            .gyldigFom(LocalDate.now().minusMonths(3))
            .gyldigTom(Optional.of(LocalDate.now().plusMonths(3)))
            .build();
        saveOrgtilknytninger(List.of(existingOrgtilknytning));

        var leggTilOrgtilknytningRequest = new LeggTilOrgtilknytningRequest(
            navident,
            orgEnhet.id(),
            LocalDate.now(),
            Optional.of(LocalDate.now().plusMonths(5))
        );

        webTestClient
            .post()
            .uri("/app/orgtilknytninger")
            .headers(httpHeaders -> {
                httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                httpHeaders.setBearerAuth(nomWriteUserToken());
            })
            .bodyValue(leggTilOrgtilknytningRequest)
            .exchange()
            .expectStatus().is2xxSuccessful();

        verify(
            orgtilknytningCommandService,
            times(1)
        ).opprettOrgtilknytning(
            opprettOrgtilknytningArgumentCaptor.capture()
        );

        verify(
            orgtilknytningCommandService,
            times(1)
        ).opprettOrgtilknytning(
            opprettOrgtilknytningArgumentCaptor.capture()
        );

        assertThat(opprettOrgtilknytningArgumentCaptor.getValue())
            .matches(opprettOrgtilknytning ->
                opprettOrgtilknytning.navident().equals(leggTilOrgtilknytningRequest.navident())
            )
            .matches(opprettOrgtilknytning ->
                opprettOrgtilknytning.orgenhetId().equals(leggTilOrgtilknytningRequest.orgEnhetId())
            );
    }

    @Test
    @DisplayName("Verify navident in orgtilknytning exists")
    void test_navident_for_orgtilknytning_exists() {
        var navident = "Y654321";
        var orgenhet = orgEnhetState();
        saveOrgenheter(List.of(orgenhet));

        var leggTilOrgtilknytningRequest = new LeggTilOrgtilknytningRequest(
            navident,
            orgenhet.id(),
            LocalDate.now(),
            Optional.of(LocalDate.now().plusYears(1))
        );

        webTestClient
            .post()
            .uri("/app/orgtilknytninger")
            .headers(httpHeaders -> {
                httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                httpHeaders.setBearerAuth(nomWriteUserToken());
            })
            .bodyValue(leggTilOrgtilknytningRequest)
            .exchange()
            .expectStatus().isBadRequest();

        verify(
            orgtilknytningCommandService,
            times(0)
        ).opprettOrgtilknytning(
            opprettOrgtilknytningArgumentCaptor.capture()
        );
    }

    @Test
    @DisplayName("Verify orgEnhetId in orgtilknytning exists")
    void test_orgEnhet_for_orgtilknytning_exists() {
        var navident = "Y654321";
        var orgEnhetId = "orgEnhet123";

        saveRessurser(List.of(
            ressursState().toBuilder()
                .navident(navident)
                .sektor(Sektor.EKSTERN)
                .build()
        ));

        var leggTilOrgtilknytningRequest = new LeggTilOrgtilknytningRequest(
            navident,
            orgEnhetId,
            LocalDate.now(),
            Optional.of(LocalDate.now().plusYears(1))
        );

        webTestClient
            .post()
            .uri("/app/orgtilknytninger")
            .headers(httpHeaders -> {
                httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                httpHeaders.setBearerAuth(nomWriteUserToken());
            })
            .bodyValue(leggTilOrgtilknytningRequest)
            .exchange()
            .expectStatus().isBadRequest();

        verify(
            orgtilknytningCommandService,
            times(0)
        ).opprettOrgtilknytning(
            opprettOrgtilknytningArgumentCaptor.capture()
        );
    }

    @Test
    @DisplayName("Verify no overlapping period for unique orgenhetId + navident combination")
    void test_unique_orgenhet_navident_for_orgtilknytning() {
        var navident = "Y654321";
        var orgEnhet = orgEnhetState();

        var existingOrgtilknytning = orgtilknytningState().toBuilder()
            .navident(navident)
            .orgEnhetId(orgEnhet.id())
            .gyldigFom(LocalDate.now())
            .gyldigTom(Optional.empty())
            .build();

        saveOrgtilknytninger(List.of(existingOrgtilknytning));
        saveRessurser(List.of(
            ressursState().toBuilder()
                .navident(navident)
                .sektor(Sektor.EKSTERN)
                .build()
        ));
        saveOrgenheter(List.of(orgEnhet));


        var leggTilOrgtilknytningRequest = new LeggTilOrgtilknytningRequest(
            navident,
            orgEnhet.id(),
            LocalDate.now().plusMonths(1),
            Optional.of(LocalDate.now().plusMonths(6))
        );

        webTestClient
            .post()
            .uri("/app/orgtilknytninger")
            .headers(httpHeaders -> {
                httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                httpHeaders.setBearerAuth(nomWriteUserToken());
            })
            .bodyValue(leggTilOrgtilknytningRequest)
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.CONFLICT);

        verify(
            orgtilknytningCommandService,
            times(0)
        ).opprettOrgtilknytning(
            opprettOrgtilknytningArgumentCaptor.capture()
        );
    }

    @Test
    @DisplayName("Should get orgtilknytning for fid")
    void test_get_orgtilknytning() {
        var orgtilknytningStateToGet = orgtilknytningState();

        saveOrgtilknytninger(List.of(orgtilknytningStateToGet, orgtilknytningState()));

        var responseBody =  webTestClient
            .get()
            .uri(uriBuilder -> uriBuilder
                .path("/app/orgtilknytninger/{fid}")
                .build(orgtilknytningStateToGet.fid())
            )
            .headers(httpHeaders -> {
                httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                httpHeaders.setBearerAuth(noScopesUserToken());
            })
            .exchange()
            .expectStatus().is2xxSuccessful()
            .expectBody(OrgtilknytningState.class)
            .returnResult().getResponseBody();

        assertThat(responseBody)
            .usingRecursiveComparison()
            .isEqualTo(orgtilknytningStateToGet);
    }

    @Test
    @DisplayName("Should get 404 if orgtilknytning with fid is not found")
    void test_get_orgtilknytning_not_found() {
        saveOrgtilknytninger(List.of(orgtilknytningState()));

        webTestClient
            .get()
            .uri(uriBuilder -> uriBuilder
                .path("/app/orgtilknytninger/{fid}")
                .build(UUID.randomUUID().toString())
            )
            .headers(httpHeaders -> {
                httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                httpHeaders.setBearerAuth(noScopesUserToken());
            })
            .exchange()
            .expectStatus().is4xxClientError();

    }

}
