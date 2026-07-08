package no.nav.nom.apps.api.orgtilknytning.api.app;

import no.nav.nom.apps.api.TestTokensGenerator;
import no.nav.nom.apps.api.authorize.AuthResolver;
import no.nav.nom.apps.api.authorize.redigereressurs.AuthorizedNavident;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.orgenhet.implementations.OrgEnhet;
import no.nav.nom.apps.api.orgenhetleder.OrgEnhetLederService;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.OrgtilknytningCommandService;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.EndreDagligOppfolgingOrgtilknytningCommand;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.EndreOrgtilknytningDatoCommand;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.EndreOrgtilknytningSektorCommand;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.EndreTilknytningtypeCommand;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.OpprettOrgtilknytning;
import no.nav.nom.apps.api.remedy.RemedyEnhetMapperService;
import no.nav.nom.apps.api.ressurs.FysiskBrukerstedService;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.RessursCommandService;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OppdaterSluttdatoCommand;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OppdaterStartdatoCommand;
import no.nav.nom.apps.api.ressurs.model.FysiskBrukersted;
import no.nav.nom.apps.api.rest.RestTestBase;
import no.nav.nom.apps.api.rest.common.StandardResponse;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.Tilknytningtype;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateRepository;
import no.nav.nom.libs.state.ressurs.NomStateRessursListener;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static java.util.Objects.nonNull;
import static no.nav.nom.apps.api.TestTokensGenerator.LOGGED_IN_USER_NAVIDENT;
import static no.nav.nom.apps.api.common.TestData.orgEnhet;
import static no.nav.nom.apps.api.common.TestData.orgtilknytningState;
import static no.nav.nom.apps.api.common.TestData.ressursState;
import static no.nav.nom.apps.api.orgtilknytning.api.app.OrgtilknytningRestService.BRUK_NY_TILGANGSVALIDERING;
import static no.nav.nom.contract.nom.IdentType.ROBOT;
import static no.nav.nom.contract.nom.IdentType.STANDARD;
import static no.nav.nom.contract.nom.IdentType.UTENLANDSK_KONSULENT;
import static no.nav.nom.contract.nom.IdentType.VIKAFOSSEN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class OrgtilknytningControllerTest extends RestTestBase {

    @MockitoBean
    OrgtilknytningCommandService orgtilknytningCommandService;

    @MockitoBean
    RessursCommandService ressursCommandService;

    @MockitoBean
    NomStateRessursListener nomStateRessursListener;

    @MockitoBean
    OrgenhetService orgenhetService;

    @MockitoBean
    OrgEnhetLederService orgEnhetLederService;

    @Autowired
    AuthResolver authResolver;

    // TODO: Probably need to inject this instead of mock it
    @MockitoBean
    AuthorizedNavident authorizedNavident;

    @MockitoBean
    RessursStateRepository ressursStateRepository;

    @MockitoBean
    RemedyEnhetMapperService remedyEnhetMapperService;

    @MockitoBean
    FysiskBrukerstedService fysiskBrukerstedService;

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

    @Captor
    ArgumentCaptor<EndreOrgtilknytningSektorCommand> endreOrgtilknytningSektorCommandArgumentCaptor;

    private AutoCloseable mocks;

    @BeforeEach
    void setInitialLoadCompleteAndEnableUnleashAndClearData() {
        when(nomStateRessursListener.hasReadToEndOnStartup()).thenReturn(true);
        when(unleash.isEnabled(any())).thenReturn(true);
        when(fysiskBrukerstedService.findFysiskBrukersted(any())).thenReturn(Optional.of(new FysiskBrukersted("2700", "Test Brukersted", "L1234", "Match alley 1", "1234", "Hellfire", "Tigerstad")));
        clearOrgtilknytninger();
        ressursStateRepository.clear();
        mocks = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (mocks != null) {
            mocks.close();
        }
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
    @DisplayName("Should update tilknytningtype for orgtilknytning - gammel tilgangsvalidering")
    void test_update_orgtilknytning_type_gammel_tilgangsvalidering() {
        when(unleash.isEnabled(BRUK_NY_TILGANGSVALIDERING)).thenReturn(false);

        var orgtilknytningToUpdate = orgtilknytningState();
        when(ressursStateRepository.findByNavident(orgtilknytningToUpdate.navident())).thenReturn(Optional.of(ressursState().toBuilder()
            .sektor(Sektor.EKSTERN)
            .build()));
        saveOrgtilknytninger(List.of(orgtilknytningToUpdate));

        when(authorizedNavident.kanBrukerRedigereRessurs(LOGGED_IN_USER_NAVIDENT, orgtilknytningToUpdate.navident())).thenReturn(true);

        saveRessurser(List.of(
            ressursState().toBuilder()
                .navident(orgtilknytningToUpdate.navident())
                .sektor(Sektor.EKSTERN)
                .build()
        ));

        var endreTilknytningtypeRequest = endreTilknytningtypeRequest().toBuilder()
            .tilknytningtype(Tilknytningtype.KONSULENT)
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
                Objects.equals(endreTilknytningtypeCommand.tilknytningtype(), endreTilknytningtypeRequest.tilknytningtype())
            );
    }

    @Test
    @DisplayName("Should update tilknytningtype for orgtilknytning - ny tilgangsvalidering")
    void test_update_orgtilknytning_type_ny_tilgangsvalidering() {
        var orgtilknytningToUpdate = orgtilknytningState();
        when(ressursStateRepository.findByNavident(orgtilknytningToUpdate.navident())).thenReturn(Optional.of(ressursState().toBuilder()
            .sektor(Sektor.EKSTERN)
            .build()));
        saveOrgtilknytninger(List.of(orgtilknytningToUpdate));

        when(authorizedNavident.kanBrukerRedigereRessurs(LOGGED_IN_USER_NAVIDENT, orgtilknytningToUpdate.navident())).thenReturn(true);

        saveRessurser(List.of(
            ressursState().toBuilder()
                .navident(orgtilknytningToUpdate.navident())
                .sektor(Sektor.EKSTERN)
                .build()
        ));

        var endreTilknytningtypeRequest = endreTilknytningtypeRequest().toBuilder()
            .tilknytningtype(Tilknytningtype.KONSULENT)
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
                Objects.equals(endreTilknytningtypeCommand.tilknytningtype(), endreTilknytningtypeRequest.tilknytningtype())
            );
    }

    @Test
    @DisplayName("Should block update tilknytningtype for orgtilknytning if tilknyttet ressurs is not EKSTERN or NAV_STATLIG - gammel tilgangsvalidering")
    void test_update_orgtilknytning_type_for_ekstern_only_gammel_tilgangsvalidering() {
        when(unleash.isEnabled(BRUK_NY_TILGANGSVALIDERING)).thenReturn(false);

        when(ressursStateRepository.findByNavident("X123456")).thenReturn(Optional.of(ressursState().toBuilder().sektor(Sektor.NAV_STATLIG).build()));
        var orgtilknytningToUpdate = orgtilknytningState();
        saveOrgtilknytninger(List.of(orgtilknytningToUpdate));

        when(authorizedNavident.kanBrukerRedigereRessurs(LOGGED_IN_USER_NAVIDENT, orgtilknytningToUpdate.navident())).thenReturn(true);

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
        ).expectStatus().isOk();
    }

    @Test
    @DisplayName(
        "Should allow update tilknytningtype for orgtilknytning if tilknyttet ressurs is UTENLANDSK_KONSULENT og har rett tilgang - ny tilgangsvalidering"
    )
    void test_allow_update_orgtilknytning_type_for_utenlandsk_konsulent_med_tilgang_ny_tilgangsvalidering() {
        when(authorizedNavident.kanBrukerRedigereRessurs(any(), any())).thenReturn(false); // Ikke leder tilgang for ressurs

        when(ressursStateRepository.findByNavident("X123456")).thenReturn(Optional.of(
            ressursState().toBuilder()
                .sektor(Sektor.NAV_STATLIG)
                .identType(UTENLANDSK_KONSULENT)
                .build()
        ));

        var orgtilknytningToUpdate = orgtilknytningState();
        saveOrgtilknytninger(List.of(orgtilknytningToUpdate));

        postEndreTilknytningtypeRequest(
            orgtilknytningToUpdate.fid(),
            testToken.nomWriteUtlKonsulentGroupAndNomUiAzp(),
            endreTilknytningtypeRequest()
        ).expectStatus().isOk();
    }

    @Test
    @DisplayName("Should block update tilknytningtype for orgtilknytning if tilknyttet ressurs is VIKAFOSSEN og ikke har rett tilgang - ny tilgangsvalidering")
    void test_block_update_orgtilknytning_type_for_vikafossen_uten_tilgang_ny_tilgangsvalidering() {
        when(authorizedNavident.kanBrukerRedigereRessurs(any(), any())).thenReturn(false);

        when(ressursStateRepository.findByNavident("X123456")).thenReturn(Optional.of(
            ressursState().toBuilder()
                .sektor(Sektor.NAV_STATLIG)
                .identType(VIKAFOSSEN)
                .build()
        ));

        var orgtilknytningToUpdate = orgtilknytningState();
        saveOrgtilknytninger(List.of(orgtilknytningToUpdate));

        postEndreTilknytningtypeRequest(
            orgtilknytningToUpdate.fid(),
            testToken.nomWriteGroupAndNomUiAzp(),
            endreTilknytningtypeRequest()
        ).expectStatus().isForbidden();
    }

    @Test
    @DisplayName("Should allow update tilknytningtype for orgtilknytning if tilknyttet ressurs is VIKAFOSSEN og har rett tilgang - ny tilgangsvalidering")
    void test_allow_update_orgtilknytning_type_for_vikafossen_med_tilgang_ny_tilgangsvalidering() {
        when(authorizedNavident.kanBrukerRedigereRessurs(any(), any())).thenReturn(false);

        when(ressursStateRepository.findByNavident("X123456")).thenReturn(Optional.of(
            ressursState().toBuilder()
                .sektor(Sektor.NAV_STATLIG)
                .identType(VIKAFOSSEN)
                .build()
        ));

        var orgtilknytningToUpdate = orgtilknytningState();
        saveOrgtilknytninger(List.of(orgtilknytningToUpdate));

        postEndreTilknytningtypeRequest(
            orgtilknytningToUpdate.fid(),
            testToken.nomWriteVikafossenGroupAndNomUiAzp(),
            endreTilknytningtypeRequest()
        ).expectStatus().isOk();
    }

    @Test
    @DisplayName("Should block update tilknytningtype for orgtilknytning if tilknyttet ressurs is ROBOT og ikke har rett tilgang - ny tilgangsvalidering")
    void test_block_update_orgtilknytning_type_for_robot_uten_tilgang_ny_tilgangsvalidering() {
        when(authorizedNavident.kanBrukerRedigereRessurs(any(), any())).thenReturn(false);

        when(ressursStateRepository.findByNavident("X123456")).thenReturn(Optional.of(
            ressursState().toBuilder()
                .sektor(Sektor.NAV_STATLIG)
                .identType(ROBOT)
                .build()
        ));

        var orgtilknytningToUpdate = orgtilknytningState();
        saveOrgtilknytninger(List.of(orgtilknytningToUpdate));

        postEndreTilknytningtypeRequest(
            orgtilknytningToUpdate.fid(),
            testToken.nomWriteGroupAndNomUiAzp(),
            endreTilknytningtypeRequest()
        ).expectStatus().isForbidden();
    }

    @Test
    @DisplayName("Should allow update tilknytningtype for orgtilknytning if tilknyttet ressurs is ROBOT og har rett tilgang - ny tilgangsvalidering")
    void test_allow_update_orgtilknytning_type_for_robot_med_tilgang_ny_tilgangsvalidering() {
        when(authorizedNavident.kanBrukerRedigereRessurs(any(), any())).thenReturn(true);

        when(ressursStateRepository.findByNavident("X123456")).thenReturn(Optional.of(
            ressursState().toBuilder()
                .sektor(Sektor.NAV_STATLIG)
                .identType(ROBOT)
                .build()
        ));

        var orgtilknytningToUpdate = orgtilknytningState();
        saveOrgtilknytninger(List.of(orgtilknytningToUpdate));

        postEndreTilknytningtypeRequest(
            orgtilknytningToUpdate.fid(),
            testToken.nomWriteRobotGroupAndNomUiAzp(),
            endreTilknytningtypeRequest()
        ).expectStatus().isOk();
    }

    @Test
    @DisplayName("Should forbid update orgtilknytning type if user user has 'MUTATE_RESSURS' but is not authorized for ressurs - gammel tilgangsvalidering")
    void test_update_orgtilknytning_type_forbidden_gammel_tilgangsvalidering() {
        when(unleash.isEnabled(BRUK_NY_TILGANGSVALIDERING)).thenReturn(false);

        when(authorizedNavident.kanBrukerRedigereRessurs(any(), any())).thenReturn(false);

        var orgtilknytningToUpdate = orgtilknytningState();
        saveOrgtilknytninger(List.of(orgtilknytningToUpdate));

        when(ressursStateRepository.findByNavident("X123456")).thenReturn(Optional.of(
            ressursState().toBuilder()
                .sektor(Sektor.NAV_STATLIG)
                .identType(STANDARD)
                .build()
        ));

        postEndreTilknytningtypeRequest(orgtilknytningToUpdate.fid(), testToken.nomWriteGroupAndNomUiAzp(), endreTilknytningtypeRequest())
            .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("Should forbid update orgtilknytning type if user user has 'MUTATE_RESSURS' but is not authorized for ressurs - ny tilgangsvalidering")
    void test_update_orgtilknytning_type_forbidden_ny_tilgangsvalidering() {
        when(authorizedNavident.kanBrukerRedigereRessurs(any(), any())).thenReturn(false);

        var orgtilknytningToUpdate = orgtilknytningState();
        saveOrgtilknytninger(List.of(orgtilknytningToUpdate));

        when(ressursStateRepository.findByNavident("X123456")).thenReturn(Optional.of(
            ressursState().toBuilder()
                .sektor(Sektor.NAV_STATLIG)
                .identType(STANDARD)
                .build()
        ));

        postEndreTilknytningtypeRequest(orgtilknytningToUpdate.fid(), testToken.nomWriteGroupAndNomUiAzp(), endreTilknytningtypeRequest())
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
        when(authorizedNavident.kanBrukerRedigereRessurs(any(), any())).thenReturn(true);

        var tilknyttetRessurs = ressursState().toBuilder()
            .sektor(Sektor.EKSTERN).build();
        when(ressursStateRepository.findByNavident(tilknyttetRessurs.navident())).thenReturn(Optional.of(tilknyttetRessurs));
        saveRessurser(List.of(tilknyttetRessurs));

        var existingOrgtilknytning = orgtilknytningState().toBuilder()
            .navident(tilknyttetRessurs.navident())
            .gyldigFom(LocalDate.now())
            .gyldigTom(LocalDate.now().plusMonths(1))
            .build();

        saveOrgtilknytninger(List.of(existingOrgtilknytning));

        var newStartdato = existingOrgtilknytning.gyldigFom().plusMonths(1);
        LocalDate newSluttdato = null;

        var endreOrgtilknytningRequest = endreOrgtilknytningRequest(existingOrgtilknytning).toBuilder()
            .gyldigFom(
                newStartdato
            )
            .gyldigTom(newSluttdato)
            .primaryFysiskBrukerstedNummer("2700")
            .optionalFysiskBrukersteder(Set.of("1000", "2000"))
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
        when(authorizedNavident.kanBrukerRedigereRessurs(any(), any())).thenReturn(true);

        when(orgEnhetLederService.getEnheterByLeder("A321321")).thenReturn(List.of(orgEnhet()));
        var tilknyttetRessurs = ressursState().toBuilder()
            .sektor(Sektor.EKSTERN).build();

        when(ressursStateRepository.findByNavident(tilknyttetRessurs.navident())).thenReturn(Optional.of(tilknyttetRessurs));
        saveRessurser(List.of(tilknyttetRessurs));

        var existingOrgtilknytning = orgtilknytningState().toBuilder()
            .navident(tilknyttetRessurs.navident())
            .gyldigFom(LocalDate.now())
            .gyldigTom(LocalDate.now().plusMonths(1))
            .build();

        saveOrgtilknytninger(List.of(existingOrgtilknytning));

        var newStartdato = existingOrgtilknytning.gyldigFom().plusMonths(1);
        LocalDate newSluttdato = null;

        var endreOrgtilknytningRequest = endreOrgtilknytningRequest(existingOrgtilknytning).toBuilder()
            .gyldigFom(
                newStartdato
            )
            .gyldigTom(newSluttdato)
            .primaryFysiskBrukerstedNummer("2700")
            .optionalFysiskBrukersteder(Set.of("1000", "2000"))
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
    @DisplayName("POST /app/orgtilknytninger/forRessurs/{navident} stop user from adding more than five optional fysiskbrukersted")
    void test_update_orgtilknytninger_for_ressurs_handle_over_fem_optionalFysisk_brukersted() {
        when(authorizedNavident.kanBrukerRedigereRessurs(any(), any())).thenReturn(true);

        when(orgEnhetLederService.getEnheterByLeder("X987654")).thenReturn(List.of(orgEnhet()));
        var tilknyttetRessurs = ressursState().toBuilder()
            .sektor(Sektor.EKSTERN).build();

        when(ressursStateRepository.findByNavident(tilknyttetRessurs.navident())).thenReturn(Optional.of(tilknyttetRessurs));
        saveRessurser(List.of(tilknyttetRessurs));

        var existingOrgtilknytning = orgtilknytningState().toBuilder()
            .navident(tilknyttetRessurs.navident())
            .gyldigFom(LocalDate.now())
            .gyldigTom(LocalDate.now().plusMonths(1))
            .build();

        saveOrgtilknytninger(List.of(existingOrgtilknytning));

        var newStartdato = existingOrgtilknytning.gyldigFom().plusMonths(1);
        LocalDate newSluttdato = null;

        var endreOrgtilknytningRequest = endreOrgtilknytningRequest(existingOrgtilknytning).toBuilder()
            .gyldigFom(
                newStartdato
            )
            .gyldigTom(newSluttdato)
            .primaryFysiskBrukerstedNummer("2700")
            .optionalFysiskBrukersteder(Set.of("1000", "2000", "3000", "4000", "5000", "6000"))
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
            .expectStatus().isBadRequest()
            .expectBody(StandardResponse.class).isEqualTo(StandardResponse.builder()
                .message("Listen for fysiske brukersteder kan ikke være mer enn fem elementer")
                .build());
    }

    @Test
    @DisplayName("POST /app/orgtilknytninger/forRessurs/{navident} stop user from adding primary brukersted as optional too")
    void test_update_orgtilknytninger_for_ressurs_handle_ikke_kunne_ha_primaer_fysisk_brukersted_i_optional_liste() {
        when(authorizedNavident.kanBrukerRedigereRessurs(any(), any())).thenReturn(true);

        when(orgEnhetLederService.getEnheterByLeder("X987654")).thenReturn(List.of(orgEnhet()));
        var tilknyttetRessurs = ressursState().toBuilder()
            .sektor(Sektor.EKSTERN).build();

        when(ressursStateRepository.findByNavident(tilknyttetRessurs.navident())).thenReturn(Optional.of(tilknyttetRessurs));
        saveRessurser(List.of(tilknyttetRessurs));

        var existingOrgtilknytning = orgtilknytningState().toBuilder()
            .navident(tilknyttetRessurs.navident())
            .gyldigFom(LocalDate.now())
            .gyldigTom(LocalDate.now().plusMonths(1))
            .build();

        saveOrgtilknytninger(List.of(existingOrgtilknytning));

        var newStartdato = existingOrgtilknytning.gyldigFom().plusMonths(1);
        LocalDate newSluttdato = null;

        var endreOrgtilknytningRequest = endreOrgtilknytningRequest(existingOrgtilknytning).toBuilder()
            .gyldigFom(
                newStartdato
            )
            .gyldigTom(newSluttdato)
            .primaryFysiskBrukerstedNummer("2700")
            .optionalFysiskBrukersteder(Set.of("1000", "2700"))
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
            .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("POST /app/orgtilknytninger/forRessurs/{navident} kan ikke redigere orgtilknytning som er over 6 maaneder gammel for ressurs")
    void test_rediger_orgtilknytning_som_er_over_seks_mnd_gammel() {
        when(authorizedNavident.kanBrukerRedigereRessurs(any(), any())).thenReturn(true);

        OrgtilknytningStateRepository orgtilknytningStateRepository = Mockito.mock(OrgtilknytningStateRepository.class);

        when(orgEnhetLederService.getEnheterByLeder("A321321")).thenReturn(List.of(orgEnhet()));
        var tilknyttetRessurs = ressursState().toBuilder()
            .sektor(Sektor.EKSTERN).build();

        when(ressursStateRepository.findByNavident(tilknyttetRessurs.navident())).thenReturn(Optional.of(tilknyttetRessurs));
        saveRessurser(List.of(tilknyttetRessurs));

        var existingOrgtilknytning = orgtilknytningState().toBuilder()
            .navident(tilknyttetRessurs.navident())
            .gyldigFom(LocalDate.now())
            .gyldigTom(LocalDate.now().minusMonths(6).minusDays(1))
            .build();


        saveOrgtilknytninger(List.of(existingOrgtilknytning));
        when(orgtilknytningStateRepository.findByFid(existingOrgtilknytning.fid())).thenReturn(Optional.of(existingOrgtilknytning));

        var newStartdato = existingOrgtilknytning.gyldigFom().minusMonths(24);
        LocalDate newSluttdato = nonNull(existingOrgtilknytning.gyldigTom()) ? existingOrgtilknytning.gyldigTom().minusMonths(3).minusDays(1) : null;

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
    @DisplayName("POST /app/orgtilknytninger/forRessurs/{navident} kan redigere orgtilknytning som er under 2 maaneder gammel for ressurs")
    void test_rediger_orgtilknytning_som_er_under_seks_mnd_gammel() {
        when(authorizedNavident.kanBrukerRedigereRessurs(any(), any())).thenReturn(true);

        OrgtilknytningStateRepository orgtilknytningStateRepository = Mockito.mock(OrgtilknytningStateRepository.class);

        when(orgEnhetLederService.getEnheterByLeder("A321321")).thenReturn(List.of(orgEnhet()));
        var tilknyttetRessurs = ressursState().toBuilder()
            .sektor(Sektor.NAV_STATLIG).build();

        when(ressursStateRepository.findByNavident(tilknyttetRessurs.navident())).thenReturn(Optional.of(tilknyttetRessurs));
        saveRessurser(List.of(tilknyttetRessurs));

        var existingOrgtilknytning = orgtilknytningState().toBuilder()
            .navident(tilknyttetRessurs.navident())
            .gyldigFom(LocalDate.now())
            .gyldigTom(LocalDate.now().minusMonths(1))
            .build();

        saveOrgtilknytninger(List.of(existingOrgtilknytning));
        when(orgtilknytningStateRepository.findByFid(existingOrgtilknytning.fid())).thenReturn(Optional.of(existingOrgtilknytning));

        var endreOrgtilknytningRequest = endreOrgtilknytningRequest(existingOrgtilknytning).toBuilder()
            .gyldigTom(null)
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
    @DisplayName("POST /app/orgtilknytninger/forRessurs/{navident} kan ikke redigere orgtilknytning du ikke er leder for - gammel tilgangsvalidering")
    void test_rediger_orgtilknytning_som_ikke_er_leder_for_gammel_tilgangsvalidering() {
        when(authorizedNavident.kanBrukerRedigereRessurs(any(), any())).thenReturn(false);
        when(unleash.isEnabled(BRUK_NY_TILGANGSVALIDERING)).thenReturn(false);

        OrgtilknytningStateRepository orgtilknytningStateRepository = Mockito.mock(OrgtilknytningStateRepository.class);
        when(authorizedNavident.orgenheterLederKanRedigere(any())).thenReturn(List.of(orgEnhet("orgEnhet321")));
        when(orgEnhetLederService.getEnheterByLeder("A321321")).thenReturn(List.of(orgEnhet()));
        var tilknyttetRessurs = ressursState().toBuilder()
            .sektor(Sektor.EKSTERN).build();

        when(ressursStateRepository.findByNavident(tilknyttetRessurs.navident())).thenReturn(Optional.of(tilknyttetRessurs));
        saveRessurser(List.of(tilknyttetRessurs));

        var existingOrgtilknytning = orgtilknytningState().toBuilder()
            .navident(tilknyttetRessurs.navident())
            .gyldigFom(LocalDate.now())
            .gyldigTom(LocalDate.now().minusDays(1))
            .build();


        saveOrgtilknytninger(List.of(existingOrgtilknytning));
        when(orgtilknytningStateRepository.findByFid(existingOrgtilknytning.fid())).thenReturn(Optional.of(existingOrgtilknytning));

        var newStartdato = existingOrgtilknytning.gyldigFom().minusMonths(24);
        LocalDate newSluttdato = nonNull(existingOrgtilknytning.gyldigTom()) ? existingOrgtilknytning.gyldigTom().minusMonths(3).minusDays(1) : null;

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
    @DisplayName("POST /app/orgtilknytninger/forRessurs/{navident} kan ikke redigere orgtilknytning du ikke er leder for  - ny tilgangsvalidering")
    void test_rediger_orgtilknytning_som_ikke_er_leder_for_ny_tilgangsvalidering() {
        OrgtilknytningStateRepository orgtilknytningStateRepository = Mockito.mock(OrgtilknytningStateRepository.class);
        when(authorizedNavident.kanBrukerRedigereRessurs(any(), any())).thenReturn(false);

        when(authorizedNavident.orgenheterLederKanRedigere(any())).thenReturn(List.of(orgEnhet("orgEnhet321")));
        when(orgEnhetLederService.getEnheterByLeder("A321321")).thenReturn(List.of(orgEnhet()));
        var tilknyttetRessurs = ressursState().toBuilder()
            .sektor(Sektor.EKSTERN).build();

        when(ressursStateRepository.findByNavident(tilknyttetRessurs.navident())).thenReturn(Optional.of(tilknyttetRessurs));
        saveRessurser(List.of(tilknyttetRessurs));

        var existingOrgtilknytning = orgtilknytningState().toBuilder()
            .navident(tilknyttetRessurs.navident())
            .gyldigFom(LocalDate.now())
            .gyldigTom(LocalDate.now().minusDays(1))
            .build();


        saveOrgtilknytninger(List.of(existingOrgtilknytning));
        when(orgtilknytningStateRepository.findByFid(existingOrgtilknytning.fid())).thenReturn(Optional.of(existingOrgtilknytning));

        var newStartdato = existingOrgtilknytning.gyldigFom().minusMonths(24);
        LocalDate newSluttdato = nonNull(existingOrgtilknytning.gyldigTom()) ? existingOrgtilknytning.gyldigTom().minusMonths(3).minusDays(1) : null;

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
    @DisplayName("POST /app/orgtilknytninger/forRessurs/{navident} kan ikke redigere orgtilknytning du ikke er leder for på ROBOT - ny tilgangsvalidering")
    void test_rediger_orgtilknytning_som_ikke_er_leder_for_paa_robot_ny_tilgangsvalidering() {
        OrgtilknytningStateRepository orgtilknytningStateRepository = Mockito.mock(OrgtilknytningStateRepository.class);
        when(authorizedNavident.kanBrukerRedigereRessurs(any(), any())).thenReturn(false);

        when(authorizedNavident.orgenheterLederKanRedigere(any())).thenReturn(List.of(orgEnhet("orgEnhet321")));
        when(orgEnhetLederService.getEnheterByLeder("A321321")).thenReturn(List.of(orgEnhet()));
        var tilknyttetRessurs = ressursState().toBuilder()
            .sektor(Sektor.EKSTERN)
            .identType(ROBOT)
            .build();

        when(ressursStateRepository.findByNavident(tilknyttetRessurs.navident())).thenReturn(Optional.of(tilknyttetRessurs));
        saveRessurser(List.of(tilknyttetRessurs));

        var existingOrgtilknytning = orgtilknytningState().toBuilder()
            .navident(tilknyttetRessurs.navident())
            .gyldigFom(LocalDate.now())
            .gyldigTom(LocalDate.now().minusDays(1))
            .build();


        saveOrgtilknytninger(List.of(existingOrgtilknytning));
        when(orgtilknytningStateRepository.findByFid(existingOrgtilknytning.fid())).thenReturn(Optional.of(existingOrgtilknytning));

        var newStartdato = existingOrgtilknytning.gyldigFom().minusMonths(24);
        LocalDate newSluttdato = nonNull(existingOrgtilknytning.gyldigTom()) ? existingOrgtilknytning.gyldigTom().minusMonths(3).minusDays(1) : null;

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
    @DisplayName(
        "POST /app/orgtilknytninger/forRessurs/{navident} kan redigere orgtilknytning du ikke er leder for på ROBOT med robot admin - ny tilgangsvalidering"
    )
    void test_rediger_orgtilknytning_som_ikke_er_leder_for_paa_robot_ny_som_robot_admin_tilgangsvalidering() {
        OrgtilknytningStateRepository orgtilknytningStateRepository = Mockito.mock(OrgtilknytningStateRepository.class);
        when(authorizedNavident.kanBrukerRedigereRessurs(any(), any())).thenReturn(false);

        when(authorizedNavident.orgenheterLederKanRedigere(any())).thenReturn(List.of(orgEnhet("Annen enhet")));
        when(orgEnhetLederService.getEnheterByLeder("A321321")).thenReturn(List.of(orgEnhet()));
        var tilknyttetRessurs = ressursState().toBuilder()
            .sektor(Sektor.EKSTERN)
            .identType(ROBOT)
            .build();

        when(ressursStateRepository.findByNavident(tilknyttetRessurs.navident())).thenReturn(Optional.of(tilknyttetRessurs));
        saveRessurser(List.of(tilknyttetRessurs));

        var existingOrgtilknytning = orgtilknytningState().toBuilder()
            .navident(tilknyttetRessurs.navident())
            .gyldigFom(LocalDate.now())
            .gyldigTom(LocalDate.now().minusDays(1))
            .build();

        saveOrgtilknytninger(List.of(existingOrgtilknytning));
        when(orgtilknytningStateRepository.findByFid(existingOrgtilknytning.fid())).thenReturn(Optional.of(existingOrgtilknytning));

        var newStartdato = existingOrgtilknytning.gyldigFom().minusMonths(24);
        LocalDate newSluttdato = nonNull(existingOrgtilknytning.gyldigTom()) ? existingOrgtilknytning.gyldigTom().minusMonths(3).minusDays(1) : null;

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
            testToken.nomWriteRobotGroupAndNomUiAzp(),
            oppdaterOrgtilknytningerForRessursRequest
        )
            .expectStatus().isOk();
    }

    @Test
    @DisplayName(
        "POST /app/orgtilknytninger/forRessurs/{navident} kan redigere orgtilknytning du ikke er leder for på ROBOT med NOM admin - ny tilgangsvalidering"
    )
    void test_rediger_orgtilknytning_som_ikke_er_leder_for_paa_robot_ny_som_nom_admin_tilgangsvalidering() {
        OrgtilknytningStateRepository orgtilknytningStateRepository = Mockito.mock(OrgtilknytningStateRepository.class);
        when(authorizedNavident.kanBrukerRedigereRessurs(any(), any())).thenReturn(false);

        when(authorizedNavident.orgenheterLederKanRedigere(any())).thenReturn(List.of(orgEnhet("Annen enhet")));
        when(orgEnhetLederService.getEnheterByLeder("A321321")).thenReturn(List.of(orgEnhet()));
        var tilknyttetRessurs = ressursState().toBuilder()
            .sektor(Sektor.EKSTERN)
            .identType(ROBOT)
            .build();

        when(ressursStateRepository.findByNavident(tilknyttetRessurs.navident())).thenReturn(Optional.of(tilknyttetRessurs));
        saveRessurser(List.of(tilknyttetRessurs));

        var existingOrgtilknytning = orgtilknytningState().toBuilder()
            .navident(tilknyttetRessurs.navident())
            .gyldigFom(LocalDate.now())
            .gyldigTom(LocalDate.now().minusDays(1))
            .build();

        saveOrgtilknytninger(List.of(existingOrgtilknytning));
        when(orgtilknytningStateRepository.findByFid(existingOrgtilknytning.fid())).thenReturn(Optional.of(existingOrgtilknytning));

        var newStartdato = existingOrgtilknytning.gyldigFom().minusMonths(24);
        LocalDate newSluttdato = nonNull(existingOrgtilknytning.gyldigTom()) ? existingOrgtilknytning.gyldigTom().minusMonths(3).minusDays(1) : null;

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
            testToken.nomAdminUser(),
            oppdaterOrgtilknytningerForRessursRequest
        )
            .expectStatus().isOk();
    }

    @Test
    @DisplayName(
        "POST /app/orgtilknytninger/forRessurs/{navident} kan redigere orgtilknytning du ikke er leder for på VIKAFOSSEN med VIKAFOSSEN admin - ny tilgangsvalidering"
    )
    void test_rediger_orgtilknytning_som_ikke_er_leder_for_paa_vikafossen_ny_som_vikafossen_admin_tilgangsvalidering() {
        OrgtilknytningStateRepository orgtilknytningStateRepository = Mockito.mock(OrgtilknytningStateRepository.class);
        when(authorizedNavident.kanBrukerRedigereRessurs(any(), any())).thenReturn(false);

        when(authorizedNavident.orgenheterLederKanRedigere(any())).thenReturn(List.of(orgEnhet("Annen enhet")));
        when(orgEnhetLederService.getEnheterByLeder("A321321")).thenReturn(List.of(orgEnhet()));
        var tilknyttetRessurs = ressursState().toBuilder()
            .sektor(Sektor.EKSTERN)
            .identType(VIKAFOSSEN)
            .build();

        when(ressursStateRepository.findByNavident(tilknyttetRessurs.navident())).thenReturn(Optional.of(tilknyttetRessurs));
        saveRessurser(List.of(tilknyttetRessurs));

        var existingOrgtilknytning = orgtilknytningState().toBuilder()
            .navident(tilknyttetRessurs.navident())
            .gyldigFom(LocalDate.now())
            .gyldigTom(LocalDate.now().minusDays(1))
            .build();

        saveOrgtilknytninger(List.of(existingOrgtilknytning));
        when(orgtilknytningStateRepository.findByFid(existingOrgtilknytning.fid())).thenReturn(Optional.of(existingOrgtilknytning));

        var newStartdato = existingOrgtilknytning.gyldigFom().minusMonths(24);
        LocalDate newSluttdato = nonNull(existingOrgtilknytning.gyldigTom()) ? existingOrgtilknytning.gyldigTom().minusMonths(3).minusDays(1) : null;

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
            testToken.nomWriteVikafossenGroupAndNomUiAzp(),
            oppdaterOrgtilknytningerForRessursRequest
        )
            .expectStatus().isOk();
    }

    @Test
    @DisplayName(
        "POST /app/orgtilknytninger/forRessurs/{navident} kan ikke legge til orgtilknytning hvis du ikke er leder for gammel eller ny OT - gammel tilgangsvalidering"
    )
    void test_legg_til_orgtilknytning_som_ikke_er_leder_for_gammel_tilgangsvalidering() {
        when(unleash.isEnabled(BRUK_NY_TILGANGSVALIDERING)).thenReturn(false);

        OrgtilknytningStateRepository orgtilknytningStateRepository = Mockito.mock(OrgtilknytningStateRepository.class);
        when(authorizedNavident.kanBrukerRedigereRessurs(any(), any())).thenReturn(false);

        when(authorizedNavident.orgenheterLederKanRedigere(any())).thenReturn(List.of(orgEnhet("orgEnhet321")));
        when(orgEnhetLederService.getEnheterByLeder("A321321")).thenReturn(List.of(orgEnhet()));
        var tilknyttetRessurs = ressursState().toBuilder()
            .sektor(Sektor.EKSTERN).build();

        when(ressursStateRepository.findByNavident(tilknyttetRessurs.navident())).thenReturn(Optional.of(tilknyttetRessurs));
        saveRessurser(List.of(tilknyttetRessurs));

        var existingOrgtilknytning = orgtilknytningState().toBuilder()
            .navident(tilknyttetRessurs.navident())
            .gyldigFom(LocalDate.now())
            .gyldigTom(LocalDate.now().plusYears(1))
            .erDagligOppfolging(true)
            .build();

        saveOrgtilknytninger(List.of(existingOrgtilknytning));
        when(orgtilknytningStateRepository.findByFid(existingOrgtilknytning.fid())).thenReturn(Optional.of(existingOrgtilknytning));

        var endreOrgtilknytningRequest = endreOrgtilknytningRequest(existingOrgtilknytning);
        var nyOrgtilknytningRequest = leggTilOrgtilknytningRequest().toBuilder()
            .orgEnhetId("orgEnhet456")
            .sektor(Sektor.NAV_STATLIG)
            .gyldigFom(LocalDate.now().plusYears(1).plusDays(1))
            .erDagligOppfolging(true)
            .gyldigTom(null)
            .tilknytningtype(Tilknytningtype.FAST)
            .build();

        var oppdaterOrgtilknytningerForRessursRequest = oppdaterOrgtilknytningerForRessursRequest().toBuilder()
            .eksisterendeOrgtilknytninger(List.of(endreOrgtilknytningRequest))
            .nyeOrgtilknytninger(List.of(nyOrgtilknytningRequest))
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
    @DisplayName(
        "POST /app/orgtilknytninger/forRessurs/{navident} kan ikke legge til orgtilknytning hvis du ikke er leder for gammel eller ny OT - ny tilgangsvalidering"
    )
    void test_legg_til_orgtilknytning_som_ikke_er_leder_for_ny_tilgangsvalidering() {
        OrgtilknytningStateRepository orgtilknytningStateRepository = Mockito.mock(OrgtilknytningStateRepository.class);
        when(authorizedNavident.kanBrukerRedigereRessurs(any(), any())).thenReturn(false);

        when(authorizedNavident.orgenheterLederKanRedigere(any())).thenReturn(List.of(orgEnhet("orgEnhet321")));
        when(orgEnhetLederService.getEnheterByLeder("A321321")).thenReturn(List.of(orgEnhet()));
        var tilknyttetRessurs = ressursState().toBuilder()
            .sektor(Sektor.EKSTERN).build();

        when(ressursStateRepository.findByNavident(tilknyttetRessurs.navident())).thenReturn(Optional.of(tilknyttetRessurs));
        saveRessurser(List.of(tilknyttetRessurs));

        var existingOrgtilknytning = orgtilknytningState().toBuilder()
            .navident(tilknyttetRessurs.navident())
            .gyldigFom(LocalDate.now())
            .gyldigTom(LocalDate.now().plusYears(1))
            .erDagligOppfolging(true)
            .build();

        saveOrgtilknytninger(List.of(existingOrgtilknytning));
        when(orgtilknytningStateRepository.findByFid(existingOrgtilknytning.fid())).thenReturn(Optional.of(existingOrgtilknytning));

        var endreOrgtilknytningRequest = endreOrgtilknytningRequest(existingOrgtilknytning);
        var nyOrgtilknytningRequest = leggTilOrgtilknytningRequest().toBuilder()
            .orgEnhetId("orgEnhet456")
            .sektor(Sektor.NAV_STATLIG)
            .gyldigFom(LocalDate.now().plusYears(1).plusDays(1))
            .erDagligOppfolging(true)
            .gyldigTom(null)
            .tilknytningtype(Tilknytningtype.FAST)
            .build();

        var oppdaterOrgtilknytningerForRessursRequest = oppdaterOrgtilknytningerForRessursRequest().toBuilder()
            .eksisterendeOrgtilknytninger(List.of(endreOrgtilknytningRequest))
            .nyeOrgtilknytninger(List.of(nyOrgtilknytningRequest))
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
    @DisplayName(
        "POST /app/orgtilknytninger/forRessurs/{navident} kan legge til orgtilknytning hvis du ikke er leder for gammel eller ny OT som NOM ADMIN - ny tilgangsvalidering"
    )
    void test_legg_til_orgtilknytning_som_ikke_er_leder_for__som_nom_admin_ny_tilgangsvalidering() {
        OrgtilknytningStateRepository orgtilknytningStateRepository = Mockito.mock(OrgtilknytningStateRepository.class);
        when(authorizedNavident.kanBrukerRedigereRessurs(any(), any())).thenReturn(false);

        when(authorizedNavident.orgenheterLederKanRedigere(any())).thenReturn(List.of(orgEnhet("orgEnhet321")));
        when(orgEnhetLederService.getEnheterByLeder("A321321")).thenReturn(List.of(orgEnhet()));
        var tilknyttetRessurs = ressursState().toBuilder()
            .sektor(Sektor.EKSTERN)
            .build();

        when(ressursStateRepository.findByNavident(tilknyttetRessurs.navident())).thenReturn(Optional.of(tilknyttetRessurs));
        saveRessurser(List.of(tilknyttetRessurs));

        var existingOrgtilknytning = orgtilknytningState().toBuilder()
            .navident(tilknyttetRessurs.navident())
            .gyldigFom(LocalDate.now())
            .gyldigTom(LocalDate.now().plusYears(1))
            .erDagligOppfolging(true)
            .build();

        saveOrgtilknytninger(List.of(existingOrgtilknytning));
        when(orgtilknytningStateRepository.findByFid(existingOrgtilknytning.fid())).thenReturn(Optional.of(existingOrgtilknytning));

        var endreOrgtilknytningRequest = endreOrgtilknytningRequest(existingOrgtilknytning);
        var nyOrgtilknytningRequest = leggTilOrgtilknytningRequest().toBuilder()
            .orgEnhetId("orgEnhet456")
            .sektor(Sektor.NAV_STATLIG)
            .gyldigFom(LocalDate.now().plusYears(1).plusDays(1))
            .erDagligOppfolging(true)
            .gyldigTom(null)
            .tilknytningtype(Tilknytningtype.FAST)
            .build();

        var oppdaterOrgtilknytningerForRessursRequest = oppdaterOrgtilknytningerForRessursRequest().toBuilder()
            .eksisterendeOrgtilknytninger(List.of(endreOrgtilknytningRequest))
            .nyeOrgtilknytninger(List.of(nyOrgtilknytningRequest))
            .setRessursStartdato(true)
            .setRessursSluttdato(true)
            .build();

        postOppdaterOrgtilknytningerForRessursRequest(
            tilknyttetRessurs.navident(),
            testToken.nomAdminUser(),
            oppdaterOrgtilknytningerForRessursRequest
        )
            .expectStatus().isOk();
    }

    @Test
    @DisplayName(
        "POST /app/orgtilknytninger/forRessurs/{navident} kan legge til orgtilknytning på UTENLANDSK_KONSULENT hvis du ikke er leder for gammel eller ny OT som NOM ADMIN - ny tilgangsvalidering"
    )
    void test_legg_til_orgtilknytning_paa_utenlandsk_konsulent_som_ikke_er_leder_for_som_nom_admin_ny_tilgangsvalidering() {
        OrgtilknytningStateRepository orgtilknytningStateRepository = Mockito.mock(OrgtilknytningStateRepository.class);
        when(authorizedNavident.kanBrukerRedigereRessurs(any(), any())).thenReturn(false);

        when(authorizedNavident.orgenheterLederKanRedigere(any())).thenReturn(List.of(orgEnhet("orgEnhet321")));
        when(orgEnhetLederService.getEnheterByLeder("A321321")).thenReturn(List.of(orgEnhet()));
        var tilknyttetRessurs = ressursState().toBuilder()
            .sektor(Sektor.EKSTERN)
            .identType(UTENLANDSK_KONSULENT)
            .build();

        when(ressursStateRepository.findByNavident(tilknyttetRessurs.navident())).thenReturn(Optional.of(tilknyttetRessurs));
        saveRessurser(List.of(tilknyttetRessurs));

        var existingOrgtilknytning = orgtilknytningState().toBuilder()
            .navident(tilknyttetRessurs.navident())
            .gyldigFom(LocalDate.now())
            .gyldigTom(LocalDate.now().plusYears(1))
            .erDagligOppfolging(true)
            .build();

        saveOrgtilknytninger(List.of(existingOrgtilknytning));
        when(orgtilknytningStateRepository.findByFid(existingOrgtilknytning.fid())).thenReturn(Optional.of(existingOrgtilknytning));

        var endreOrgtilknytningRequest = endreOrgtilknytningRequest(existingOrgtilknytning);
        var nyOrgtilknytningRequest = leggTilOrgtilknytningRequest().toBuilder()
            .orgEnhetId("orgEnhet456")
            .sektor(Sektor.NAV_STATLIG)
            .gyldigFom(LocalDate.now().plusYears(1).plusDays(1))
            .erDagligOppfolging(true)
            .gyldigTom(null)
            .tilknytningtype(Tilknytningtype.FAST)
            .build();

        var oppdaterOrgtilknytningerForRessursRequest = oppdaterOrgtilknytningerForRessursRequest().toBuilder()
            .eksisterendeOrgtilknytninger(List.of(endreOrgtilknytningRequest))
            .nyeOrgtilknytninger(List.of(nyOrgtilknytningRequest))
            .setRessursStartdato(true)
            .setRessursSluttdato(true)
            .build();

        postOppdaterOrgtilknytningerForRessursRequest(
            tilknyttetRessurs.navident(),
            testToken.nomAdminUser(),
            oppdaterOrgtilknytningerForRessursRequest
        )
            .expectStatus().isOk();
    }

    @Test
    @DisplayName(
        "POST /app/orgtilknytninger/forRessurs/{navident} kan ikke legge til orgtilknytning på VIKAFOSSEN hvis du er leder for gammel eller ny OT - ny tilgangsvalidering"
    )
    void test_legg_til_orgtilknytning_paa_vikafossen_som_ikke_leder_for_ny_tilgangsvalidering() {
        OrgtilknytningStateRepository orgtilknytningStateRepository = Mockito.mock(OrgtilknytningStateRepository.class);
        when(authorizedNavident.kanBrukerRedigereRessurs(any(), any())).thenReturn(true);

        when(authorizedNavident.orgenheterLederKanRedigere(any())).thenReturn(List.of(orgEnhet("orgEnhet321")));
        when(orgEnhetLederService.getEnheterByLeder("A321321")).thenReturn(List.of(orgEnhet()));
        var tilknyttetRessurs = ressursState().toBuilder()
            .sektor(Sektor.EKSTERN)
            .identType(VIKAFOSSEN)
            .build();

        when(ressursStateRepository.findByNavident(tilknyttetRessurs.navident())).thenReturn(Optional.of(tilknyttetRessurs));
        saveRessurser(List.of(tilknyttetRessurs));

        var existingOrgtilknytning = orgtilknytningState().toBuilder()
            .navident(tilknyttetRessurs.navident())
            .gyldigFom(LocalDate.now())
            .gyldigTom(LocalDate.now().plusYears(1))
            .erDagligOppfolging(true)
            .build();

        saveOrgtilknytninger(List.of(existingOrgtilknytning));
        when(orgtilknytningStateRepository.findByFid(existingOrgtilknytning.fid())).thenReturn(Optional.of(existingOrgtilknytning));

        var endreOrgtilknytningRequest = endreOrgtilknytningRequest(existingOrgtilknytning);
        var nyOrgtilknytningRequest = leggTilOrgtilknytningRequest().toBuilder()
            .orgEnhetId("orgEnhet456")
            .sektor(Sektor.NAV_STATLIG)
            .gyldigFom(LocalDate.now().plusYears(1).plusDays(1))
            .erDagligOppfolging(true)
            .gyldigTom(null)
            .tilknytningtype(Tilknytningtype.FAST)
            .build();

        var oppdaterOrgtilknytningerForRessursRequest = oppdaterOrgtilknytningerForRessursRequest().toBuilder()
            .eksisterendeOrgtilknytninger(List.of(endreOrgtilknytningRequest))
            .nyeOrgtilknytninger(List.of(nyOrgtilknytningRequest))
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
    @DisplayName("POST /app/orgtilknytninger/forRessurs/{navident} kan legge til orgtilknytning på VIKAFOSSEN som VIKAFOSSEN admin - ny tilgangsvalidering")
    void test_legg_til_orgtilknytning_paa_vikafossen_som_vikafossen_admin_ny_tilgangsvalidering() {
        OrgtilknytningStateRepository orgtilknytningStateRepository = Mockito.mock(OrgtilknytningStateRepository.class);
        when(authorizedNavident.kanBrukerRedigereRessurs(any(), any())).thenReturn(false);

        when(authorizedNavident.orgenheterLederKanRedigere(any())).thenReturn(List.of(orgEnhet("orgEnhet321")));
        when(orgEnhetLederService.getEnheterByLeder("A321321")).thenReturn(List.of(orgEnhet()));
        var tilknyttetRessurs = ressursState().toBuilder()
            .sektor(Sektor.EKSTERN)
            .identType(VIKAFOSSEN)
            .build();

        when(ressursStateRepository.findByNavident(tilknyttetRessurs.navident())).thenReturn(Optional.of(tilknyttetRessurs));
        saveRessurser(List.of(tilknyttetRessurs));

        var existingOrgtilknytning = orgtilknytningState().toBuilder()
            .navident(tilknyttetRessurs.navident())
            .gyldigFom(LocalDate.now())
            .gyldigTom(LocalDate.now().plusYears(1))
            .erDagligOppfolging(true)
            .build();

        saveOrgtilknytninger(List.of(existingOrgtilknytning));
        when(orgtilknytningStateRepository.findByFid(existingOrgtilknytning.fid())).thenReturn(Optional.of(existingOrgtilknytning));

        var endreOrgtilknytningRequest = endreOrgtilknytningRequest(existingOrgtilknytning);
        var nyOrgtilknytningRequest = leggTilOrgtilknytningRequest().toBuilder()
            .orgEnhetId("dummyVikafossenId")
            .sektor(Sektor.NAV_STATLIG)
            .gyldigFom(LocalDate.now().plusYears(1).plusDays(1))
            .erDagligOppfolging(true)
            .gyldigTom(null)
            .tilknytningtype(Tilknytningtype.FAST)
            .build();

        var oppdaterOrgtilknytningerForRessursRequest = oppdaterOrgtilknytningerForRessursRequest().toBuilder()
            .eksisterendeOrgtilknytninger(List.of(endreOrgtilknytningRequest))
            .nyeOrgtilknytninger(List.of(nyOrgtilknytningRequest))
            .setRessursStartdato(true)
            .setRessursSluttdato(true)
            .build();

        postOppdaterOrgtilknytningerForRessursRequest(
            tilknyttetRessurs.navident(),
            testToken.nomWriteVikafossenGroupAndNomUiAzp(),
            oppdaterOrgtilknytningerForRessursRequest
        )
            .expectStatus().isOk();
    }

    @Test
    @DisplayName("POST /app/orgtilknytninger/forRessurs/{navident} avvis legge til orgtilknytning på VIKAFOSSEN som VIKAFOSSEN admin - på feil enhet")
    void test_avvis_legg_til_orgtilknytning_paa_vikafossen_som_vikafossen_admin_paa_feil_enhet() {
        OrgtilknytningStateRepository orgtilknytningStateRepository = Mockito.mock(OrgtilknytningStateRepository.class);
        when(authorizedNavident.kanBrukerRedigereRessurs(any(), any())).thenReturn(false);

        when(authorizedNavident.orgenheterLederKanRedigere(any())).thenReturn(List.of(orgEnhet("orgEnhet321")));
        when(orgEnhetLederService.getEnheterByLeder("A321321")).thenReturn(List.of(orgEnhet()));
        var tilknyttetRessurs = ressursState().toBuilder()
            .sektor(Sektor.EKSTERN)
            .identType(VIKAFOSSEN)
            .build();

        when(ressursStateRepository.findByNavident(tilknyttetRessurs.navident())).thenReturn(Optional.of(tilknyttetRessurs));
        saveRessurser(List.of(tilknyttetRessurs));

        var existingOrgtilknytning = orgtilknytningState().toBuilder()
            .navident(tilknyttetRessurs.navident())
            .gyldigFom(LocalDate.now())
            .gyldigTom(LocalDate.now().plusYears(1))
            .erDagligOppfolging(true)
            .build();

        saveOrgtilknytninger(List.of(existingOrgtilknytning));
        when(orgtilknytningStateRepository.findByFid(existingOrgtilknytning.fid())).thenReturn(Optional.of(existingOrgtilknytning));

        var endreOrgtilknytningRequest = endreOrgtilknytningRequest(existingOrgtilknytning);
        var nyOrgtilknytningRequest = leggTilOrgtilknytningRequest().toBuilder()
            .orgEnhetId("ikke_vikafossen")
            .sektor(Sektor.NAV_STATLIG)
            .gyldigFom(LocalDate.now().plusYears(1).plusDays(1))
            .erDagligOppfolging(true)
            .gyldigTom(null)
            .tilknytningtype(Tilknytningtype.FAST)
            .build();

        var oppdaterOrgtilknytningerForRessursRequest = oppdaterOrgtilknytningerForRessursRequest().toBuilder()
            .eksisterendeOrgtilknytninger(List.of(endreOrgtilknytningRequest))
            .nyeOrgtilknytninger(List.of(nyOrgtilknytningRequest))
            .setRessursStartdato(true)
            .setRessursSluttdato(true)
            .build();

        postOppdaterOrgtilknytningerForRessursRequest(
            tilknyttetRessurs.navident(),
            testToken.nomWriteVikafossenGroupAndNomUiAzp(),
            oppdaterOrgtilknytningerForRessursRequest
        )
            .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("POST /app/orgtilknytninger/forRessurs/{navident} kan legge til orgtilknytning hvis du er leder for gammel OT - gammel tilgangsvalidering")
    void test_legg_til_orgtilknytning_leder_for_eksisterende_gammel_tilgangsvalidering() {
        when(unleash.isEnabled(BRUK_NY_TILGANGSVALIDERING)).thenReturn(false);

        OrgtilknytningStateRepository orgtilknytningStateRepository = Mockito.mock(OrgtilknytningStateRepository.class);
        when(authorizedNavident.kanBrukerRedigereRessurs(any(), any())).thenReturn(true);

        when(orgEnhetLederService.getEnheterByLeder("A321321")).thenReturn(List.of(orgEnhet()));
        var tilknyttetRessurs = ressursState().toBuilder()
            .sektor(Sektor.EKSTERN).build();

        when(ressursStateRepository.findByNavident(tilknyttetRessurs.navident())).thenReturn(Optional.of(tilknyttetRessurs));
        saveRessurser(List.of(tilknyttetRessurs));

        var existingOrgtilknytning = orgtilknytningState().toBuilder()
            .navident(tilknyttetRessurs.navident())
            .gyldigFom(LocalDate.now())
            .gyldigTom(LocalDate.now().plusYears(1))
            .erDagligOppfolging(true)
            .build();

        saveOrgtilknytninger(List.of(existingOrgtilknytning));
        when(orgtilknytningStateRepository.findByFid(existingOrgtilknytning.fid())).thenReturn(Optional.of(existingOrgtilknytning));

        var endreOrgtilknytningRequest = endreOrgtilknytningRequest(existingOrgtilknytning);
        var nyOrgtilknytningRequest = leggTilOrgtilknytningRequest().toBuilder()
            .orgEnhetId("orgEnhet456")
            .sektor(Sektor.NAV_STATLIG)
            .gyldigFom(LocalDate.now().plusYears(1).plusDays(1))
            .erDagligOppfolging(true)
            .gyldigTom(null)
            .tilknytningtype(Tilknytningtype.FAST)
            .build();

        var oppdaterOrgtilknytningerForRessursRequest = oppdaterOrgtilknytningerForRessursRequest().toBuilder()
            .eksisterendeOrgtilknytninger(List.of(endreOrgtilknytningRequest))
            .nyeOrgtilknytninger(List.of(nyOrgtilknytningRequest))
            .setRessursStartdato(true)
            .setRessursSluttdato(true)
            .build();

        postOppdaterOrgtilknytningerForRessursRequest(
            tilknyttetRessurs.navident(),
            testToken.nomWriteGroupAndNomUiAzp(),
            oppdaterOrgtilknytningerForRessursRequest
        )
            .expectStatus().isOk();
    }

    @Test
    @DisplayName("POST /app/orgtilknytninger/forRessurs/{navident} kan legge til orgtilknytning hvis du er leder for gammel OT - ny tilgangsvalidering")
    void test_legg_til_orgtilknytning_leder_for_eksisterende_ny_tilgangsvalidering() {
        OrgtilknytningStateRepository orgtilknytningStateRepository = Mockito.mock(OrgtilknytningStateRepository.class);
        when(authorizedNavident.kanBrukerRedigereRessurs(any(), any())).thenReturn(true);

        when(orgEnhetLederService.getEnheterByLeder("A321321")).thenReturn(List.of(orgEnhet()));
        var tilknyttetRessurs = ressursState().toBuilder()
            .sektor(Sektor.EKSTERN).build();

        when(ressursStateRepository.findByNavident(tilknyttetRessurs.navident())).thenReturn(Optional.of(tilknyttetRessurs));
        saveRessurser(List.of(tilknyttetRessurs));

        var existingOrgtilknytning = orgtilknytningState().toBuilder()
            .navident(tilknyttetRessurs.navident())
            .gyldigFom(LocalDate.now())
            .gyldigTom(LocalDate.now().plusYears(1))
            .erDagligOppfolging(true)
            .build();

        saveOrgtilknytninger(List.of(existingOrgtilknytning));
        when(orgtilknytningStateRepository.findByFid(existingOrgtilknytning.fid())).thenReturn(Optional.of(existingOrgtilknytning));

        var endreOrgtilknytningRequest = endreOrgtilknytningRequest(existingOrgtilknytning);
        var nyOrgtilknytningRequest = leggTilOrgtilknytningRequest().toBuilder()
            .orgEnhetId("orgEnhet456")
            .sektor(Sektor.NAV_STATLIG)
            .gyldigFom(LocalDate.now().plusYears(1).plusDays(1))
            .erDagligOppfolging(true)
            .gyldigTom(null)
            .tilknytningtype(Tilknytningtype.FAST)
            .build();

        var oppdaterOrgtilknytningerForRessursRequest = oppdaterOrgtilknytningerForRessursRequest().toBuilder()
            .eksisterendeOrgtilknytninger(List.of(endreOrgtilknytningRequest))
            .nyeOrgtilknytninger(List.of(nyOrgtilknytningRequest))
            .setRessursStartdato(true)
            .setRessursSluttdato(true)
            .build();

        postOppdaterOrgtilknytningerForRessursRequest(
            tilknyttetRessurs.navident(),
            testToken.nomWriteGroupAndNomUiAzp(),
            oppdaterOrgtilknytningerForRessursRequest
        )
            .expectStatus().isOk();
    }

    @Test
    @DisplayName(
        "POST /app/orgtilknytninger/forRessurs/{navident} kan legge til orgtilknytning for ROBOT hvis du er leder for gammel OT - ny tilgangsvalidering"
    )
    void test_legg_til_orgtilknytning_for_robot_leder_for_eksisterende_ny_tilgangsvalidering() {
        OrgtilknytningStateRepository orgtilknytningStateRepository = Mockito.mock(OrgtilknytningStateRepository.class);
        when(authorizedNavident.kanBrukerRedigereRessurs(any(), any())).thenReturn(true);

        when(orgEnhetLederService.getEnheterByLeder("A321321")).thenReturn(List.of(orgEnhet()));
        var tilknyttetRessurs = ressursState().toBuilder()
            .sektor(Sektor.EKSTERN)
            .identType(ROBOT)
            .build();

        when(ressursStateRepository.findByNavident(tilknyttetRessurs.navident())).thenReturn(Optional.of(tilknyttetRessurs));
        saveRessurser(List.of(tilknyttetRessurs));

        var existingOrgtilknytning = orgtilknytningState().toBuilder()
            .navident(tilknyttetRessurs.navident())
            .gyldigFom(LocalDate.now())
            .gyldigTom(LocalDate.now().plusYears(1))
            .erDagligOppfolging(true)
            .build();

        saveOrgtilknytninger(List.of(existingOrgtilknytning));
        when(orgtilknytningStateRepository.findByFid(existingOrgtilknytning.fid())).thenReturn(Optional.of(existingOrgtilknytning));

        var endreOrgtilknytningRequest = endreOrgtilknytningRequest(existingOrgtilknytning);
        var nyOrgtilknytningRequest = leggTilOrgtilknytningRequest().toBuilder()
            .orgEnhetId("orgEnhet456")
            .sektor(Sektor.NAV_STATLIG)
            .gyldigFom(LocalDate.now().plusYears(1).plusDays(1))
            .erDagligOppfolging(true)
            .gyldigTom(null)
            .tilknytningtype(Tilknytningtype.FAST)
            .build();

        var oppdaterOrgtilknytningerForRessursRequest = oppdaterOrgtilknytningerForRessursRequest().toBuilder()
            .eksisterendeOrgtilknytninger(List.of(endreOrgtilknytningRequest))
            .nyeOrgtilknytninger(List.of(nyOrgtilknytningRequest))
            .setRessursStartdato(true)
            .setRessursSluttdato(true)
            .build();

        postOppdaterOrgtilknytningerForRessursRequest(
            tilknyttetRessurs.navident(),
            testToken.nomWriteGroupAndNomUiAzp(),
            oppdaterOrgtilknytningerForRessursRequest
        )
            .expectStatus().isOk();
    }

    @Test
    @DisplayName("POST /app/orgtilknytninger/forRessurs/{navident} kan legge til orgtilknytning for ROBOT hvis du robot admin - ny tilgangsvalidering")
    void test_legg_til_orgtilknytning_for_robot_som_robot_admin_eksisterende_ny_tilgangsvalidering() {
        OrgtilknytningStateRepository orgtilknytningStateRepository = Mockito.mock(OrgtilknytningStateRepository.class);
        when(authorizedNavident.kanBrukerRedigereRessurs(any(), any())).thenReturn(false);

        when(orgEnhetLederService.getEnheterByLeder("A321321")).thenReturn(List.of(orgEnhet()));
        var tilknyttetRessurs = ressursState().toBuilder()
            .sektor(Sektor.EKSTERN)
            .identType(ROBOT)
            .build();

        when(ressursStateRepository.findByNavident(tilknyttetRessurs.navident())).thenReturn(Optional.of(tilknyttetRessurs));
        saveRessurser(List.of(tilknyttetRessurs));

        var existingOrgtilknytning = orgtilknytningState().toBuilder()
            .navident(tilknyttetRessurs.navident())
            .gyldigFom(LocalDate.now())
            .gyldigTom(LocalDate.now().plusYears(1))
            .erDagligOppfolging(true)
            .build();

        saveOrgtilknytninger(List.of(existingOrgtilknytning));
        when(orgtilknytningStateRepository.findByFid(existingOrgtilknytning.fid())).thenReturn(Optional.of(existingOrgtilknytning));

        var endreOrgtilknytningRequest = endreOrgtilknytningRequest(existingOrgtilknytning);
        var nyOrgtilknytningRequest = leggTilOrgtilknytningRequest().toBuilder()
            .orgEnhetId("orgEnhet456")
            .sektor(Sektor.NAV_STATLIG)
            .gyldigFom(LocalDate.now().plusYears(1).plusDays(1))
            .erDagligOppfolging(true)
            .gyldigTom(null)
            .tilknytningtype(Tilknytningtype.FAST)
            .build();

        var oppdaterOrgtilknytningerForRessursRequest = oppdaterOrgtilknytningerForRessursRequest().toBuilder()
            .eksisterendeOrgtilknytninger(List.of(endreOrgtilknytningRequest))
            .nyeOrgtilknytninger(List.of(nyOrgtilknytningRequest))
            .setRessursStartdato(true)
            .setRessursSluttdato(true)
            .build();

        postOppdaterOrgtilknytningerForRessursRequest(
            tilknyttetRessurs.navident(),
            testToken.nomWriteRobotGroupAndNomUiAzp(),
            oppdaterOrgtilknytningerForRessursRequest
        )
            .expectStatus().isOk();
    }

    @Test
    @DisplayName(
        "POST /app/orgtilknytninger/forRessurs/{navident} kan ikke legge til orgtilknytning for VIKAFOSSEN hvis du er leder for gammel OT - ny tilgangsvalidering"
    )
    void test_legg_til_orgtilknytning_for_vikafossen_leder_for_eksisterende_ny_tilgangsvalidering() {
        OrgtilknytningStateRepository orgtilknytningStateRepository = Mockito.mock(OrgtilknytningStateRepository.class);
        when(authorizedNavident.kanBrukerRedigereRessurs(any(), any())).thenReturn(true);

        when(orgEnhetLederService.getEnheterByLeder("A321321")).thenReturn(List.of(orgEnhet()));
        var tilknyttetRessurs = ressursState().toBuilder()
            .sektor(Sektor.EKSTERN)
            .identType(VIKAFOSSEN)
            .build();

        when(ressursStateRepository.findByNavident(tilknyttetRessurs.navident())).thenReturn(Optional.of(tilknyttetRessurs));
        saveRessurser(List.of(tilknyttetRessurs));

        var existingOrgtilknytning = orgtilknytningState().toBuilder()
            .navident(tilknyttetRessurs.navident())
            .gyldigFom(LocalDate.now())
            .gyldigTom(LocalDate.now().plusYears(1))
            .erDagligOppfolging(true)
            .build();

        saveOrgtilknytninger(List.of(existingOrgtilknytning));
        when(orgtilknytningStateRepository.findByFid(existingOrgtilknytning.fid())).thenReturn(Optional.of(existingOrgtilknytning));

        var endreOrgtilknytningRequest = endreOrgtilknytningRequest(existingOrgtilknytning);
        var nyOrgtilknytningRequest = leggTilOrgtilknytningRequest().toBuilder()
            .orgEnhetId("orgEnhet456")
            .sektor(Sektor.NAV_STATLIG)
            .gyldigFom(LocalDate.now().plusYears(1).plusDays(1))
            .erDagligOppfolging(true)
            .gyldigTom(null)
            .tilknytningtype(Tilknytningtype.FAST)
            .build();

        var oppdaterOrgtilknytningerForRessursRequest = oppdaterOrgtilknytningerForRessursRequest().toBuilder()
            .eksisterendeOrgtilknytninger(List.of(endreOrgtilknytningRequest))
            .nyeOrgtilknytninger(List.of(nyOrgtilknytningRequest))
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
    @DisplayName("POST /app/orgtilknytninger/forRessurs/{navident} kan legge til orgtilknytning for VIKAFOSSEN hvis du vikafossen admin - ny tilgangsvalidering")
    void test_legg_til_orgtilknytning_for_vikafossen_som_vikafossen_admin_eksisterende_ny_tilgangsvalidering() {
        OrgtilknytningStateRepository orgtilknytningStateRepository = Mockito.mock(OrgtilknytningStateRepository.class);
        when(authorizedNavident.kanBrukerRedigereRessurs(any(), any())).thenReturn(false);

        when(orgEnhetLederService.getEnheterByLeder("A321321")).thenReturn(List.of(orgEnhet()));
        var tilknyttetRessurs = ressursState().toBuilder()
            .sektor(Sektor.EKSTERN)
            .identType(VIKAFOSSEN)
            .build();

        when(ressursStateRepository.findByNavident(tilknyttetRessurs.navident())).thenReturn(Optional.of(tilknyttetRessurs));
        saveRessurser(List.of(tilknyttetRessurs));

        var existingOrgtilknytning = orgtilknytningState().toBuilder()
            .navident(tilknyttetRessurs.navident())
            .gyldigFom(LocalDate.now())
            .gyldigTom(LocalDate.now().plusYears(1))
            .erDagligOppfolging(true)
            .build();

        saveOrgtilknytninger(List.of(existingOrgtilknytning));
        when(orgtilknytningStateRepository.findByFid(existingOrgtilknytning.fid())).thenReturn(Optional.of(existingOrgtilknytning));

        var endreOrgtilknytningRequest = endreOrgtilknytningRequest(existingOrgtilknytning);
        var nyOrgtilknytningRequest = leggTilOrgtilknytningRequest().toBuilder()
            .orgEnhetId("dummyVikafossenId")
            .sektor(Sektor.NAV_STATLIG)
            .gyldigFom(LocalDate.now().plusYears(1).plusDays(1))
            .erDagligOppfolging(true)
            .gyldigTom(null)
            .tilknytningtype(Tilknytningtype.FAST)
            .build();

        var oppdaterOrgtilknytningerForRessursRequest = oppdaterOrgtilknytningerForRessursRequest().toBuilder()
            .eksisterendeOrgtilknytninger(List.of(endreOrgtilknytningRequest))
            .nyeOrgtilknytninger(List.of(nyOrgtilknytningRequest))
            .setRessursStartdato(true)
            .setRessursSluttdato(true)
            .build();

        postOppdaterOrgtilknytningerForRessursRequest(
            tilknyttetRessurs.navident(),
            testToken.nomWriteVikafossenGroupAndNomUiAzp(),
            oppdaterOrgtilknytningerForRessursRequest
        )
            .expectStatus().isOk();
    }

    @Test
    @DisplayName("POST /app/orgtilknytninger/forRessurs/{navident} avvis legge til orgtilknytning for VIKAFOSSEN hvis du er vikafossen admin - på feil enhet")
    void test_avvis_legg_til_orgtilknytning_for_vikafossen_som_vikafossen_admin_paa_feil_enhet() {
        OrgtilknytningStateRepository orgtilknytningStateRepository = Mockito.mock(OrgtilknytningStateRepository.class);
        when(authorizedNavident.kanBrukerRedigereRessurs(any(), any())).thenReturn(false);

        when(orgEnhetLederService.getEnheterByLeder("A321321")).thenReturn(List.of(orgEnhet()));
        var tilknyttetRessurs = ressursState().toBuilder()
            .sektor(Sektor.EKSTERN)
            .identType(VIKAFOSSEN)
            .build();

        when(ressursStateRepository.findByNavident(tilknyttetRessurs.navident())).thenReturn(Optional.of(tilknyttetRessurs));
        saveRessurser(List.of(tilknyttetRessurs));

        var existingOrgtilknytning = orgtilknytningState().toBuilder()
            .navident(tilknyttetRessurs.navident())
            .gyldigFom(LocalDate.now())
            .gyldigTom(LocalDate.now().plusYears(1))
            .erDagligOppfolging(true)
            .build();

        saveOrgtilknytninger(List.of(existingOrgtilknytning));
        when(orgtilknytningStateRepository.findByFid(existingOrgtilknytning.fid())).thenReturn(Optional.of(existingOrgtilknytning));

        var endreOrgtilknytningRequest = endreOrgtilknytningRequest(existingOrgtilknytning);
        var nyOrgtilknytningRequest = leggTilOrgtilknytningRequest().toBuilder()
            .orgEnhetId("ikke_vikafossen")
            .sektor(Sektor.NAV_STATLIG)
            .gyldigFom(LocalDate.now().plusYears(1).plusDays(1))
            .erDagligOppfolging(true)
            .gyldigTom(null)
            .tilknytningtype(Tilknytningtype.FAST)
            .build();

        var oppdaterOrgtilknytningerForRessursRequest = oppdaterOrgtilknytningerForRessursRequest().toBuilder()
            .eksisterendeOrgtilknytninger(List.of(endreOrgtilknytningRequest))
            .nyeOrgtilknytninger(List.of(nyOrgtilknytningRequest))
            .setRessursStartdato(true)
            .setRessursSluttdato(true)
            .build();

        postOppdaterOrgtilknytningerForRessursRequest(
            tilknyttetRessurs.navident(),
            testToken.nomWriteVikafossenGroupAndNomUiAzp(),
            oppdaterOrgtilknytningerForRessursRequest
        )
            .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("POST /app/orgtilknytninger/forRessurs/{navident} avvise legge til orgtilknytning for VIKAFOSSEN hvis du er nom admin - ny tilgangsvalidering")
    void test_legg_til_orgtilknytning_for_vikafossen_som_nom_admin_eksisterende_ny_tilgangsvalidering() {
        OrgtilknytningStateRepository orgtilknytningStateRepository = Mockito.mock(OrgtilknytningStateRepository.class);
        when(authorizedNavident.kanBrukerRedigereRessurs(any(), any())).thenReturn(false);

        when(orgEnhetLederService.getEnheterByLeder("A321321")).thenReturn(List.of(orgEnhet()));
        var tilknyttetRessurs = ressursState().toBuilder()
            .sektor(Sektor.EKSTERN)
            .identType(VIKAFOSSEN)
            .build();

        when(ressursStateRepository.findByNavident(tilknyttetRessurs.navident())).thenReturn(Optional.of(tilknyttetRessurs));
        saveRessurser(List.of(tilknyttetRessurs));

        var existingOrgtilknytning = orgtilknytningState().toBuilder()
            .navident(tilknyttetRessurs.navident())
            .gyldigFom(LocalDate.now())
            .gyldigTom(LocalDate.now().plusYears(1))
            .erDagligOppfolging(true)
            .build();

        saveOrgtilknytninger(List.of(existingOrgtilknytning));
        when(orgtilknytningStateRepository.findByFid(existingOrgtilknytning.fid())).thenReturn(Optional.of(existingOrgtilknytning));

        var endreOrgtilknytningRequest = endreOrgtilknytningRequest(existingOrgtilknytning);
        var nyOrgtilknytningRequest = leggTilOrgtilknytningRequest().toBuilder()
            .orgEnhetId("dummyVikafossenId")
            .sektor(Sektor.NAV_STATLIG)
            .gyldigFom(LocalDate.now().plusYears(1).plusDays(1))
            .erDagligOppfolging(true)
            .gyldigTom(null)
            .tilknytningtype(Tilknytningtype.FAST)
            .build();

        var oppdaterOrgtilknytningerForRessursRequest = oppdaterOrgtilknytningerForRessursRequest().toBuilder()
            .eksisterendeOrgtilknytninger(List.of(endreOrgtilknytningRequest))
            .nyeOrgtilknytninger(List.of(nyOrgtilknytningRequest))
            .setRessursStartdato(true)
            .setRessursSluttdato(true)
            .build();

        postOppdaterOrgtilknytningerForRessursRequest(
            tilknyttetRessurs.navident(),
            testToken.nomAdminUser(),
            oppdaterOrgtilknytningerForRessursRequest
        )
            .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("POST /app/orgtilknytninger/forRessurs/{navident} avvis at admin får lov til å endre på vikafossen ansatte")
    void test_avvis_legg_til_orgtilknytning_for_vikafossen_som_nom_admin_eksisterende_paa_feil_enhet() {
        OrgtilknytningStateRepository orgtilknytningStateRepository = Mockito.mock(OrgtilknytningStateRepository.class);
        when(authorizedNavident.kanBrukerRedigereRessurs(any(), any())).thenReturn(false);

        when(orgEnhetLederService.getEnheterByLeder("A321321")).thenReturn(List.of(orgEnhet()));
        var tilknyttetRessurs = ressursState().toBuilder()
            .sektor(Sektor.EKSTERN)
            .identType(VIKAFOSSEN)
            .build();

        when(ressursStateRepository.findByNavident(tilknyttetRessurs.navident())).thenReturn(Optional.of(tilknyttetRessurs));
        saveRessurser(List.of(tilknyttetRessurs));

        var existingOrgtilknytning = orgtilknytningState().toBuilder()
            .navident(tilknyttetRessurs.navident())
            .gyldigFom(LocalDate.now())
            .gyldigTom(LocalDate.now().plusYears(1))
            .erDagligOppfolging(true)
            .build();

        saveOrgtilknytninger(List.of(existingOrgtilknytning));
        when(orgtilknytningStateRepository.findByFid(existingOrgtilknytning.fid())).thenReturn(Optional.of(existingOrgtilknytning));

        var endreOrgtilknytningRequest = endreOrgtilknytningRequest(existingOrgtilknytning);
        var nyOrgtilknytningRequest = leggTilOrgtilknytningRequest().toBuilder()
            .orgEnhetId("ikke_vikafossen_enhet")
            .sektor(Sektor.NAV_STATLIG)
            .gyldigFom(LocalDate.now().plusYears(1).plusDays(1))
            .erDagligOppfolging(true)
            .gyldigTom(null)
            .tilknytningtype(Tilknytningtype.FAST)
            .build();

        var oppdaterOrgtilknytningerForRessursRequest = oppdaterOrgtilknytningerForRessursRequest().toBuilder()
            .eksisterendeOrgtilknytninger(List.of(endreOrgtilknytningRequest))
            .nyeOrgtilknytninger(List.of(nyOrgtilknytningRequest))
            .setRessursStartdato(true)
            .setRessursSluttdato(true)
            .build();

        postOppdaterOrgtilknytningerForRessursRequest(
            tilknyttetRessurs.navident(),
            testToken.nomAdminUser(),
            oppdaterOrgtilknytningerForRessursRequest
        )
            .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("POST /app/orgtilknytninger/forRessurs/{navident} support updating startdato and sluttdato for ressurs")
    void test_update_orgtilknytninger_for_ressurs_start_and_sluttdato() {
        when(authorizedNavident.kanBrukerRedigereRessurs(any(), any())).thenReturn(true);

        var tilknyttetRessurs = ressursState().toBuilder()
            .sektor(Sektor.EKSTERN).build();
        when(ressursStateRepository.findByNavident(tilknyttetRessurs.navident())).thenReturn(Optional.of(tilknyttetRessurs));
        saveRessurser(List.of(tilknyttetRessurs));

        var existingOrgtilknytning = orgtilknytningState().toBuilder()
            .navident(tilknyttetRessurs.navident())
            .gyldigFom(LocalDate.now())
            .gyldigTom(LocalDate.now().plusMonths(1))
            .build();

        saveOrgtilknytninger(List.of(existingOrgtilknytning));

        var newStartdato = existingOrgtilknytning.gyldigFom().plusMonths(1);
        var newSluttdato = existingOrgtilknytning.gyldigTom().plusMonths(1);

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
            .matches(oppdaterSluttdatoCommand -> oppdaterSluttdatoCommand.sluttdato().equals(newSluttdato))
            .matches(oppdaterSluttdatoCommand -> oppdaterSluttdatoCommand.fid().equals(tilknyttetRessurs.fid()));

    }

    @Test
    @DisplayName("POST /app/orgtilknytninger/forRessurs/{navident} support updating startdato and sluttdato for ressurs")
    void test_update_orgtilknytninger_for_ressurs_start_and_sluttdato_ny() {
        when(authorizedNavident.kanBrukerRedigereRessurs(any(), any())).thenReturn(true);

        when(orgEnhetLederService.getEnheterByLeder("A321321")).thenReturn(List.of(orgEnhet()));
        var tilknyttetRessurs = ressursState().toBuilder()
            .sektor(Sektor.EKSTERN).build();
        when(ressursStateRepository.findByNavident(tilknyttetRessurs.navident())).thenReturn(Optional.of(tilknyttetRessurs));
        saveRessurser(List.of(tilknyttetRessurs));

        var existingOrgtilknytning = orgtilknytningState().toBuilder()
            .navident(tilknyttetRessurs.navident())
            .gyldigFom(LocalDate.now())
            .gyldigTom(LocalDate.now().plusMonths(1))
            .build();

        saveOrgtilknytninger(List.of(existingOrgtilknytning));

        var newStartdato = existingOrgtilknytning.gyldigFom().plusMonths(1);
        var newSluttdato = existingOrgtilknytning.gyldigTom().plusMonths(1);

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
            .matches(oppdaterSluttdatoCommand -> oppdaterSluttdatoCommand.sluttdato().equals(newSluttdato))
            .matches(oppdaterSluttdatoCommand -> oppdaterSluttdatoCommand.fid().equals(tilknyttetRessurs.fid()));
    }

    @Test
    @DisplayName("POST /app/orgtilknytninger/forRessurs/{navident} should update and add new orgtilknytninger for ressurs")
    void test_update_orgtilknytninger_for_ressurs() {
        when(authorizedNavident.kanBrukerRedigereRessurs(any(), any())).thenReturn(true);

        var tilknyttetRessurs = ressursState().toBuilder()
            .sektor(Sektor.EKSTERN).build();
        when(ressursStateRepository.findByNavident(tilknyttetRessurs.navident())).thenReturn(Optional.of(tilknyttetRessurs));
        saveRessurser(List.of(tilknyttetRessurs));

        var existingOrgtilknytningAnticipatingNewDate = orgtilknytningState().toBuilder()
            .navident(tilknyttetRessurs.navident())
            .orgEnhetId("1")
            .sektor(tilknyttetRessurs.sektor())
            .orgtilknytningType(Tilknytningtype.KONSULENT)
            .erDagligOppfolging(false)
            .gyldigTom(LocalDate.now().plusMonths(1))
            .primaryFysiskBrukerstedNummer("2700")
            .additionalFysiskeBrukerstederNummer(Collections.emptyList())
            .build();

        var existingOrgtilknytningAnticipatingDolChange = orgtilknytningState().toBuilder()
            .navident(tilknyttetRessurs.navident())
            .orgEnhetId("2")
            .sektor(tilknyttetRessurs.sektor())
            .orgtilknytningType(Tilknytningtype.KONSULENT)
            .erDagligOppfolging(false)
            .gyldigFom(existingOrgtilknytningAnticipatingNewDate.gyldigTom().plusDays(1))
            .gyldigTom(existingOrgtilknytningAnticipatingNewDate.gyldigTom().plusMonths(1))
            .primaryFysiskBrukerstedNummer("2700")
            .additionalFysiskeBrukerstederNummer(Collections.emptyList())
            .build();

        saveOrgtilknytninger(List.of(
            existingOrgtilknytningAnticipatingNewDate,
            existingOrgtilknytningAnticipatingDolChange
        ));

        var endreDatoOrgtilknytningRequest = endreOrgtilknytningRequest(existingOrgtilknytningAnticipatingNewDate).toBuilder()
            .gyldigTom(
                existingOrgtilknytningAnticipatingNewDate.gyldigTom().plusMonths(1)
            )
            .build();

        var endreDolOrgtilknytningRequest = endreOrgtilknytningRequest(existingOrgtilknytningAnticipatingDolChange).toBuilder()
            .erDagligOppfolging(true)
            .build();

        var leggTilOrgtilknytningRequest = leggTilOrgtilknytningRequest().toBuilder()
            .erDagligOppfolging(false)
            .orgEnhetId("4")
            .sektor(tilknyttetRessurs.sektor())
            .tilknytningtype(Tilknytningtype.KONSULENT)
            .primaryFysiskBrukerstedNummer("2700")
            .optionalFysiskBrukersteder(Collections.emptySet())
            .build();

        var opprettetOrgtilknytningFid = UUID.randomUUID().toString();

        when(orgtilknytningCommandService.opprettOrgtilknytning(any())).thenReturn(opprettetOrgtilknytningFid);

        var oppdaterOrgtilknytningerForRessursRequest = oppdaterOrgtilknytningerForRessursRequest().toBuilder()
            .eksisterendeOrgtilknytninger(List.of(
                endreDatoOrgtilknytningRequest,
                endreDolOrgtilknytningRequest
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
                .gyldigTom(endreDolOrgtilknytningRequest.gyldigTom())
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
                .fid(opprettetOrgtilknytningFid)
                .tilknytningtype(leggTilOrgtilknytningRequest.tilknytningtype())
                .build()
            );

        verify(orgtilknytningCommandService, times(2)).endreOrgtilknytningDato(endreOrgtilknytningDatoCommandArgumentCaptor.capture());
        assertThat(endreOrgtilknytningDatoCommandArgumentCaptor.getAllValues()).usingRecursiveComparison()
            .ignoringFields("inputMetadata")
            .isEqualTo(List.of(
                EndreOrgtilknytningDatoCommand.builder()
                    .fid(endreDatoOrgtilknytningRequest.fid())
                    .gyldigFom(endreDatoOrgtilknytningRequest.gyldigFom())
                    .gyldigTom(endreDatoOrgtilknytningRequest.gyldigTom())
                    .build(),
                EndreOrgtilknytningDatoCommand.builder()
                    .fid(opprettetOrgtilknytningFid)
                    .gyldigFom(leggTilOrgtilknytningRequest.gyldigFom())
                    .gyldigTom(leggTilOrgtilknytningRequest.gyldigTom())
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
    @DisplayName("POST /app/orgtilknytninger/forRessurs/{navident} Should update sektor and tilknytningtype even if tilknytningtype is already set")
    void test_set_sektor_and_tilknytning_when_tilknytningtype_is_already_set() {
        when(authorizedNavident.kanBrukerRedigereRessurs(any(), any())).thenReturn(true);

        var tilknyttetRessurs = ressursState().toBuilder()
            .sektor(Sektor.NAV_STATLIG).build();
        when(ressursStateRepository.findByNavident(tilknyttetRessurs.navident())).thenReturn(Optional.of(tilknyttetRessurs));
        saveRessurser(List.of(tilknyttetRessurs));

        var existingExpiredOrgtilknytning = orgtilknytningState().toBuilder()
            .navident(tilknyttetRessurs.navident())
            .orgEnhetId("1")
            .sektor(null)
            .orgtilknytningType(null)
            .erDagligOppfolging(true)
            .gyldigFom(LocalDate.now().minusMonths(3))
            .gyldigTom(LocalDate.now().minusMonths(2).minusDays(2))
            .build();

        var existingOrgtilknytningAnticipatingGyldigTomChange = orgtilknytningState().toBuilder()
            .navident(tilknyttetRessurs.navident())
            .orgEnhetId("2")
            .sektor(null)
            .orgtilknytningType(Tilknytningtype.INGEN)
            .erDagligOppfolging(true)
            .gyldigFom(existingExpiredOrgtilknytning.gyldigTom().plusDays(1))
            .gyldigTom(null)
            .build();

        saveOrgtilknytninger(List.of(
            existingExpiredOrgtilknytning,
            existingOrgtilknytningAnticipatingGyldigTomChange
        ));

        var endreDatoOrgtilknytningRequest = endreOrgtilknytningRequest(existingOrgtilknytningAnticipatingGyldigTomChange).toBuilder()
            .sektor(Sektor.NAV_STATLIG)
            .tilknytningtype(Tilknytningtype.FAST)
            .gyldigTom(existingExpiredOrgtilknytning.gyldigTom().plusDays(2))
            .build();

        var expiredOrgtilknytningRequest = endreOrgtilknytningRequest(existingExpiredOrgtilknytning);

        var leggTilOrgtilknytningRequest = leggTilOrgtilknytningRequest().toBuilder()
            .tilknytningtype(Tilknytningtype.FAST)
            .sektor(Sektor.NAV_STATLIG)
            .orgEnhetId("3")
            .gyldigFom(endreDatoOrgtilknytningRequest.gyldigTom().plusDays(1))
            .gyldigTom(null)
            .erDagligOppfolging(true)
            .build();

        var opprettetOrgtilknytningFid = UUID.randomUUID().toString();

        when(orgtilknytningCommandService.opprettOrgtilknytning(any())).thenReturn(opprettetOrgtilknytningFid);

        var oppdaterOrgtilknytningerForRessursRequest = oppdaterOrgtilknytningerForRessursRequest().toBuilder()
            .eksisterendeOrgtilknytninger(List.of(
                expiredOrgtilknytningRequest,
                endreDatoOrgtilknytningRequest
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
            .ignoringFields("inputMetadata", "additionalAgressoData")
            .isEqualTo(EndreDagligOppfolgingOrgtilknytningCommand.builder()
                .fid(opprettetOrgtilknytningFid)
                .orgenhetId(leggTilOrgtilknytningRequest.orgEnhetId())
                .gyldigFom(leggTilOrgtilknytningRequest.gyldigFom())
                .gyldigTom(leggTilOrgtilknytningRequest.gyldigTom())
                .erDagligOppfolging(leggTilOrgtilknytningRequest.erDagligOppfolging())
                .navident(tilknyttetRessurs.navident())
                .additionalAgressoData(null)
                .build());

        verify(orgtilknytningCommandService, times(2)).endreTilknytningtype(endreTilknytningtypeCommandArgumentCaptor.capture());
        assertThat(endreTilknytningtypeCommandArgumentCaptor.getValue()).usingRecursiveComparison()
            .ignoringFields("inputMetadata")
            .isEqualTo(EndreTilknytningtypeCommand.builder()
                .fid(opprettetOrgtilknytningFid)
                .tilknytningtype(leggTilOrgtilknytningRequest.tilknytningtype())
                .build()
            );

        verify(orgtilknytningCommandService, times(1)).endreOrgtilknytningDato(endreOrgtilknytningDatoCommandArgumentCaptor.capture());
        assertThat(endreOrgtilknytningDatoCommandArgumentCaptor.getAllValues()).usingRecursiveComparison()
            .ignoringFields("inputMetadata")
            .isEqualTo(List.of(
                EndreOrgtilknytningDatoCommand.builder()
                    .fid(endreDatoOrgtilknytningRequest.fid())
                    .gyldigFom(endreDatoOrgtilknytningRequest.gyldigFom())
                    .gyldigTom(endreDatoOrgtilknytningRequest.gyldigTom())
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

        verify(orgtilknytningCommandService, times(1)).endreOrgtilknytningSektor(endreOrgtilknytningSektorCommandArgumentCaptor.capture());
        assertThat(endreOrgtilknytningSektorCommandArgumentCaptor.getValue()).usingRecursiveComparison()
            .ignoringFields("inputMetadata")
            .isEqualTo(
                EndreOrgtilknytningSektorCommand.builder()
                    .orgenhetId(existingOrgtilknytningAnticipatingGyldigTomChange.orgEnhetId())
                    .navident(tilknyttetRessurs.navident())
                    .fid(endreDatoOrgtilknytningRequest.fid())
                    .sektor(endreDatoOrgtilknytningRequest.sektor())
                    .build()
            );
    }

    @Test
    @DisplayName("POST /app/orgtilknytninger/forRessurs/{navident} should give exception when OT2 has same orgenhetId with startdate before OT1 enddate")
    void test_update_orgtilknytninger_duplikate_for_ressurs1() {
        when(authorizedNavident.kanBrukerRedigereRessurs(any(), any())).thenReturn(true);

        var tilknyttetRessurs = ressursState().toBuilder()
            .sektor(Sektor.EKSTERN).build();
        when(ressursStateRepository.findByNavident(tilknyttetRessurs.navident())).thenReturn(Optional.of(tilknyttetRessurs));
        saveRessurser(List.of(tilknyttetRessurs));

        var existingOrgtilknytning = orgtilknytningState().toBuilder()
            .navident(tilknyttetRessurs.navident())
            .orgEnhetId("1")
            .sektor(tilknyttetRessurs.sektor())
            .orgtilknytningType(Tilknytningtype.KONSULENT)
            .erDagligOppfolging(true)
            .gyldigFom(LocalDate.now())
            .gyldigTom(LocalDate.now().plusMonths(1))
            .build();

        saveOrgtilknytninger(List.of(
            existingOrgtilknytning
        ));

        var existerendeOrgtilknytningRequest = endreOrgtilknytningRequest(existingOrgtilknytning).toBuilder()
            .build();

        var leggTilOrgtilknytningRequest = leggTilOrgtilknytningRequest().toBuilder()
            .orgEnhetId("1")
            .sektor(tilknyttetRessurs.sektor())
            .tilknytningtype(Tilknytningtype.KONSULENT)
            .erDagligOppfolging(false)
            .gyldigFom(LocalDate.now().plusDays(1))
            .gyldigTom(LocalDate.now().plusMonths(1).plusDays(1))
            .build();

        var opprettetOrgtilknytningFid = UUID.randomUUID().toString();

        when(orgtilknytningCommandService.opprettOrgtilknytning(any())).thenReturn(opprettetOrgtilknytningFid);

        var oppdaterOrgtilknytningerForRessursRequest = oppdaterOrgtilknytningerForRessursRequest().toBuilder()
            .eksisterendeOrgtilknytninger(List.of(existerendeOrgtilknytningRequest))
            .nyeOrgtilknytninger(List.of(leggTilOrgtilknytningRequest))
            .build();

        postOppdaterOrgtilknytningerForRessursRequest(
            tilknyttetRessurs.navident(),
            testToken.nomWriteGroupAndNomUiAzp(),
            oppdaterOrgtilknytningerForRessursRequest
        )
            .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName(
        "POST /app/orgtilknytninger/forRessurs/{navident} should give exception when OT2 is active and has same orgenhetId with OT1 which is also active"
    )
    void test_update_orgtilknytninger_duplikate_for_ressurs2() {
        when(authorizedNavident.kanBrukerRedigereRessurs(any(), any())).thenReturn(true);

        var tilknyttetRessurs = ressursState().toBuilder()
            .sektor(Sektor.EKSTERN).build();
        when(ressursStateRepository.findByNavident(tilknyttetRessurs.navident())).thenReturn(Optional.of(tilknyttetRessurs));
        saveRessurser(List.of(tilknyttetRessurs));

        var existingOrgtilknytning = orgtilknytningState().toBuilder()
            .navident(tilknyttetRessurs.navident())
            .orgEnhetId("1")
            .sektor(tilknyttetRessurs.sektor())
            .orgtilknytningType(Tilknytningtype.KONSULENT)
            .erDagligOppfolging(true)
            .gyldigFom(LocalDate.now().minusMonths(1))
            .gyldigTom(LocalDate.now().plusMonths(1))
            .build();

        saveOrgtilknytninger(List.of(
            existingOrgtilknytning
        ));

        var existerendeOrgtilknytningRequest = endreOrgtilknytningRequest(existingOrgtilknytning).toBuilder()
            .build();

        var leggTilOrgtilknytningRequest = leggTilOrgtilknytningRequest().toBuilder()
            .orgEnhetId("1")
            .sektor(tilknyttetRessurs.sektor())
            .tilknytningtype(Tilknytningtype.KONSULENT)
            .erDagligOppfolging(false)
            .gyldigFom(LocalDate.now().minusMonths(1).plusDays(1))
            .gyldigTom(LocalDate.now().plusMonths(1).plusDays(1))
            .build();

        var opprettetOrgtilknytningFid = UUID.randomUUID().toString();

        when(orgtilknytningCommandService.opprettOrgtilknytning(any())).thenReturn(opprettetOrgtilknytningFid);

        var oppdaterOrgtilknytningerForRessursRequest = oppdaterOrgtilknytningerForRessursRequest().toBuilder()
            .eksisterendeOrgtilknytninger(List.of(existerendeOrgtilknytningRequest))
            .nyeOrgtilknytninger(List.of(leggTilOrgtilknytningRequest))
            .build();

        postOppdaterOrgtilknytningerForRessursRequest(
            tilknyttetRessurs.navident(),
            testToken.nomWriteGroupAndNomUiAzp(),
            oppdaterOrgtilknytningerForRessursRequest
        )
            .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName(
        "POST /app/orgtilknytninger/forRessurs/{navident} should give exception when OT2 is added and has same orgenhetId with OT1 which is has no end date"
    )
    void test_update_orgtilknytninger_duplikate_for_ressurs3() {
        when(authorizedNavident.kanBrukerRedigereRessurs(any(), any())).thenReturn(true);
        var tilknyttetRessurs = ressursState().toBuilder()
            .sektor(Sektor.NAV_STATLIG).build();
        when(ressursStateRepository.findByNavident(tilknyttetRessurs.navident())).thenReturn(Optional.of(tilknyttetRessurs));
        saveRessurser(List.of(tilknyttetRessurs));

        var existingOrgtilknytning = orgtilknytningState().toBuilder()
            .navident(tilknyttetRessurs.navident())
            .orgEnhetId("1")
            .sektor(tilknyttetRessurs.sektor())
            .orgtilknytningType(Tilknytningtype.FAST)
            .erDagligOppfolging(true)
            .gyldigFom(LocalDate.now().minusMonths(1))
            .gyldigTom(null)
            .build();

        saveOrgtilknytninger(List.of(
            existingOrgtilknytning
        ));

        var existerendeOrgtilknytningRequest = endreOrgtilknytningRequest(existingOrgtilknytning).toBuilder()
            .build();

        var leggTilOrgtilknytningRequest = leggTilOrgtilknytningRequest().toBuilder()
            .orgEnhetId("1")
            .sektor(tilknyttetRessurs.sektor())
            .tilknytningtype(Tilknytningtype.FAST)
            .erDagligOppfolging(false)
            .gyldigFom(LocalDate.now().plusMonths(1))
            .gyldigTom(null)
            .build();

        var opprettetOrgtilknytningFid = UUID.randomUUID().toString();

        when(orgtilknytningCommandService.opprettOrgtilknytning(any())).thenReturn(opprettetOrgtilknytningFid);

        var oppdaterOrgtilknytningerForRessursRequest = oppdaterOrgtilknytningerForRessursRequest().toBuilder()
            .eksisterendeOrgtilknytninger(List.of(existerendeOrgtilknytningRequest))
            .nyeOrgtilknytninger(List.of(leggTilOrgtilknytningRequest))
            .build();

        postOppdaterOrgtilknytningerForRessursRequest(
            tilknyttetRessurs.navident(),
            testToken.nomAdminUser(),
            oppdaterOrgtilknytningerForRessursRequest
        )
            .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName(
        "POST /app/orgtilknytninger/forRessurs/{navident} should not exception when OT2 is added and has same orgenhetId with OT1 and both are active, but sektor is different"
    )
    void test_update_orgtilknytninger_duplikate_for_ressurs4() {
        when(authorizedNavident.kanBrukerRedigereRessurs(any(), any())).thenReturn(true);
        var tilknyttetRessurs = ressursState().toBuilder()
            .sektor(Sektor.NAV_STATLIG).build();
        when(ressursStateRepository.findByNavident(tilknyttetRessurs.navident())).thenReturn(Optional.of(tilknyttetRessurs));
        saveRessurser(List.of(tilknyttetRessurs));

        var existingOrgtilknytning = orgtilknytningState().toBuilder()
            .navident(tilknyttetRessurs.navident())
            .orgEnhetId("1")
            .sektor(tilknyttetRessurs.sektor())
            .orgtilknytningType(Tilknytningtype.FAST)
            .erDagligOppfolging(true)
            .gyldigFom(LocalDate.now().minusMonths(1))
            .gyldigTom(null)
            .build();

        saveOrgtilknytninger(List.of(
            existingOrgtilknytning
        ));

        var existerendeOrgtilknytningRequest = endreOrgtilknytningRequest(existingOrgtilknytning).toBuilder()
            .build();

        var leggTilOrgtilknytningRequest = leggTilOrgtilknytningRequest().toBuilder()
            .orgEnhetId("1")
            .sektor(Sektor.EKSTERN)
            .tilknytningtype(Tilknytningtype.KONSULENT)
            .erDagligOppfolging(false)
            .gyldigFom(LocalDate.now().plusMonths(1))
            .gyldigTom(LocalDate.now().plusMonths(2))
            .build();

        var opprettetOrgtilknytningFid = UUID.randomUUID().toString();

        when(orgtilknytningCommandService.opprettOrgtilknytning(any())).thenReturn(opprettetOrgtilknytningFid);

        var oppdaterOrgtilknytningerForRessursRequest = oppdaterOrgtilknytningerForRessursRequest().toBuilder()
            .eksisterendeOrgtilknytninger(List.of(existerendeOrgtilknytningRequest))
            .nyeOrgtilknytninger(List.of(leggTilOrgtilknytningRequest))
            .build();

        postOppdaterOrgtilknytningerForRessursRequest(
            tilknyttetRessurs.navident(),
            testToken.nomAdminUser(),
            oppdaterOrgtilknytningerForRessursRequest
        )
            .expectStatus().is2xxSuccessful();
    }

    @Test
    @DisplayName("POST /app/orgtilknytninger/forRessurs/{navident} should update orgtilknytninger and add one new orgtilknytninger for ressurs")
    void test_update_orgtilknytninger_for_ressurs_ny() {
        when(authorizedNavident.kanBrukerRedigereRessurs(any(), any())).thenReturn(true);

        when(orgEnhetLederService.getEnheterByLeder("A321321")).thenReturn(List.of(orgEnhet()));
        var tilknyttetRessurs = ressursState().toBuilder()
            .sektor(Sektor.EKSTERN).build();
        when(ressursStateRepository.findByNavident(tilknyttetRessurs.navident())).thenReturn(Optional.of(tilknyttetRessurs));
        saveRessurser(List.of(tilknyttetRessurs));

        var existingOrgtilknytningAnticipatingNewDate = orgtilknytningState().toBuilder()
            .navident(tilknyttetRessurs.navident())
            .orgEnhetId("1")
            .erDagligOppfolging(false)
            .sektor(tilknyttetRessurs.sektor())
            .orgtilknytningType(Tilknytningtype.KONSULENT)
            .gyldigTom(LocalDate.now().plusMonths(1))
            .build();

        var existingOrgtilknytningAnticipatingDolChange = orgtilknytningState().toBuilder()
            .navident(tilknyttetRessurs.navident())
            .orgEnhetId("2")
            .erDagligOppfolging(false)
            .sektor(tilknyttetRessurs.sektor())
            .gyldigFom(existingOrgtilknytningAnticipatingNewDate.gyldigTom().plusDays(1))
            .gyldigTom(existingOrgtilknytningAnticipatingNewDate.gyldigTom().plusMonths(1))
            .build();

        var existingOrgtilknytningAnticipatingNewTilknytningtype = orgtilknytningState().toBuilder()
            .navident(tilknyttetRessurs.navident())
            .orgEnhetId("3")
            .erDagligOppfolging(false)
            .sektor(tilknyttetRessurs.sektor())
            .orgtilknytningType(null)
            .build();

        saveOrgtilknytninger(List.of(
            existingOrgtilknytningAnticipatingNewDate,
            existingOrgtilknytningAnticipatingDolChange,
            existingOrgtilknytningAnticipatingNewTilknytningtype
        ));

        var endreDatoOrgtilknytningRequest = endreOrgtilknytningRequest(existingOrgtilknytningAnticipatingNewDate).toBuilder()
            .gyldigTom(
                existingOrgtilknytningAnticipatingNewDate.gyldigTom().plusMonths(1)
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
            .tilknytningtype(Tilknytningtype.KONSULENT)
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
                .gyldigTom(endreDolOrgtilknytningRequest.gyldigTom())
                .erDagligOppfolging(endreDolOrgtilknytningRequest.erDagligOppfolging())
                .navident(existingOrgtilknytningAnticipatingDolChange.navident())
                .orgenhetId(existingOrgtilknytningAnticipatingDolChange.orgEnhetId())
                .additionalAgressoData(null)
                .build()
            );

        verify(orgtilknytningCommandService, times(2)).endreTilknytningtype(endreTilknytningtypeCommandArgumentCaptor.capture());
        assertThat(endreTilknytningtypeCommandArgumentCaptor.getAllValues()).usingRecursiveComparison()
            .ignoringFields("inputMetadata")
            .isEqualTo(
                List.of(
                    EndreTilknytningtypeCommand.builder()
                        .fid(endreTilknytningtypeRequest.fid())
                        .tilknytningtype(endreTilknytningtypeRequest.tilknytningtype())
                        .build(),
                    EndreTilknytningtypeCommand.builder()
                        .fid(opprettetOrgtilknytningFid)
                        .tilknytningtype(leggTilOrgtilknytningRequest.tilknytningtype())
                        .build()
                )
            );

        verify(orgtilknytningCommandService, times(2)).endreOrgtilknytningDato(endreOrgtilknytningDatoCommandArgumentCaptor.capture());
        assertThat(endreOrgtilknytningDatoCommandArgumentCaptor.getAllValues()).usingRecursiveComparison()
            .ignoringFields("inputMetadata")
            .isEqualTo(List.of(
                EndreOrgtilknytningDatoCommand.builder()
                    .fid(endreDatoOrgtilknytningRequest.fid())
                    .gyldigFom(endreDatoOrgtilknytningRequest.gyldigFom())
                    .gyldigTom(endreDatoOrgtilknytningRequest.gyldigTom())
                    .build(),
                EndreOrgtilknytningDatoCommand.builder()
                    .fid(opprettetOrgtilknytningFid)
                    .gyldigFom(leggTilOrgtilknytningRequest.gyldigFom())
                    .gyldigTom(leggTilOrgtilknytningRequest.gyldigTom())
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
        when(authorizedNavident.kanBrukerRedigereRessurs(any(), any())).thenReturn(true);

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
                .gyldigTom(leggTilDagligOppfolgingOrgtilknytningWithTilknytningtypeRequest.gyldigTom())
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
                .tilknytningtype(leggTilDagligOppfolgingOrgtilknytningWithTilknytningtypeRequest.tilknytningtype())
                .build()
            );
    }

    @Test
    @DisplayName("POST /app/orgtilknytninger/forRessurs/{navident} should not update orgtilknytninger where primaryFysiskBrukersted only differs between empty and null")
    void test_update_orgtilknytninger_for_ressurs_treat_empty_as_null_for_primary_fysisk_brukersted() {
        when(authorizedNavident.kanBrukerRedigereRessurs(any(), any())).thenReturn(true);

        var tilknyttetRessurs = ressursState().toBuilder()
            .sektor(Sektor.NAV_STATLIG).build();
        when(ressursStateRepository.findByNavident(tilknyttetRessurs.navident())).thenReturn(Optional.of(tilknyttetRessurs));
        saveRessurser(List.of(tilknyttetRessurs));

        var existingOrgtilknytningEmptyPF = orgtilknytningState().toBuilder()
            .navident(tilknyttetRessurs.navident())
            .orgEnhetId("1")
            .sektor(tilknyttetRessurs.sektor())
            .orgtilknytningType(Tilknytningtype.FAST)
            .erDagligOppfolging(false)
            .gyldigTom(LocalDate.now().plusMonths(1))
            .primaryFysiskBrukerstedNummer("")
            .additionalFysiskeBrukerstederNummer(Collections.emptyList())
            .build();

        var existingOrgtilknytningNullPF = orgtilknytningState().toBuilder()
            .navident(tilknyttetRessurs.navident())
            .orgEnhetId("2")
            .sektor(tilknyttetRessurs.sektor())
            .orgtilknytningType(Tilknytningtype.FAST)
            .erDagligOppfolging(false)
            .gyldigFom(existingOrgtilknytningEmptyPF.gyldigTom().plusDays(1))
            .gyldigTom(existingOrgtilknytningEmptyPF.gyldigTom().plusMonths(1))
            .primaryFysiskBrukerstedNummer(null)
            .additionalFysiskeBrukerstederNummer(Collections.emptyList())
            .build();

        saveOrgtilknytninger(List.of(
            existingOrgtilknytningEmptyPF,
            existingOrgtilknytningNullPF
        ));

        var endreOrgtilknytningEmptyPFgRequest = endreOrgtilknytningRequest(existingOrgtilknytningEmptyPF).toBuilder()
            .primaryFysiskBrukerstedNummer(null)
            .build();

        var endreOrgtilknytningNullPF = endreOrgtilknytningRequest(existingOrgtilknytningNullPF).toBuilder()
            .primaryFysiskBrukerstedNummer("")
            .build();

        var oppdaterOrgtilknytningerForRessursRequest = oppdaterOrgtilknytningerForRessursRequest().toBuilder()
            .nyeOrgtilknytninger(List.of())
            .eksisterendeOrgtilknytninger(List.of(
                endreOrgtilknytningEmptyPFgRequest,
                endreOrgtilknytningNullPF
            ))
            .build();

        postOppdaterOrgtilknytningerForRessursRequest(
            tilknyttetRessurs.navident(),
            testToken.nomWriteGroupAndNomUiAzp(),
            oppdaterOrgtilknytningerForRessursRequest
        )
            .expectStatus().is2xxSuccessful();

        verify(orgtilknytningCommandService, never()).endreFysiskeBrukersted(any());
    }

    @Test
    @DisplayName(
        "POST /app/orgtilknytninger/forRessurs/{navident} Should forbid update orgtilknytning type if user user has 'MUTATE_RESSURS' but is not authorized for ressurs"
    )
    void test_update_orgtilknytninger_for_ressurs_forbidden() {
        when(authorizedNavident.kanBrukerRedigereRessurs(any(), any())).thenReturn(false);

        var tilknyttetRessurs = ressursState().toBuilder()
            .sektor(Sektor.EKSTERN).build();
        when(ressursStateRepository.findByNavident(tilknyttetRessurs.navident())).thenReturn(Optional.of(tilknyttetRessurs));
        saveRessurser(List.of(tilknyttetRessurs));

        var existingOrgtilknytning = orgtilknytningState().toBuilder()
            .navident(tilknyttetRessurs.navident())
            .gyldigTom(LocalDate.now().plusMonths(1))
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

        postOppdaterOrgtilknytningerForRessursRequest(
            tilknyttetRessurs.navident(),
            testToken.nomWriteGroupAndNomUiAzp(),
            oppdaterOrgtilknytningerForRessursRequest
        )
            .expectStatus().isForbidden();
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("Should return 400 if request has overlapping daglige oppfolginger")
    @MethodSource("test_no_overlapping_daglig_oppfolginger_input")
    void test_no_overlapping_daglig_oppfolginger(OppdaterOrgtilknytningerForRessursRequest oppdaterOrgtilknytningerForRessursRequest) {
        when(authorizedNavident.kanBrukerRedigereRessurs(any(), any())).thenReturn(true);
        var tilknyttetRessurs = ressursState().toBuilder()
            .sektor(Sektor.EKSTERN).build();
        when(ressursStateRepository.findByNavident(tilknyttetRessurs.navident())).thenReturn(Optional.of(tilknyttetRessurs));
        saveOrgtilknytninger(
            oppdaterOrgtilknytningerForRessursRequest
                .eksisterendeOrgtilknytninger().stream()
                .map(EndreOrgtilknytningRequest::fid)
                .map(fid ->
                    orgtilknytningState().toBuilder()
                        .fid(fid)
                        .orgEnhetId(UUID.randomUUID().toString())
                        .sektor(tilknyttetRessurs.sektor())
                        .erDagligOppfolging(false)
                        .navident(tilknyttetRessurs.navident())
                        .orgtilknytningType(Tilknytningtype.KONSULENT)
                        .build()
                ).toList());

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
        when(authorizedNavident.kanBrukerRedigereRessurs(any(), any())).thenReturn(true);
        when(orgEnhetLederService.getEnheterByLeder("A321321")).thenReturn(List.of(orgEnhet()));
        when(ressursStateRepository.findByNavident("whatever")).thenReturn(Optional.of(ressursState().toBuilder().sektor(Sektor.EKSTERN).build()));

        saveOrgtilknytninger(
            oppdaterOrgtilknytningerForRessursRequest
                .eksisterendeOrgtilknytninger().stream()
                .map(EndreOrgtilknytningRequest::fid)
                .map(fid ->
                    orgtilknytningState().toBuilder()
                        .fid(fid)
                        .orgEnhetId(UUID.randomUUID().toString())
                        .sektor(Sektor.NAV_STATLIG)
                        .erDagligOppfolging(false)
                        .navident("whatever")
                        .orgtilknytningType(Tilknytningtype.KONSULENT)
                        .build()
                ).toList());

        postOppdaterOrgtilknytningerForRessursRequest(
            "whatever",
            testToken.nomWriteGroupAndNomUiAzp(),
            oppdaterOrgtilknytningerForRessursRequest
        ).expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("Should throw InvalidSektorException if sektor is null in active existing OT")
    void test_throw_exception_if_sektor_is_null_01() {
        when(authorizedNavident.kanBrukerRedigereRessurs(any(), any())).thenReturn(true);
        var tilknyttetRessurs = ressursState().toBuilder()
            .sektor(Sektor.EKSTERN).build();
        when(ressursStateRepository.findByNavident(tilknyttetRessurs.navident())).thenReturn(Optional.of(tilknyttetRessurs));
        saveRessurser(List.of(tilknyttetRessurs));

        var existingOrgtilknytningAnticipatingNewDate = orgtilknytningState().toBuilder()
            .navident(tilknyttetRessurs.navident())
            .orgEnhetId("1")
            .sektor(tilknyttetRessurs.sektor())
            .orgtilknytningType(Tilknytningtype.KONSULENT)
            .erDagligOppfolging(false)
            .gyldigTom(LocalDate.now().plusMonths(1))
            .build();

        var existingOrgtilknytningAnticipatingDolChange = orgtilknytningState().toBuilder()
            .navident(tilknyttetRessurs.navident())
            .orgEnhetId("2")
            .sektor(tilknyttetRessurs.sektor())
            .orgtilknytningType(Tilknytningtype.KONSULENT)
            .erDagligOppfolging(false)
            .gyldigFom(existingOrgtilknytningAnticipatingNewDate.gyldigTom().plusDays(1))
            .gyldigTom(existingOrgtilknytningAnticipatingNewDate.gyldigTom().plusMonths(1))
            .build();

        var existingOrgtilknytningAnticipatingNewTilknytningtype = orgtilknytningState().toBuilder()
            .navident(tilknyttetRessurs.navident())
            .orgEnhetId("3")
            .sektor(tilknyttetRessurs.sektor())
            .orgtilknytningType(Tilknytningtype.KONSULENT)
            .erDagligOppfolging(false)
            .orgtilknytningType(null)
            .build();

        saveOrgtilknytninger(List.of(
            existingOrgtilknytningAnticipatingNewDate,
            existingOrgtilknytningAnticipatingDolChange,
            existingOrgtilknytningAnticipatingNewTilknytningtype
        ));

        var endreDatoOrgtilknytningRequest = endreOrgtilknytningRequest(existingOrgtilknytningAnticipatingNewDate).toBuilder()
            .gyldigTom(
                existingOrgtilknytningAnticipatingNewDate.gyldigTom().plusMonths(1)
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
            .tilknytningtype(Tilknytningtype.KONSULENT)
            .build();

        var opprettetOrgtilknytningFid = UUID.randomUUID().toString();

        when(orgtilknytningCommandService.opprettOrgtilknytning(any())).thenReturn(opprettetOrgtilknytningFid);

        var oppdaterOrgtilknytningerForRessursRequest = oppdaterOrgtilknytningerForRessursRequest().toBuilder()
            .eksisterendeOrgtilknytninger(List.of(
                endreDatoOrgtilknytningRequest.toBuilder().sektor(null).build(),
                endreDolOrgtilknytningRequest.toBuilder().sektor(null).build(),
                endreTilknytningtypeRequest.toBuilder().sektor(null).build()
            ))
            .nyeOrgtilknytninger(List.of(leggTilOrgtilknytningRequest))
            .build();

        postOppdaterOrgtilknytningerForRessursRequest(
            tilknyttetRessurs.navident(),
            testToken.nomWriteGroupAndNomUiAzp(),
            oppdaterOrgtilknytningerForRessursRequest
        )
            .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("Should throw InvalidSektorException if sektor is null in active or future new OT")
    void test_throw_exception_if_sektor_is_null_02() {
        when(authorizedNavident.kanBrukerRedigereRessurs(any(), any())).thenReturn(true);
        var tilknyttetRessurs = ressursState().toBuilder()
            .sektor(Sektor.EKSTERN).build();
        when(ressursStateRepository.findByNavident(tilknyttetRessurs.navident())).thenReturn(Optional.of(tilknyttetRessurs));
        saveRessurser(List.of(tilknyttetRessurs));

        var existingOrgtilknytningAnticipatingNewDate = orgtilknytningState().toBuilder()
            .navident(tilknyttetRessurs.navident())
            .orgEnhetId("1")
            .sektor(tilknyttetRessurs.sektor())
            .erDagligOppfolging(false)
            .gyldigTom(LocalDate.now().plusMonths(1))
            .orgtilknytningType(Tilknytningtype.KONSULENT)
            .build();

        var existingOrgtilknytningAnticipatingDolChange = orgtilknytningState().toBuilder()
            .navident(tilknyttetRessurs.navident())
            .orgEnhetId("2")
            .sektor(tilknyttetRessurs.sektor())
            .orgtilknytningType(Tilknytningtype.KONSULENT)
            .erDagligOppfolging(false)
            .gyldigFom(existingOrgtilknytningAnticipatingNewDate.gyldigTom().plusDays(1))
            .gyldigTom(existingOrgtilknytningAnticipatingNewDate.gyldigTom().plusMonths(1))
            .build();

        var existingOrgtilknytningAnticipatingNewTilknytningtype = orgtilknytningState().toBuilder()
            .navident(tilknyttetRessurs.navident())
            .orgEnhetId("3")
            .sektor(tilknyttetRessurs.sektor())
            .orgtilknytningType(Tilknytningtype.KONSULENT)
            .erDagligOppfolging(false)
            .orgtilknytningType(null)
            .build();

        saveOrgtilknytninger(List.of(
            existingOrgtilknytningAnticipatingNewDate,
            existingOrgtilknytningAnticipatingDolChange,
            existingOrgtilknytningAnticipatingNewTilknytningtype
        ));

        var endreDatoOrgtilknytningRequest = endreOrgtilknytningRequest(existingOrgtilknytningAnticipatingNewDate).toBuilder()
            .gyldigTom(
                existingOrgtilknytningAnticipatingNewDate.gyldigTom().plusMonths(1)
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
            .sektor(null)
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
            .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("Should not throw InvalidOrgtilknytningTileException if Tilknytningtype is null or INGEN if Sektor is NAV_KOMMUNAL")
    void test_should_not_throw_exception_if_sektor_is_nav_kommunal() {
        when(authorizedNavident.kanBrukerRedigereRessurs(any(), any())).thenReturn(true);
        var tilknyttetRessurs = ressursState().toBuilder()
            .sektor(Sektor.NAV_KOMMUNAL).build();
        when(ressursStateRepository.findByNavident(tilknyttetRessurs.navident())).thenReturn(Optional.of(tilknyttetRessurs));
        saveRessurser(List.of(tilknyttetRessurs));

        var existingOrgtilknytningAnticipatingNewDate = orgtilknytningState().toBuilder()
            .navident(tilknyttetRessurs.navident())
            .orgEnhetId("1")
            .sektor(tilknyttetRessurs.sektor())
            .erDagligOppfolging(false)
            .gyldigTom(LocalDate.now().plusMonths(1))
            .orgtilknytningType(null)
            .build();

        saveOrgtilknytninger(List.of(
            existingOrgtilknytningAnticipatingNewDate
        ));

        var endreDatoOrgtilknytningRequest = endreOrgtilknytningRequest(existingOrgtilknytningAnticipatingNewDate).toBuilder()
            .gyldigTom(
                existingOrgtilknytningAnticipatingNewDate.gyldigTom().plusMonths(1)
            )
            .build();

        var oppdaterOrgtilknytningerForRessursRequest = oppdaterOrgtilknytningerForRessursRequest().toBuilder()
            .eksisterendeOrgtilknytninger(List.of(
                endreDatoOrgtilknytningRequest
            ))
            .nyeOrgtilknytninger(List.of())
            .build();

        postOppdaterOrgtilknytningerForRessursRequest(
            tilknyttetRessurs.navident(),
            testToken.nomWriteGroupAndNomUiAzp(),
            oppdaterOrgtilknytningerForRessursRequest
        )
            .expectStatus().is2xxSuccessful();
    }

    @Test
    @DisplayName("Should throw InvalidOrgtilknytningTileException if Tilknytningtype is null or INGEN if Sektor is NOT NAV_KOMMUNAL")
    void test_should_not_throw_exception_if_sektor_is_nav_not_kommunal() {
        when(authorizedNavident.kanBrukerRedigereRessurs(any(), any())).thenReturn(true);
        var tilknyttetRessurs = ressursState().toBuilder()
            .sektor(Sektor.NAV_STATLIG).build();
        when(ressursStateRepository.findByNavident(tilknyttetRessurs.navident())).thenReturn(Optional.of(tilknyttetRessurs));
        saveRessurser(List.of(tilknyttetRessurs));

        var existingOrgtilknytningAnticipatingNewDate = orgtilknytningState().toBuilder()
            .navident(tilknyttetRessurs.navident())
            .orgEnhetId("1")
            .sektor(tilknyttetRessurs.sektor())
            .erDagligOppfolging(false)
            .gyldigTom(LocalDate.now().plusMonths(1))
            .orgtilknytningType(null)
            .build();

        saveOrgtilknytninger(List.of(
            existingOrgtilknytningAnticipatingNewDate
        ));

        var endreDatoOrgtilknytningRequest = endreOrgtilknytningRequest(existingOrgtilknytningAnticipatingNewDate).toBuilder()
            .gyldigTom(
                existingOrgtilknytningAnticipatingNewDate.gyldigTom().plusMonths(1)
            )
            .build();

        var oppdaterOrgtilknytningerForRessursRequest = oppdaterOrgtilknytningerForRessursRequest().toBuilder()
            .eksisterendeOrgtilknytninger(List.of(
                endreDatoOrgtilknytningRequest
            ))
            .nyeOrgtilknytninger(List.of())
            .build();

        postOppdaterOrgtilknytningerForRessursRequest(
            tilknyttetRessurs.navident(),
            testToken.nomWriteGroupAndNomUiAzp(),
            oppdaterOrgtilknytningerForRessursRequest
        )
            .expectStatus().is4xxClientError();
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
        when(authorizedNavident.kanBrukerRedigereRessurs(any(), any())).thenReturn(true);

        var tilknyttetRessurs = ressursState().toBuilder()
            .sektor(Sektor.EKSTERN).build();
        when(ressursStateRepository.findByNavident(tilknyttetRessurs.navident())).thenReturn(Optional.of(tilknyttetRessurs));
        saveRessurser(List.of(tilknyttetRessurs));

        saveOrgtilknytninger(existingOrgtilknytningFids.stream().map(fid ->
            orgtilknytningState().toBuilder()
                .fid(fid)
                .orgEnhetId(UUID.randomUUID().toString())
                .erDagligOppfolging(false)
                .sektor(tilknyttetRessurs.sektor())
                .navident(tilknyttetRessurs.navident())
                .build()
        ).toList());

        saveOrgtilknytninger(
            oppdaterOrgtilknytningerForRessursRequest.eksisterendeOrgtilknytninger().stream().map(EndreOrgtilknytningRequest::fid)
                .map(fid ->
                    orgtilknytningState().toBuilder()
                        .fid(fid)
                        .orgEnhetId(UUID.randomUUID().toString())
                        .sektor(tilknyttetRessurs.sektor())
                        .erDagligOppfolging(false)
                        .navident("ANNEN IDENT")
                        .orgtilknytningType(Tilknytningtype.KONSULENT)
                        .build()
                ).toList()
        );

        postOppdaterOrgtilknytningerForRessursRequest(
            tilknyttetRessurs.navident(),
            testToken.nomWriteGroupAndNomUiAzp(),
            oppdaterOrgtilknytningerForRessursRequest
        ).expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("Should return bad request if request changes already set tilknytningtype")
    void test_tilknytningtype_change_returns_bad_request() {
        when(authorizedNavident.kanBrukerRedigereRessurs(any(), any())).thenReturn(true);
        var tilknyttetRessurs = ressursState().toBuilder()
            .sektor(Sektor.NAV_STATLIG).build();
        when(ressursStateRepository.findByNavident(tilknyttetRessurs.navident())).thenReturn(Optional.of(tilknyttetRessurs));
        saveRessurser(List.of(tilknyttetRessurs));

        var existingOrgtilknytningAnticipatingNewTilknytningtype = orgtilknytningState().toBuilder()
            .navident(tilknyttetRessurs.navident())
            .orgEnhetId("1")
            .sektor(tilknyttetRessurs.sektor())
            .erDagligOppfolging(false)
            .gyldigTom(LocalDate.now().plusMonths(1))
            .orgtilknytningType(Tilknytningtype.FAST)
            .build();

        saveOrgtilknytninger(List.of(
            existingOrgtilknytningAnticipatingNewTilknytningtype
        ));

        var endreDatoOrgtilknytningRequest = endreOrgtilknytningRequest(existingOrgtilknytningAnticipatingNewTilknytningtype).toBuilder()
            .tilknytningtype(Tilknytningtype.PENSJONIST)
            .build();

        var oppdaterOrgtilknytningerForRessursRequest = oppdaterOrgtilknytningerForRessursRequest().toBuilder()
            .eksisterendeOrgtilknytninger(List.of(
                endreDatoOrgtilknytningRequest
            ))
            .nyeOrgtilknytninger(List.of())
            .build();

        postOppdaterOrgtilknytningerForRessursRequest(
            tilknyttetRessurs.navident(),
            testToken.nomWriteGroupAndNomUiAzp(),
            oppdaterOrgtilknytningerForRessursRequest
        )
            .expectStatus().is4xxClientError()
            .expectBody().jsonPath("$.message").isEqualTo("Tilknytningstype kan ikke endres når den allerede er satt på eksisterende Orgtilknytninger");

    }

    @ParameterizedTest(name = "existing fids: {0} \noppdaterOrgtilknytningerForRessursRequest: {1}")
    @DisplayName("Should return 400 if eksisterendeOrgtilknytninger does not contain exactly all existing orgtilknytninger")
    @MethodSource("test_oppdaterOrgtilknytningerForRessursRequest_contains_exactly_all_existing_orgtilknytninger_input")
    void test_eksisterendeOrgtilknytninger_contains_exactly_all_existing_orgtilknytninger_ny(
        List<String> existingOrgtilknytningFids,
        OppdaterOrgtilknytningerForRessursRequest oppdaterOrgtilknytningerForRessursRequest
    ) {
        when(authorizedNavident.kanBrukerRedigereRessurs(any(), any())).thenReturn(true);

        when(orgEnhetLederService.getEnheterByLeder("A321321")).thenReturn(List.of(orgEnhet()));
        var tilknyttetRessurs = ressursState().toBuilder()
            .sektor(Sektor.EKSTERN).build();
        when(ressursStateRepository.findByNavident(tilknyttetRessurs.navident())).thenReturn(Optional.of(tilknyttetRessurs));
        saveRessurser(List.of(tilknyttetRessurs));

        saveOrgtilknytninger(existingOrgtilknytningFids.stream().map(fid ->
            orgtilknytningState().toBuilder()
                .fid(fid)
                .orgEnhetId(UUID.randomUUID().toString())
                .sektor(tilknyttetRessurs.sektor())
                .erDagligOppfolging(false)
                .navident(tilknyttetRessurs.navident())
                .orgtilknytningType(Tilknytningtype.KONSULENT)
                .build()
        ).toList());

        saveOrgtilknytninger(
            oppdaterOrgtilknytningerForRessursRequest.eksisterendeOrgtilknytninger().stream().map(EndreOrgtilknytningRequest::fid)
                .map(fid ->
                    orgtilknytningState().toBuilder()
                        .fid(fid)
                        .orgEnhetId(UUID.randomUUID().toString())
                        .sektor(tilknyttetRessurs.sektor())
                        .erDagligOppfolging(false)
                        .navident("ANNEN IDENT")
                        .orgtilknytningType(Tilknytningtype.KONSULENT)
                        .build()
                ).toList()
        );

        postOppdaterOrgtilknytningerForRessursRequest(
            tilknyttetRessurs.navident(),
            testToken.nomWriteGroupAndNomUiAzp(),
            oppdaterOrgtilknytningerForRessursRequest
        ).expectStatus().isBadRequest();
    }

    private static Stream<Arguments> test_oppdaterOrgtilknytningerForRessursRequest_contains_exactly_all_existing_orgtilknytninger_input() {
        var existingOrgtilknytningFidsForRessurs = List.of(
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString()
        );

        var validExistingOrgtilknytninger = existingOrgtilknytningFidsForRessurs.stream().map(fid ->
            endreOrgtilknytningRequest().toBuilder()
                .erDagligOppfolging(false)
                .fid(fid)
                .build()
        ).toList();

        var oneTooMany = Stream.concat(
            validExistingOrgtilknytninger.stream(),
            Stream.of(endreOrgtilknytningRequest())
        ).toList();

        var oneTooFew = validExistingOrgtilknytninger.subList(0, 1);

        var duplicateFids = Stream.concat(
            validExistingOrgtilknytninger.stream(),
            Stream.of(endreOrgtilknytningRequest().toBuilder().fid(existingOrgtilknytningFidsForRessurs.get(1)).build())
        ).toList();

        return Stream.of(
            Arguments.of(
                existingOrgtilknytningFidsForRessurs,
                oppdaterOrgtilknytningerForRessursRequest().toBuilder()
                    .eksisterendeOrgtilknytninger(oneTooMany)
                    .nyeOrgtilknytninger(Collections.emptyList())
                    .build()
            ),
            Arguments.of(
                existingOrgtilknytningFidsForRessurs,
                oppdaterOrgtilknytningerForRessursRequest().toBuilder()
                    .eksisterendeOrgtilknytninger(oneTooFew)
                    .nyeOrgtilknytninger(Collections.emptyList())
                    .build()
            ),
            Arguments.of(
                existingOrgtilknytningFidsForRessurs,
                oppdaterOrgtilknytningerForRessursRequest().toBuilder()
                    .eksisterendeOrgtilknytninger(duplicateFids)
                    .nyeOrgtilknytninger(Collections.emptyList())
                    .build()
            )
        );
    }

    @Test
    void test_ny_masseflytting_should_return_200_OK() {
        var masseflyttingRequest = new MasseFlyttingRequest(
            List.of("fid1", "fid2"),
            "newOrgEnhetId",
            true
        );

        LocalDate flytteDato = LocalDate.now();

        var orgtilknytningState1 = new OrgtilknytningState(
            "fid1",
            "navident1",
            "orgEnhetId1",
            Sektor.NAV_STATLIG,
            false,
            flytteDato.minusMonths(1),
            null,
            Tilknytningtype.FAST,
            "2700",
            Collections.emptyList(),
            null,
            "S123456"
        );
        var orgtilknytningState2 = new OrgtilknytningState(
            "fid2",
            "navident2",
            "orgEnhetId2",
            Sektor.NAV_STATLIG,
            true,
            flytteDato.plusMonths(1),
            null, Tilknytningtype.FAST,
            "2700",
            Collections.emptyList(),
            null,
            "S123456"
        );
        saveOrgtilknytninger(List.of(orgtilknytningState1, orgtilknytningState2));

        when(orgenhetService.getById("orgEnhetId1")).thenReturn(Optional.of(new OrgEnhet(OrgEnhetState.builder()
            .fid("FID_1")
            .id("orgEnhetId1")
            .navn("Enhet 1")
            .tilgangsenhetId("1234")
            .build())));
        // Enhet uten tilgangsenhetId – dekker null-grenen i getOrgEnhetNavn (Optional.ofNullable)
        when(orgenhetService.getById("orgEnhetId2")).thenReturn(Optional.of(new OrgEnhet(OrgEnhetState.builder()
            .fid("FID_2")
            .id("orgEnhetId2")
            .navn("Enhet 2")
            .build())));

        // Ny enhet
        when(orgenhetService.getById("newOrgEnhetId")).thenReturn(Optional.of(new OrgEnhet(
            OrgEnhetState.builder()
                .fid("FID_3")
                .id("newOrgEnhetId")
                .navn("Ny enhet")
                .tilgangsenhetId("5678")
                .gyldigFom(LocalDate.now().minusYears(1))
                .gyldigTom(null)
                .build())
        ));


        when(orgEnhetLederService.getEnheterByLeder("A321321")).thenReturn(List.of(orgEnhet()));
        when(ressursStateRepository.findByNavident("A321321")).thenReturn(Optional.of(ressursState().toBuilder().visningsnavn(new Visningsnavn("John", Optional.empty(), "Doe")).build()));

        when(ressursStateRepository.findByNavident("navident1")).thenReturn(Optional.of(ressursState().toBuilder().visningsnavn(new Visningsnavn("Jane", Optional.empty(), "Doe")).build()));
        when(ressursStateRepository.findByNavident("navident2")).thenReturn(Optional.of(ressursState().toBuilder().visningsnavn(new Visningsnavn("Ola", Optional.empty(), "Normann")).build()));

        postMasseflyttingRequest(
            testToken.nomAdminUser(),
            masseflyttingRequest
        ).expectStatus().isOk();

        verify(orgtilknytningCommandService, times(1)).fjernOrgtilknytning(any(), any());
        verify(orgtilknytningCommandService, times(1)).avvikleOrgtilknytning(any(), any());
        verify(orgtilknytningCommandService, times(2)).endreDagligOppfolgingOrgtilknytning(any());
        verify(orgtilknytningCommandService, times(2)).opprettOrgtilknytning(any());
    }

    @Test
    void test_masseflytting_should_return_401_UNAUTHENTICATED() {
        var masseflyttingRequest = new MasseFlyttingRequest(
            List.of("fid1", "fid2"),
            "newOrgEnhetId",
            true
        );

        postMasseflyttingRequest(
            testToken.nomWriteGroupAndNomUiAzp(),
            masseflyttingRequest
        ).expectStatus().isForbidden();
    }

    private static EndreTilknytningtypeRequest endreTilknytningtypeRequest() {
        return EndreTilknytningtypeRequest.builder()
            .tilknytningtype(null)
            .build();
    }

    private static LeggTilOrgtilknytningRequest leggTilOrgtilknytningRequest() {
        return LeggTilOrgtilknytningRequest.builder()
            .orgEnhetId("some-new-orgenhet")
            .erDagligOppfolging(false)
            .sektor(Sektor.EKSTERN)
            .tilknytningtype(Tilknytningtype.KONSULENT)
            .gyldigFom(LocalDate.now())
            .gyldigTom(LocalDate.now().plusMonths(12))
            .primaryFysiskBrukerstedNummer("2700")
            .optionalFysiskBrukersteder(Collections.emptySet())
            .build();
    }

    private static EndreOrgtilknytningRequest endreOrgtilknytningRequest() {
        return EndreOrgtilknytningRequest.builder()
            .fid(UUID.randomUUID().toString())
            .sektor(Sektor.EKSTERN)
            .gyldigFom(LocalDate.now().minusMonths(4))
            .gyldigTom(LocalDate.now().plusMonths(1))
            .tilknytningtype(Tilknytningtype.KONSULENT)
            .erDagligOppfolging(false)
            .primaryFysiskBrukerstedNummer("2700")
            .optionalFysiskBrukersteder(Collections.emptySet())
            .build();
    }

    private static EndreOrgtilknytningRequest endreOrgtilknytningRequest(OrgtilknytningState fromOrgtilknytningState) {
        return EndreOrgtilknytningRequest.builder()
            .fid(fromOrgtilknytningState.fid())
            .gyldigFom(fromOrgtilknytningState.gyldigFom())
            .gyldigTom(fromOrgtilknytningState.gyldigTom())
            .erDagligOppfolging(fromOrgtilknytningState.erDagligOppfolging())
            .sektor(fromOrgtilknytningState.sektor())
            .tilknytningtype(fromOrgtilknytningState.orgtilknytningType())
            .primaryFysiskBrukerstedNummer(fromOrgtilknytningState.primaryFysiskBrukerstedNummer())
            .optionalFysiskBrukersteder(new HashSet<>(fromOrgtilknytningState.additionalFysiskeBrukerstederNummer()))
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

    private WebTestClient.ResponseSpec postMasseflyttingRequest(
        String accessToken,
        MasseFlyttingRequest masseflyttingRequest
    ) {
        return webTestClient
            .post()
            .uri("/app/orgtilknytninger/masseflytting")
            .headers(httpHeaders -> {
                httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                httpHeaders.setBearerAuth(accessToken);
            })
            .bodyValue(masseflyttingRequest)
            .exchange();
    }
}
