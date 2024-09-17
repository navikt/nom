package no.nav.nom.apps.api.orgtilknytning.api.app;

import no.nav.nom.apps.api.TestTokensGenerator;
import no.nav.nom.apps.api.authorize.AuthResolver;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.orgtilknytning.api.exceptions.OrgtilknytningPeriodeOverlapException;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.OrgtilknytningCommandService;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.EndreDagligOppfolgingOrgtilknytning;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.EndreDagligOppfolgingOrgtilknytningCommand;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.EndreOrgtilknytningDato;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.EndreOrgtilknytningDatoCommand;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.EndreTilknytningtypeCommand;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.OpprettOrgtilknytning;
import no.nav.nom.apps.api.remedy.RemedyEnhet;
import no.nav.nom.apps.api.remedy.RemedyEnhetMapperService;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.RessursCommandService;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OppdaterSluttdatoCommand;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OppdaterStartdatoCommand;
import no.nav.nom.apps.api.rest.RestTestBase;
import no.nav.nom.contract.nom.Tilknytningtype;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.libs.state.ressurs.NomStateRessursListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static no.nav.nom.apps.api.common.TestData.orgEnhet;
import static no.nav.nom.apps.api.common.TestData.orgEnhetState;
import static no.nav.nom.apps.api.common.TestData.orgtilknytningState;
import static no.nav.nom.apps.api.common.TestData.ressursState;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class OrgtilknytningControllerTest extends RestTestBase {

    @MockBean
    private OrgtilknytningCommandService orgtilknytningCommandService;

    @MockBean
    private RessursCommandService ressursCommandService;

    @MockBean
    private NomStateRessursListener nomStateRessursListener;

    @MockBean
    private RemedyEnhetMapperService remedyEnhetMapperService;

    @MockBean
    private OrgenhetService orgenhetService;

    @MockBean
    private AuthResolver authResolver;

    @Autowired
    TestTokensGenerator testToken;

    @Captor
    ArgumentCaptor<String> stringArgumentCaptor;

    @Captor
    ArgumentCaptor<EndreOrgtilknytningDato> endreOrgtilknytningDatoArgumentCaptor;

    @Captor
    ArgumentCaptor<EndreOrgtilknytningDatoCommand> endreOrgtilknytningDatoCommandArgumentCaptor;

    @Captor
    ArgumentCaptor<EndreTilknytningtypeCommand> endreTilknytningtypeCommandArgumentCaptor;

    @Captor
    ArgumentCaptor<OpprettOrgtilknytning> opprettOrgtilknytningArgumentCaptor;

    @Captor
    ArgumentCaptor<OppdaterStartdatoCommand> oppdaterStartdatoCommandArgumentCaptor;

    @Captor
    ArgumentCaptor<OppdaterSluttdatoCommand> oppdaterSluttdatoCommandArgumentCaptor;

    @Captor
    ArgumentCaptor<EndreDagligOppfolgingOrgtilknytningCommand> endreDagligOppfolgingOrgtilknytningCommandArgumentCaptor;

    @BeforeEach
    void setInitialLoadCompleteAndEnableUnleashAndClearData() {
        when(nomStateRessursListener.hasReadToEndOnStartup()).thenReturn(true);
        when(unleash.isEnabled(any())).thenReturn(true);
        when(authResolver.kanRedigereRessurs(any())).thenReturn(true);
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

        var responseBody = webTestClient
            .get()
            .uri("/app/orgtilknytninger")
            .headers(httpHeaders -> {
                httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                httpHeaders.setBearerAuth(testToken.noScopesUser());
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
                httpHeaders.setBearerAuth(testToken.noScopesUser());
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
            orgtilknytningToUpdate.fid(), testToken.nomWriteGroupAndNomUiAzp(), endreOrgtilknytningDatoRequest
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

        verify(ressursCommandService, times(0)).oppdaterRessursSluttdatoEvent(any());
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
            orgtilknytningToUpdate.fid(), testToken.nomWriteGroupAndNomUiAzp(), endreOrgtilknytningDatoRequest
        ).expectStatus().is2xxSuccessful();

        var oppdaterSluttdatoCommandArgumentCaptor = ArgumentCaptor.forClass(OppdaterSluttdatoCommand.class);

        verify(
            ressursCommandService,
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
    @DisplayName(
        "POST /app/orgtilknytninger/{fid}/dato: Should NOT update sluttdato for ressurs on update orgtilknytning dato when setRessursSluttdato=true if another orgtilknytning has a later gyldigTom"
    )
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
            orgtilknytningToUpdate.fid(), testToken.nomWriteGroupAndNomUiAzp(), endreOrgtilknytningDatoRequest
        ).expectStatus().is2xxSuccessful();

        verify(
            ressursCommandService,
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
            orgtilknytningToChange.fid(), testToken.nomWriteGroupAndNomUiAzp(), endreOrgtilknytningDatoRequest
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
            dagligOppfolgingToChange.fid(), testToken.nomWriteGroupAndNomUiAzp(), endreOrgtilknytningDatoRequest
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
            orgtilknytningToUpdate.fid(), testToken.nomWriteGroupAndNomUiAzp(), endreOrgtilknytningDatoRequest
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
            orgtilknytningToUpdate.fid(), testToken.nomWriteGroupAndNomUiAzp(), endreOrgtilknytningDatoRequest()
        ).expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("POST /app/orgtilknytninger/{fid}/dato: Should forbid update dato if user does not have granted authority 'MUTATE_RESSURS'")
    void test_update_orgtilknytning_forbidden() {

        postEndreOrgtilknytningDatoRequest(
            "some-fid", testToken.noScopesUser(), endreOrgtilknytningDatoRequest()
        ).expectStatus().isForbidden();
    }

    @Test
    @DisplayName("POST /app/orgtilknytninger/{fid}/dato: Should forbid when user has 'MUTATE_RESSURS' but is not authorized for ressurs")
    void test_update_orgtilknytning_date_not_authorized() {
        when(authResolver.kanRedigereRessurs(any())).thenReturn(false);

        var orgtilknytningToUpdate = orgtilknytningState();
        saveOrgtilknytninger(List.of(orgtilknytningToUpdate));

        postEndreOrgtilknytningDatoRequest(
            orgtilknytningToUpdate.fid(), testToken.nomWriteGroupAndNomUiAzp(), endreOrgtilknytningDatoRequest()
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
            testToken.nomWriteGroupAndNomUiAzp(),
            endreTilknytningtypeRequest
        ).expectStatus().is2xxSuccessful();

        verify(
            orgtilknytningCommandService,
            times(1)
        ).endreTilknytningtype(
            endreTilknytningtypeCommandArgumentCaptor.capture()
        );

        assertThat(endreTilknytningtypeCommandArgumentCaptor.getValue())
            .matches(endreTilknytningtypeCommand ->
                endreTilknytningtypeCommand.fid().equals(orgtilknytningToUpdate.fid()))
            .matches(endreTilknytningtypeCommand ->
                endreTilknytningtypeCommand.tilknytningtype().equals(endreTilknytningtypeRequest.tilknytningtype())
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
            testToken.nomWriteGroupAndNomUiAzp(),
            endreTilknytningtypeRequest()
        ).expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("Should forbid update orgtilknytning type if user does not have granted authority 'MUTATE_RESSURS'")
    void test_update_orgtilknytning_not_authorized() {

        postEndreTilknytningtypeRequest(
            "some-fid",
            testToken.noScopesUser(),
            endreTilknytningtypeRequest()
        ).expectStatus().isForbidden();
    }

    @Test
    @DisplayName("Should forbid update orgtilknytning type if user user has 'MUTATE_RESSURS' but is not authorized for ressurs")
    void test_update_orgtilknytning_type_forbidden() {
        when(authResolver.kanRedigereRessurs(any())).thenReturn(false);

        var orgtilknytningToUpdate = orgtilknytningState();
        saveOrgtilknytninger(List.of(orgtilknytningToUpdate));

        postEndreTilknytningtypeRequest(orgtilknytningToUpdate.fid(), testToken.nomWriteGroupAndNomUiAzp(),endreTilknytningtypeRequest())
            .expectStatus().isForbidden();
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

        var leggTilOrgtilknytningRequest = leggTilOrgtilknytningRequestOld().toBuilder()
            .navident(navident)
            .orgEnhetId(orgEnhet.id())
            .erDagligOppfolging(Optional.of(erDagligOppfolging))
            .build();

        postLeggTilOrgtilknytningRequest(testToken.nomWriteGroupAndNomUiAzp(), leggTilOrgtilknytningRequest)
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

        var leggTilOrgtilknytningRequest = leggTilOrgtilknytningRequestOld().toBuilder()
            .navident(tilknyttetRessurs.navident())
            .orgEnhetId(tilknyttetOrgenhet.id())
            .gyldigTom(Optional.of(LocalDate.now().plusMonths(5)))
            .setRessursSluttdato(Optional.of(true))
            .build();

        postLeggTilOrgtilknytningRequest(testToken.nomWriteGroupAndNomUiAzp(), leggTilOrgtilknytningRequest)
            .expectStatus().is2xxSuccessful();

        var oppdaterSluttdatoCommandArgumentCaptor = ArgumentCaptor.forClass(OppdaterSluttdatoCommand.class);

        verify(ressursCommandService, times(1))
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

        var leggTilOrgtilknytningRequest = leggTilOrgtilknytningRequestOld().toBuilder()
            .navident(tilknyttetRessurs.navident())
            .orgEnhetId(tilknyttetOrgenhet.id())
            .gyldigTom(Optional.of(LocalDate.now().plusMonths(5)))
            .setRessursSluttdato(setRessursSluttdato)
            .build();

        postLeggTilOrgtilknytningRequest(testToken.nomWriteGroupAndNomUiAzp(), leggTilOrgtilknytningRequest)
            .expectStatus().is2xxSuccessful();

        verify(ressursCommandService, times(0))
            .oppdaterRessursSluttdatoEvent(any());
    }

    private static Stream<Arguments> test_add_orgtilknytning_should_not_set_ressurs_sluttdato_input() {
        return Stream.of(
            Arguments.of(Optional.empty()),
            Arguments.of(Optional.of(false))
        );
    }

    @Test
    @DisplayName(
        "POST /app/orgtilknytninger: Should not set ressurs sluttdato when setRessursSluttdato=true if there exists another orgtilknytning with later gyldigTom"
    )
    void test_add_orgtilknytning_should_not_set_ressurs_sluttdato_when_another_orgtilknytning_has_later_gyldigTom() {
        var tilknyttetRessurs = ressursState().toBuilder()
            .sluttdato(Optional.empty())
            .sektor(Sektor.EKSTERN)
            .build();

        var tilknyttetOrgenhet = orgEnhetState();

        saveOrgenheter(List.of(tilknyttetOrgenhet));
        saveRessurser(List.of(tilknyttetRessurs));

        var leggTilOrgtilknytningRequest = leggTilOrgtilknytningRequestOld().toBuilder()
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

        postLeggTilOrgtilknytningRequest(testToken.nomWriteGroupAndNomUiAzp(), leggTilOrgtilknytningRequest)
            .expectStatus().is2xxSuccessful();

        verify(ressursCommandService, times(0))
            .oppdaterRessursSluttdatoEvent(any());
    }

    @Test
    @DisplayName("POST /app/orgtilknytninger: Should ensure navident in orgtilknytning exists")
    void test_navident_for_orgtilknytning_exists() {
        var navident = "Y654321";
        var orgenhet = orgEnhetState();
        saveOrgenheter(List.of(orgenhet));

        var leggTilOrgtilknytningRequest = leggTilOrgtilknytningRequestOld().toBuilder()
            .navident(navident)
            .orgEnhetId(orgenhet.id())
            .build();

        postLeggTilOrgtilknytningRequest(testToken.nomWriteGroupAndNomUiAzp(), leggTilOrgtilknytningRequest)
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

        var leggTilOrgtilknytningRequest = leggTilOrgtilknytningRequestOld().toBuilder()
            .navident(navident)
            .orgEnhetId(orgEnhetId)
            .build();

        postLeggTilOrgtilknytningRequest(testToken.nomWriteGroupAndNomUiAzp(), leggTilOrgtilknytningRequest)
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


        var leggTilOrgtilknytningRequest = leggTilOrgtilknytningRequestOld().toBuilder()
            .navident(navident)
            .orgEnhetId(orgEnhet.id())
            .gyldigFom(LocalDate.now().plusMonths(1))
            .gyldigTom(Optional.of(LocalDate.now().plusMonths(6)))
            .build();

        postLeggTilOrgtilknytningRequest(testToken.nomWriteGroupAndNomUiAzp(), leggTilOrgtilknytningRequest)
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

        var leggTilOrgtilknytningRequest = leggTilOrgtilknytningRequestOld().toBuilder()
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

        postLeggTilOrgtilknytningRequest(testToken.nomWriteGroupAndNomUiAzp(), leggTilOrgtilknytningRequest)
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
    @DisplayName(
        "POST /app/orgtilknytninger: Should ensure that gyldigFom is not after tomorrow if new daglig oppfølging changes remedy enhet (https://jira.adeo.no/browse/ORG-1759)"
    )
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

        var leggTilOrgtilknytningRequest = leggTilOrgtilknytningRequestOld().toBuilder()
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

        postLeggTilOrgtilknytningRequest(testToken.nomWriteGroupAndNomUiAzp(), leggTilOrgtilknytningRequest)
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
    @DisplayName("POST /app/orgtilknytninger Should forbid update orgtilknytning type if user user has 'MUTATE_RESSURS' but is not authorized for ressurs")
    void test_update_orgtilknytninger_forbidden() {
        when(authResolver.kanRedigereRessurs(any())).thenReturn(false);

        var orgenhet = orgEnhetState();
        saveOrgenheter(List.of(orgenhet));

        var leggTilOrgtilknytningRequest = leggTilOrgtilknytningRequestOld().toBuilder()
            .navident("Y654321")
            .orgEnhetId(orgenhet.id())
            .build();

        postLeggTilOrgtilknytningRequest(testToken.nomWriteGroupAndNomUiAzp(), leggTilOrgtilknytningRequest)
            .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("Should get orgtilknytning for fid")
    void test_get_orgtilknytning() {
        var orgtilknytningStateToGet = orgtilknytningState();

        saveOrgtilknytninger(List.of(orgtilknytningStateToGet, orgtilknytningState()));

        var responseBody = webTestClient
            .get()
            .uri(uriBuilder -> uriBuilder
                .path("/app/orgtilknytninger/{fid}")
                .build(orgtilknytningStateToGet.fid())
            )
            .headers(httpHeaders -> {
                httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                httpHeaders.setBearerAuth(testToken.noScopesUser());
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
                httpHeaders.setBearerAuth(testToken.noScopesUser());
            })
            .exchange()
            .expectStatus().is4xxClientError();

    }

    @Test
    @DisplayName("POST /app/orgtilknytninger/forRessurs/{navident} support updating null (= max date) dato for ressurs")
    void test_update_orgtilknytninger_for_ressurs_handle_null_dato() {
        when(unleash.isEnabled("nom.ressurs.ny-move-prosess")).thenReturn(false);
        var tilknyttetRessurs = ressursState().toBuilder()
            .sektor(Sektor.EKSTERN).build();

        saveRessurser(List.of(tilknyttetRessurs));

        var existingOrgtilknytning = orgtilknytningState().toBuilder()
            .navident(tilknyttetRessurs.navident())
            .gyldigFom(LocalDate.now())
            .gyldigTom(Optional.of(LocalDate.now().plusMonths(1)))
            .build();

        saveOrgtilknytninger(List.of(existingOrgtilknytning));

        var newStartdato = existingOrgtilknytning.gyldigFom().plusMonths(1);
        LocalDate newSluttdato = null;

        var endreOrgtilknytningRequest = endreOrgtilknytningRequest(existingOrgtilknytning).toBuilder()
            .gyldigFom(
                newStartdato
            )
            .gyldigTom(newSluttdato)
            .build();

        var oppdaterOrgtilknytningerForRessursRequest = oppdaterOrgtilknytningerForRessursRequest().toBuilder()
            .eksisterendeOrgtilknytninger(List.of(endreOrgtilknytningRequest))
            .nyeOrgtilknytninger(Collections.emptyList())
            .setRessursStartdato(true)
            .setRessursSluttdato(true)
            .build();

        postOppdaterOrgtilknytningerForRessursRequest(
            tilknyttetRessurs.navident(),
            testToken.nomWriteGroupAndNomUiAzp(),
            oppdaterOrgtilknytningerForRessursRequest
        )
            .expectStatus().is2xxSuccessful();
    }

    @Test
    @DisplayName("POST /app/orgtilknytninger/forRessurs/{navident} support updating null (= max date) dato for ressurs")
    void test_update_orgtilknytninger_for_ressurs_handle_null_dato_ny() {
        when(unleash.isEnabled("nom.ressurs.ny-move-prosess")).thenReturn(true);
        when(orgenhetService.getEnheterByLeder("A321321")).thenReturn(List.of(orgEnhet()));

        var tilknyttetRessurs = ressursState().toBuilder()
            .sektor(Sektor.EKSTERN).build();

        saveRessurser(List.of(tilknyttetRessurs));

        var existingOrgtilknytning = orgtilknytningState().toBuilder()
            .navident(tilknyttetRessurs.navident())
            .gyldigFom(LocalDate.now())
            .gyldigTom(Optional.of(LocalDate.now().plusMonths(1)))
            .build();

        saveOrgtilknytninger(List.of(existingOrgtilknytning));

        var newStartdato = existingOrgtilknytning.gyldigFom().plusMonths(1);
        LocalDate newSluttdato = null;

        var endreOrgtilknytningRequest = endreOrgtilknytningRequest(existingOrgtilknytning).toBuilder()
            .gyldigFom(
                newStartdato
            )
            .gyldigTom(newSluttdato)
            .build();

        var oppdaterOrgtilknytningerForRessursRequest = oppdaterOrgtilknytningerForRessursRequest().toBuilder()
            .eksisterendeOrgtilknytninger(List.of(endreOrgtilknytningRequest))
            .nyeOrgtilknytninger(Collections.emptyList())
            .setRessursStartdato(true)
            .setRessursSluttdato(true)
            .build();

        postOppdaterOrgtilknytningerForRessursRequest(
            tilknyttetRessurs.navident(),
            testToken.nomWriteGroupAndNomUiAzp(),
            oppdaterOrgtilknytningerForRessursRequest
        )
            .expectStatus().is2xxSuccessful();
    }

    @Test
    @DisplayName("POST /app/orgtilknytninger/forRessurs/{navident} kan ikke redigere orgtilknytning som er over 2 maaneder gammel for ressurs")
    void test_rediger_orgtilknytning_som_er_over_to_mnd_gammel() {
        when(unleash.isEnabled("nom.ressurs.ny-move-prosess")).thenReturn(true);
        when(orgenhetService.getEnheterByLeder("A321321")).thenReturn(List.of(orgEnhet()));
        doReturn(Optional.of(orgEnhet())).when(orgenhetService).getByFid(any());

        var tilknyttetRessurs = ressursState().toBuilder()
            .sektor(Sektor.EKSTERN).build();

        saveRessurser(List.of(tilknyttetRessurs));

        var existingOrgtilknytning = orgtilknytningState().toBuilder()
            .navident(tilknyttetRessurs.navident())
            .gyldigFom(LocalDate.now())
            .gyldigTom(Optional.of(LocalDate.now().plusMonths(1)))
            .build();

        saveOrgtilknytninger(List.of(existingOrgtilknytning));

        var newStartdato = existingOrgtilknytning.gyldigFom().minusMonths(24);
        LocalDate newSluttdato = existingOrgtilknytning.gyldigTom().get().minusMonths(3).minusDays(1);

        var endreOrgtilknytningRequest = endreOrgtilknytningRequest(existingOrgtilknytning).toBuilder()
            .gyldigFom(
                newStartdato
            )
            .gyldigTom(newSluttdato)
            .build();

        var oppdaterOrgtilknytningerForRessursRequest = oppdaterOrgtilknytningerForRessursRequest().toBuilder()
            .eksisterendeOrgtilknytninger(List.of(endreOrgtilknytningRequest))
            .nyeOrgtilknytninger(Collections.emptyList())
            .setRessursStartdato(true)
            .setRessursSluttdato(true)
            .build();

        postOppdaterOrgtilknytningerForRessursRequest(
            tilknyttetRessurs.navident(),
            testToken.nomWriteGroupAndNomUiAzp(),
            oppdaterOrgtilknytningerForRessursRequest
        )
            .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("POST /app/orgtilknytninger/forRessurs/{navident} support updating startdato and sluttdato for ressurs")
    void test_update_orgtilknytninger_for_ressurs_start_and_sluttdato() {
        when(unleash.isEnabled("nom.ressurs.ny-move-prosess")).thenReturn(false);
        var tilknyttetRessurs = ressursState().toBuilder()
            .sektor(Sektor.EKSTERN).build();

        saveRessurser(List.of(tilknyttetRessurs));

        var existingOrgtilknytning = orgtilknytningState().toBuilder()
            .navident(tilknyttetRessurs.navident())
            .gyldigFom(LocalDate.now())
            .gyldigTom(Optional.of(LocalDate.now().plusMonths(1)))
            .build();

        saveOrgtilknytninger(List.of(existingOrgtilknytning));

        var newStartdato = existingOrgtilknytning.gyldigFom().plusMonths(1);
        var newSluttdato = existingOrgtilknytning.gyldigTom().orElseThrow().plusMonths(1);

        var endreOrgtilknytningRequest = endreOrgtilknytningRequest(existingOrgtilknytning).toBuilder()
            .gyldigFom(
                newStartdato
            )
            .gyldigTom(newSluttdato)
            .build();

        var oppdaterOrgtilknytningerForRessursRequest = oppdaterOrgtilknytningerForRessursRequest().toBuilder()
            .eksisterendeOrgtilknytninger(List.of(endreOrgtilknytningRequest))
            .nyeOrgtilknytninger(Collections.emptyList())
            .setRessursStartdato(true)
            .setRessursSluttdato(true)
            .build();

        postOppdaterOrgtilknytningerForRessursRequest(
            tilknyttetRessurs.navident(),
            testToken.nomWriteGroupAndNomUiAzp(),
            oppdaterOrgtilknytningerForRessursRequest
        )
            .expectStatus().is2xxSuccessful();

        verify(orgtilknytningCommandService, times(1)).endreOrgtilknytningDato(any());
        verify(ressursCommandService, times(1)).oppdaterRessursStartdatoEvent(oppdaterStartdatoCommandArgumentCaptor.capture(), any());
        verify(ressursCommandService, times(1)).oppdaterRessursSluttdatoEvent(oppdaterSluttdatoCommandArgumentCaptor.capture());

        assertThat(oppdaterStartdatoCommandArgumentCaptor.getValue())
            .matches(oppdaterStartdatoCommand -> oppdaterStartdatoCommand.startdato().equals(newStartdato))
            .matches(oppdaterStartdatoCommand -> oppdaterStartdatoCommand.fid().equals(tilknyttetRessurs.fid()));

        assertThat(oppdaterSluttdatoCommandArgumentCaptor.getValue())
            .matches(oppdaterSluttdatoCommand -> oppdaterSluttdatoCommand.sluttdato().orElseThrow().equals(newSluttdato))
            .matches(oppdaterSluttdatoCommand -> oppdaterSluttdatoCommand.fid().equals(tilknyttetRessurs.fid()));

    }

    @Test
    @DisplayName("POST /app/orgtilknytninger/forRessurs/{navident} support updating startdato and sluttdato for ressurs")
    void test_update_orgtilknytninger_for_ressurs_start_and_sluttdato_ny() {
        when(unleash.isEnabled("nom.ressurs.ny-move-prosess")).thenReturn(true);
        when(orgenhetService.getEnheterByLeder("A321321")).thenReturn(List.of(orgEnhet()));
        var tilknyttetRessurs = ressursState().toBuilder()
            .sektor(Sektor.EKSTERN).build();

        saveRessurser(List.of(tilknyttetRessurs));

        var existingOrgtilknytning = orgtilknytningState().toBuilder()
            .navident(tilknyttetRessurs.navident())
            .gyldigFom(LocalDate.now())
            .gyldigTom(Optional.of(LocalDate.now().plusMonths(1)))
            .build();

        saveOrgtilknytninger(List.of(existingOrgtilknytning));

        var newStartdato = existingOrgtilknytning.gyldigFom().plusMonths(1);
        var newSluttdato = existingOrgtilknytning.gyldigTom().orElseThrow().plusMonths(1);

        var endreOrgtilknytningRequest = endreOrgtilknytningRequest(existingOrgtilknytning).toBuilder()
            .gyldigFom(
                newStartdato
            )
            .gyldigTom(newSluttdato)
            .build();

        var oppdaterOrgtilknytningerForRessursRequest = oppdaterOrgtilknytningerForRessursRequest().toBuilder()
            .eksisterendeOrgtilknytninger(List.of(endreOrgtilknytningRequest))
            .nyeOrgtilknytninger(Collections.emptyList())
            .setRessursStartdato(true)
            .setRessursSluttdato(true)
            .build();

        postOppdaterOrgtilknytningerForRessursRequest(
            tilknyttetRessurs.navident(),
            testToken.nomWriteGroupAndNomUiAzp(),
            oppdaterOrgtilknytningerForRessursRequest
        )
            .expectStatus().is2xxSuccessful();

        verify(orgtilknytningCommandService, times(1)).endreOrgtilknytningDato(any());
        verify(ressursCommandService, times(1)).oppdaterRessursStartdatoEvent(oppdaterStartdatoCommandArgumentCaptor.capture(), any());
        verify(ressursCommandService, times(1)).oppdaterRessursSluttdatoEvent(oppdaterSluttdatoCommandArgumentCaptor.capture());

        assertThat(oppdaterStartdatoCommandArgumentCaptor.getValue())
            .matches(oppdaterStartdatoCommand -> oppdaterStartdatoCommand.startdato().equals(newStartdato))
            .matches(oppdaterStartdatoCommand -> oppdaterStartdatoCommand.fid().equals(tilknyttetRessurs.fid()));

        assertThat(oppdaterSluttdatoCommandArgumentCaptor.getValue())
            .matches(oppdaterSluttdatoCommand -> oppdaterSluttdatoCommand.sluttdato().orElseThrow().equals(newSluttdato))
            .matches(oppdaterSluttdatoCommand -> oppdaterSluttdatoCommand.fid().equals(tilknyttetRessurs.fid()));

    }

    @Test
    @DisplayName("POST /app/orgtilknytninger/forRessurs/{navident} should update update and add new orgtilknytninger for ressurs")
    void test_update_orgtilknytninger_for_ressurs() {
        when(unleash.isEnabled("nom.ressurs.ny-move-prosess")).thenReturn(false);
        var tilknyttetRessurs = ressursState().toBuilder()
            .sektor(Sektor.EKSTERN).build();

        saveRessurser(List.of(tilknyttetRessurs));

        var existingOrgtilknytningAnticipatingNewDate = orgtilknytningState().toBuilder()
            .navident(tilknyttetRessurs.navident())
            .orgEnhetId("1")
            .erDagligOppfolging(false)
            .gyldigTom(Optional.of(LocalDate.now().plusMonths(1)))
            .build();

        var existingOrgtilknytningAnticipatingDolChange = orgtilknytningState().toBuilder()
            .navident(tilknyttetRessurs.navident())
            .orgEnhetId("2")
            .erDagligOppfolging(false)
            .gyldigFom(existingOrgtilknytningAnticipatingNewDate.gyldigTom().orElseThrow().plusDays(1))
            .gyldigTom(existingOrgtilknytningAnticipatingNewDate.gyldigTom().map(gyldigTom -> gyldigTom.plusMonths(1)))
            .build();

        var existingOrgtilknytningAnticipatingNewTilknytningtype = orgtilknytningState().toBuilder()
            .navident(tilknyttetRessurs.navident())
            .orgEnhetId("3")
            .erDagligOppfolging(false)
            .orgtilknytningType(Optional.empty())
            .build();

        saveOrgtilknytninger(List.of(
            existingOrgtilknytningAnticipatingNewDate,
            existingOrgtilknytningAnticipatingDolChange,
            existingOrgtilknytningAnticipatingNewTilknytningtype
        ));

        var endreDatoOrgtilknytningRequest = endreOrgtilknytningRequest(existingOrgtilknytningAnticipatingNewDate).toBuilder()
            .gyldigTom(
                existingOrgtilknytningAnticipatingNewDate.gyldigTom().map(localDate -> localDate.plusMonths(1)).orElseThrow()
            )
            .build();

        var endreDolOrgtilknytningRequest = endreOrgtilknytningRequest(existingOrgtilknytningAnticipatingDolChange).toBuilder()
            .erDagligOppfolging(true)
            .build();

        var endreTilknytningtypeRequest = endreOrgtilknytningRequest(existingOrgtilknytningAnticipatingNewTilknytningtype).toBuilder()
            .tilknytningtype(Tilknytningtype.KONSULENT)
            .build();

        var leggTilOrgtilknytningRequest = leggTilOrgtilknytningRequest().toBuilder()
            .erDagligOppfolging(false)
            .orgEnhetId("4")
            .tilknytningtype(null)
            .build();

        var opprettetOrgtilknytningFid = UUID.randomUUID().toString();

        when(orgtilknytningCommandService.opprettOrgtilknytning(any())).thenReturn(opprettetOrgtilknytningFid);

        var oppdaterOrgtilknytningerForRessursRequest = oppdaterOrgtilknytningerForRessursRequest().toBuilder()
            .eksisterendeOrgtilknytninger(List.of(
                endreDatoOrgtilknytningRequest,
                endreDolOrgtilknytningRequest,
                endreTilknytningtypeRequest
            ))
            .nyeOrgtilknytninger(List.of(leggTilOrgtilknytningRequest))
            .build();

        postOppdaterOrgtilknytningerForRessursRequest(
            tilknyttetRessurs.navident(),
            testToken.nomWriteGroupAndNomUiAzp(),
            oppdaterOrgtilknytningerForRessursRequest
        )
            .expectStatus().is2xxSuccessful();

        verify(orgtilknytningCommandService, times(1)).endreDagligOppfolgingOrgtilknytning(endreDagligOppfolgingOrgtilknytningCommandArgumentCaptor.capture());
        assertThat(endreDagligOppfolgingOrgtilknytningCommandArgumentCaptor.getValue()).usingRecursiveComparison()
            .ignoringFields("inputMetadata")
            .isEqualTo(EndreDagligOppfolgingOrgtilknytningCommand.builder()
                .fid(endreDolOrgtilknytningRequest.fid())
                .gyldigFom(endreDolOrgtilknytningRequest.gyldigFom())
                .gyldigTom(Optional.ofNullable(endreDolOrgtilknytningRequest.gyldigTom()))
                .erDagligOppfolging(endreDolOrgtilknytningRequest.erDagligOppfolging())
                .navident(existingOrgtilknytningAnticipatingDolChange.navident())
                .orgenhetId(existingOrgtilknytningAnticipatingDolChange.orgEnhetId())
                .additionalAgressoData(null)
                .build()
            );

        verify(orgtilknytningCommandService, times(1)).endreTilknytningtype(endreTilknytningtypeCommandArgumentCaptor.capture());
        assertThat(endreTilknytningtypeCommandArgumentCaptor.getValue()).usingRecursiveComparison()
            .ignoringFields("inputMetadata")
            .isEqualTo(EndreTilknytningtypeCommand.builder()
                .fid(endreTilknytningtypeRequest.fid())
                .tilknytningtype(Optional.ofNullable(endreTilknytningtypeRequest.tilknytningtype()))
                .build()
            );

        verify(orgtilknytningCommandService, times(2)).endreOrgtilknytningDato(endreOrgtilknytningDatoCommandArgumentCaptor.capture());
        assertThat(endreOrgtilknytningDatoCommandArgumentCaptor.getAllValues()).usingRecursiveComparison()
            .ignoringFields("inputMetadata")
            .isEqualTo(List.of(
                EndreOrgtilknytningDatoCommand.builder()
                    .fid(endreDatoOrgtilknytningRequest.fid())
                    .gyldigFom(endreDatoOrgtilknytningRequest.gyldigFom())
                    .gyldigTom(Optional.ofNullable(endreDatoOrgtilknytningRequest.gyldigTom()))
                    .build(),
                EndreOrgtilknytningDatoCommand.builder()
                    .fid(opprettetOrgtilknytningFid)
                    .gyldigFom(leggTilOrgtilknytningRequest.gyldigFom())
                    .gyldigTom(Optional.ofNullable(leggTilOrgtilknytningRequest.gyldigTom()))
                    .build()
            ));

        verify(orgtilknytningCommandService, times(1)).opprettOrgtilknytning(opprettOrgtilknytningArgumentCaptor.capture());
        assertThat(opprettOrgtilknytningArgumentCaptor.getValue()).usingRecursiveComparison()
            .ignoringFields("inputMetadata")
            .isEqualTo(OpprettOrgtilknytning.builder()
                .navident(tilknyttetRessurs.navident())
                .orgenhetId(leggTilOrgtilknytningRequest.orgEnhetId())
                .gyldigFom(leggTilOrgtilknytningRequest.gyldigFom())
                .additionalAgressoData(null)
                .build()
            );
    }

    @Test
    @DisplayName("POST /app/orgtilknytninger/forRessurs/{navident} should update update and add new orgtilknytninger for ressurs")
    void test_update_orgtilknytninger_for_ressurs_ny() {
        when(unleash.isEnabled("nom.ressurs.ny-move-prosess")).thenReturn(true);
        when(orgenhetService.getEnheterByLeder("A321321")).thenReturn(List.of(orgEnhet()));
        var tilknyttetRessurs = ressursState().toBuilder()
            .sektor(Sektor.EKSTERN).build();

        saveRessurser(List.of(tilknyttetRessurs));

        var existingOrgtilknytningAnticipatingNewDate = orgtilknytningState().toBuilder()
            .navident(tilknyttetRessurs.navident())
            .orgEnhetId("1")
            .erDagligOppfolging(false)
            .gyldigTom(Optional.of(LocalDate.now().plusMonths(1)))
            .build();

        var existingOrgtilknytningAnticipatingDolChange = orgtilknytningState().toBuilder()
            .navident(tilknyttetRessurs.navident())
            .orgEnhetId("2")
            .erDagligOppfolging(false)
            .gyldigFom(existingOrgtilknytningAnticipatingNewDate.gyldigTom().orElseThrow().plusDays(1))
            .gyldigTom(existingOrgtilknytningAnticipatingNewDate.gyldigTom().map(gyldigTom -> gyldigTom.plusMonths(1)))
            .build();

        var existingOrgtilknytningAnticipatingNewTilknytningtype = orgtilknytningState().toBuilder()
            .navident(tilknyttetRessurs.navident())
            .orgEnhetId("3")
            .erDagligOppfolging(false)
            .orgtilknytningType(Optional.empty())
            .build();

        saveOrgtilknytninger(List.of(
            existingOrgtilknytningAnticipatingNewDate,
            existingOrgtilknytningAnticipatingDolChange,
            existingOrgtilknytningAnticipatingNewTilknytningtype
        ));

        var endreDatoOrgtilknytningRequest = endreOrgtilknytningRequest(existingOrgtilknytningAnticipatingNewDate).toBuilder()
            .gyldigTom(
                existingOrgtilknytningAnticipatingNewDate.gyldigTom().map(localDate -> localDate.plusMonths(1)).orElseThrow()
            )
            .build();

        var endreDolOrgtilknytningRequest = endreOrgtilknytningRequest(existingOrgtilknytningAnticipatingDolChange).toBuilder()
            .erDagligOppfolging(true)
            .build();

        var endreTilknytningtypeRequest = endreOrgtilknytningRequest(existingOrgtilknytningAnticipatingNewTilknytningtype).toBuilder()
            .tilknytningtype(Tilknytningtype.KONSULENT)
            .build();

        var leggTilOrgtilknytningRequest = leggTilOrgtilknytningRequest().toBuilder()
            .erDagligOppfolging(false)
            .orgEnhetId("4")
            .tilknytningtype(null)
            .build();

        var opprettetOrgtilknytningFid = UUID.randomUUID().toString();

        when(orgtilknytningCommandService.opprettOrgtilknytning(any())).thenReturn(opprettetOrgtilknytningFid);

        var oppdaterOrgtilknytningerForRessursRequest = oppdaterOrgtilknytningerForRessursRequest().toBuilder()
            .eksisterendeOrgtilknytninger(List.of(
                endreDatoOrgtilknytningRequest,
                endreDolOrgtilknytningRequest,
                endreTilknytningtypeRequest
            ))
            .nyeOrgtilknytninger(List.of(leggTilOrgtilknytningRequest))
            .build();

        postOppdaterOrgtilknytningerForRessursRequest(
            tilknyttetRessurs.navident(),
            testToken.nomWriteGroupAndNomUiAzp(),
            oppdaterOrgtilknytningerForRessursRequest
        )
            .expectStatus().is2xxSuccessful();

        verify(orgtilknytningCommandService, times(1)).endreDagligOppfolgingOrgtilknytning(endreDagligOppfolgingOrgtilknytningCommandArgumentCaptor.capture());
        assertThat(endreDagligOppfolgingOrgtilknytningCommandArgumentCaptor.getValue()).usingRecursiveComparison()
            .ignoringFields("inputMetadata")
            .isEqualTo(EndreDagligOppfolgingOrgtilknytningCommand.builder()
                .fid(endreDolOrgtilknytningRequest.fid())
                .gyldigFom(endreDolOrgtilknytningRequest.gyldigFom())
                .gyldigTom(Optional.ofNullable(endreDolOrgtilknytningRequest.gyldigTom()))
                .erDagligOppfolging(endreDolOrgtilknytningRequest.erDagligOppfolging())
                .navident(existingOrgtilknytningAnticipatingDolChange.navident())
                .orgenhetId(existingOrgtilknytningAnticipatingDolChange.orgEnhetId())
                .additionalAgressoData(null)
                .build()
            );

        verify(orgtilknytningCommandService, times(1)).endreTilknytningtype(endreTilknytningtypeCommandArgumentCaptor.capture());
        assertThat(endreTilknytningtypeCommandArgumentCaptor.getValue()).usingRecursiveComparison()
            .ignoringFields("inputMetadata")
            .isEqualTo(EndreTilknytningtypeCommand.builder()
                .fid(endreTilknytningtypeRequest.fid())
                .tilknytningtype(Optional.ofNullable(endreTilknytningtypeRequest.tilknytningtype()))
                .build()
            );

        verify(orgtilknytningCommandService, times(2)).endreOrgtilknytningDato(endreOrgtilknytningDatoCommandArgumentCaptor.capture());
        assertThat(endreOrgtilknytningDatoCommandArgumentCaptor.getAllValues()).usingRecursiveComparison()
            .ignoringFields("inputMetadata")
            .isEqualTo(List.of(
                EndreOrgtilknytningDatoCommand.builder()
                    .fid(endreDatoOrgtilknytningRequest.fid())
                    .gyldigFom(endreDatoOrgtilknytningRequest.gyldigFom())
                    .gyldigTom(Optional.ofNullable(endreDatoOrgtilknytningRequest.gyldigTom()))
                    .build(),
                EndreOrgtilknytningDatoCommand.builder()
                    .fid(opprettetOrgtilknytningFid)
                    .gyldigFom(leggTilOrgtilknytningRequest.gyldigFom())
                    .gyldigTom(Optional.ofNullable(leggTilOrgtilknytningRequest.gyldigTom()))
                    .build()
            ));

        verify(orgtilknytningCommandService, times(1)).opprettOrgtilknytning(opprettOrgtilknytningArgumentCaptor.capture());
        assertThat(opprettOrgtilknytningArgumentCaptor.getValue()).usingRecursiveComparison()
            .ignoringFields("inputMetadata")
            .isEqualTo(OpprettOrgtilknytning.builder()
                .navident(tilknyttetRessurs.navident())
                .orgenhetId(leggTilOrgtilknytningRequest.orgEnhetId())
                .gyldigFom(leggTilOrgtilknytningRequest.gyldigFom())
                .additionalAgressoData(null)
                .build()
            );
    }

    @Test
    @DisplayName("POST /app/orgtilknytninger/forRessurs/{navident} should create new orgtilknytning with erDagligOppfolging=true and tilknytningtype not empty")
    void test_add_dol_with_tilknytningtype_to_ressurs() {
        when(unleash.isEnabled("nom.ressurs.ny-move-prosess")).thenReturn(false);

        var tilknyttetRessurs = ressursState().toBuilder()
            .sektor(Sektor.EKSTERN).build();

        saveRessurser(List.of(tilknyttetRessurs));

        var leggTilDagligOppfolgingOrgtilknytningWithTilknytningtypeRequest = leggTilOrgtilknytningRequest().toBuilder()
            .erDagligOppfolging(true)
            .tilknytningtype(Tilknytningtype.KONSULENT)
            .build();

        var opprettetOrgtilknytningFid = UUID.randomUUID().toString();

        when(orgtilknytningCommandService.opprettOrgtilknytning(any())).thenReturn(opprettetOrgtilknytningFid);

        var oppdaterOrgtilknytningerForRessursRequest = oppdaterOrgtilknytningerForRessursRequest().toBuilder()
            .eksisterendeOrgtilknytninger(Collections.emptyList())
            .nyeOrgtilknytninger(List.of(leggTilDagligOppfolgingOrgtilknytningWithTilknytningtypeRequest))
            .build();

        postOppdaterOrgtilknytningerForRessursRequest(
            tilknyttetRessurs.navident(),
            testToken.nomWriteGroupAndNomUiAzp(),
            oppdaterOrgtilknytningerForRessursRequest
        )
            .expectStatus().is2xxSuccessful();

        verify(orgtilknytningCommandService, times(1)).opprettOrgtilknytning(opprettOrgtilknytningArgumentCaptor.capture());
        assertThat(opprettOrgtilknytningArgumentCaptor.getValue()).usingRecursiveComparison()
            .ignoringFields("inputMetadata")
            .isEqualTo(OpprettOrgtilknytning.builder()
                .navident(tilknyttetRessurs.navident())
                .orgenhetId(leggTilDagligOppfolgingOrgtilknytningWithTilknytningtypeRequest.orgEnhetId())
                .gyldigFom(leggTilDagligOppfolgingOrgtilknytningWithTilknytningtypeRequest.gyldigFom())
                .additionalAgressoData(null)
                .build()
            );

        verify(orgtilknytningCommandService, times(1)).endreDagligOppfolgingOrgtilknytning(endreDagligOppfolgingOrgtilknytningCommandArgumentCaptor.capture());
        assertThat(endreDagligOppfolgingOrgtilknytningCommandArgumentCaptor.getValue()).usingRecursiveComparison()
            .ignoringFields("inputMetadata")
            .isEqualTo(EndreDagligOppfolgingOrgtilknytningCommand.builder()
                .fid(opprettetOrgtilknytningFid)
                .gyldigFom(leggTilDagligOppfolgingOrgtilknytningWithTilknytningtypeRequest.gyldigFom())
                .gyldigTom(Optional.ofNullable(leggTilDagligOppfolgingOrgtilknytningWithTilknytningtypeRequest.gyldigTom()))
                .erDagligOppfolging(leggTilDagligOppfolgingOrgtilknytningWithTilknytningtypeRequest.erDagligOppfolging())
                .navident(tilknyttetRessurs.navident())
                .orgenhetId(leggTilDagligOppfolgingOrgtilknytningWithTilknytningtypeRequest.orgEnhetId())
                .additionalAgressoData(null)
                .build()
            );

        verify(orgtilknytningCommandService, times(1)).endreTilknytningtype(endreTilknytningtypeCommandArgumentCaptor.capture());
        assertThat(endreTilknytningtypeCommandArgumentCaptor.getValue()).usingRecursiveComparison()
            .ignoringFields("inputMetadata")
            .isEqualTo(EndreTilknytningtypeCommand.builder()
                .fid(opprettetOrgtilknytningFid)
                .tilknytningtype(Optional.ofNullable(leggTilDagligOppfolgingOrgtilknytningWithTilknytningtypeRequest.tilknytningtype()))
                .build()
            );
    }

    @Test
    @DisplayName("POST /app/orgtilknytninger/forRessurs/{navident} Should forbid update orgtilknytning type if user user has 'MUTATE_RESSURS' but is not authorized for ressurs")
    void test_update_orgtilknytninger_for_ressurs_forbidden() {
        when(authResolver.kanRedigereRessurs(any())).thenReturn(false);

        var tilknyttetRessurs = ressursState().toBuilder()
            .sektor(Sektor.EKSTERN).build();

        saveRessurser(List.of(tilknyttetRessurs));

        var existingOrgtilknytning = orgtilknytningState().toBuilder()
            .navident(tilknyttetRessurs.navident())
            .gyldigTom(Optional.of(LocalDate.now().plusMonths(1)))
            .build();

        saveOrgtilknytninger(List.of(existingOrgtilknytning));

        var leggTilDagligOppfolgingOrgtilknytningWithTilknytningtypeRequest = leggTilOrgtilknytningRequest().toBuilder()
            .erDagligOppfolging(true)
            .tilknytningtype(Tilknytningtype.KONSULENT)
            .build();

        var oppdaterOrgtilknytningerForRessursRequest = oppdaterOrgtilknytningerForRessursRequest().toBuilder()
            .eksisterendeOrgtilknytninger(Collections.emptyList())
            .nyeOrgtilknytninger(List.of(leggTilDagligOppfolgingOrgtilknytningWithTilknytningtypeRequest))
            .build();

        postOppdaterOrgtilknytningerForRessursRequest(tilknyttetRessurs.navident(),testToken.nomWriteGroupAndNomUiAzp(),oppdaterOrgtilknytningerForRessursRequest)
            .expectStatus().isForbidden();
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("Should return 400 if request has overlapping daglige oppfolginger")
    @MethodSource("test_no_overlapping_daglig_oppfolginger_input")
    void test_no_overlapping_daglig_oppfolginger(OppdaterOrgtilknytningerForRessursRequest oppdaterOrgtilknytningerForRessursRequest) {
        when(unleash.isEnabled("nom.ressurs.ny-move-prosess")).thenReturn(false);
        postOppdaterOrgtilknytningerForRessursRequest(
            "whatever",
            testToken.nomWriteGroupAndNomUiAzp(),
            oppdaterOrgtilknytningerForRessursRequest
        ).expectStatus().isBadRequest();
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("Should return 400 if request has overlapping daglige oppfolginger")
    @MethodSource("test_no_overlapping_daglig_oppfolginger_input")
    void test_no_overlapping_daglig_oppfolginger_ny(OppdaterOrgtilknytningerForRessursRequest oppdaterOrgtilknytningerForRessursRequest) {
        when(unleash.isEnabled("nom.ressurs.ny-move-prosess")).thenReturn(true);
        when(orgenhetService.getEnheterByLeder("A321321")).thenReturn(List.of(orgEnhet()));
        postOppdaterOrgtilknytningerForRessursRequest(
            "whatever",
            testToken.nomWriteGroupAndNomUiAzp(),
            oppdaterOrgtilknytningerForRessursRequest
        ).expectStatus().isBadRequest();
    }

    private static Stream<Arguments> test_no_overlapping_daglig_oppfolginger_input() {
        var gyldigFom = LocalDate.now();
        var gyldigTom = LocalDate.now().plusMonths(11);


        var endreOrgtilknytningRequest = endreOrgtilknytningRequest().toBuilder()
            .gyldigFom(gyldigFom)
            .gyldigTom(gyldigTom)
            .erDagligOppfolging(true)
            .build();
        var leggtilOrgtilknytningRequest = leggTilOrgtilknytningRequest().toBuilder()
            .gyldigFom(gyldigFom)
            .gyldigTom(gyldigTom)
            .erDagligOppfolging(true)
            .build();

        return Stream.of(
            // eksisterendeOrgtilknytninger og nyeOrgtilknytninger inneholder overlapp
            Arguments.of(
                oppdaterOrgtilknytningerForRessursRequest().toBuilder()
                    .eksisterendeOrgtilknytninger(List.of(endreOrgtilknytningRequest))
                    .nyeOrgtilknytninger(List.of(leggtilOrgtilknytningRequest))
                    .build()
            ),
            // eksisterendeOrgtilknytninger inneholder overlapp
            Arguments.of(
                oppdaterOrgtilknytningerForRessursRequest().toBuilder()
                    .eksisterendeOrgtilknytninger(List.of(
                        endreOrgtilknytningRequest,
                        endreOrgtilknytningRequest.toBuilder().fid(UUID.randomUUID().toString()).build()
                    ))
                    .nyeOrgtilknytninger(Collections.emptyList())
                    .build()
            ),
            // nyeOrgtilknytninger inneholder overlapp
            Arguments.of(
                oppdaterOrgtilknytningerForRessursRequest().toBuilder()
                    .eksisterendeOrgtilknytninger(Collections.emptyList())
                    .nyeOrgtilknytninger(List.of(
                        leggtilOrgtilknytningRequest,
                        leggtilOrgtilknytningRequest.toBuilder()
                            .orgEnhetId("adadafa")
                            .build()
                    ))
                    .build()
            )
        );
    }



    @ParameterizedTest(name = "existing fids: {0} \noppdaterOrgtilknytningerForRessursRequest: {1}")
    @DisplayName("Should return 400 if eksisterendeOrgtilknytninger does not contain exactly all existing orgtilknytninger")
    @MethodSource("test_oppdaterOrgtilknytningerForRessursRequest_contains_exactly_all_existing_orgtilknytninger_input")
    void test_eksisterendeOrgtilknytninger_contains_exactly_all_existing_orgtilknytninger(
        List<String> existingOrgtilknytningFids,
        OppdaterOrgtilknytningerForRessursRequest oppdaterOrgtilknytningerForRessursRequest
    ) {
        when(unleash.isEnabled("nom.ressurs.ny-move-prosess")).thenReturn(false);
        var tilknyttetRessurs = ressursState().toBuilder()
            .sektor(Sektor.EKSTERN).build();

        saveRessurser(List.of(tilknyttetRessurs));

        saveOrgtilknytninger(existingOrgtilknytningFids.stream().map(fid ->
            orgtilknytningState().toBuilder()
                .fid(fid)
                .orgEnhetId(UUID.randomUUID().toString())
                .erDagligOppfolging(false)
                .navident(tilknyttetRessurs.navident())
                .build()
        ).toList());

        postOppdaterOrgtilknytningerForRessursRequest(
            tilknyttetRessurs.navident(),
            testToken.nomWriteGroupAndNomUiAzp(),
            oppdaterOrgtilknytningerForRessursRequest
        ).expectStatus().isBadRequest();
    }

    @ParameterizedTest(name = "existing fids: {0} \noppdaterOrgtilknytningerForRessursRequest: {1}")
    @DisplayName("Should return 400 if eksisterendeOrgtilknytninger does not contain exactly all existing orgtilknytninger")
    @MethodSource("test_oppdaterOrgtilknytningerForRessursRequest_contains_exactly_all_existing_orgtilknytninger_input")
    void test_eksisterendeOrgtilknytninger_contains_exactly_all_existing_orgtilknytninger_ny(
        List<String> existingOrgtilknytningFids,
        OppdaterOrgtilknytningerForRessursRequest oppdaterOrgtilknytningerForRessursRequest
    ) {
        when(unleash.isEnabled("nom.ressurs.ny-move-prosess")).thenReturn(true);
        when(orgenhetService.getEnheterByLeder("A321321")).thenReturn(List.of(orgEnhet()));
        var tilknyttetRessurs = ressursState().toBuilder()
            .sektor(Sektor.EKSTERN).build();

        saveRessurser(List.of(tilknyttetRessurs));

        saveOrgtilknytninger(existingOrgtilknytningFids.stream().map(fid ->
            orgtilknytningState().toBuilder()
                .fid(fid)
                .orgEnhetId(UUID.randomUUID().toString())
                .erDagligOppfolging(false)
                .navident(tilknyttetRessurs.navident())
                .build()
        ).toList());

        postOppdaterOrgtilknytningerForRessursRequest(
            tilknyttetRessurs.navident(),
            testToken.nomWriteGroupAndNomUiAzp(),
            oppdaterOrgtilknytningerForRessursRequest
        ).expectStatus().isBadRequest();
    }

    private static Stream<Arguments> test_oppdaterOrgtilknytningerForRessursRequest_contains_exactly_all_existing_orgtilknytninger_input() {
        var existinOrgtilknytningFidsForRessurs = List.of(
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString()
        );

        var validExistingOrgtilknytniner = existinOrgtilknytningFidsForRessurs.stream().map(fid ->
            endreOrgtilknytningRequest().toBuilder()
                .erDagligOppfolging(false)
                .fid(fid)
                .build()
        ).toList();

        var oneTooMany = Stream.concat(
            validExistingOrgtilknytniner.stream(),
            Stream.of(endreOrgtilknytningRequest())
        ).toList();

        var oneTooFew = validExistingOrgtilknytniner.subList(0, 1);

        var duplicateFids = Stream.concat(
            validExistingOrgtilknytniner.stream(),
            Stream.of(endreOrgtilknytningRequest().toBuilder().fid(existinOrgtilknytningFidsForRessurs.get(1)).build())
        ).toList();

        return Stream.of(
            Arguments.of(
                existinOrgtilknytningFidsForRessurs,
                oppdaterOrgtilknytningerForRessursRequest().toBuilder()
                    .eksisterendeOrgtilknytninger(oneTooMany)
                    .nyeOrgtilknytninger(Collections.emptyList())
                    .build()
            ),
            Arguments.of(
                existinOrgtilknytningFidsForRessurs,
                oppdaterOrgtilknytningerForRessursRequest().toBuilder()
                    .eksisterendeOrgtilknytninger(oneTooFew)
                    .nyeOrgtilknytninger(Collections.emptyList())
                    .build()
            ),
            Arguments.of(
                existinOrgtilknytningFidsForRessurs,
                oppdaterOrgtilknytningerForRessursRequest().toBuilder()
                    .eksisterendeOrgtilknytninger(duplicateFids)
                    .nyeOrgtilknytninger(Collections.emptyList())
                    .build()
            )
        );
    }

    // TODO: Påse at kun brukere med hasAuthority('MUTATE_RESSURS') && hasAuthority('NOM_UI_APP') kan kalle POST /forRessurs/{navident}

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

    private static LeggTilOrgtilknytningRequestOld leggTilOrgtilknytningRequestOld() {
        return LeggTilOrgtilknytningRequestOld.builder()
            .navident("X12345")
            .orgEnhetId("some-orgenhet")
            .gyldigFom(LocalDate.now())
            .gyldigTom(Optional.of(LocalDate.now().plusMonths(5)))
            .erDagligOppfolging(Optional.empty())
            .setRessursSluttdato(Optional.empty())
            .build();
    }

    private static LeggTilOrgtilknytningRequest leggTilOrgtilknytningRequest() {
        return LeggTilOrgtilknytningRequest.builder()
            .orgEnhetId("some-new-orgenhet")
            .erDagligOppfolging(false)
            .gyldigFom(LocalDate.now())
            .gyldigTom(LocalDate.now().plusMonths(12))
            .tilknytningtype(null)
            .build();
    }

    private static EndreOrgtilknytningRequest endreOrgtilknytningRequest() {
        return EndreOrgtilknytningRequest.builder()
            .fid(UUID.randomUUID().toString())
            .gyldigFom(LocalDate.now().minusMonths(4))
            .gyldigTom(LocalDate.now().plusMonths(1))
            .erDagligOppfolging(false)
            .build();
    }

    private static EndreOrgtilknytningRequest endreOrgtilknytningRequest(OrgtilknytningState fromOrgtilknytningState) {
        return EndreOrgtilknytningRequest.builder()
            .fid(fromOrgtilknytningState.fid())
            .gyldigFom(fromOrgtilknytningState.gyldigFom())
            .gyldigTom(fromOrgtilknytningState.gyldigTom().orElse(null))
            .erDagligOppfolging(fromOrgtilknytningState.erDagligOppfolging())
            .tilknytningtype(fromOrgtilknytningState.orgtilknytningType().orElse(null))
            .build();
    }

    private static OppdaterOrgtilknytningerForRessursRequest oppdaterOrgtilknytningerForRessursRequest() {
        return OppdaterOrgtilknytningerForRessursRequest.builder()
            .nyeOrgtilknytninger(List.of(leggTilOrgtilknytningRequest()))
            .eksisterendeOrgtilknytninger(List.of(endreOrgtilknytningRequest()))
            .setRessursSluttdato(false)
            .setRessursStartdato(false)
            .build();
    }

    private WebTestClient.ResponseSpec postEndreTilknytningtypeRequest(
        String fid,
        String accessToken,
        EndreTilknytningtypeRequest endreTilknytningtypeRequest
    ) {
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
        LeggTilOrgtilknytningRequestOld leggTilOrgtilknytningRequest
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

    private WebTestClient.ResponseSpec postOppdaterOrgtilknytningerForRessursRequest(
        String navident,
        String accessToken,
        OppdaterOrgtilknytningerForRessursRequest oppdaterOrgtilknytningerForRessursRequest
    ) {
        return webTestClient
            .post()
            .uri(uriBuilder -> uriBuilder
                .path("/app/orgtilknytninger/forRessurs/{navident}")
                .build(navident)
            )
            .headers(httpHeaders -> {
                httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                httpHeaders.setBearerAuth(accessToken);
            })
            .bodyValue(oppdaterOrgtilknytningerForRessursRequest)
            .exchange();
    }
}
