package no.nav.nom.apps.api.rest.admin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.nom.apps.api.TestTokensGenerator;
import no.nav.nom.apps.api.authorize.redigereressurs.MockBasisTestOrganisering;
import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.dvh.dto.DvhLeder;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.orgenhet.implementations.OrgEnhet;
import no.nav.nom.apps.api.orgtilknytning.OrgtilknytningService;
import no.nav.nom.apps.api.rest.RestTestBase;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.Tilknytningtype;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class SanityCheckControllerTest extends RestTestBase {

    final String ORG_ENHET_NAVN_A = "OrgEnhet A";
    final String AGRESSO_ID_A = "A";
    final String AGRESSO_ORG_NIVAA_A = "NAV";

    final String ORG_ENHET_NAVN_B = "OrgEnhet B";
    final String AGRESSO_ID_B = "B";
    final String AGRESSO_ORG_NIVAA_B = "ORGNIV3";

    @Autowired
    TestTokensGenerator testToken;

    @MockitoBean
    OrgenhetService orgenhetServiceMock;

    @MockitoBean
    DvhRepository dvhRepositoryMock;

    @MockitoBean
    OrgtilknytningService orgtilknytningServiceMock;

    @MockitoBean
    RessursStateRepository ressursStateRepository;

    @Test
    @DisplayName("Henter orgenheter uten leder og tom NAV hierarkiet")
    void test_listOrgenheterUtenLeder_no_leaders_no_hierarchy() {

        when(orgenhetServiceMock.getAllEnheterMedOrganisering())
            .thenReturn(Collections.emptyList());
        when(dvhRepositoryMock.getLederByAgressoIdAndNivAndNavn(any(), any(), any()))
            .thenReturn(Collections.emptyList());
        webTestClient.get()
            .uri("/api/admin/SanityCheck/listOrgenheterUtenLeder")
            .headers(httpHeaders -> httpHeaders.setBearerAuth(testToken.nomAdminUser()))
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class)
            .value(result -> assertThat(result).isNull());
    }

    @Test
    @DisplayName("Henter orgenheter uten leder og NAV hierarkiet")
    void test_listOrgenheterUtenLeder_no_leaders_with_hierarchy() {

        when(orgenhetServiceMock.getAllEnheterMedOrganisering())
            .thenReturn(getMockEnheterMedOrganisering());
        when(dvhRepositoryMock.getLederByAgressoIdAndNivAndNavn(any(), any(), any()))
            .thenReturn(Collections.emptyList());
        webTestClient.get()
            .uri("/api/admin/SanityCheck/listOrgenheterUtenLeder")
            .headers(httpHeaders -> httpHeaders.setBearerAuth(testToken.nomAdminUser()))
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class)
            .value(result -> {
                assertThat(result).isNotNull();
                assertThat(result.contains(ORG_ENHET_NAVN_B)).isTrue();
                assertThat(result.contains(ORG_ENHET_NAVN_A)).isTrue();
             });
    }

    @Test
    @DisplayName("Henter orgenheter med leder i NAV hierarkiet")
    void test_listOrgenheterUtenLeder_with_leaders() throws JsonProcessingException {
        when(orgenhetServiceMock.getAllEnheterMedOrganisering())
            .thenReturn(getMockEnheterMedOrganisering());
        // simulate that orgenhet B has a leader
        when(dvhRepositoryMock.getLederByAgressoIdAndNivAndNavn(AGRESSO_ID_B, AGRESSO_ORG_NIVAA_B, ORG_ENHET_NAVN_B))
            .thenReturn(getMockLeaders());

        webTestClient.get()
            .uri("/api/admin/SanityCheck/listOrgenheterUtenLeder")
            .headers(httpHeaders -> httpHeaders.setBearerAuth(testToken.nomAdminUser()))
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class)
            .value(result -> {
                assertThat(result).isNotNull();
                assertThat(result.contains(ORG_ENHET_NAVN_B)).isFalse();
                assertThat(result.contains(ORG_ENHET_NAVN_A)).isTrue();
            });
    }

    @Test
    @DisplayName("Sjekk at ressurs sluttdato og orgTilknytning sluttdato er like")
    void test_ressurs_sluttdato_og_orgTilknytning_sluttdato_er_ulike() {
        var navidenter = getMockRessurserState();
        when(ressursStateRepository.findAllActiveRessurser())
            .thenReturn(navidenter);
        when(orgtilknytningServiceMock.getByNavidenter(eq(navidenter.keySet()), any(Function.class)))
            .thenReturn(getMockOrgtilknytning());

        webTestClient.get()
            .uri("/api/admin/SanityCheck/ressursSluttdatoUlikOrgTilknytningSluttdato")
            .headers(httpHeaders -> httpHeaders.setBearerAuth(testToken.nomAdminUser()))
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class)
            .value(result -> {
                assertThat(result).isNotNull();
                assertThat(result.equals("S987654;TestId1;RessursSluttdato:null;OrgGyldigTom:2024-12-31\n" +
                    "S987654;TestId2;RessursSluttdato:null;OrgGyldigTom:2024-12-31\n" +
                    "S123456;TestId;RessursSluttdato:null;OrgGyldigTom:2023-12-31\n")).isTrue();
            });
    }

    @Test
    @DisplayName("Test at ressurs har to eller flere DOL med overlappende datoer")
    void test_ressurs_har_to_eller_flere_DOL_med_overlappende_datoer() {
        var ressurs = getMockRessursEkstern().toBuilder()
            .sektor(Sektor.NAV_STATLIG)
            .sluttdato(null)
            .build();
        Map<String, RessursState> ressursMap = new HashMap<>();
        ressursMap.put(ressurs.navident(), ressurs);

        var orgtilknytninger = List.of(
            getOrgtilknytningStateKonsulentMock().toBuilder()
                .orgtilknytningType(Optional.empty())
                .orgEnhetId("TestEnhetId1")
                .navident(ressurs.navident())
                .gyldigFom(LocalDate.of(2024, 1, 1))
                .gyldigTom(Optional.empty())
                .erDagligOppfolging(true)
                .build(),
            getOrgtilknytningStateKonsulentMock().toBuilder()
                .orgtilknytningType(Optional.empty())
                .orgEnhetId("TestEnhetId2")
                .navident(ressurs.navident())
                .gyldigFom(LocalDate.of(2024, 6, 1))
                .gyldigTom(Optional.of(LocalDate.of(2025, 12, 31)))
                .erDagligOppfolging(true)
                .build());

        Map<String, Collection<OrgtilknytningState>> orgtilknytningMap = new HashMap<>();
        orgtilknytningMap.put(orgtilknytninger.get(0).navident(), List.of(orgtilknytninger.get(0), orgtilknytninger.get(1)));

        var orgEnhetStateList = List.of(
            getOrgEnhetStateMock().toBuilder()
                .id("TestEnhetId1")
                .build(),
            getOrgEnhetStateMock().toBuilder()
                .id("TestEnhetId2")
                .navn("IT-Utvikling 3B")
                .build()
        );

        var orgenhet = orgEnhetStateList.stream().map(this::getOrgenhetMock).toList();

        when(ressursStateRepository.findAllActiveRessurser())
            .thenReturn(ressursMap);
        when(orgtilknytningServiceMock.getByNavidenter(eq(orgtilknytningMap.keySet()), any(Function.class)))
            .thenReturn(orgtilknytningMap);
        when(orgenhetServiceMock.getAllEnheter())
            .thenReturn(orgenhet);

        webTestClient.get()
            .uri("/api/admin/SanityCheck/ressursMedFlereEnnEnDOL")
            .headers(httpHeaders -> httpHeaders.setBearerAuth(testToken.nomAdminUser()))
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class)
            .value(result -> {
                assertThat(result).isNotNull();
                assertThat(result.equals("X123456;IT-Utvikling 3A=TestEnhetId1;2024-01-01;Ingen Sluttdato;IT-Utvikling 3B=TestEnhetId2;2024-06-01;2025-12-31;\n")).isTrue();
            });
    }

    @Test
    @DisplayName("Test at ressurs har to eller flere DOL med ikke overlappende datoer")
    void test_ressurs_har_to_eller_flere_DOL() {
        var ressurs = getMockRessursEkstern().toBuilder()
            .sektor(Sektor.NAV_STATLIG)
            .sluttdato(null)
            .build();
        Map<String, RessursState> ressursMap = new HashMap<>();
        ressursMap.put(ressurs.navident(), ressurs);

        var orgtilknytninger = List.of(
            getOrgtilknytningStateKonsulentMock().toBuilder()
                .orgtilknytningType(Optional.empty())
                .orgEnhetId("TestEnhetId1")
                .navident(ressurs.navident())
                .gyldigFom(LocalDate.of(2024, 1, 1))
                .gyldigTom(Optional.of(LocalDate.of(2024, 5, 31)))
                .erDagligOppfolging(true)
                .build(),
            getOrgtilknytningStateKonsulentMock().toBuilder()
                .orgtilknytningType(Optional.empty())
                .orgEnhetId("TestEnhetId2")
                .navident(ressurs.navident())
                .gyldigFom(LocalDate.of(2024, 6, 1))
                .gyldigTom(Optional.of(LocalDate.of(2025, 12, 31)))
                .erDagligOppfolging(true)
                .build());

        Map<String, Collection<OrgtilknytningState>> orgtilknytningMap = new HashMap<>();
        orgtilknytningMap.put(orgtilknytninger.get(0).navident(), List.of(orgtilknytninger.get(0), orgtilknytninger.get(1)));

        var orgEnhetStateList = List.of(
            getOrgEnhetStateMock().toBuilder()
                .id("TestEnhetId1")
                .build(),
            getOrgEnhetStateMock().toBuilder()
                .id("TestEnhetId2")
                .navn("IT-Utvikling 3B")
                .build()
        );

        var orgenhet = orgEnhetStateList.stream().map(this::getOrgenhetMock).toList();

        when(ressursStateRepository.findAllActiveRessurser())
            .thenReturn(ressursMap);
        when(orgtilknytningServiceMock.getByNavidenter(eq(orgtilknytningMap.keySet()), any(Function.class)))
            .thenReturn(orgtilknytningMap);
        when(orgenhetServiceMock.getAllEnheter())
            .thenReturn(orgenhet);

        webTestClient.get()
            .uri("/api/admin/SanityCheck/ressursMedFlereEnnEnDOL")
            .headers(httpHeaders -> httpHeaders.setBearerAuth(testToken.nomAdminUser()))
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class)
            .value(result -> {
                assertThat(result).isNotNull();
                assertThat(result.equals("Ingen ressurs med flere DOL")).isTrue();
            });
    }

    @Test
    @DisplayName("Test at ressurs har to eller flere DOL med siste_dato_som_overlapper")
    void test_ressurs_har_to_eller_flere_DOL_med_siste_dato_som_overlapper() {
        var ressurs = getMockRessursEkstern().toBuilder()
            .sektor(Sektor.NAV_STATLIG)
            .sluttdato(null)
            .build();
        Map<String, RessursState> ressursMap = new HashMap<>();
        ressursMap.put(ressurs.navident(), ressurs);

        var orgtilknytninger = List.of(
            getOrgtilknytningStateKonsulentMock().toBuilder()
                .orgtilknytningType(Optional.empty())
                .orgEnhetId("TestEnhetId1")
                .navident(ressurs.navident())
                .gyldigFom(LocalDate.of(2024, 1, 1))
                .gyldigTom(Optional.of(LocalDate.of(2024, 5, 31)))
                .erDagligOppfolging(true)
                .build(),
            getOrgtilknytningStateKonsulentMock().toBuilder()
                .orgtilknytningType(Optional.empty())
                .orgEnhetId("TestEnhetId2")
                .navident(ressurs.navident())
                .gyldigFom(LocalDate.of(2024, 5, 31))
                .gyldigTom(Optional.of(LocalDate.of(2025, 12, 31)))
                .erDagligOppfolging(true)
                .build());

        Map<String, Collection<OrgtilknytningState>> orgtilknytningMap = new HashMap<>();
        orgtilknytningMap.put(orgtilknytninger.get(0).navident(), List.of(orgtilknytninger.get(0), orgtilknytninger.get(1)));

        var orgEnhetStateList = List.of(
            getOrgEnhetStateMock().toBuilder()
                .id("TestEnhetId1")
                .build(),
            getOrgEnhetStateMock().toBuilder()
                .id("TestEnhetId2")
                .navn("IT-Utvikling 3B")
                .build()
        );

        var orgenhet = orgEnhetStateList.stream().map(this::getOrgenhetMock).toList();

        when(ressursStateRepository.findAllActiveRessurser())
            .thenReturn(ressursMap);
        when(orgtilknytningServiceMock.getByNavidenter(eq(orgtilknytningMap.keySet()), any(Function.class)))
            .thenReturn(orgtilknytningMap);
        when(orgenhetServiceMock.getAllEnheter())
            .thenReturn(orgenhet);

        webTestClient.get()
            .uri("/api/admin/SanityCheck/ressursMedFlereEnnEnDOL")
            .headers(httpHeaders -> httpHeaders.setBearerAuth(testToken.nomAdminUser()))
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class)
            .value(result -> {
                assertThat(result).isNotNull();
                assertThat(result.equals("X123456;IT-Utvikling 3A=TestEnhetId1;2024-01-01;2024-05-31;IT-Utvikling 3B=TestEnhetId2;2024-05-31;2025-12-31;\n")).isTrue();
            });
    }

    @Test
    @DisplayName("Validering av ressurser uten Orgtilknytninger")
    void validering_av_ressurser_uten_orgtilknytninger() {
        var navidenter = getMockRessurserState();
        when(ressursStateRepository.findAllActiveRessurser())
            .thenReturn(navidenter);
        var orgtilknytninger = List.of(
            getOrgtilknytningStateKonsulentMock().toBuilder()
                .orgtilknytningType(Optional.empty())
                .orgEnhetId("TestEnhetId1")
                .navident("X123456")
                .gyldigFom(LocalDate.of(2024, 1, 1))
                .gyldigTom(Optional.of(LocalDate.of(2024, 2, 25)))
                .erDagligOppfolging(true)
                .build(),
            getOrgtilknytningStateKonsulentMock().toBuilder()
                .orgtilknytningType(Optional.empty())
                .orgEnhetId("TestEnhetId1")
                .navident("S123456")
                .gyldigFom(LocalDate.of(2024, 1, 1))
                .gyldigTom(Optional.of(LocalDate.MAX))
                .erDagligOppfolging(true)
                .build());

        Map<String, Collection<OrgtilknytningState>> orgtilknytningMap = new HashMap<>();
        orgtilknytningMap.put("X123456", List.of(orgtilknytninger.get(0)));
        orgtilknytningMap.put("S123456", List.of(orgtilknytninger.get(1)));
        orgtilknytningMap.put("X987654", List.of());
        orgtilknytningMap.put("S987654", List.of());

        when(ressursStateRepository.findAllActiveRessurser())
            .thenReturn(navidenter);
        when(orgtilknytningServiceMock.getByNavidenter(eq(orgtilknytningMap.keySet()), any(Function.class)))
            .thenReturn(orgtilknytningMap);

        webTestClient.get()
            .uri("/api/admin/SanityCheck/ressursUtenOrgtilknytning")
            .headers(httpHeaders -> httpHeaders.setBearerAuth(testToken.nomAdminUser()))
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class)
            .value(result -> {
                assertThat(result).isNotNull();
                assertThat(result.equals("X987654;\nS987654;\nX123456;\n")).isTrue();
            });
    }

    private List<DvhLeder> getMockLeaders() throws JsonProcessingException {
        String jsonStr = "{" +
            "\"leder_navident\": \"JohnDoe\"," +
            "\"orgnode_kode\": \"B\"," +
            "\"orgnode_niva\": \"ORGNIV3\"," +
            "\"orgnode_navn\": \"OrgEnhet B\"" +
            "}";
        DvhLeder leder = DvhLeder.fromOrds(new ObjectMapper().readTree(jsonStr));
        return new ArrayList<>(){{add(leder);}};
    }

    private Collection<IOrgenhet> getMockEnheterMedOrganisering() {
        IOrgenhet orgenhet_A = MockBasisTestOrganisering.MockOrgenhet.builder()
            .id(AGRESSO_ID_A)
            .navn(ORG_ENHET_NAVN_A)
            .agressoId(AGRESSO_ID_A)
            .uniqueAgressoId(AGRESSO_ID_A+"-"+ORG_ENHET_NAVN_A+"-"+AGRESSO_ORG_NIVAA_A)
            .orgNiv(AGRESSO_ORG_NIVAA_A)
            .gyldigTom(Optional.of(LocalDate.now().plusDays(1)))
            .build();

        IOrgenhet orgenhet_B = MockBasisTestOrganisering.MockOrgenhet.builder()
            .id(AGRESSO_ID_B)
            .navn(ORG_ENHET_NAVN_B)
            .agressoId(AGRESSO_ID_B)
            .uniqueAgressoId(AGRESSO_ID_B+"-"+ORG_ENHET_NAVN_B+"-"+AGRESSO_ORG_NIVAA_B)
            .orgNiv(AGRESSO_ORG_NIVAA_B)
            .gyldigTom(Optional.of(LocalDate.now().plusDays(1)))
            .build();
        return List.of(orgenhet_A, orgenhet_B);
    }

    private Map<String, RessursState> getMockRessurserState() {
        Map<String, RessursState> ressursStateMap = new HashMap<>();
        RessursState ressursEksternErAktiv = getMockRessursEkstern().toBuilder()
            .personident("01234598765")
            .navident("X123456")
            .visningsnavn(new Visningsnavn("Nav", Optional.empty(), "Navesen"))
            .primaryTelefonnummer("12345678")
            .sluttdato(LocalDate.of(2024, 12, 31))
            .startdato(LocalDate.of(2024, 1, 1))
            .build();

        RessursState ressursEksternErIkkeAktiv = getMockRessursEkstern().toBuilder()
            .personident("98765412345")
            .navident("X987654")
            .visningsnavn(new Visningsnavn("Her", Optional.empty(), "Hermansen"))
            .primaryTelefonnummer("87654321")
            .sluttdato(LocalDate.of(2023, 6, 30))
            .startdato(LocalDate.of(2023, 1, 1))
            .build();

        RessursState ressursStatligAktiv = getMockRessursEkstern().toBuilder()
            .personident("48294612345")
            .navident("S123456")
            .sektor(Sektor.NAV_STATLIG)
            .visningsnavn(new Visningsnavn("Lars", Optional.empty(), "Larsen"))
            .primaryTelefonnummer(null)
            .sluttdato(null)
            .startdato(LocalDate.of(2022, 1, 1))
            .build();

        RessursState ressursStatligAktivTo = getMockRessursEkstern().toBuilder()
            .personident("93461812345")
            .navident("S987654")
            .sektor(Sektor.NAV_STATLIG)
            .visningsnavn(new Visningsnavn("Trude", Optional.empty(), "Trudesen"))
            .primaryTelefonnummer(null)
            .sluttdato(null)
            .startdato(LocalDate.of(2021, 1, 1))
            .build();

        ressursStateMap.put(ressursStatligAktiv.navident(), ressursStatligAktiv);
        ressursStateMap.put(ressursStatligAktivTo.navident(), ressursStatligAktivTo);
        ressursStateMap.put(ressursEksternErAktiv.navident(), ressursEksternErAktiv);
        ressursStateMap.put(ressursEksternErIkkeAktiv.navident(), ressursEksternErIkkeAktiv);
        return ressursStateMap;
    }

    private RessursState getMockRessursEkstern() {
        return RessursState.builder()
            .fid("R-FID")
            .personident("01234598765")
            .navident("X123456")
            .sektor(Sektor.EKSTERN)
            .visningsnavn(new Visningsnavn("Nav", Optional.empty(), "Navesen"))
            .primaryTelefonnummer("12345678")
            .sluttdato(LocalDate.of(2024, 12, 31))
            .startdato(LocalDate.of(2024, 1, 1))
            .primaryFysiskBrukersted("2700")
            .fysiskeBrukersteder(List.of("2700"))
            .build();
    }

    private Map<String, Collection<OrgtilknytningState>> getMockOrgtilknytning() {
        Map<String, Collection<OrgtilknytningState>> orgtilknytningMap = new HashMap<>();

        OrgtilknytningState orgtilknytningEksternAktiv = getOrgtilknytningStateKonsulentMock();

        OrgtilknytningState orgtilknytningStatligAktiv = getOrgtilknytningStateKonsulentMock().toBuilder()
            .navident("S123456")
            .gyldigFom(LocalDate.of(2022, 1, 1))
            .gyldigTom(Optional.of(LocalDate.of(2023, 12, 31)))
            .orgtilknytningType(Optional.empty())
            .build();

        OrgtilknytningState orgtilknytningStatligAktivTo = OrgtilknytningState.builder()
            .fid("O-FID")
            .navident("S987654")
            .orgEnhetId("TestId1")
            .gyldigFom(LocalDate.of(2024, 1, 1))
            .gyldigTom(Optional.of(LocalDate.of(2024, 12, 31)))
            .orgtilknytningType(Optional.empty())
            .build();

        OrgtilknytningState orgtilknytningStatligAktivTre = OrgtilknytningState.builder()
            .fid("O-FID")
            .navident("S987654")
            .orgEnhetId("TestId2")
            .gyldigFom(LocalDate.of(2024, 1, 1))
            .gyldigTom(Optional.of(LocalDate.of(2024, 12, 31)))
            .orgtilknytningType(Optional.empty())
            .build();

        orgtilknytningMap.put(orgtilknytningEksternAktiv.navident(), List.of(orgtilknytningEksternAktiv));
        orgtilknytningMap.put(orgtilknytningStatligAktiv.navident(), List.of(orgtilknytningStatligAktiv));
        orgtilknytningMap.put(orgtilknytningStatligAktivTo.navident(), List.of(orgtilknytningStatligAktivTo, orgtilknytningStatligAktivTre));
        return orgtilknytningMap;
    }

    private OrgtilknytningState getOrgtilknytningStateKonsulentMock() {
        return OrgtilknytningState.builder()
            .fid("O-FID")
            .navident("X123456")
            .orgEnhetId("TestId")
            .erDagligOppfolging(true)
            .gyldigFom(LocalDate.of(2024, 1, 1))
            .gyldigTom(Optional.of(LocalDate.of(2024, 12, 31)))
            .orgtilknytningType(Optional.of(Tilknytningtype.KONSULENT))
            .build();
    }

    private OrgEnhetState getOrgEnhetStateMock() {
        return OrgEnhetState.builder()
            .fid("OH-FID")
            .id("TestId")
            .navn("IT-Utvikling 3A")
            .gyldigFom(LocalDate.of(2020, 1, 1))
            .gyldigTom(Optional.empty())
            .overordnetOrgEnhetFid(Optional.empty())
            .lederFid(Optional.empty())
            .orgEnhetsType(Optional.empty())
            .nomNivaa(Optional.empty())
            .uniqueAgressoId(Optional.empty())
            .agressoId(Optional.empty())
            .agressoOrgNivaa(Optional.empty())
            .agressoOrgenhetType(Optional.empty())
            .build();
    }

    private IOrgenhet getOrgenhetMock(OrgEnhetState orgEnhetState) {
        return new OrgEnhet(orgEnhetState);
    }
}
