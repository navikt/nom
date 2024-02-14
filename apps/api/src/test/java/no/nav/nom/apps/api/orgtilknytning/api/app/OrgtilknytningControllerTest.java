package no.nav.nom.apps.api.orgtilknytning.api.app;

import no.nav.nom.apps.api.orgtilknytning.api.exceptions.OrgtilknytningPeriodeOverlapException;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.OrgtilknytningCommandService;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.EndreDagligOppfolgingOrgtilknytning;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.EndreOrgtilknytningDato;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.EndreTilknytningtype;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.OpprettOrgtilknytning;
import no.nav.nom.apps.api.remedy.RemedyEnhet;
import no.nav.nom.apps.api.remedy.RemedyEnhetMapperService;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.RessursCommadService;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OppdaterSluttdatoCommand;
import no.nav.nom.apps.api.ressurs.eventsourcing.state.RessursStateListener;
import no.nav.nom.apps.api.rest.RestTestBase;
import no.nav.nom.contract.nom.Tilknytningtype;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

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
    private RessursCommadService ressursCommadService;

    @MockBean
    private RessursStateListener ressursStateListener;

    @MockBean
    private RemedyEnhetMapperService remedyEnhetMapperService;

    @Captor
    ArgumentCaptor<String> stringArgumentCaptor;

    @Captor
    ArgumentCaptor<EndreOrgtilknytningDato> endreOrgtilknytningDatoArgumentCaptor;

    @Captor
    ArgumentCaptor<EndreTilknytningtype> endreTilknytningtypeArgumentCaptor;

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
    @DisplayName("POST /app/orgtilknytninger/{fid}/dato: Should update dato for orgtilknytning and NOT update ressurs sluttdato")
    void test_update_orgtilknytning_date() {

        var orgtilknytningToUpdate = orgtilknytningState();
        saveOrgtilknytninger(List.of(orgtilknytningToUpdate));

        saveRessurser(List.of(
            ressursState().toBuilder()
                .navident(orgtilknytningToUpdate.navident())
                .sektor(Sektor.EKSTERN)
                .build()
        ));

        var endreOrgtilknytningDatoRequest = endreOrgtilknytningDatoRequest().toBuilder()
            .setRessursSluttdato(Optional.empty())
            .build();

        postEndreOrgtilknytningDatoRequest(
            orgtilknytningToUpdate.fid(), nomWriteGroupAndNomUiAzpToken(), endreOrgtilknytningDatoRequest
        ).expectStatus().is2xxSuccessful();

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

        verify(ressursCommadService, times(0)).oppdaterRessursSluttdatoEvent(any());
    }

    @Test
    @DisplayName("POST /app/orgtilknytninger/{fid}/dato: Should update sluttdato for ressurs on update orgtilknytning dato when setRessursSluttdato=true")
    void test_update_ressurs_sluttdato_on_update_orgtilknytning_dato() {

        var orgtilknytningToUpdate = orgtilknytningState();
        saveOrgtilknytninger(List.of(orgtilknytningToUpdate));

        saveRessurser(List.of(
            ressursState().toBuilder()
                .navident(orgtilknytningToUpdate.navident())
                .sektor(Sektor.EKSTERN)
                .build()
        ));

        var newSluttdato = LocalDate.now().plusMonths(5);
        var endreOrgtilknytningDatoRequest = endreOrgtilknytningDatoRequest().toBuilder()
            .gyldigTom(Optional.of(newSluttdato))
            .setRessursSluttdato(Optional.of(true))
            .build();

        postEndreOrgtilknytningDatoRequest(
            orgtilknytningToUpdate.fid(), nomWriteGroupAndNomUiAzpToken(), endreOrgtilknytningDatoRequest
        ).expectStatus().is2xxSuccessful();

        var oppdaterSluttdatoCommandArgumentCaptor = ArgumentCaptor.forClass(OppdaterSluttdatoCommand.class);

        verify(
            ressursCommadService,
            times(1)
        ).oppdaterRessursSluttdatoEvent(
            oppdaterSluttdatoCommandArgumentCaptor.capture()
        );

        assertThat(oppdaterSluttdatoCommandArgumentCaptor.getValue())
            .matches(oppdaterSluttdatoCommand ->
                oppdaterSluttdatoCommand.sluttdato().equals(endreOrgtilknytningDatoRequest.gyldigTom())
            );
    }

    @Test
    @DisplayName("POST /app/orgtilknytninger/{fid}/dato: Should NOT update sluttdato for ressurs on update orgtilknytning dato when setRessursSluttdato=true if another orgtilknytning has a later gyldigTom")
    void test_not_update_ressurs_sluttdato_on_update_orgtilknytning_dato_when_other_orgtilknytning_has_later_gyldig_tom() {
        var newSluttdato = LocalDate.now().plusMonths(5);

        var orgtilknytningToUpdate = orgtilknytningState();
        saveOrgtilknytninger(List.of(
            orgtilknytningToUpdate,
            orgtilknytningState().toBuilder()
                .gyldigTom(Optional.of(newSluttdato.plusMonths(1)))
                .orgEnhetId("orgtilknytning-with-later-gyldigTom")
                .build()
        ));

        saveRessurser(List.of(
            ressursState().toBuilder()
                .navident(orgtilknytningToUpdate.navident())
                .sektor(Sektor.EKSTERN)
                .build()
        ));

        var endreOrgtilknytningDatoRequest = endreOrgtilknytningDatoRequest().toBuilder()
            .gyldigTom(Optional.of(newSluttdato))
            .setRessursSluttdato(Optional.of(true))
            .build();

        postEndreOrgtilknytningDatoRequest(
            orgtilknytningToUpdate.fid(), nomWriteGroupAndNomUiAzpToken(), endreOrgtilknytningDatoRequest
        ).expectStatus().is2xxSuccessful();

        verify(
            ressursCommadService,
            times(0)
        ).oppdaterRessursSluttdatoEvent(
            any()
        );
    }

    @Test
    @DisplayName("POST /app/orgtilknytninger/{fid}/dato: Should ensure no overlapping period for orgenhet when updating period")
    void test_no_overlapping_periode_for_orgenhet_navident_on_update_date() {
        var navident = "Y654321";
        var orgEnhet = orgEnhetState();

        var existingOrgtilknytningWithConflictingPeriod = orgtilknytningState().toBuilder()
            .navident(navident)
            .orgEnhetId(orgEnhet.id())
            .gyldigFom(LocalDate.now())
            .gyldigTom(Optional.empty())
            .build();

        var orgtilknytningToChange = orgtilknytningState().toBuilder()
            .navident(navident)
            .orgEnhetId(orgEnhet.id()).build();

        saveOrgtilknytninger(List.of(existingOrgtilknytningWithConflictingPeriod, orgtilknytningToChange));
        saveRessurser(List.of(
            ressursState().toBuilder()
                .navident(navident)
                .sektor(Sektor.EKSTERN)
                .build()
        ));
        saveOrgenheter(List.of(orgEnhet));


        var endreOrgtilknytningDatoRequest = endreOrgtilknytningDatoRequest().toBuilder()
            .gyldigFom(LocalDate.now().plusMonths(1))
            .gyldigTom(Optional.of(LocalDate.now().plusMonths(6)))
            .build();

        postEndreOrgtilknytningDatoRequest(
            orgtilknytningToChange.fid(), nomWriteGroupAndNomUiAzpToken(), endreOrgtilknytningDatoRequest
        ).expectStatus().isEqualTo(HttpStatus.CONFLICT)
            .expectBody()
            .jsonPath("message")
            .isEqualTo("Det finnes allerede en orgtilknytning for samme orgenhet innenfor forespurt periode");

        verify(
            orgtilknytningCommandService,
            times(0)
        ).endreOrgtilknytningDato(any(), any());
    }

    @Test
    @DisplayName("POST /app/orgtilknytninger/{fid}/dato: Should ensure no overlapping period for daglig oppfolging when updating period")
    void test_no_overlapping_periode_for_daglig_oppfolging_on_update_date() {
        var navident = "Y654321";
        saveRessurser(List.of(
            ressursState().toBuilder()
                .navident(navident)
                .sektor(Sektor.EKSTERN)
                .build()
        ));


        var orgEnhetForDagligOppfolgingToChange = orgEnhetState();
        var orgEnhetForDagligOppfolgingWithConflictingPeriod = orgEnhetState().toBuilder()
            .id("some-other-orgenhet")
            .navn("En org i NAV")
            .build();
        saveOrgenheter(List.of(orgEnhetForDagligOppfolgingToChange, orgEnhetForDagligOppfolgingWithConflictingPeriod));

        var dagligOppfolgingWithConflictingPeriod = orgtilknytningState().toBuilder()
            .navident(navident)
            .orgEnhetId(orgEnhetForDagligOppfolgingWithConflictingPeriod.id())
            .erDagligOppfolging(true)
            .gyldigFom(LocalDate.now())
            .gyldigTom(Optional.empty())
            .build();
        var dagligOppfolgingToChange = orgtilknytningState().toBuilder()
            .navident(navident)
            .erDagligOppfolging(true)
            .orgEnhetId(orgEnhetForDagligOppfolgingToChange.id()).build();
        saveOrgtilknytninger(List.of(dagligOppfolgingWithConflictingPeriod, dagligOppfolgingToChange));

        var endreOrgtilknytningDatoRequest = endreOrgtilknytningDatoRequest().toBuilder()
            .gyldigFom(LocalDate.now().plusMonths(1))
            .gyldigTom(Optional.of(LocalDate.now().plusMonths(6)))
            .build();

        postEndreOrgtilknytningDatoRequest(
            dagligOppfolgingToChange.fid(), nomWriteGroupAndNomUiAzpToken(), endreOrgtilknytningDatoRequest
        ).expectStatus().isEqualTo(HttpStatus.CONFLICT)
            .expectBody()
            .jsonPath("message")
            .isEqualTo(OrgtilknytningPeriodeOverlapException.forDagligOppfolgingInPeriod(
                orgEnhetForDagligOppfolgingWithConflictingPeriod.navn(),
                dagligOppfolgingWithConflictingPeriod.gyldigFom(),
                dagligOppfolgingWithConflictingPeriod.gyldigTom()
            ).getMessage());

        verify(
            orgtilknytningCommandService,
            times(0)
        ).endreOrgtilknytningDato(any(), any());
    }

    @Test
    @DisplayName("POST /app/orgtilknytninger/{fid}/dato: Should not be able to update dato if it leads to no active orgtilknytninger")
    void test_disallow_update_if_no_remaining_active() {

        var orgtilknytningToUpdate = orgtilknytningState();
        saveOrgtilknytninger(List.of(orgtilknytningToUpdate));

        saveRessurser(List.of(
            ressursState().toBuilder()
                .navident(orgtilknytningToUpdate.navident())
                .sektor(Sektor.EKSTERN)
                .build()
        ));

        var endreOrgtilknytningDatoRequest = endreOrgtilknytningDatoRequest().toBuilder()
            .gyldigFom(orgtilknytningToUpdate.gyldigFom())
            .gyldigTom(Optional.of(LocalDate.now().minusDays(1)))
            .build();

        postEndreOrgtilknytningDatoRequest(
            orgtilknytningToUpdate.fid(), nomWriteGroupAndNomUiAzpToken(), endreOrgtilknytningDatoRequest
        ).expectStatus().isBadRequest();

        verify(
            orgtilknytningCommandService,
            times(0)
        ).endreOrgtilknytningDato(
            stringArgumentCaptor.capture(),
            endreOrgtilknytningDatoArgumentCaptor.capture()
        );
    }

    @Test
    @DisplayName("POST /app/orgtilknytninger/{fid}/dato: Should block update dato for orgtilknytning if tilknyttet ressurs is not EKSTERN")
    void test_update_orgtilknytning_for_ekstern_only_date() {
        var orgtilknytningToUpdate = orgtilknytningState();
        saveOrgtilknytninger(List.of(orgtilknytningToUpdate));

        saveRessurser(List.of(
            ressursState().toBuilder()
                .navident(orgtilknytningToUpdate.navident())
                .sektor(Sektor.NAV_STATLIG)
                .build()
        ));

        postEndreOrgtilknytningDatoRequest(
            orgtilknytningToUpdate.fid(), nomWriteGroupAndNomUiAzpToken(), endreOrgtilknytningDatoRequest()
        ).expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("POST /app/orgtilknytninger/{fid}/dato: Should forbid update dato if user does not have granted authority 'MUTATE_RESSURS'")
    void test_update_orgtilknytning_forbidden() {

        postEndreOrgtilknytningDatoRequest(
            "some-fid", noScopesUserToken(), endreOrgtilknytningDatoRequest()
        ).expectStatus().isForbidden();
    }

    @Test
    @DisplayName("Should update tilknytningtype for orgtilknytning")
    void test_update_orgtilknytning_type() {

        var orgtilknytningToUpdate = orgtilknytningState();
        saveOrgtilknytninger(List.of(orgtilknytningToUpdate));

        saveRessurser(List.of(
            ressursState().toBuilder()
                .navident(orgtilknytningToUpdate.navident())
                .sektor(Sektor.EKSTERN)
                .build()
        ));

        var endreTilknytningtypeRequest = endreTilknytningtypeRequest().toBuilder()
            .tilknytningtype(Optional.of((Tilknytningtype.KONSULENT)))
            .build();

        postEndreTilknytningtypeRequest(
            orgtilknytningToUpdate.fid(),
            nomWriteGroupAndNomUiAzpToken(),
            endreTilknytningtypeRequest
        ).expectStatus().is2xxSuccessful();

        verify(
            orgtilknytningCommandService,
            times(1)
        ).endreTilknytningtype(
            stringArgumentCaptor.capture(),
            endreTilknytningtypeArgumentCaptor.capture()
        );

        assertThat(stringArgumentCaptor.getValue()).isEqualTo(orgtilknytningToUpdate.fid());
        assertThat(endreTilknytningtypeArgumentCaptor.getValue())
            .matches(endreTilknytningtype ->
                endreTilknytningtype.tilknytningtype().equals(endreTilknytningtypeRequest.tilknytningtype())
            );
    }

    @Test
    @DisplayName("Should block update tilknytningtype for orgtilknytning if tilknyttet ressurs is not EKSTERN")
    void test_update_orgtilknytning_type_for_ekstern_only() {
        var orgtilknytningToUpdate = orgtilknytningState();
        saveOrgtilknytninger(List.of(orgtilknytningToUpdate));

        saveRessurser(List.of(
            ressursState().toBuilder()
                .navident(orgtilknytningToUpdate.navident())
                .sektor(Sektor.NAV_STATLIG)
                .build()
        ));

        postEndreTilknytningtypeRequest(
            orgtilknytningToUpdate.fid(),
            nomWriteGroupAndNomUiAzpToken(),
            endreTilknytningtypeRequest()
        ).expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("Should forbid update orgtilknytning type if user does not have granted authority 'MUTATE_RESSURS'")
    void test_update_orgtilknytning_type_forbidden() {

        postEndreTilknytningtypeRequest(
            "some-fid",
            noScopesUserToken(),
            endreTilknytningtypeRequest()
        ).expectStatus().isForbidden();
    }

    @ParameterizedTest(name = "POST /app/orgtilknytninger: Should add new orgtilknytning with erDagligOppfolging = {0}")
    @MethodSource("test_add_orgtilknytning_input")
    void test_add_orgtilknytning(Boolean erDagligOppfolging) {
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

        var leggTilOrgtilknytningRequest = leggTilOrgtilknytningRequest().toBuilder()
            .navident(navident)
            .orgEnhetId(orgEnhet.id())
            .erDagligOppfolging(Optional.of(erDagligOppfolging))
            .build();

        postLeggTilOrgtilknytningRequest(nomWriteGroupAndNomUiAzpToken(), leggTilOrgtilknytningRequest)
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

        if (erDagligOppfolging) {
            var endreDagligOppfolgingOrgtilknytningArgumentCaptor = ArgumentCaptor.forClass(EndreDagligOppfolgingOrgtilknytning.class);

            verify(
                orgtilknytningCommandService,
                times(1)
            ).endreDagligOppfolgingOrgtilknytning(
                any(), endreDagligOppfolgingOrgtilknytningArgumentCaptor.capture()
            );

            assertThat(endreDagligOppfolgingOrgtilknytningArgumentCaptor.getValue())
                .matches(endreDagligOppfolgingOrgtilknytning ->
                    endreDagligOppfolgingOrgtilknytning.navident().equals(leggTilOrgtilknytningRequest.navident())
                )
                .matches(endreDagligOppfolgingOrgtilknytning ->
                    endreDagligOppfolgingOrgtilknytning.orgenhetId().equals(leggTilOrgtilknytningRequest.orgEnhetId())
                )
                .matches(endreDagligOppfolgingOrgtilknytning ->
                    endreDagligOppfolgingOrgtilknytning.gyldigFom().equals(leggTilOrgtilknytningRequest.gyldigFom())
                )
                .matches(endreDagligOppfolgingOrgtilknytning ->
                    endreDagligOppfolgingOrgtilknytning.gyldigTom().equals(leggTilOrgtilknytningRequest.gyldigTom())
                );
        } else {
            verify(orgtilknytningCommandService, times(0)).endreDagligOppfolgingOrgtilknytning(any(), any());
        }
    }

    private static Stream<Arguments> test_add_orgtilknytning_input() {
        return Stream.of(
            Arguments.of(true),
            Arguments.of(false)
        );
    }

    @Test
    @DisplayName("POST /app/orgtilknytninger: Should set ressurs sluttdato when setRessursSluttdato=true")
    void test_add_orgtilknytning_set_ressurs_sluttdato() {
        var tilknyttetRessurs = ressursState().toBuilder()
            .sluttdato(Optional.empty())
            .sektor(Sektor.EKSTERN)
            .build();

        var tilknyttetOrgenhet = orgEnhetState();

        saveOrgenheter(List.of(tilknyttetOrgenhet));
        saveRessurser(List.of(tilknyttetRessurs));

        var leggTilOrgtilknytningRequest = leggTilOrgtilknytningRequest().toBuilder()
            .navident(tilknyttetRessurs.navident())
            .orgEnhetId(tilknyttetOrgenhet.id())
            .gyldigTom(Optional.of(LocalDate.now().plusMonths(5)))
            .setRessursSluttdato(Optional.of(true))
            .build();

        postLeggTilOrgtilknytningRequest(nomWriteGroupAndNomUiAzpToken(), leggTilOrgtilknytningRequest)
            .expectStatus().is2xxSuccessful();

        var oppdaterSluttdatoCommandArgumentCaptor = ArgumentCaptor.forClass(OppdaterSluttdatoCommand.class);

        verify(ressursCommadService, times(1))
            .oppdaterRessursSluttdatoEvent(oppdaterSluttdatoCommandArgumentCaptor.capture());

        assertThat(oppdaterSluttdatoCommandArgumentCaptor.getValue())
            .matches(oppdaterSluttdatoCommand ->
                oppdaterSluttdatoCommand.sluttdato().equals(leggTilOrgtilknytningRequest.gyldigTom())
            )
            .matches(oppdaterSluttdatoCommand ->
                oppdaterSluttdatoCommand.fid().equals(tilknyttetRessurs.fid())
            );
    }

    @ParameterizedTest(name = "When setRessursSluttdato={0}")
    @MethodSource("test_add_orgtilknytning_should_not_set_ressurs_sluttdato_input")
    @DisplayName("POST /app/orgtilknytninger: Should NOT set ressurs sluttdato")
    void test_add_orgtilknytning_should_not_set_ressurs_sluttdato(Optional<Boolean> setRessursSluttdato) {
        var tilknyttetRessurs = ressursState().toBuilder()
            .sluttdato(Optional.empty())
            .sektor(Sektor.EKSTERN)
            .build();

        var tilknyttetOrgenhet = orgEnhetState();

        saveOrgenheter(List.of(tilknyttetOrgenhet));
        saveRessurser(List.of(tilknyttetRessurs));

        var leggTilOrgtilknytningRequest = leggTilOrgtilknytningRequest().toBuilder()
            .navident(tilknyttetRessurs.navident())
            .orgEnhetId(tilknyttetOrgenhet.id())
            .gyldigTom(Optional.of(LocalDate.now().plusMonths(5)))
            .setRessursSluttdato(setRessursSluttdato)
            .build();

        postLeggTilOrgtilknytningRequest(nomWriteGroupAndNomUiAzpToken(), leggTilOrgtilknytningRequest)
            .expectStatus().is2xxSuccessful();

        verify(ressursCommadService, times(0))
            .oppdaterRessursSluttdatoEvent(any());
    }

    private static Stream<Arguments> test_add_orgtilknytning_should_not_set_ressurs_sluttdato_input() {
        return Stream.of(
            Arguments.of(Optional.empty()),
            Arguments.of(Optional.of(false))
        );
    }

    @Test
    @DisplayName("POST /app/orgtilknytninger: Should not set ressurs sluttdato when setRessursSluttdato=true if there exists another orgtilknytning with later gyldigTom")
    void test_add_orgtilknytning_should_not_set_ressurs_sluttdato_when_another_orgtilknytning_has_later_gyldigTom() {
        var tilknyttetRessurs = ressursState().toBuilder()
            .sluttdato(Optional.empty())
            .sektor(Sektor.EKSTERN)
            .build();

        var tilknyttetOrgenhet = orgEnhetState();

        saveOrgenheter(List.of(tilknyttetOrgenhet));
        saveRessurser(List.of(tilknyttetRessurs));

        var leggTilOrgtilknytningRequest = leggTilOrgtilknytningRequest().toBuilder()
            .navident(tilknyttetRessurs.navident())
            .orgEnhetId(tilknyttetOrgenhet.id())
            .gyldigTom(Optional.of(LocalDate.now().plusMonths(5)))
            .setRessursSluttdato(Optional.of(true))
            .build();

        saveOrgtilknytninger(List.of(
            orgtilknytningState().toBuilder()
                .orgEnhetId("orgtilknytning-with-later-gyldigTom")
                .gyldigTom(leggTilOrgtilknytningRequest.gyldigTom().map(gyldigTomInRequest ->
                    gyldigTomInRequest.plusMonths(1))
                ).build()
        ));

        postLeggTilOrgtilknytningRequest(nomWriteGroupAndNomUiAzpToken(), leggTilOrgtilknytningRequest)
            .expectStatus().is2xxSuccessful();

        verify(ressursCommadService, times(0))
            .oppdaterRessursSluttdatoEvent(any());
    }

    @Test
    @DisplayName("POST /app/orgtilknytninger: Should ensure navident in orgtilknytning exists")
    void test_navident_for_orgtilknytning_exists() {
        var navident = "Y654321";
        var orgenhet = orgEnhetState();
        saveOrgenheter(List.of(orgenhet));

        var leggTilOrgtilknytningRequest = leggTilOrgtilknytningRequest().toBuilder()
            .navident(navident)
            .orgEnhetId(orgenhet.id())
            .build();

        postLeggTilOrgtilknytningRequest(nomWriteGroupAndNomUiAzpToken(), leggTilOrgtilknytningRequest)
            .expectStatus().isBadRequest();

        verify(
            orgtilknytningCommandService,
            times(0)
        ).opprettOrgtilknytning(
            opprettOrgtilknytningArgumentCaptor.capture()
        );
    }

    @Test
    @DisplayName("POST /app/orgtilknytninger: Should ensure orgEnhetId in orgtilknytning exists")
    void test_orgEnhet_for_orgtilknytning_exists() {
        var navident = "Y654321";
        var orgEnhetId = "orgEnhet123";

        saveRessurser(List.of(
            ressursState().toBuilder()
                .navident(navident)
                .sektor(Sektor.EKSTERN)
                .build()
        ));

        var leggTilOrgtilknytningRequest = leggTilOrgtilknytningRequest().toBuilder()
            .navident(navident)
            .orgEnhetId(orgEnhetId)
            .build();

        postLeggTilOrgtilknytningRequest(nomWriteGroupAndNomUiAzpToken(), leggTilOrgtilknytningRequest)
            .expectStatus().isBadRequest();

        verify(
            orgtilknytningCommandService,
            times(0)
        ).opprettOrgtilknytning(
            opprettOrgtilknytningArgumentCaptor.capture()
        );
    }

    @Test
    @DisplayName("POST /app/orgtilknytninger: Should ensure no overlapping period for unique orgenhetId + navident combination when creating orgtilknytning")
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


        var leggTilOrgtilknytningRequest = leggTilOrgtilknytningRequest().toBuilder()
            .navident(navident)
            .orgEnhetId(orgEnhet.id())
            .gyldigFom(LocalDate.now().plusMonths(1))
            .gyldigTom(Optional.of(LocalDate.now().plusMonths(6)))
            .build();

        postLeggTilOrgtilknytningRequest(nomWriteGroupAndNomUiAzpToken(), leggTilOrgtilknytningRequest)
            .expectStatus().isEqualTo(HttpStatus.CONFLICT);

        verify(
            orgtilknytningCommandService,
            times(0)
        ).opprettOrgtilknytning(
            opprettOrgtilknytningArgumentCaptor.capture()
        );
    }

    @Test
    @DisplayName("POST /app/orgtilknytninger: Should ensure no overlapping period for daglig oppfolging")
    void test_no_overlapping_daglig_oppfolging() {
        var navident = "Y654321";
        var orgEnhetForExistingDagligOppfolging = orgEnhetState();

        var existingDagligOppfolging = orgtilknytningState().toBuilder()
            .navident(navident)
            .orgEnhetId(orgEnhetForExistingDagligOppfolging.id())
            .gyldigFom(LocalDate.now())
            .gyldigTom(Optional.empty())
            .erDagligOppfolging(true)
            .build();

        saveOrgtilknytninger(List.of(existingDagligOppfolging));
        saveRessurser(List.of(
            ressursState().toBuilder()
                .navident(navident)
                .sektor(Sektor.EKSTERN)
                .build()
        ));

        var leggTilOrgtilknytningRequest = leggTilOrgtilknytningRequest().toBuilder()
            .navident(navident)
            .orgEnhetId("some-other-orgenhet")
            .gyldigFom(LocalDate.now().plusMonths(1))
            .gyldigTom(Optional.of(LocalDate.now().plusMonths(6)))
            .erDagligOppfolging(Optional.of(true))
            .build();

        saveOrgenheter(List.of(
            orgEnhetForExistingDagligOppfolging,
            orgEnhetForExistingDagligOppfolging.toBuilder().id(leggTilOrgtilknytningRequest.orgEnhetId()).build()
        ));

        postLeggTilOrgtilknytningRequest(nomWriteGroupAndNomUiAzpToken(), leggTilOrgtilknytningRequest)
            .expectStatus().isEqualTo(HttpStatus.CONFLICT)
            .expectBody()
            .jsonPath("message")
            .isEqualTo(OrgtilknytningPeriodeOverlapException.forDagligOppfolgingInPeriod(
                orgEnhetForExistingDagligOppfolging.navn(),
                existingDagligOppfolging.gyldigFom(),
                existingDagligOppfolging.gyldigTom()
            ).getMessage());

        verify(
            orgtilknytningCommandService,
            times(0)
        ).opprettOrgtilknytning(
            opprettOrgtilknytningArgumentCaptor.capture()
        );
    }

    @Test
    @DisplayName("POST /app/orgtilknytninger: Should ensure that gyldigFom is not after tomorrow if new daglig oppfølging changes remedy enhet (https://jira.adeo.no/browse/ORG-1759)")
    void test_new_remedy_enhet_valid_date() {
        var navident = "Y654321";
        var orgEnhet = orgEnhetState();

        var existingOrgtilknytning = orgtilknytningState().toBuilder()
            .navident(navident)
            .orgEnhetId(orgEnhet.id())
            .gyldigFom(LocalDate.now().minusMonths(2))
            .gyldigTom(Optional.of(LocalDate.now()))
            .erDagligOppfolging(true)
            .build();

        saveOrgtilknytninger(List.of(existingOrgtilknytning));
        saveRessurser(List.of(
            ressursState().toBuilder()
                .navident(navident)
                .sektor(Sektor.EKSTERN)
                .build()
        ));

        var leggTilOrgtilknytningRequest = leggTilOrgtilknytningRequest().toBuilder()
            .navident(navident)
            .orgEnhetId("some-other-orgenhet")
            .gyldigFom(LocalDate.now().plusMonths(1))
            .gyldigTom(Optional.of(LocalDate.now().plusMonths(6)))
            .erDagligOppfolging(Optional.of(true))
            .build();

        saveOrgenheter(List.of(
            orgEnhet,
            orgEnhet.toBuilder().id(leggTilOrgtilknytningRequest.orgEnhetId()).build()
        ));

        when(remedyEnhetMapperService.findByNomId(existingOrgtilknytning.orgEnhetId()))
            .thenReturn(Optional.of(RemedyEnhet.builder().remedyEnhetId("currentRemedyEnhet").build()));

        when(remedyEnhetMapperService.findByNomId(existingOrgtilknytning.orgEnhetId()))
            .thenReturn(Optional.of(RemedyEnhet.builder().remedyEnhetId("newRemedyEnhet").build()));

        postLeggTilOrgtilknytningRequest(nomWriteGroupAndNomUiAzpToken(), leggTilOrgtilknytningRequest)
            .expectStatus().isEqualTo(HttpStatus.BAD_REQUEST)
            .expectBody()
            .jsonPath("message")
            .isEqualTo("Siden den nye daglig oppfolging orgtilknytningen vil endre den ansattes remedy enhet kan 'gyldigFom' være maks én virkedag frem i tid");

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

    private static EndreTilknytningtypeRequest endreTilknytningtypeRequest() {
        return EndreTilknytningtypeRequest.builder()
            .tilknytningtype(Optional.empty())
            .build();
    }

    private static EndreOrgtilknytningDatoRequest endreOrgtilknytningDatoRequest() {
        return EndreOrgtilknytningDatoRequest.builder()
            .gyldigFom(LocalDate.now())
            .gyldigTom(Optional.of(LocalDate.now().plusMonths(11)))
            .setRessursSluttdato(Optional.empty())
            .build();
    }

    private static LeggTilOrgtilknytningRequest leggTilOrgtilknytningRequest() {
        return LeggTilOrgtilknytningRequest.builder()
            .navident("X12345")
            .orgEnhetId("some-orgenhet")
            .gyldigFom(LocalDate.now())
            .gyldigTom(Optional.of(LocalDate.now().plusMonths(5)))
            .erDagligOppfolging(Optional.empty())
            .setRessursSluttdato(Optional.empty())
            .build();
    }

    private WebTestClient.ResponseSpec postEndreTilknytningtypeRequest(
        String fid,
        String accessToken,
        EndreTilknytningtypeRequest endreTilknytningtypeRequest) {
        return webTestClient
            .post()
            .uri(uriBuilder -> uriBuilder
                .path("/app/orgtilknytninger/{fid}/tilknytningtype")
                .build(fid)
            )
            .headers(httpHeaders -> {
                httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                httpHeaders.setBearerAuth(accessToken);
            })
            .bodyValue(endreTilknytningtypeRequest)
            .exchange();
    }

    private WebTestClient.ResponseSpec postEndreOrgtilknytningDatoRequest(
        String fid,
        String accessToken,
        EndreOrgtilknytningDatoRequest endreOrgtilknytningDatoRequest
    ) {
        return webTestClient
            .post()
            .uri(uriBuilder -> uriBuilder
                .path("/app/orgtilknytninger/{fid}/dato")
                .build(fid)
            )
            .headers(httpHeaders -> {
                httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                httpHeaders.setBearerAuth(accessToken);
            })
            .bodyValue(endreOrgtilknytningDatoRequest)
            .exchange();
    }

    private WebTestClient.ResponseSpec postLeggTilOrgtilknytningRequest(
        String accessToken,
        LeggTilOrgtilknytningRequest leggTilOrgtilknytningRequest
    ) {
        return webTestClient
            .post()
            .uri("/app/orgtilknytninger")
            .headers(httpHeaders -> {
                httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                httpHeaders.setBearerAuth(accessToken);
            })
            .bodyValue(leggTilOrgtilknytningRequest)
            .exchange();
    }
}
