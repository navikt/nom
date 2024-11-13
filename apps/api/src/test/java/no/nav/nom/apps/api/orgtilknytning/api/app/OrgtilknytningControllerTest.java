package no.nav.nom.apps.api.orgtilknytning.api.app;

import no.nav.nom.apps.api.TestTokensGenerator;
import no.nav.nom.apps.api.authorize.AuthResolver;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.OrgtilknytningCommandService;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.EndreDagligOppfolgingOrgtilknytningCommand;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.EndreOrgtilknytningDatoCommand;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.EndreTilknytningtypeCommand;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.OpprettOrgtilknytning;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.RessursCommandService;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OppdaterSluttdatoCommand;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OppdaterStartdatoCommand;
import no.nav.nom.apps.api.rest.RestTestBase;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.Tilknytningtype;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateRepository;
import no.nav.nom.libs.state.ressurs.NomStateRessursListener;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static no.nav.nom.apps.api.common.TestData.orgEnhet;
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
    private RessursCommandService ressursCommandService;

    @MockBean
    private NomStateRessursListener nomStateRessursListener;

    @MockBean
    private OrgenhetService orgenhetService;

    @MockBean
    private AuthResolver authResolver;

    @MockBean
    private RessursStateRepository ressursStateRepository;

    @Autowired
    TestTokensGenerator testToken;

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
    @DisplayName("Should update tilknytningtype for orgtilknytning")
    void test_update_orgtilknytning_type() {
        var orgtilknytningToUpdate = orgtilknytningState();
        when(ressursStateRepository.findByNavident(orgtilknytningToUpdate.navident())).thenReturn(Optional.of(ressursState().toBuilder().sektor(Sektor.EKSTERN).build()));
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
        when(unleash.isEnabled("nom.ressurs.opprett_statlig")).thenReturn(false);
        when(ressursStateRepository.findByNavident("X123456")).thenReturn(Optional.of(ressursState().toBuilder().sektor(Sektor.NAV_STATLIG).build()));
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
    @DisplayName("Should forbid update orgtilknytning type if user user has 'MUTATE_RESSURS' but is not authorized for ressurs")
    void test_update_orgtilknytning_type_forbidden() {
        when(authResolver.kanRedigereRessurs(any())).thenReturn(false);

        var orgtilknytningToUpdate = orgtilknytningState();
        saveOrgtilknytninger(List.of(orgtilknytningToUpdate));

        postEndreTilknytningtypeRequest(orgtilknytningToUpdate.fid(), testToken.nomWriteGroupAndNomUiAzp(),endreTilknytningtypeRequest())
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

        var tilknyttetRessurs = ressursState().toBuilder()
            .sektor(Sektor.EKSTERN).build();
        when(ressursStateRepository.findByNavident(tilknyttetRessurs.navident())).thenReturn(Optional.of(tilknyttetRessurs));
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

        when(orgenhetService.getEnheterByLeder("A321321")).thenReturn(List.of(orgEnhet()));
        var tilknyttetRessurs = ressursState().toBuilder()
            .sektor(Sektor.EKSTERN).build();

        when(ressursStateRepository.findByNavident(tilknyttetRessurs.navident())).thenReturn(Optional.of(tilknyttetRessurs));
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
        OrgtilknytningStateRepository orgtilknytningStateRepository = Mockito.mock(OrgtilknytningStateRepository.class);

        when(orgenhetService.getEnheterByLeder("A321321")).thenReturn(List.of(orgEnhet()));
        var tilknyttetRessurs = ressursState().toBuilder()
            .sektor(Sektor.EKSTERN).build();

        when(ressursStateRepository.findByNavident(tilknyttetRessurs.navident())).thenReturn(Optional.of(tilknyttetRessurs));
        saveRessurser(List.of(tilknyttetRessurs));

        var existingOrgtilknytning = orgtilknytningState().toBuilder()
            .navident(tilknyttetRessurs.navident())
            .gyldigFom(LocalDate.now())
            .gyldigTom(Optional.of(LocalDate.now().minusMonths(2).minusDays(1)))
            .build();


        saveOrgtilknytninger(List.of(existingOrgtilknytning));
        when(orgtilknytningStateRepository.findByFid(existingOrgtilknytning.fid())).thenReturn(Optional.of(existingOrgtilknytning));

        var newStartdato = existingOrgtilknytning.gyldigFom().minusMonths(24);
        LocalDate newSluttdato = existingOrgtilknytning.gyldigTom().isPresent() ? existingOrgtilknytning.gyldigTom().get().minusMonths(3).minusDays(1) : null;

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

        var tilknyttetRessurs = ressursState().toBuilder()
            .sektor(Sektor.EKSTERN).build();
        when(ressursStateRepository.findByNavident(tilknyttetRessurs.navident())).thenReturn(Optional.of(tilknyttetRessurs));
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

        when(orgenhetService.getEnheterByLeder("A321321")).thenReturn(List.of(orgEnhet()));
        var tilknyttetRessurs = ressursState().toBuilder()
            .sektor(Sektor.EKSTERN).build();
        when(ressursStateRepository.findByNavident(tilknyttetRessurs.navident())).thenReturn(Optional.of(tilknyttetRessurs));
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

        var tilknyttetRessurs = ressursState().toBuilder()
            .sektor(Sektor.EKSTERN).build();
        when(ressursStateRepository.findByNavident(tilknyttetRessurs.navident())).thenReturn(Optional.of(tilknyttetRessurs));
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
            .sektor(tilknyttetRessurs.sektor())
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
                .sektor(leggTilOrgtilknytningRequest.sektor())
                .gyldigFom(leggTilOrgtilknytningRequest.gyldigFom())
                .additionalAgressoData(null)
                .build()
            );
    }

    @Test
    @DisplayName("POST /app/orgtilknytninger/forRessurs/{navident} should update update and add new orgtilknytninger for ressurs")
    void test_update_orgtilknytninger_for_ressurs_ny() {

        when(orgenhetService.getEnheterByLeder("A321321")).thenReturn(List.of(orgEnhet()));
        var tilknyttetRessurs = ressursState().toBuilder()
            .sektor(Sektor.EKSTERN).build();
        when(ressursStateRepository.findByNavident(tilknyttetRessurs.navident())).thenReturn(Optional.of(tilknyttetRessurs));
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
            .sektor(tilknyttetRessurs.sektor())
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
                .sektor(leggTilOrgtilknytningRequest.sektor())
                .gyldigFom(leggTilOrgtilknytningRequest.gyldigFom())
                .additionalAgressoData(null)
                .build()
            );
    }

    @Test
    @DisplayName("POST /app/orgtilknytninger/forRessurs/{navident} should create new orgtilknytning with erDagligOppfolging=true and tilknytningtype not empty")
    void test_add_dol_with_tilknytningtype_to_ressurs() {


        var tilknyttetRessurs = ressursState().toBuilder()
            .sektor(Sektor.EKSTERN).build();
        when(ressursStateRepository.findByNavident(tilknyttetRessurs.navident())).thenReturn(Optional.of(tilknyttetRessurs));
        saveRessurser(List.of(tilknyttetRessurs));

        var leggTilDagligOppfolgingOrgtilknytningWithTilknytningtypeRequest = leggTilOrgtilknytningRequest().toBuilder()
            .erDagligOppfolging(true)
            .sektor(tilknyttetRessurs.sektor())
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
                .sektor(leggTilDagligOppfolgingOrgtilknytningWithTilknytningtypeRequest.sektor())
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
        when(ressursStateRepository.findByNavident(tilknyttetRessurs.navident())).thenReturn(Optional.of(tilknyttetRessurs));
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
        var tilknyttetRessurs = ressursState().toBuilder()
            .sektor(Sektor.EKSTERN).build();
        when(ressursStateRepository.findByNavident(tilknyttetRessurs.navident())).thenReturn(Optional.of(tilknyttetRessurs));
        postOppdaterOrgtilknytningerForRessursRequest(
            tilknyttetRessurs.navident(),
            testToken.nomWriteGroupAndNomUiAzp(),
            oppdaterOrgtilknytningerForRessursRequest
        ).expectStatus().isBadRequest();
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("Should return 400 if request has overlapping daglige oppfolginger")
    @MethodSource("test_no_overlapping_daglig_oppfolginger_input")
    void test_no_overlapping_daglig_oppfolginger_ny(OppdaterOrgtilknytningerForRessursRequest oppdaterOrgtilknytningerForRessursRequest) {
        when(unleash.isEnabled("nom.ressurs.opprett_statlig")).thenReturn(true);
        when(orgenhetService.getEnheterByLeder("A321321")).thenReturn(List.of(orgEnhet()));
        when(ressursStateRepository.findByNavident("whatever")).thenReturn(Optional.of(ressursState().toBuilder().sektor(Sektor.EKSTERN).build()));
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

        var tilknyttetRessurs = ressursState().toBuilder()
            .sektor(Sektor.EKSTERN).build();
        when(ressursStateRepository.findByNavident(tilknyttetRessurs.navident())).thenReturn(Optional.of(tilknyttetRessurs));
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

        when(orgenhetService.getEnheterByLeder("A321321")).thenReturn(List.of(orgEnhet()));
        var tilknyttetRessurs = ressursState().toBuilder()
            .sektor(Sektor.EKSTERN).build();
        when(ressursStateRepository.findByNavident(tilknyttetRessurs.navident())).thenReturn(Optional.of(tilknyttetRessurs));
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
