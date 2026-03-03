package no.nav.nom.apps.api.orgenhet.api;

import no.nav.nom.apps.api.TestTokensGenerator;
import no.nav.nom.apps.api.organisering.eventsourcing.producer.OrganiseringEventCommandService;
import no.nav.nom.apps.api.organisering.eventsourcing.producer.commands.OpprettOrganiseringCmd;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.orgenhet.api.app.SetOrgEnhetsTypeRequest;
import no.nav.nom.apps.api.orgenhet.api.app.dto.AvvikleOrgEnhetRequest;
import no.nav.nom.apps.api.orgenhet.api.app.dto.OppdaterOrgEnhetRequest;
import no.nav.nom.apps.api.orgenhet.api.app.dto.OpprettOrgEnhetRequest;
import no.nav.nom.apps.api.orgenhet.api.app.dto.OpprettOrgEnhetResponse;
import no.nav.nom.apps.api.orgenhet.api.app.dto.OrgEnhetMedLedernavn;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.OrgEnhetCommandService;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.commands.EndreOrgEnhetsType;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.commands.OpprettOrgEnhet;
import no.nav.nom.apps.api.orgenhet.implementations.OrgEnhet;
import no.nav.nom.apps.api.orgenhetleder.eventsourcing.producer.OrgEnhetLederCommandService;
import no.nav.nom.apps.api.orgenhetleder.eventsourcing.producer.commands.OppdaterOrgEnhetLederStartdato;
import no.nav.nom.apps.api.orgenhetleder.eventsourcing.producer.commands.OpprettOrgEnhetLeder;
import no.nav.nom.apps.api.ressurs.RessursService;
import no.nav.nom.apps.api.rest.RestTestBase;
import no.nav.nom.apps.api.rest.common.StandardResponse;
import no.nav.nom.contract.nom.OrgEnhetsType;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.Tilknytningtype;
import no.nav.nom.contract.nom.state.OrgEnhetLederState;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.contract.nom.state.OrganiseringState;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.contract.nom.state.RessursState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static no.nav.nom.apps.api.common.TestData.orgEnhetState;
import static no.nav.nom.apps.api.common.TestData.organiseringState;
import static no.nav.nom.apps.api.common.TestData.ressursState;
import static no.nav.nom.apps.api.orgenhet.OrgenhetTestUtils.orgEnhetStateBuilderWithDefaultValues;
import static no.nav.nom.apps.api.orgenhet.OrgenhetTestUtils.orgEnhetStateToOrganiseringState;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class OrgEnhetControllerTest extends RestTestBase {

    @MockitoBean
    OrgEnhetCommandService orgEnhetCommandService;

    @Autowired
    TestTokensGenerator testToken;

    @Captor
    ArgumentCaptor<EndreOrgEnhetsType> endreOrgEnhetsTypeArgumentCaptor;

    @MockitoSpyBean
    private OrgenhetService orgenhetService;
    @MockitoSpyBean
    private RessursService ressursService;
    @MockitoBean
    private OrganiseringEventCommandService organiseringEventCommandService;
    @MockitoBean
    private OrgEnhetLederCommandService orgEnhetLederCommandService;


    @BeforeEach
    public void init() {
        orgEnhetStateRepository.clear();
    }

    @Test
    public void should_set_new_orgEnhetsType() {

        var existingOrgEnhet = orgEnhetStateBuilderWithDefaultValues().toBuilder()
                .orgEnhetsType(null)
                .build();

        var setOrgEnhetsTypeRequest = setOrgEnhetsTypeRequest().toBuilder()
                .nomId(existingOrgEnhet.id())
                .orgEnhetsType(OrgEnhetsType.ARBEIDSLIVSSENTER)
                .build();

        orgEnhetStateRepository.update(existingOrgEnhet);

        postOrgenhetType(testToken.nomAdminUser(), setOrgEnhetsTypeRequest)
                .expectStatus().isOk();

        verify(orgEnhetCommandService, times(1))
                .endreOrgEnhetsType(eq(existingOrgEnhet.fid()), endreOrgEnhetsTypeArgumentCaptor.capture());

        assertEquals(endreOrgEnhetsTypeArgumentCaptor.getValue().orgEnhetsType(), setOrgEnhetsTypeRequest.getOrgEnhetsType());
    }

    @Test
    public void should_not_send_EndreOrgEnhetsType_event_when_existing_value_is_same_as_new() {

        var existingOrgenhetsType = OrgEnhetsType.FYLKE;

        var existingOrgEnhet = orgEnhetStateBuilderWithDefaultValues().toBuilder()
                .orgEnhetsType(existingOrgenhetsType)
                .build();

        orgEnhetStateRepository.update(existingOrgEnhet);

        var requestBody = setOrgEnhetsTypeRequest().toBuilder()
                .nomId(existingOrgEnhet.id())
                .orgEnhetsType(existingOrgenhetsType)
                .build();

        postOrgenhetType(testToken.nomAdminUser(), requestBody)
                .expectStatus().isOk();

        verify(orgEnhetCommandService, times(0)).endreOrgEnhetsType(any(), any());
    }

    @Test
    public void should_return_404_when_org_enhet_not_found() {

        postOrgenhetType(testToken.nomAdminUser(), setOrgEnhetsTypeRequest())
                .expectStatus().isNotFound();

        verify(orgEnhetCommandService, times(0))
                .endreOrgEnhetsType(any(), any());

    }

    @Test
    public void should_return_403_for_users_without_access() {
        postOrgenhetType(testToken.noScopesUser(), setOrgEnhetsTypeRequest())
                .expectStatus().isForbidden();
    }

    @Test
    public void should_return_orgEnheter_that_has_parent_as_orgEnheter_with_manager_navIdenter() {
        String orgEnhetId = "ab123c";
        String orgEnhetOverId = "de456f";

        OrgEnhetState orgEnhet = orgEnhetStateBuilderWithDefaultValues().toBuilder()
                .id(orgEnhetId)
                .gyldigFom(LocalDate.now().minusMonths(6))
                .agressoOrgNivaa("ORGENHET")
                .build();
        
        OrganiseringState organisering = organiseringState().toBuilder()
                .id(orgEnhetId)
                .idOver(orgEnhetOverId)
                .build();
        
        orgEnhetStateRepository.update(orgEnhet);
        organiseringStateRepository.update(organisering);

        RessursState lederRessurs = ressursState();
        ressursStateRepository.update(lederRessurs);

        orgEnhetLederStateRepository.update("FID_1", OrgEnhetLederState.builder()
                .fid("FID_1")
                .navident(lederRessurs.navident())
                .orgEnhetId(orgEnhetId)
                .startdato(LocalDate.now().minusMonths(3))
                .build());

        List<OrgEnhetMedLedernavn> result = webTestClient.get()
                .uri("/app/orgenheter/kanHaRessurser")
                .headers(httpHeaders -> httpHeaders.setBearerAuth(testToken.noScopesUser()))
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBodyList(OrgEnhetMedLedernavn.class)
                .returnResult().getResponseBody();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().ledere()).isEqualTo(List.of(lederRessurs.visningsnavn().getFormattedVisningsnavn()));
    }

    @Test
    void should_return_200_and_response_when_creating_orgenhet() throws ExecutionException, InterruptedException, TimeoutException {
        var overordnetId = "orgEnhet123";
        var lederNavident = "X123456";
        var opprettOrgEnhetRequest = OpprettOrgEnhetRequest.builder()
            .overordnetOrgEnhetId(overordnetId)
            .navn("Nav ny orgEnhet")
            .gyldigFom(LocalDate.now())
            .lederRessursNavident(lederNavident)
            .firesiffer("1234")
            .orgEnhetsType(OrgEnhetsType.DIREKTORAT)
            .build();

        var overordnetOrgEnhet = map(orgEnhetState());

        var ressursState = Optional.of(ressursState().toBuilder().startdato(LocalDate.now().minusMonths(3)).build());

        var orgEnhetState = orgEnhetStateBuilderWithDefaultValues().toBuilder()
            .fid("FID_1")
            .overordnetOrgEnhetFid(overordnetOrgEnhet.getFid())
            .navn("Nav Ny OrgEnhet")
            .gyldigFom(opprettOrgEnhetRequest.gyldigFom())
            .gyldigTom(null)
            .lederFid(ressursState.get().fid())
            .orgEnhetsType(opprettOrgEnhetRequest.orgEnhetsType())
            .build();

        when(unleash.isEnabled("nom-api.orgenhet-mastring")).thenReturn(true);

        when(orgenhetService.getById(overordnetId)).thenReturn(Optional.of(overordnetOrgEnhet));

        when(ressursService.findByNavident(lederNavident)).thenReturn(ressursState);

        when(orgenhetService.activeOrFutureWithEqualName(opprettOrgEnhetRequest.navn())).thenReturn(false);

        when(orgEnhetCommandService.opprettOrgEnhetSync(any(OpprettOrgEnhet.class))).thenReturn(orgEnhetState);

        when(orgEnhetLederCommandService.opprettOrgEnhetLeder(any(OpprettOrgEnhetLeder.class))).thenReturn("LEDERFID_1");

        var response = webTestClient.post()
            .uri("/app/orgenheter/opprett")
            .headers(httpHeaders -> httpHeaders.setBearerAuth(testToken.nomAdminUser()))
            .bodyValue(opprettOrgEnhetRequest)
            .exchange()
            .expectStatus().isCreated()
            .expectBody(OpprettOrgEnhetResponse.class)
            .returnResult()
            .getResponseBody();

        verify(orgEnhetCommandService, times(1)).opprettOrgEnhetSync(any(OpprettOrgEnhet.class));
        verify(orgEnhetCommandService, times(1)).endreOrgEnhetsType(anyString(), any(EndreOrgEnhetsType.class));

        verify(organiseringEventCommandService, times(1)).opprettOrganisering(any(OpprettOrganiseringCmd.class));

        verify(orgEnhetLederCommandService, times(1)).opprettOrgEnhetLeder(any(OpprettOrgEnhetLeder.class));
        verify(orgEnhetLederCommandService, times(1)).oppdaterOrgEnhetLederStartdato(anyString(), any(OppdaterOrgEnhetLederStartdato.class));

        assertThat(response).isEqualTo(
            OpprettOrgEnhetResponse.builder()
                .fid(orgEnhetState.fid())
                .id(orgEnhetState.id())
                .navn(orgEnhetState.navn())
                .overordnetOrgEnhetId(overordnetOrgEnhet.getId())
                .overordnetOrgEnhetNavn(overordnetOrgEnhet.getNavn())
                .gyldigFom(orgEnhetState.gyldigFom())
                .gyldigTom(orgEnhetState.gyldigTom())
                .lederRessursNavident(ressursState.get().navident())
                .lederRessursNavn(ressursState.get().visningsnavn().getFormattedVisningsnavn())
                .firesiffer(opprettOrgEnhetRequest.firesiffer())
                .orgEnhetsType(orgEnhetState.orgEnhetsType())
                .build()
        );
    }

    @Test
    void should_return_403_if_non_admin_user() {
        var overordnetId = "orgEnhet123";
        var lederNavident = "X123456";
        var opprettOrgEnhetRequest = OpprettOrgEnhetRequest.builder()
            .overordnetOrgEnhetId(overordnetId)
            .navn("Nav ny orgEnhet")
            .gyldigFom(LocalDate.now())
            .lederRessursNavident(lederNavident)
            .firesiffer("1234")
            .orgEnhetsType(OrgEnhetsType.DIREKTORAT)
            .build();

        webTestClient.post()
            .uri("/app/orgenheter/opprett")
            .headers(httpHeaders -> httpHeaders.setBearerAuth(testToken.nomWriteGroupAndNomUiAzp()))
            .bodyValue(opprettOrgEnhetRequest)
            .exchange()
            .expectStatus().isForbidden();
    }

    @Test
    void should_throw_exception_when_ressurs_does_not_exist() {
        var overordnetId = "orgEnhet123";
        var lederNavident = "X123456";
        var opprettOrgEnhetRequest = OpprettOrgEnhetRequest.builder()
            .overordnetOrgEnhetId(overordnetId)
            .navn("Nav ny orgEnhet")
            .gyldigFom(LocalDate.now())
            .lederRessursNavident(lederNavident)
            .firesiffer("1234")
            .orgEnhetsType(OrgEnhetsType.DIREKTORAT)
            .build();

        var overordnetOrgEnhet = map(orgEnhetState());

        when(unleash.isEnabled("nom-api.orgenhet-mastring")).thenReturn(true);

        when(orgenhetService.getById(overordnetId)).thenReturn(Optional.of(overordnetOrgEnhet));

        when(ressursService.findByNavident(lederNavident)).thenReturn(Optional.empty());

        webTestClient.post()
            .uri("/app/orgenheter/opprett")
            .headers(httpHeaders -> httpHeaders.setBearerAuth(testToken.nomAdminUser()))
            .bodyValue(opprettOrgEnhetRequest)
            .exchange()
            .expectStatus().isNotFound();
    }

    @Test
    void should_update_orgEnhetNavn() {
        String orgEnhetId = "ab123c";
        String orgEnhetOverId = "de456f";

        OrgEnhetState orgEnhet = orgEnhetStateBuilderWithDefaultValues().toBuilder()
            .navn("Nav Lokal Kontor")
            .id(orgEnhetId)
            .gyldigFom(LocalDate.now().minusMonths(6))
            .gyldigTom(null)
            .agressoOrgNivaa("ORGENHET")
            .build();

        OrganiseringState organisering = organiseringState().toBuilder()
            .id(orgEnhetId)
            .idOver(orgEnhetOverId)
            .build();

        orgEnhetStateRepository.update(orgEnhet);
        organiseringStateRepository.update(organisering);

        RessursState lederRessurs = ressursState();
        ressursStateRepository.update(lederRessurs);

        orgEnhetLederStateRepository.update("FID_1", OrgEnhetLederState.builder()
            .fid("FID_1")
            .navident(lederRessurs.navident())
            .orgEnhetId(orgEnhetId)
            .startdato(LocalDate.now().minusMonths(3))
            .build());

        var oppdaterOrgEnhetRequest = OppdaterOrgEnhetRequest.builder().id(orgEnhet.id())
            .navn("Nav Ny Lokal Kontor")
            .gyldigFom(orgEnhet.gyldigFom())
            .gyldigTom(orgEnhet.gyldigTom())
            .lederRessursNavident(lederRessurs.navident())
            .firesiffer("1234")
            .orgEnhetsType(orgEnhet.orgEnhetsType())
            .build();

        when(unleash.isEnabled("nom-api.orgenhet-mastring")).thenReturn(true);

        var oppdatertOrgEnhet = webTestClient.put().uri("/app/orgenheter/oppdater")
            .headers(httpHeaders -> httpHeaders.setBearerAuth(testToken.nomAdminUser()))
            .bodyValue(oppdaterOrgEnhetRequest)
            .exchange()
            .expectStatus().isOk()
            .expectBody(StandardResponse.class)
            .returnResult()
            .getResponseBody();

        String standardResponseMessageExpectation = String.format("OrgEnhet har endret navn fra %s til %s\\n", orgEnhet.navn(), oppdaterOrgEnhetRequest.navn());

        assertNotNull(oppdatertOrgEnhet);
        assertThat(standardResponseMessageExpectation).isEqualTo(oppdatertOrgEnhet.getMessage());
    }

    @Test
    void test_should_throw_exception_not_found_when_missing_organisering() {
        String orgEnhetId = "ab123c";

        OrgEnhetState orgEnhet = orgEnhetStateBuilderWithDefaultValues().toBuilder()
            .navn("Nav Lokal Kontor")
            .id(orgEnhetId)
            .gyldigFom(LocalDate.now().minusMonths(6))
            .gyldigTom(null)
            .agressoOrgNivaa("ORGENHET")
            .build();

        orgEnhetStateRepository.update(orgEnhet);

        RessursState lederRessurs = ressursState();
        ressursStateRepository.update(lederRessurs);

        orgEnhetLederStateRepository.update("FID_1", OrgEnhetLederState.builder()
            .fid("FID_1")
            .navident(lederRessurs.navident())
            .orgEnhetId(orgEnhetId)
            .startdato(LocalDate.now().minusMonths(3))
            .build());

        var oppdaterOrgEnhetRequest = OppdaterOrgEnhetRequest.builder().id(orgEnhet.id())
            .navn("Nav Ny Lokal Kontor")
            .gyldigFom(orgEnhet.gyldigFom())
            .gyldigTom(orgEnhet.gyldigTom())
            .lederRessursNavident(lederRessurs.navident())
            .firesiffer("1234")
            .orgEnhetsType(orgEnhet.orgEnhetsType())
            .build();

        when(unleash.isEnabled("nom-api.orgenhet-mastring")).thenReturn(true);

        var oppdatertOrgEnhet = webTestClient.put().uri("/app/orgenheter/oppdater")
            .headers(httpHeaders -> httpHeaders.setBearerAuth(testToken.nomAdminUser()))
            .bodyValue(oppdaterOrgEnhetRequest)
            .exchange()
            .expectStatus().isNotFound()
            .expectBody(StandardResponse.class)
            .returnResult()
            .getResponseBody();

        var standardResponseMessageExpectation = String.format("Organisering id %s ble ikke funnet", orgEnhet.id());

        assertNotNull(oppdatertOrgEnhet);
        assertThat(standardResponseMessageExpectation).isEqualTo(oppdatertOrgEnhet.getMessage());
    }

    @Test
    void test_should_throw_forbidden_exception_when_if_non_admin_user() {
        String orgEnhetId = "ab123c";

        OrgEnhetState orgEnhet = orgEnhetStateBuilderWithDefaultValues().toBuilder()
            .navn("Nav Lokal Kontor")
            .id(orgEnhetId)
            .gyldigFom(LocalDate.now().minusMonths(6))
            .gyldigTom(null)
            .agressoOrgNivaa("ORGENHET")
            .build();

        RessursState lederRessurs = ressursState();

        var oppdaterOrgEnhetRequest = OppdaterOrgEnhetRequest.builder().id(orgEnhet.id())
            .navn("Nav Ny Lokal Kontor")
            .gyldigFom(orgEnhet.gyldigFom())
            .gyldigTom(orgEnhet.gyldigTom())
            .lederRessursNavident(lederRessurs.navident())
            .firesiffer("1234")
            .orgEnhetsType(orgEnhet.orgEnhetsType())
            .build();


        when(unleash.isEnabled("nom-api.orgenhet-mastring")).thenReturn(true);

        webTestClient.put().uri("/app/orgenheter/oppdater")
            .headers(httpHeaders -> httpHeaders.setBearerAuth(testToken.nomWriteGroupAndNomUiAzp()))
            .bodyValue(oppdaterOrgEnhetRequest)
            .exchange()
            .expectStatus().isForbidden();
    }

    @Test
    void test_should_set_end_date_on_orgenhet_with_ressurser() {
        String orgEnhetId = "ab123c";
        String orgEnhetOverId = "de456f";

        OrgEnhetState orgEnhet = orgEnhetStateBuilderWithDefaultValues().toBuilder()
            .navn("Nav Lokal Kontor")
            .id(orgEnhetId)
            .gyldigFom(LocalDate.now().minusMonths(6))
            .gyldigTom(null)
            .agressoOrgNivaa("ORGENHET")
            .build();

        OrgEnhetState overOrgEnhetState = orgEnhetStateBuilderWithDefaultValues().toBuilder()
            .id(orgEnhetOverId)
            .gyldigTom(null)
            .build();

        OrganiseringState organisering = organiseringState().toBuilder()
            .id(orgEnhetId)
            .idOver(orgEnhetOverId)
            .build();

        OrgtilknytningState orgtilknytningStateEn = OrgtilknytningState.builder().
            fid("FID_1")
            .navident("S987654")
            .orgEnhetId(orgEnhet.id())
            .sektor(Sektor.NAV_STATLIG)
            .erDagligOppfolging(true)
            .gyldigFom(LocalDate.now().minusYears(1))
            .gyldigTom(LocalDate.now().plusWeeks(2))
            .orgtilknytningType(Tilknytningtype.FAST)
            .masseflyttetFraFid(null)
            .sistEndretAv("X123456")
            .build();

        OrgtilknytningState orgtilknytningStateTo = OrgtilknytningState.builder().
            fid("FID_2")
            .navident("S987653")
            .orgEnhetId(orgEnhet.id())
            .sektor(Sektor.NAV_STATLIG)
            .erDagligOppfolging(true)
            .gyldigFom(LocalDate.now().minusYears(1))
            .gyldigTom(LocalDate.now().plusWeeks(2))
            .orgtilknytningType(Tilknytningtype.FAST)
            .masseflyttetFraFid(null)
            .sistEndretAv("X123456")
            .build();

        orgEnhetStateRepository.update(orgEnhet);
        orgEnhetStateRepository.update(overOrgEnhetState);
        organiseringStateRepository.update(organisering);
        orgtilknytningStateRepository.update(orgtilknytningStateEn.fid(),  orgtilknytningStateEn);
        orgtilknytningStateRepository.update(orgtilknytningStateTo.fid(),  orgtilknytningStateTo);

        var avvikleOrgEnhetRequest = AvvikleOrgEnhetRequest.builder().gyldigTom(LocalDate.now().plusWeeks(2)).build();

        when(unleash.isEnabled("nom-api.orgenhet-mastring")).thenReturn(true);

        var avvikletOrgEnhet = webTestClient.patch().uri("/app/orgenheter/avvikle/{orgEnhetId}", orgEnhetId)
            .headers(httpHeaders -> httpHeaders.setBearerAuth(testToken.nomAdminUser()))
            .bodyValue(avvikleOrgEnhetRequest)
            .exchange()
            .expectStatus().isOk()
            .expectBody(StandardResponse.class)
            .returnResult()
            .getResponseBody();

        String standardResponseMessageExpectation = String.format("Enhet %s (%s) har fått satt sluttdato til %s", orgEnhet.navn(), orgEnhet.id(), avvikleOrgEnhetRequest.gyldigTom());

        assertNotNull(avvikletOrgEnhet);
        assertThat(standardResponseMessageExpectation).isEqualTo(avvikletOrgEnhet.getMessage());
    }

    @Test
    void test_should_set_end_date_on_orgenhet_with_underenheter_and_ressurser() {
        String orgEnhetId = "ab123c";
        String orgEnhetOverId = "de456f";

        OrgEnhetState overOrgEnhetState = orgEnhetStateBuilderWithDefaultValues().toBuilder()
            .fid("FID_1")
            .id(orgEnhetOverId)
            .gyldigTom(null)
            .build();

        OrgEnhetState orgEnhet = orgEnhetStateBuilderWithDefaultValues().toBuilder()
            .fid("FID_2")
            .navn("Nav Lokal Kontor")
            .id(orgEnhetId)
            .gyldigFom(LocalDate.now().minusMonths(6))
            .gyldigTom(null)
            .agressoOrgNivaa("ORGENHET")
            .build();

        var underOrgEnhetEn = orgEnhetStateBuilderWithDefaultValues().toBuilder()
            .fid("FID_3")
            .navn("Nav Lokal Underkontor 1")
            .id("ab124c")
            .gyldigFom(LocalDate.now().minusMonths(3))
            .gyldigTom(LocalDate.now().plusWeeks(2))
            .agressoOrgNivaa("ORGENHET")
            .build();

        var underOrgEnhetTo = orgEnhetStateBuilderWithDefaultValues().toBuilder()
            .fid("FID_4")
            .navn("Nav Lokal Underkontor 2")
            .id("ab125c")
            .gyldigFom(LocalDate.now().minusMonths(3))
            .gyldigTom(LocalDate.now().plusWeeks(2))
            .agressoOrgNivaa("ORGENHET")
            .build();

        OrganiseringState overOrganisering = organiseringState().toBuilder()
            .id(overOrgEnhetState.id())
            .idOver(null)
            .gyldigFom(LocalDate.of(2022,1,1))
            .build();

        OrganiseringState organisering = orgEnhetStateToOrganiseringState(1, orgEnhet, overOrgEnhetState);

        OrganiseringState underOrganiseringEn = orgEnhetStateToOrganiseringState(2, underOrgEnhetEn, orgEnhet);
        OrganiseringState underOrganiseringTo = orgEnhetStateToOrganiseringState(3, underOrgEnhetTo, orgEnhet);

        OrgtilknytningState orgtilknytningStateEn = OrgtilknytningState.builder()
            .fid("FID_1")
            .navident("S987654")
            .orgEnhetId(underOrgEnhetEn.id())
            .sektor(Sektor.NAV_STATLIG)
            .erDagligOppfolging(true)
            .gyldigFom(LocalDate.now().minusYears(1))
            .gyldigTom(LocalDate.now().plusWeeks(2))
            .orgtilknytningType(Tilknytningtype.FAST)
            .masseflyttetFraFid(null)
            .sistEndretAv("X123456")
            .build();

        OrgtilknytningState orgtilknytningStateTo = OrgtilknytningState.builder()
            .fid("FID_2")
            .navident("S987653")
            .orgEnhetId(underOrgEnhetTo.id())
            .sektor(Sektor.NAV_STATLIG)
            .erDagligOppfolging(true)
            .gyldigFom(LocalDate.now().minusYears(1))
            .gyldigTom(LocalDate.now().plusWeeks(2))
            .orgtilknytningType(Tilknytningtype.FAST)
            .masseflyttetFraFid(null)
            .sistEndretAv("X123456")
            .build();

        orgEnhetStateRepository.update(orgEnhet);

        orgEnhetStateRepository.update(overOrgEnhetState);

        orgEnhetStateRepository.update(underOrgEnhetEn);
        orgEnhetStateRepository.update(underOrgEnhetTo);

        organiseringStateRepository.update(organisering);

        organiseringStateRepository.update(overOrganisering);

        organiseringStateRepository.update(underOrganiseringEn);
        organiseringStateRepository.update(underOrganiseringTo);

        orgtilknytningStateRepository.update(orgtilknytningStateEn.fid(),  orgtilknytningStateEn);
        orgtilknytningStateRepository.update(orgtilknytningStateTo.fid(),  orgtilknytningStateTo);

        var avvikleOrgEnhetRequest = AvvikleOrgEnhetRequest.builder().gyldigTom(LocalDate.now().plusWeeks(2)).build();

        when(unleash.isEnabled("nom-api.orgenhet-mastring")).thenReturn(true);

        var avvikletOrgEnhet = webTestClient.patch().uri("/app/orgenheter/avvikle/{orgEnhetId}", orgEnhetId)
            .headers(httpHeaders -> httpHeaders.setBearerAuth(testToken.nomAdminUser()))
            .bodyValue(avvikleOrgEnhetRequest)
            .exchange()
            .expectStatus().isOk()
            .expectBody(StandardResponse.class)
            .returnResult()
            .getResponseBody();

        String standardResponseMessageExpectation = String.format("Enhet %s (%s) har fått satt sluttdato til %s", orgEnhet.navn(), orgEnhet.id(), avvikleOrgEnhetRequest.gyldigTom());

        assertNotNull(avvikletOrgEnhet);
        assertThat(standardResponseMessageExpectation).isEqualTo(avvikletOrgEnhet.getMessage());
    }

    @Test
    void test_should_refuse_to_set_end_date_on_orgenhet_because_of_over_orgenhet() {
        String orgEnhetId = "ab123c";
        String orgEnhetOverId = "de456f";

        OrgEnhetState overOrgEnhetState = orgEnhetStateBuilderWithDefaultValues().toBuilder()
            .fid("FID_1")
            .id(orgEnhetOverId)
            .gyldigTom(LocalDate.now().plusWeeks(1))
            .build();

        OrgEnhetState orgEnhet = orgEnhetStateBuilderWithDefaultValues().toBuilder()
            .fid("FID_2")
            .navn("Nav Lokal Kontor")
            .id(orgEnhetId)
            .gyldigFom(LocalDate.now().minusMonths(6))
            .gyldigTom(null)
            .agressoOrgNivaa("ORGENHET")
            .build();

        OrganiseringState overOrganisering = organiseringState().toBuilder()
            .id(overOrgEnhetState.id())
            .idOver(null)
            .gyldigFom(LocalDate.of(2022,1,1))
            .build();

        OrganiseringState organisering = orgEnhetStateToOrganiseringState(1, orgEnhet, overOrgEnhetState);

        orgEnhetStateRepository.update(orgEnhet);
        orgEnhetStateRepository.update(overOrgEnhetState);

        organiseringStateRepository.update(organisering);
        organiseringStateRepository.update(overOrganisering);

        var avvikleOrgEnhetRequest = AvvikleOrgEnhetRequest.builder().gyldigTom(LocalDate.now().plusWeeks(2)).build();

        when(unleash.isEnabled("nom-api.orgenhet-mastring")).thenReturn(true);

        var ikkeAvvikletOrgEnhet = webTestClient.patch().uri("/app/orgenheter/avvikle/{orgEnhetId}", orgEnhetId)
            .headers(httpHeaders -> httpHeaders.setBearerAuth(testToken.nomAdminUser()))
            .bodyValue(avvikleOrgEnhetRequest)
            .exchange()
            .expectStatus().is4xxClientError()
            .expectBody(StandardResponse.class)
            .returnResult()
            .getResponseBody();

        var responseMessage = String.format(
            "Kan ikke sette avviklings dato til enhet %s (%s) til å være etter overenheten %s (%s) sin avviklingsdato",
            orgEnhet.navn(),
            avvikleOrgEnhetRequest.gyldigTom(),
            overOrgEnhetState.navn(),
            overOrgEnhetState.gyldigTom());

        assertNotNull(ikkeAvvikletOrgEnhet);
        assertThat(responseMessage).isEqualTo(ikkeAvvikletOrgEnhet.getMessage());
    }

    @Test
    void test_should_refuse_to_set_end_date_on_orgenhet_because_of_under_orgenhet() {
        String orgEnhetId = "ab123c";
        String orgEnhetOverId = "de456f";

        OrgEnhetState overOrgEnhetState = orgEnhetStateBuilderWithDefaultValues().toBuilder()
            .fid("FID_1")
            .id(orgEnhetOverId)
            .gyldigTom(null)
            .build();

        OrgEnhetState orgEnhet = orgEnhetStateBuilderWithDefaultValues().toBuilder()
            .fid("FID_2")
            .navn("Nav Lokal Kontor")
            .id(orgEnhetId)
            .gyldigFom(LocalDate.now().minusMonths(6))
            .gyldigTom(null)
            .agressoOrgNivaa("ORGENHET")
            .build();

        var underOrgEnhetEn = orgEnhetStateBuilderWithDefaultValues().toBuilder()
            .fid("FID_3")
            .navn("Nav Lokal Underkontor 1")
            .id("ab124c")
            .gyldigFom(LocalDate.now().minusMonths(3))
            .gyldigTom(LocalDate.now().plusWeeks(3))
            .agressoOrgNivaa("ORGENHET")
            .build();

        OrganiseringState overOrganisering = organiseringState().toBuilder()
            .id(overOrgEnhetState.id())
            .idOver(null)
            .gyldigFom(LocalDate.of(2022,1,1))
            .build();

        OrganiseringState organisering = orgEnhetStateToOrganiseringState(1, orgEnhet, overOrgEnhetState);
        OrganiseringState underOrganiseringEn = orgEnhetStateToOrganiseringState(2, underOrgEnhetEn, orgEnhet);

        orgEnhetStateRepository.update(orgEnhet);
        orgEnhetStateRepository.update(overOrgEnhetState);
        orgEnhetStateRepository.update(underOrgEnhetEn);

        organiseringStateRepository.update(organisering);
        organiseringStateRepository.update(overOrganisering);
        organiseringStateRepository.update(underOrganiseringEn);

        var avvikleOrgEnhetRequest = AvvikleOrgEnhetRequest.builder().gyldigTom(LocalDate.now().plusWeeks(2)).build();

        when(unleash.isEnabled("nom-api.orgenhet-mastring")).thenReturn(true);

        var ikkeAvvikletOrgEnhet = webTestClient.patch().uri("/app/orgenheter/avvikle/{orgEnhetId}", orgEnhetId)
            .headers(httpHeaders -> httpHeaders.setBearerAuth(testToken.nomAdminUser()))
            .bodyValue(avvikleOrgEnhetRequest)
            .exchange()
            .expectStatus().is4xxClientError()
            .expectBody(StandardResponse.class)
            .returnResult()
            .getResponseBody();

        var responseMessage = String.format(
            "Underliggende enhet %s (%s) har sluttdato etter %s (%s)",
            underOrgEnhetEn.navn(),
            underOrgEnhetEn.id(),
            orgEnhet.navn(),
            orgEnhet.id());

        assertNotNull(ikkeAvvikletOrgEnhet);
        assertThat(responseMessage).isEqualTo(ikkeAvvikletOrgEnhet.getMessage());
    }

    @Test
    void test_should_refuse_to_set_end_date_on_orgenhet_because_of_ressurs() {
        String orgEnhetId = "ab123c";
        String orgEnhetOverId = "de456f";

        OrgEnhetState overOrgEnhetState = orgEnhetStateBuilderWithDefaultValues().toBuilder()
            .fid("FID_1")
            .id(orgEnhetOverId)
            .gyldigTom(null)
            .build();

        OrgEnhetState orgEnhet = orgEnhetStateBuilderWithDefaultValues().toBuilder()
            .fid("FID_2")
            .navn("Nav Lokal Kontor")
            .id(orgEnhetId)
            .gyldigFom(LocalDate.now().minusMonths(6))
            .gyldigTom(null)
            .agressoOrgNivaa("ORGENHET")
            .build();

        OrganiseringState overOrganisering = organiseringState().toBuilder()
            .id(overOrgEnhetState.id())
            .idOver(null)
            .gyldigFom(LocalDate.of(2022,1,1))
            .build();

        OrganiseringState organisering = orgEnhetStateToOrganiseringState(1, orgEnhet, overOrgEnhetState);

        OrgtilknytningState orgtilknytningStateEn = OrgtilknytningState.builder()
            .fid("FID_1")
            .navident("S987654")
            .orgEnhetId(orgEnhet.id())
            .sektor(Sektor.NAV_STATLIG)
            .erDagligOppfolging(true)
            .gyldigFom(LocalDate.now().minusYears(1))
            .gyldigTom(LocalDate.now().plusWeeks(3))
            .orgtilknytningType(Tilknytningtype.FAST)
            .masseflyttetFraFid(null)
            .sistEndretAv("X123456")
            .build();

        orgEnhetStateRepository.update(orgEnhet);
        orgEnhetStateRepository.update(overOrgEnhetState);

        organiseringStateRepository.update(organisering);
        organiseringStateRepository.update(overOrganisering);

        orgtilknytningStateRepository.update(orgtilknytningStateEn.fid(), orgtilknytningStateEn);

        var avvikleOrgEnhetRequest = AvvikleOrgEnhetRequest.builder().gyldigTom(LocalDate.now().plusWeeks(2)).build();

        when(unleash.isEnabled("nom-api.orgenhet-mastring")).thenReturn(true);

        var ikkeAvvikletOrgEnhet = webTestClient.patch().uri("/app/orgenheter/avvikle/{orgEnhetId}", orgEnhetId)
            .headers(httpHeaders -> httpHeaders.setBearerAuth(testToken.nomAdminUser()))
            .bodyValue(avvikleOrgEnhetRequest)
            .exchange()
            .expectStatus().is4xxClientError()
            .expectBody(StandardResponse.class)
            .returnResult()
            .getResponseBody();

        var responseMessage = String.format(
            "En eller flere ressurser i enhet %s (%s) har tilknytning til enheten etter gitt dato %s",
            orgEnhet.id(),
            orgEnhet.navn(),
            avvikleOrgEnhetRequest.gyldigTom()
        );

        assertNotNull(ikkeAvvikletOrgEnhet);
        assertThat(responseMessage).isEqualTo(ikkeAvvikletOrgEnhet.getMessage());
    }

    @Test
    void test_should_refuse_to_set_end_date_on_orgenhet_because_of_under_orgenhet_ressurs() {
        String orgEnhetId = "ab123c";
        String orgEnhetOverId = "de456f";

        OrgEnhetState overOrgEnhetState = orgEnhetStateBuilderWithDefaultValues().toBuilder()
            .fid("FID_1")
            .id(orgEnhetOverId)
            .gyldigTom(null)
            .build();

        OrgEnhetState orgEnhet = orgEnhetStateBuilderWithDefaultValues().toBuilder()
            .fid("FID_2")
            .navn("Nav Lokal Kontor")
            .id(orgEnhetId)
            .gyldigFom(LocalDate.now().minusMonths(6))
            .gyldigTom(null)
            .agressoOrgNivaa("ORGENHET")
            .build();

        var underOrgEnhetEn = orgEnhetStateBuilderWithDefaultValues().toBuilder()
            .fid("FID_3")
            .navn("Nav Lokal Underkontor 1")
            .id("ab124c")
            .gyldigFom(LocalDate.now().minusMonths(3))
            .gyldigTom(LocalDate.now().plusWeeks(2))
            .agressoOrgNivaa("ORGENHET")
            .build();

        OrganiseringState overOrganisering = organiseringState().toBuilder()
            .id(overOrgEnhetState.id())
            .idOver(null)
            .gyldigFom(LocalDate.of(2022,1,1))
            .build();

        OrganiseringState organisering = orgEnhetStateToOrganiseringState(1, orgEnhet, overOrgEnhetState);
        OrganiseringState underOrganiseringEn = orgEnhetStateToOrganiseringState(2, underOrgEnhetEn, orgEnhet);

        OrgtilknytningState orgtilknytningStateEn = OrgtilknytningState.builder()
            .fid("FID_1")
            .navident("S987654")
            .orgEnhetId(underOrgEnhetEn.id())
            .sektor(Sektor.NAV_STATLIG)
            .erDagligOppfolging(true)
            .gyldigFom(LocalDate.now().minusYears(1))
            .gyldigTom(null)
            .orgtilknytningType(Tilknytningtype.FAST)
            .masseflyttetFraFid(null)
            .sistEndretAv("X123456")
            .build();

        orgEnhetStateRepository.update(orgEnhet);
        orgEnhetStateRepository.update(overOrgEnhetState);
        orgEnhetStateRepository.update(underOrgEnhetEn);

        organiseringStateRepository.update(organisering);
        organiseringStateRepository.update(overOrganisering);
        organiseringStateRepository.update(underOrganiseringEn);


        orgtilknytningStateRepository.update(orgtilknytningStateEn.fid(), orgtilknytningStateEn);

        var avvikleOrgEnhetRequest = AvvikleOrgEnhetRequest.builder().gyldigTom(LocalDate.now().plusWeeks(2)).build();

        when(unleash.isEnabled("nom-api.orgenhet-mastring")).thenReturn(true);

        var ikkeAvvikletOrgEnhet = webTestClient.patch().uri("/app/orgenheter/avvikle/{orgEnhetId}", orgEnhetId)
            .headers(httpHeaders -> httpHeaders.setBearerAuth(testToken.nomAdminUser()))
            .bodyValue(avvikleOrgEnhetRequest)
            .exchange()
            .expectStatus().is4xxClientError()
            .expectBody(StandardResponse.class)
            .returnResult()
            .getResponseBody();

        var responseMessage = String.format(
            "En eller flere ressurser i enhet %s (%s) har tilknytning til enheten etter gitt dato %s",
            underOrgEnhetEn.id(),
            underOrgEnhetEn.navn(),
            avvikleOrgEnhetRequest.gyldigTom()
        );

        assertNotNull(ikkeAvvikletOrgEnhet);
        assertThat(responseMessage).isEqualTo(ikkeAvvikletOrgEnhet.getMessage());
    }

    private WebTestClient.ResponseSpec postOrgenhetType(String accessToken, SetOrgEnhetsTypeRequest requestBody) {
        return webTestClient.post()
                .uri("/app/orgenheter/orgEnhetsType")
                .headers(httpHeaders -> httpHeaders.setBearerAuth(accessToken))
                .bodyValue(requestBody)
                .exchange();
    }

    private SetOrgEnhetsTypeRequest setOrgEnhetsTypeRequest() {
        return new SetOrgEnhetsTypeRequest("yobrah", OrgEnhetsType.ARBEIDSLIVSSENTER);
    }

    private IOrgenhet map(OrgEnhetState orgEnhetState) {
        return new OrgEnhet(orgEnhetState);
    }
}
