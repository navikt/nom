package no.nav.nom.apps.api.ressurs.api.app;

import lombok.SneakyThrows;
import lombok.val;
import no.nav.nom.apps.api.TestTokensGenerator;
import no.nav.nom.apps.api.authorize.AuthResolver;
import no.nav.nom.apps.api.nik.NIKClient;
import no.nav.nom.apps.api.nik.OpprettNavidentRequest;
import no.nav.nom.apps.api.nik.OpprettNavidentResponse;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.OrgtilknytningCommandService;
import no.nav.nom.apps.api.pdl.PdlClient;
import no.nav.nom.apps.api.ressurs.NomMastered;
import no.nav.nom.apps.api.ressurs.api.exceptions.InvalidSektorException;
import no.nav.nom.apps.api.ressurs.api.exceptions.NomMasteredException;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.RessursCommandService;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OppdaterFysiskeBrukerstederCommand;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OppdaterPersonaliaCommand;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OppdaterPrimaryTelefonnummerCommand;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OpphorRessursCommand;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OpprettRessursCommand;
import no.nav.nom.apps.api.rest.RestTestBase;
import no.nav.nom.apps.api.rest.common.StandardResponse;
import no.nav.nom.contract.nom.AnsettelsesType;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.Tilknytningtype;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepository;
import no.nav.nom.libs.state.ressurs.NomStateRessursListener;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.Assert;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.Collections.emptySet;
import static no.nav.nom.apps.api.common.TestData.orgEnhetState;
import static no.nav.nom.apps.api.common.TestData.orgtilknytningState;
import static no.nav.nom.apps.api.common.TestData.ressursState;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RessursControllerTest extends RestTestBase {

    @MockBean
    private NIKClient nikClient;

    @MockBean
    private PdlClient pdlClient;

    @MockBean
    private NomRemedyIntegrasjonClient nomRemedyIntegrasjonClient;

    @MockBean
    private RessursCommandService ressursCommandService;

    @MockBean
    private OrgtilknytningCommandService orgtilknytningCommandService;

    @MockBean
    private RessursStateRepository ressursStateRepository;

    @MockBean
    private OrgEnhetStateRepository orgEnhetStateRepository;

    @MockBean
    NomStateRessursListener nomStateRessursListener;

    @MockBean
    private AuthResolver authResolver;

    @MockBean
    private NomMastered nomMastered;

    @Autowired
    TestTokensGenerator testToken;

    @BeforeEach
    void setMockExpectations(){
        when(authResolver.kanRedigereRessurs(any())).thenReturn(true);
        when(authResolver.kanOppretteRessurs(any())).thenReturn(true);
    }

    @Test
    @DisplayName("Should get ressurser that satisfies filter values")
    void test_getRessurser() {

        var sluttDatoFilterValue = LocalDate.now();
        var sektorFilterValue = Sektor.EKSTERN;

        when(ressursStateRepository.getAll()).thenReturn(List.of(
            ressursState().toBuilder()
                .sluttdato(Optional.of(sluttDatoFilterValue))
                .sektor(sektorFilterValue).build(),
            ressursState(),
            ressursState()
        ));

        var ressurser = webTestClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/app/ressurser")
                .queryParam("sektor", Sektor.EKSTERN)
                .queryParam("sluttdato", LocalDate.now())
                .build()
            )
            .headers(httpHeaders -> httpHeaders.setBearerAuth(testToken.nomWriteGroupAndNomUiAzp()))
            .exchange()
            .expectStatus().isOk()
            .expectBodyList(RessursState.class)
            .returnResult().getResponseBody();

        assertThat(ressurser)
            .hasSize(1)
            .allMatch(ressursState -> ressursState.sektor().equals(sektorFilterValue))
            .allMatch(ressursState -> ressursState.sluttdato().equals(Optional.of(sluttDatoFilterValue)));


    }

    @Test
    void skal_ikke_kunne_opprette_statlig_ressurs() {
        postOpprettRessursRequest(
            testToken.nomWriteGroupAndNomUiAzp(),
            opprettRessursRequest().toBuilder().sektor(Sektor.NAV_STATLIG).build()
        )
            .expectStatus().isBadRequest()
            .expectBody(StandardResponse.class)
            .isEqualTo(StandardResponse.builder()
                .message(InvalidSektorException.actionNotAllowedForSektor(Sektor.NAV_STATLIG).getMessage())
                .build()
            );
    }

    @Test
    void skal_ikke_opprette_ressurs_med_feilformatert_etternavn() {
        postOpprettRessursRequest(
            testToken.nomWriteGroupAndNomUiAzp(),
            opprettRessursRequest().toBuilder().sektor(Sektor.EKSTERN).visningsetternavn("har mellomrom")
                .sluttDato(Optional.of(LocalDate.now().plusDays(10)))
                .startDato(LocalDate.now().plusDays(10)
                ).build()
        )
            .expectStatus().isBadRequest()
            .expectBody(String.class).value(it ->
                Assert.isTrue(it.contains("Etternavn kan ikke inneholde"),"Uventet feilmelding")
            );
    }

    @Test
    void skal_ikke_oppdatere_ressurs_med_feilformatert_etternavn() {

        when(ressursStateRepository.findByNavident(any()))
            .thenReturn(Optional.of(RessursState.builder().sektor(Sektor.EKSTERN).build()));

        postUpdateVisningsnavnRequest(
            "navident",
            testToken.nomWriteGroupAndNomUiAzp(),
            new UpdateVisningsnavnRequest("fornavn",null,"har mellomrom")
        )
            .expectStatus().isBadRequest()
            .expectBody(String.class).value(it ->
                Assert.isTrue(it.contains("Etternavn kan ikke inneholde"),"Uventet feilmelding")
            );
    }

    @Test
    @SneakyThrows
    void skal_opprette_ekstern_ressurs() {
        String personident = "12345678910";
        String navident = "x123456";
        val navn = pdlUtils.mockSinglePersonResponse();
        String primaryTelefonnummer = "12345678";

        when(nikClient.opprettNavidentOnBehalfOfUser(
            eq(OpprettNavidentRequest.builder()
                .personid(personident)
                .identPrefix(navn.getEtternavn().substring(0, 1))
                .identType("STANDARD")
                .build())
        )).thenReturn(ResponseEntity.ok(
            OpprettNavidentResponse.builder()
                .navident(navident)
                .build()));

        when(pdlClient.hentPerson(eq(personident))).thenReturn(Optional.empty());

        var request = opprettRessursRequest().toBuilder()
            .personident(personident)
            .visningsfornavn(navn.getFornavn())
            .visningsmellomnavn(navn.getMellomnavn())
            .visningsetternavn(navn.getEtternavn())
            .primaryTelefonnummer(primaryTelefonnummer)
            .sektor(Sektor.EKSTERN)
            .startDato(LocalDate.now())
            .sluttDato(Optional.of(LocalDate.now().plusMonths(1)))
            .tilknytningtype(Tilknytningtype.KONSULENT)
            .ansettelsesType(null)
            .fysiskBrukerstedNr("")
            .orgEnhetId("")
            .build();

        when(nomRemedyIntegrasjonClient.getFysiskeBrukerstedNrs(eq(List.of(request.fysiskBrukerstedNr()))))
            .thenReturn(Set.of(request.fysiskBrukerstedNr()));

        when(ressursCommandService.createRessursSync(any(OpprettRessursCommand.class)))
            .thenReturn(ressursState());

        postOpprettRessursRequest(
            testToken.nomWriteGroupAndNomUiAzp(),
            request
        )
            .expectStatus().is2xxSuccessful()
            .expectBody(OpprettRessursResponse.class);

        verify(ressursCommandService, times(1)).createRessursSync(any());
        verify(ressursCommandService, times(1)).avsluttRessursEvent(any(), any());

        verify(orgtilknytningCommandService, times(1)).opprettOrgtilknytning(any());
        verify(orgtilknytningCommandService, times(1)).avvikleOrgtilknytning(any(), any());
        verify(orgtilknytningCommandService, times(1)).endreDagligOppfolgingOrgtilknytning(any(), any());
    }

    @Test
    @SneakyThrows
    @DisplayName("Skal reaktivere sluttet ekstern ressurs")
    void skal_reaktivere_ekstern_ressurs() {
        String personident = "12345678910";
        String navident = "x123456";
        val navn = pdlUtils.mockSinglePersonResponse();
        String primaryTelefonnummer = "12345678";

        when(pdlClient.hentPerson(eq(personident))).thenReturn(Optional.empty());

        var request = opprettRessursRequest().toBuilder()
            .personident(personident)
            .visningsfornavn(navn.getFornavn())
            .visningsmellomnavn("")
            .visningsetternavn(navn.getEtternavn())
            .primaryTelefonnummer(primaryTelefonnummer)
            .sektor(Sektor.EKSTERN)
            .startDato(LocalDate.now())
            .sluttDato(Optional.of(LocalDate.now().plusMonths(1)))
            .tilknytningtype(Tilknytningtype.KONSULENT)
            .ansettelsesType(null)
            .fysiskBrukerstedNr("")
            .orgEnhetId("")
            .build();

        RessursState existingRessurs = RessursState.builder()
            .fid("Ressurs-XvSgpsGRF")
            .personident(request.personident())
            .navident(navident)
            .sektor(Sektor.NAV_STATLIG)
            .visningsnavn(
                new Visningsnavn(
                    request.visningsfornavn(),
                    Optional.of("YEP"),
                    request.visningsetternavn()
            ))
            .primaryTelefonnummer(Optional.of("87654321"))
            .sluttdato(Optional.of(LocalDate.MIN))
            .startdato(Optional.of(LocalDate.MIN.plusYears(1)))
            .primaryFysiskBrukersted(Optional.of("2700"))
            .fysiskeBrukersteder(List.of())
            .build();

        when(ressursStateRepository.findByPersonident(request.personident())).thenReturn(Optional.of(existingRessurs));

        when(nomRemedyIntegrasjonClient.getFysiskeBrukerstedNrs(eq(List.of(request.fysiskBrukerstedNr()))))
            .thenReturn(Set.of(request.fysiskBrukerstedNr()));

        postOpprettRessursRequest(
            testToken.nomWriteGroupAndNomUiAzp(),
            request
        )
            .expectStatus().is2xxSuccessful()
            .expectBody(OpprettRessursResponse.class);

        verify(ressursCommandService, times(1)).avsluttRessursEvent(any(), any());
        verify(ressursCommandService, times(1)).oppdaterPersonaliaEvent(any(), any());
        verify(ressursCommandService, times(1)).oppdatertPrimaryTelefonnummer(any(), any());
        verify(ressursCommandService, times(1)).oppdaterSektorEvent(any(), any());
        verify(ressursCommandService, times(1)).oppdaterSektorEvent(any(), any());

        verify(orgtilknytningCommandService, times(1)).opprettOrgtilknytning(any());
        verify(orgtilknytningCommandService, times(1)).avvikleOrgtilknytning(any(), any());
        verify(orgtilknytningCommandService, times(1)).endreDagligOppfolgingOrgtilknytning(any(), any());
    }

    @Test
    @SneakyThrows
    void skal_ikke_opprette_ekstern_ressurs_uten_sluttDato() {

        postOpprettRessursRequest(
            testToken.nomWriteGroupAndNomUiAzp(),
            opprettRessursRequest().toBuilder()
                .sluttDato(Optional.empty()).build()
        )
            .expectStatus().isBadRequest()
            .expectBody()
            .jsonPath("message")
            .isEqualTo("Sluttdato må oppgis for opprettelse av ekstern ressurs");


        verify(ressursCommandService, times(0)).createRessursSync(any());
        verify(ressursCommandService, times(0)).avsluttRessursEvent(any(), any());

        verify(orgtilknytningCommandService, times(0)).opprettOrgtilknytning(any());
        verify(orgtilknytningCommandService, times(0)).avvikleOrgtilknytning(any(), any());
        verify(orgtilknytningCommandService, times(0)).endreDagligOppfolgingOrgtilknytning(any(), any());
    }

    @Test
    @SneakyThrows
    void skal_opprette_kommunal_ressurs() {
        String personident = "12345678910";
        String navident = "x123456";
        val navn = pdlUtils.mockSinglePersonResponse();
        String primaryTelefonnummer = "12345678";

        when(nikClient.opprettNavidentOnBehalfOfUser(
            eq(OpprettNavidentRequest.builder()
                .personid(personident)
                .identPrefix(navn.getEtternavn().substring(0, 1))
                .identType("STANDARD")
                .build())
        )).thenReturn(ResponseEntity.ok(
            OpprettNavidentResponse.builder()
                .navident(navident)
                .build()));

        when(pdlClient.hentPerson(eq(personident))).thenReturn(Optional.empty());

        var request = opprettRessursRequest().toBuilder()
            .personident(personident)
            .visningsfornavn(navn.getFornavn())
            .visningsmellomnavn(navn.getMellomnavn())
            .visningsetternavn(navn.getEtternavn())
            .primaryTelefonnummer(primaryTelefonnummer)
            .sektor(Sektor.NAV_KOMMUNAL)
            .startDato(LocalDate.now())
            .sluttDato(Optional.empty())
            .tilknytningtype(Tilknytningtype.INGEN)
            .ansettelsesType(null)
            .orgEnhetId("orgEnhet123")
            .fysiskBrukerstedNr("")
            .build();

        var orgEnhet = orgEnhetState().toBuilder()
            .agressoId(Optional.of("123456"))
            .agressoOrgNivaa(Optional.of("4"))
            .navn("NAV")
            .build();


        when(nomRemedyIntegrasjonClient.getFysiskeBrukerstedNrs(eq(List.of(request.fysiskBrukerstedNr()))))
            .thenReturn(Set.of(request.fysiskBrukerstedNr()));

        when(ressursCommandService.createRessursSync(any(OpprettRessursCommand.class)))
            .thenReturn(ressursState());

        when(orgEnhetStateRepository.findById(request.orgEnhetId())).thenReturn(Optional.of(orgEnhet));

        postOpprettRessursRequest(
            testToken.nomWriteGroupAndNomUiAzp(),
            request
        )
            .expectStatus().is2xxSuccessful()
            .expectBody(OpprettRessursResponse.class);

        verify(ressursCommandService, times(1)).createRessursSync(any());

        verify(orgtilknytningCommandService, times(1)).opprettOrgtilknytning(any());
        verify(orgtilknytningCommandService, times(1)).endreDagligOppfolgingOrgtilknytning(any(), any());
    }

    @Test
    void opprette_ressurs_skal_gi_forbidden_nar_bruker_ikke_er_leder_for_enhet() {
        when(authResolver.kanOppretteRessurs(any())).thenReturn(false);

        postOpprettRessursRequest(
            testToken.nomWriteGroupAndNomUiAzp(),
            opprettRessursRequest().toBuilder()
                .sluttDato(Optional.empty()).build()
        )
            .expectStatus().isForbidden();
    }

    @Test
    void skal_kunne_endre_navn_for_eksterne() {
        var existingRessurs = ressursState().toBuilder()
            .sektor(Sektor.EKSTERN)
            .build();

        when(ressursStateRepository.findByNavident(existingRessurs.navident()))
            .thenReturn(Optional.of(existingRessurs));

        var updateVisningsnavnRequest = new UpdateVisningsnavnRequest(
            "Erik", Optional.of("Ten"), "Haag"
        );

        postUpdateVisningsnavnRequest(
            existingRessurs.navident(),
            testToken.nomWriteGroupAndNomUiAzp(),
            updateVisningsnavnRequest
        )
            .expectStatus().isOk();

        var oppdaterPersonaliaCommandArgumentCaptor = ArgumentCaptor.forClass(OppdaterPersonaliaCommand.class);
        verify(ressursCommandService, times(1))
            .oppdaterPersonaliaEvent(oppdaterPersonaliaCommandArgumentCaptor.capture(), any());

        assertThat(oppdaterPersonaliaCommandArgumentCaptor.getValue())
            .matches(oppdaterPersonaliaCommand -> oppdaterPersonaliaCommand.fid().equals(existingRessurs.fid()))
            .matches(oppdaterPersonaliaCommand -> oppdaterPersonaliaCommand.visningsnavn().equals(
                new Visningsnavn(updateVisningsnavnRequest.fornavn(), updateVisningsnavnRequest.mellomnavn(), updateVisningsnavnRequest.etternavn())
            ));
    }

    @Test
    void endre_navn_for_eksterne_skal_gi_forbidden_nar_bruker_ikke_er_leder_for_ressurs() {
        when(authResolver.kanRedigereRessurs(any())).thenReturn(false);

        var existingRessurs = ressursState().toBuilder()
            .sektor(Sektor.EKSTERN)
            .build();

        var updateVisningsnavnRequest = new UpdateVisningsnavnRequest(
            "Erik", Optional.of("Ten"), "Haag"
        );

        postUpdateVisningsnavnRequest(
            existingRessurs.navident(),
            testToken.nomWriteGroupAndNomUiAzp(),
            updateVisningsnavnRequest
        )
            .expectStatus().isForbidden();
    }

    @Test
    void skal_kunne_avvikle_ressurs() {
        var ressursSomSkalAvvikles = ressursState().toBuilder()
            .sektor(Sektor.EKSTERN).build();

        when(ressursStateRepository.findByFid(ressursSomSkalAvvikles.fid()))
            .thenReturn(Optional.of(ressursSomSkalAvvikles));

        var existingOrgtilknytninger = List.of(
            orgtilknytningState().toBuilder().navident(ressursSomSkalAvvikles.navident()).build(),
            orgtilknytningState().toBuilder()
                .navident(ressursSomSkalAvvikles.navident())
                .gyldigTom(Optional.of(LocalDate.now().minusDays(1)))
                .build()
        );

        saveOrgtilknytninger(existingOrgtilknytninger);

        var request = new AvvikleRessursRequest(
            LocalDate.now().plusMonths(1)
        );

        webTestClient.post()
            .uri(uriBuilder -> uriBuilder
                .path("/app/ressurser/{fid}/avvikle")
                .build(ressursSomSkalAvvikles.fid())
            )
            .headers(httpHeaders -> {
                httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                httpHeaders.setBearerAuth(testToken.nomWriteGroupAndNomUiAzp());
            })
            .bodyValue(request)
            .exchange()
            .expectStatus().isOk();

        var opphorRessursCommandArgumentCaptor = ArgumentCaptor.forClass(OpphorRessursCommand.class);

        verify(ressursCommandService, times(1)).avsluttRessursEvent(opphorRessursCommandArgumentCaptor.capture(), any());
        verify(orgtilknytningCommandService, times(1)).avvikleOrgtilknytning(any(), any());

        assertThat(opphorRessursCommandArgumentCaptor.getValue())
            .matches(opphorRessursCommand -> opphorRessursCommand.fid().equals(ressursSomSkalAvvikles.fid()));
    }

    @Test
    void avvikle_ressurs_skal_gi_forbidden_nar_bruker_ikke_er_leder_for_ressurs() {
        when(authResolver.kanRedigereRessurs(any())).thenReturn(false);

        var ressursSomSkalAvvikles = ressursState().toBuilder()
            .sektor(Sektor.EKSTERN).build();

        when(ressursStateRepository.findByFid(ressursSomSkalAvvikles.fid()))
            .thenReturn(Optional.of(ressursSomSkalAvvikles));

        var existingOrgtilknytninger = List.of(
            orgtilknytningState().toBuilder().navident(ressursSomSkalAvvikles.navident()).build(),
            orgtilknytningState().toBuilder()
                .navident(ressursSomSkalAvvikles.navident())
                .gyldigTom(Optional.of(LocalDate.now().minusDays(1)))
                .build()
        );

        saveOrgtilknytninger(existingOrgtilknytninger);

        var request = new AvvikleRessursRequest(
            LocalDate.now().plusMonths(1)
        );

        webTestClient.post()
            .uri(uriBuilder -> uriBuilder
                .path("/app/ressurser/{fid}/avvikle")
                .build(ressursSomSkalAvvikles.fid())
            )
            .headers(httpHeaders -> {
                httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                httpHeaders.setBearerAuth(testToken.nomWriteGroupAndNomUiAzp());
            })
            .bodyValue(request)
            .exchange()
            .expectStatus().isForbidden();
    }

    @Test
    void skal_ikke_kunne_endre_navn_for_statlig() {
        var existingRessurs = ressursState().toBuilder()
                .sektor(Sektor.NAV_STATLIG)
                    .build();

        when(ressursStateRepository.findByNavident(existingRessurs.navident()))
            .thenReturn(Optional.of(existingRessurs));


        var updateVisningsnavnRequest = new UpdateVisningsnavnRequest(
            "Erik",
            Optional.of("Ten"),
            "Haag"
        );

        postUpdateVisningsnavnRequest(
            existingRessurs.navident(),
            testToken.nomWriteGroupAndNomUiAzp(),
            updateVisningsnavnRequest
        )
            .expectStatus().isBadRequest();

        verify(ressursCommandService, times(0)).oppdaterPersonaliaEvent(any(), any());
    }

    @Test
    @DisplayName("Skal kunne endre primarytelefonnnummer på ekstern ressurs")
    void skal_kunne_endre_telefonnummer_ekstern() {
        var existingRessurs = ressursState().toBuilder()
            .sektor(Sektor.EKSTERN)
            .build();

        when(ressursStateRepository.findByNavident(existingRessurs.navident()))
            .thenReturn(Optional.of(existingRessurs));

        when(authResolver.kanSelvbetjene(anyString())).thenReturn(false);
        when(nomMastered.isMastered(anyString())).thenReturn(true);

        var updatePrimaryTelefonnummer = new UpdatePrimarytelefonnummer(
            "12345678"
        );

        postUpdatePrimaryTelefonnummerRequest(
            existingRessurs.navident(),
            testToken.nomWriteGroupAndNomUiAzp(),
            updatePrimaryTelefonnummer
        )
            .expectStatus().isOk();

        var oppdaterMobilnummerCommandArgumentCaptor = ArgumentCaptor.forClass(OppdaterPrimaryTelefonnummerCommand.class);
        verify(ressursCommandService, times(1))
            .oppdatertPrimaryTelefonnummer(oppdaterMobilnummerCommandArgumentCaptor.capture(), any());

        assertThat(oppdaterMobilnummerCommandArgumentCaptor.getValue())
            .matches(oppdaterPersonaliaCommand -> oppdaterPersonaliaCommand.fid().equals(existingRessurs.fid()))
            .matches(oppdaterPersonaliaCommand -> oppdaterPersonaliaCommand.primaryTelefonnummer().equals(updatePrimaryTelefonnummer.nummer()));
    }

    @ParameterizedTest(name = "GET {0}")
    @DisplayName("Skal kunne endre primarytelefonnnummer på seg selv, kommunal og ekstern ressurs")
    @MethodSource("getSektors")
    void skal_kunne_endre_telefonnummer_selv(Sektor sektor) {
        var existingRessurs = ressursState().toBuilder()
            .sektor(sektor)
            .build();

        when(ressursStateRepository.findByNavident(existingRessurs.navident()))
            .thenReturn(Optional.of(existingRessurs));

        when(authResolver.kanSelvbetjene(anyString())).thenReturn(true);
        when(authResolver.kanRedigereRessurs(anyString())).thenReturn(false);
        when(nomMastered.isMastered(anyString())).thenReturn(true);

        var updatePrimaryTelefonnummer = new UpdatePrimarytelefonnummer(
            "12345678"
        );

        postUpdatePrimaryTelefonnummerRequest(
            existingRessurs.navident(),
            testToken.nomUiClient(existingRessurs.navident()),
            updatePrimaryTelefonnummer
        )
            .expectStatus().isOk();

        var oppdaterMobilnummerCommandArgumentCaptor = ArgumentCaptor.forClass(OppdaterPrimaryTelefonnummerCommand.class);
        verify(ressursCommandService, times(1))
            .oppdatertPrimaryTelefonnummer(oppdaterMobilnummerCommandArgumentCaptor.capture(), any());

        assertThat(oppdaterMobilnummerCommandArgumentCaptor.getValue())
            .matches(oppdaterPersonaliaCommand -> oppdaterPersonaliaCommand.fid().equals(existingRessurs.fid()))
            .matches(oppdaterPersonaliaCommand -> oppdaterPersonaliaCommand.primaryTelefonnummer().equals(updatePrimaryTelefonnummer.nummer()));
    }

    @Test
    @DisplayName("Skal ikke kunne endre primarytelefonnnummer på seg selv, statlig ressurs")
    void skal_ikke_kunne_endre_telefonnummer_selv() {
        var existingRessurs = ressursState().toBuilder()
            .sektor(Sektor.NAV_STATLIG)
            .build();

        when(ressursStateRepository.findByNavident(existingRessurs.navident()))
            .thenReturn(Optional.of(existingRessurs));

        when(authResolver.kanSelvbetjene(anyString())).thenReturn(true);
        when(authResolver.kanRedigereRessurs(anyString())).thenReturn(false);
        when(nomMastered.isMastered(anyString())).thenReturn(false);

        var updatePrimaryTelefonnummer = new UpdatePrimarytelefonnummer(
            "12345678"
        );

        postUpdatePrimaryTelefonnummerRequest(
            existingRessurs.navident(),
            testToken.nomUiClient(existingRessurs.navident()),
            updatePrimaryTelefonnummer
        )
            .expectStatus().is4xxClientError();

        var oppdaterMobilnummerCommandArgumentCaptor = ArgumentCaptor.forClass(OppdaterPrimaryTelefonnummerCommand.class);
        verify(ressursCommandService, times(0))
            .oppdatertPrimaryTelefonnummer(oppdaterMobilnummerCommandArgumentCaptor.capture(), any());

        assertThrows(InvalidSektorException.class, () -> {
            throw InvalidSektorException.actionNotAllowedForSektor(Sektor.NAV_STATLIG);
        });
    }

    @Test
    @DisplayName("Skal ikke kunne endre primarytelefonnnummer på seg selv, ikke nom-mastered")
    void skal_ikke_kunne_endre_telefonnummer_selv_2() {
        var existingRessurs = ressursState().toBuilder()
            .sektor(Sektor.NAV_KOMMUNAL)
            .build();

        when(ressursStateRepository.findByNavident(existingRessurs.navident()))
            .thenReturn(Optional.of(existingRessurs));

        when(authResolver.kanSelvbetjene(anyString())).thenReturn(true);
        when(authResolver.kanRedigereRessurs(anyString())).thenReturn(false);
        when(nomMastered.isMastered(anyString())).thenReturn(false);

        var updatePrimaryTelefonnummer = new UpdatePrimarytelefonnummer(
            "12345678"
        );

        postUpdatePrimaryTelefonnummerRequest(
            existingRessurs.navident(),
            testToken.nomUiClient(existingRessurs.navident()),
            updatePrimaryTelefonnummer
        )
            .expectStatus().is4xxClientError();

        var oppdaterMobilnummerCommandArgumentCaptor = ArgumentCaptor.forClass(OppdaterPrimaryTelefonnummerCommand.class);
        verify(ressursCommandService, times(0))
            .oppdatertPrimaryTelefonnummer(oppdaterMobilnummerCommandArgumentCaptor.capture(), any());

        assertThrows(NomMasteredException.class, () -> {
            throw NomMasteredException.actionNotAllowedForNonNomMastered();
        });
    }


    static Stream<Arguments> getSektors(){
        return Stream.of(
            Arguments.of(Sektor.NAV_KOMMUNAL),
            Arguments.of(Sektor.EKSTERN)
        );
    }



    @ParameterizedTest(name = "GET {0}")
    @DisplayName("Skjuler sensitiv data for vanlige brukere")
    @MethodSource("getRessursPathsTestInput")
    void skalSkjuleSensitivDataForVanligeBrukere(String endpointPath) {

        var personident = "01010154321";

        var ressursState = ressursState().toBuilder()
            .personident(personident)
            .primaryTelefonnummer(Optional.of("12345678"))
            .primaryFysiskBrukersted(Optional.of("1234"))
            .fysiskeBrukersteder(List.of("1234"))
            .build();

        when(authResolver.kanRedigereRessurs(anyString())).thenReturn(false);

        when(ressursStateRepository.findByNavident(anyString()))
            .thenReturn(Optional.of(ressursState));

        when(ressursStateRepository.findByFid(anyString()))
            .thenReturn(Optional.of(ressursState));

        webTestClient.get()
            .uri(endpointPath)
            .headers(httpHeaders -> httpHeaders.setBearerAuth(testToken.nomWriteGroupAndNomUiAzp()))
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("personident")
            .isEqualTo(personident.replaceFirst(".{5}$", "*****"))
            .jsonPath("primaryFysiskBrukersted")
            .isEqualTo(null)
            .jsonPath("fysiskeBrukersteder")
            .isEmpty();

    }

    @ParameterizedTest(name = "GET {0}")
    @DisplayName("Skjuler ikke sensitiv data på seg selv")
    @MethodSource("getRessursPathsTestInput")
    void skalIkkeSkjuleSensitivDataForOppslagPaaSegSelv(String endpointPath) {

        var personident = "01010154321";

        var ressursState = ressursState().toBuilder()
            .personident(personident)
            .navident(TestTokensGenerator.LOGGED_IN_USER_NAVIDENT)
            .primaryTelefonnummer(Optional.of("12345678"))
            .primaryFysiskBrukersted(Optional.of("1234"))
            .fysiskeBrukersteder(new ArrayList<>(List.of("1234")))
            .build();

        when(authResolver.kanRedigereRessurs(anyString())).thenReturn(false);

        when(ressursStateRepository.findByNavident(anyString()))
            .thenReturn(Optional.of(ressursState));

        when(ressursStateRepository.findByFid(anyString()))
            .thenReturn(Optional.of(ressursState));

        webTestClient.get()
            .uri(endpointPath)
            .headers(httpHeaders -> httpHeaders.setBearerAuth(testToken.nomWriteGroupAndNomUiAzp()))
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("personident")
            .isEqualTo(personident)
            .jsonPath("primaryFysiskBrukersted")
            .isEqualTo(ressursState.primaryFysiskBrukersted().get())
            .jsonPath("fysiskeBrukersteder")
            .isEqualTo(ressursState.fysiskeBrukersteder());
    }

    @ParameterizedTest(name = "GET {0}")
    @DisplayName("Skjuler ikke brukersted, men personident for leder til ressurs")
    @MethodSource("getRessursPathsTestInput")
    void skalSkjulePersonIdentMenIkkeFysiskbrukerstedForLeder(String endpointPath) {

        var personident = "01010154321";

        var ressursState = ressursState().toBuilder()
            .personident(personident)
            .primaryTelefonnummer(Optional.of("12345678"))
            .primaryFysiskBrukersted(Optional.of("1234"))
            .fysiskeBrukersteder(new ArrayList<>(List.of("1234")))
            .build();

        when(authResolver.kanRedigereRessurs(anyString())).thenReturn(true);

        when(ressursStateRepository.findByNavident(anyString()))
            .thenReturn(Optional.of(ressursState));

        when(ressursStateRepository.findByFid(anyString()))
            .thenReturn(Optional.of(ressursState));

        webTestClient.get()
            .uri(endpointPath)
            .headers(httpHeaders -> httpHeaders.setBearerAuth(testToken.nomWriteGroupAndNomUiAzp()))
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("personident")
            .isEqualTo(personident.replaceFirst(".{5}$", "*****"))
            .jsonPath("primaryFysiskBrukersted")
            .isEqualTo(ressursState.primaryFysiskBrukersted().get())
            .jsonPath("fysiskeBrukersteder")
            .isEqualTo(ressursState.fysiskeBrukersteder());
    }


    @ParameterizedTest(name = "GET {0}")
    @DisplayName("Skjuler ikke sensitiv data for admin brukere")
    @MethodSource("getRessursPathsTestInput")
    void skalIkkeSkjuleSensitivDataForAdmin(String endpointPath) {

        var personident = "01010154321";

        var ressursState = ressursState().toBuilder()
            .primaryFysiskBrukersted(Optional.of("1234"))
            .fysiskeBrukersteder(List.of("1234"))
            .personident(personident).build();

        when(ressursStateRepository.findByNavident(anyString()))
            .thenReturn(Optional.of(ressursState));

        when(ressursStateRepository.findByFid(anyString()))
            .thenReturn(Optional.of(ressursState));

        webTestClient.get()
            .uri(endpointPath)
            .headers(httpHeaders -> httpHeaders.setBearerAuth(testToken.nomAdminUser()))
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("personident")
            .isEqualTo(personident);

    }

    @ParameterizedTest(name = "GET {0}")
    @DisplayName("Skjuler ikke sensitiv data for nom-remedy-integrasjon")
    @MethodSource("getRessursPathsTestInput")
    void skalIkkeSkjuleSensitivDataForNomRemedyIntegrasjon(String endpointPath) {

        var personident = "01010154321";

        var ressursState = ressursState().toBuilder()
            .primaryFysiskBrukersted(Optional.of("1234"))
            .fysiskeBrukersteder(List.of("1234"))
            .personident(personident).build();

        when(ressursStateRepository.findByNavident(anyString()))
            .thenReturn(Optional.of(ressursState));

        when(ressursStateRepository.findByFid(anyString()))
            .thenReturn(Optional.of(ressursState));

        webTestClient.get()
            .uri(endpointPath)
            .headers(httpHeaders -> httpHeaders.setBearerAuth(testToken.nomRemedyIntegrasjonAzp()))
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("personident")
            .isEqualTo(personident);

    }

    static Stream<Arguments> getRessursPathsTestInput(){
        return Stream.of(
            Arguments.of("/app/ressurser/byNavident/whatever"),
            Arguments.of("/app/ressurser/byFid/whatever")
        );
    }


    @Test
    void skal_oppdatere_fysiske_brukersteder(){
        when(unleash.isEnabled("nom.ressurs.opprett_statlig")).thenReturn(false);

        var existingRessurs = ressursState()
            .toBuilder()
            .fysiskeBrukersteder(null)
            .sektor(Sektor.NAV_KOMMUNAL)
            .build();

        var updateFysiskeBrukerstederRequest = UpdateFysiskeBrukerstederRequest.builder()
            .primaryFysiskBrukersted("fyrstikken-id")
            .fysiskeBrukersteder(Set.of("fyrstikken-id"))
            .build();

        when(ressursStateRepository.findByFid(existingRessurs.fid())).thenReturn(Optional.of(existingRessurs));
        when(nomRemedyIntegrasjonClient.getFysiskeBrukerstedNrs(argThat(l ->
                new HashSet<>(l).equals(updateFysiskeBrukerstederRequest.fysiskeBrukersteder())
            )
        )).thenReturn(updateFysiskeBrukerstederRequest.fysiskeBrukersteder());

        postUpdateFysiskeBrukerstederRequest(existingRessurs.fid(), testToken.nomWriteGroupAndNomUiAzp(), updateFysiskeBrukerstederRequest)
            .expectStatus().isOk();

        var oppdaterFysiskeBrukerstederCommandArgumentCaptor = ArgumentCaptor.forClass(OppdaterFysiskeBrukerstederCommand.class);
        verify(ressursCommandService, times(1))
            .oppdaterFysiskeBrukerstederCommand(oppdaterFysiskeBrukerstederCommandArgumentCaptor.capture(), any());

        assertThat(oppdaterFysiskeBrukerstederCommandArgumentCaptor.getValue())
            .usingRecursiveComparison()
            .isEqualTo(OppdaterFysiskeBrukerstederCommand.builder()
                .fid(existingRessurs.fid())
                .primaryFysiskBrukersted(updateFysiskeBrukerstederRequest.primaryFysiskBrukersted())
                .fysiskeBrukersteder(updateFysiskeBrukerstederRequest.fysiskeBrukersteder())
                .build()
            );
    }

    @Test
    void oppdatere_fysiske_brukersteder_skal_gi_forbidden_nar_bruker_ikke_er_leder_for_ressurs() {
        when(authResolver.kanRedigereRessurs(any())).thenReturn(false);

        var existingRessurs = ressursState()
            .toBuilder()
            .fysiskeBrukersteder(null)
            .build();

        var updateFysiskeBrukerstederRequest = UpdateFysiskeBrukerstederRequest.builder()
            .primaryFysiskBrukersted("fyrstikken-id")
            .fysiskeBrukersteder(Set.of("fyrstikken-id"))
            .build();

        when(ressursStateRepository.findByFid(existingRessurs.fid())).thenReturn(Optional.of(existingRessurs));
        when(nomRemedyIntegrasjonClient.getFysiskeBrukerstedNrs(argThat(l ->
                new HashSet<>(l).equals(updateFysiskeBrukerstederRequest.fysiskeBrukersteder())
            )
        )).thenReturn(updateFysiskeBrukerstederRequest.fysiskeBrukersteder());

        postUpdateFysiskeBrukerstederRequest(existingRessurs.fid(), testToken.nomWriteGroupAndNomUiAzp(), updateFysiskeBrukerstederRequest)
            .expectStatus().isForbidden();
    }

    @ParameterizedTest(name = "updateFysiskeBrukersteder should give 400 with message '{1}'")
    @MethodSource("testUpdateFysiskeBrukerstederBadRequestInput")
    void testUpdateFysiskeBrukerstederBadRequest(UpdateFysiskeBrukerstederRequest updateFysiskeBrukerstederRequest, String expectedMessage){
        var existingRessurs = ressursState()
            .toBuilder()
            .fysiskeBrukersteder(null)
            .build();

        when(ressursStateRepository.findByFid(existingRessurs.fid())).thenReturn(Optional.of(existingRessurs));

        postUpdateFysiskeBrukerstederRequest(existingRessurs.fid(), testToken.nomWriteGroupAndNomUiAzp(), updateFysiskeBrukerstederRequest)
            .expectStatus().isBadRequest()
            .expectBody(StandardResponse.class)
            .isEqualTo(StandardResponse.builder().message(expectedMessage).build());
    }

    private static Stream<Arguments> testUpdateFysiskeBrukerstederBadRequestInput() {
        return Stream.of(
            Arguments.of(
                UpdateFysiskeBrukerstederRequest.builder()
                    .primaryFysiskBrukersted(null)
                    .fysiskeBrukersteder(emptySet())
                    .build(),
                "primaryFysiskBrukersted cannot be blank or null"
                ),
            Arguments.of(
                UpdateFysiskeBrukerstederRequest.builder()
                    .primaryFysiskBrukersted("oslo")
                    .fysiskeBrukersteder(Set.of("bergen"))
                    .build(),
                "Value for primaryFysiskBrukersted must be included in fysiske brukersteder"
            )
        );
    }

    private static OpprettRessursRequest opprettRessursRequest() {
        return OpprettRessursRequest.builder()
            .personident("whatever")
            .visningsfornavn("whatever")
            .visningsmellomnavn("whatever")
            .visningsetternavn("whatever")
            .primaryTelefonnummer("whatever")
            .sektor(Sektor.EKSTERN)
            .orgEnhetId("whatever")
            .ansettelsesType(AnsettelsesType.FAST)
            .fysiskBrukerstedNr("whatever")
            .tilknytningtype(Tilknytningtype.KONSULENT)
            .build();
    }

    private WebTestClient.ResponseSpec postUpdateVisningsnavnRequest(
        String navident, String accessToken, UpdateVisningsnavnRequest updateVisningsnavnRequest
    ) {
        return webTestClient.post()
            .uri(uriBuilder -> uriBuilder
                .path("/app/ressurser/{navident}/navn")
                .build(navident)
            )
            .headers(httpHeaders -> {
                httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                httpHeaders.setBearerAuth(accessToken);
            })
            .bodyValue(updateVisningsnavnRequest)
            .exchange();
    }

    private WebTestClient.ResponseSpec postUpdatePrimaryTelefonnummerRequest(
        String navident, String accessToken, UpdatePrimarytelefonnummer updatePrimarytelefonnummer
    ) {
        return webTestClient.post()
            .uri(uriBuilder -> uriBuilder
                .path("/app/ressurser/{navident}/telefonnummer")
                .build(navident)
            )
            .headers(httpHeaders -> {
                httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                httpHeaders.setBearerAuth(accessToken);
            })
            .bodyValue(updatePrimarytelefonnummer)
            .exchange();
    }

    private WebTestClient.ResponseSpec postOpprettRessursRequest(String accessToken, OpprettRessursRequest opprettRessursRequest) {
        return webTestClient.post()
            .uri("/app/ressurser")
            .headers(httpHeaders -> {
                httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                httpHeaders.setBearerAuth(accessToken);
            })
            .bodyValue(opprettRessursRequest)
            .exchange();
    }

    private WebTestClient.ResponseSpec postUpdateFysiskeBrukerstederRequest(
        String fid, String accessToken, UpdateFysiskeBrukerstederRequest updateFysiskeBrukerstederRequest
    ) {
        return webTestClient.post()
            .uri(uriBuilder ->
                uriBuilder.path("/app/ressurser/{fid}/fysiskebrukersteder").build(fid)
            )
            .headers(httpHeaders -> {
                httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                httpHeaders.setBearerAuth(accessToken);
            })
            .bodyValue(updateFysiskeBrukerstederRequest)
            .exchange();
    }
}
