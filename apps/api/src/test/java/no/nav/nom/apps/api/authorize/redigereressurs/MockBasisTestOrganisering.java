package no.nav.nom.apps.api.authorize.redigereressurs;

import io.getunleash.Unleash;
import lombok.Builder;
import lombok.Getter;
import no.nav.nom.apps.api.authorize.email.TilgangDelegertEmailService;
import no.nav.nom.apps.api.clients.msgraph.MicrosoftGraphClient;
import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.dvh.dto.DvhLeder;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.contract.nom.NomNivaa;
import no.nav.nom.contract.nom.OrgEnhetsType;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.state.OrganiseringState;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.organisering.OrganiseringStateRepository;
import no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateRepository;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

abstract public class MockBasisTestOrganisering {
    @MockitoBean
    OrgtilknytningStateRepository orgtilknytningStateRepository;
    @MockitoBean
    OrganiseringStateRepository organiseringStateRepository;
    @MockitoBean
    RessursStateRepository ressursStateRepository;
    @MockitoBean
    Unleash unleash;
    @MockitoBean
    OrgenhetService orgenhetService;
    @MockitoBean
    DvhRepository dvhRepository;
    @MockitoBean
    TilgangDelegertEmailService tilgangDelegertEmailService;
    @MockitoBean
    MicrosoftGraphClient microsoftGraphClient;
    
    @Autowired
    AuthorizedNavident authorizedNavident;

    @BeforeEach
    void setup() {
        setupBasisOrganisasjon();
        authorizedNavident.oppdaterLedereForOrgenheter();
    }

    // Sett opp basic organisasjon aka eksemplet i src/test/resources/authorize/redigere_ressurs_test_organisasjon.png
    void setupBasisOrganisasjon() {
        setupBasisOrgenheter();
        setupBasisOrganisering();
        setupBasisLedere();
        setupBasisOrgtilknytninger();
        setupRessursState();
    }

    private void setupBasisOrgenheter() {
        IOrgenhet orgenhet_X = MockOrgenhet.builder()
            .fid("FID_X")
            .id("X")
            .navn("OrgEnhet X")
            .nomNivaa(NomNivaa.LINJEENHET)
            .agressoId("X")
            .orgNiv("NAV")
            .build();

        IOrgenhet orgenhet_A = MockOrgenhet.builder()
            .fid("FID_A")
            .id("A")
            .navn("OrgEnhet A")
            .nomNivaa(NomNivaa.DRIFTSENHET)
            .agressoId("A")
            .orgNiv("NAV")
            .build();

        IOrgenhet orgenhet_A1 = MockOrgenhet.builder()
            .fid("FID_A1")
            .id("A1")
            .navn("OrgEnhet A1")
            .nomNivaa(null)
            .agressoId("A1")
            .orgNiv("ORGNIV2")
            .build();

        IOrgenhet orgenhet_A11 = MockOrgenhet.builder()
            .fid("FID_A11")
            .id("A11")
            .navn("OrgEnhet A11")
            .nomNivaa(NomNivaa.ARBEIDSOMRAADE)
            .agressoId("A11")
            .orgNiv("ORGNIV3")
            .build();

        IOrgenhet orgenhet_B = MockOrgenhet.builder()
            .fid("FID_B")
            .id("B")
            .navn("OrgEnhet B")
            .nomNivaa(NomNivaa.ARBEIDSOMRAADE)
            .agressoId("B")
            .orgNiv("ORGNIV3")
            .build();

        IOrgenhet orgenhet_B1 = MockOrgenhet.builder()
            .fid("FID_B1")
            .id("B1")
            .navn("OrgEnhet B1")
            .nomNivaa(null)
            .agressoId("B1")
            .orgNiv("ORGNIV4")
            .build();

        IOrgenhet orgenhet_B11 = MockOrgenhet.builder()
            .fid("FID_B11")
            .id("B11")
            .navn("OrgEnhet B11")
            .nomNivaa(null)
            .agressoId("B11")
            .orgNiv("ORGENHET")
            .build();

        IOrgenhet orgenhet_C = MockOrgenhet.builder()
            .fid("FID_C")
            .id("C")
            .navn("OrgEnhet C")
            .nomNivaa(NomNivaa.ARBEIDSOMRAADE)
            .agressoId("C")
            .orgNiv("ORGNIV3")
            .build();

        IOrgenhet orgenhet_C1 = MockOrgenhet.builder()
            .fid("FID_C1")
            .id("C1")
            .navn("OrgEnhet C1")
            .nomNivaa(null)
            .agressoId("C1")
            .orgNiv("ORGNIV4")
            .build();

        IOrgenhet orgenhet_C11 = MockOrgenhet.builder()
            .fid("FID_C11")
            .id("C11")
            .navn("OrgEnhet C11")
            .nomNivaa(null)
            .agressoId("C11")
            .orgNiv("ORGENHET")
            .build();

        IOrgenhet orgenhet_C2 = MockOrgenhet.builder()
            .fid("FID_C2")
            .id("C2")
            .navn("OrgEnhet C2")
            .nomNivaa(null)
            .agressoId("C2")
            .orgNiv("ORGNIV4")
            .build();

        IOrgenhet orgenhet_C22 = MockOrgenhet.builder()
            .fid("FID_C22")
            .id("C22")
            .navn("OrgEnhet C22")
            .nomNivaa(null)
            .agressoId("C22")
            .orgNiv("ORGENHET")
            .build();

        IOrgenhet orgenhet_D = MockOrgenhet.builder()
            .fid("FID_D")
            .id("D")
            .navn("OrgEnhet D")
            .nomNivaa(NomNivaa.ARBEIDSOMRAADE)
            .agressoId("D")
            .orgNiv("ORGNIV3")
            .build();

        IOrgenhet orgenhet_D1 = MockOrgenhet.builder()
            .fid("FID_D1")
            .id("D1")
            .navn("OrgEnhet D1")
            .nomNivaa(null)
            .agressoId("D1")
            .orgNiv("ORGENHET")
            .build();

        IOrgenhet orgenhet_E = MockOrgenhet.builder()
            .fid("FID_E")
            .id("E")
            .navn("OrgEnhet E")
            .nomNivaa(NomNivaa.ARBEIDSOMRAADE)
            .agressoId("E")
            .orgNiv("ORGNIV3")
            .build();

        IOrgenhet orgenhet_E1 = MockOrgenhet.builder()
            .fid("FID_E1")
            .id("E1")
            .navn("OrgEnhet E1")
            .nomNivaa(null)
            .agressoId("E1")
            .orgNiv("ORGENHET")
            .build();

        IOrgenhet orgenhet_helfo = MockOrgenhet.builder()
            .fid("FID_HELFO")
            .id("dummyHelfoId")
            .navn("OrgEnhet HELFO")
            .nomNivaa(null)
            .agressoId("HELFO")
            .orgNiv("NAV")
            .build();

        IOrgenhet orgenhet_skatt = MockOrgenhet.builder()
            .fid("FID_SKATT")
            .id("dummySkattId")
            .navn("OrgEnhet SKATT")
            .nomNivaa(null)
            .agressoId("SKATT")
            .orgNiv("NAV")
            .build();

        IOrgenhet orgenhet_riksrev = MockOrgenhet.builder()
            .fid("FID_RIKSREV")
            .id("dummyRiksrevisjonId")
            .navn("OrgEnhet RIKSREV")
            .nomNivaa(null)
            .agressoId("RIKSREV")
            .orgNiv("NAV")
            .build();

        lenient().when(orgenhetService.getById("X")).thenReturn(Optional.of(orgenhet_X));
        lenient().when(orgenhetService.getById("A")).thenReturn(Optional.of(orgenhet_A));
        lenient().when(orgenhetService.getById("A1")).thenReturn(Optional.of(orgenhet_A1));
        lenient().when(orgenhetService.getById("A11")).thenReturn(Optional.of(orgenhet_A11));
        lenient().when(orgenhetService.getById("B")).thenReturn(Optional.of(orgenhet_B));
        lenient().when(orgenhetService.getById("B1")).thenReturn(Optional.of(orgenhet_B1));
        lenient().when(orgenhetService.getById("B11")).thenReturn(Optional.of(orgenhet_B11));
        lenient().when(orgenhetService.getById("C")).thenReturn(Optional.of(orgenhet_C));
        lenient().when(orgenhetService.getById("C1")).thenReturn(Optional.of(orgenhet_C1));
        lenient().when(orgenhetService.getById("C11")).thenReturn(Optional.of(orgenhet_C11));
        lenient().when(orgenhetService.getById("C2")).thenReturn(Optional.of(orgenhet_C2));
        lenient().when(orgenhetService.getById("C22")).thenReturn(Optional.of(orgenhet_C22));
        lenient().when(orgenhetService.getById("D")).thenReturn(Optional.of(orgenhet_D));
        lenient().when(orgenhetService.getById("D1")).thenReturn(Optional.of(orgenhet_D1));
        lenient().when(orgenhetService.getById("E")).thenReturn(Optional.of(orgenhet_E));
        lenient().when(orgenhetService.getById("E1")).thenReturn(Optional.of(orgenhet_E1));
        lenient().when(orgenhetService.getById("dummyHelfoId")).thenReturn(Optional.of(orgenhet_helfo));
        lenient().when(orgenhetService.getById("dummySkattId")).thenReturn(Optional.of(orgenhet_skatt));
        lenient().when(orgenhetService.getById("dummyRiksrevisjonId")).thenReturn(Optional.of(orgenhet_riksrev));


        when(orgenhetService.getByIdBulk(argThat(list -> !list.isEmpty()))).thenAnswer(it -> {
            var outMap = new HashMap<>(Map.of(
                "C", orgenhet_C,
                "C1", orgenhet_C1,
                "C11", orgenhet_C11,
                "C2", orgenhet_C2,
                "C22", orgenhet_C22
            ));

            {// fjern orgenheter fra output det ikke ble bedt om
                List<String> args = it.getArgument(0);
                outMap.keySet().stream().filter(k -> !args.contains(k)).toList().forEach(outMap::remove);
            }
            return outMap;
        });

        when(orgenhetService.getAllEnheterMedOrganisering()).thenReturn(List.of(
            orgenhet_X,
            orgenhet_A, orgenhet_A1, orgenhet_A11,
            orgenhet_B, orgenhet_B1, orgenhet_B11,
            orgenhet_C, orgenhet_C1, orgenhet_C11, orgenhet_C2, orgenhet_C22,
            orgenhet_D, orgenhet_D1,
            orgenhet_E, orgenhet_E1
        ));

        when(orgenhetService.getEnheterByLeder("A")).thenReturn(List.of(orgenhet_A));
        when(orgenhetService.getEnheterByLeder("A1")).thenReturn(List.of(orgenhet_A1));
        when(orgenhetService.getEnheterByLeder("A11")).thenReturn(List.of(orgenhet_A11));
        when(orgenhetService.getEnheterByLeder("B")).thenReturn(List.of(orgenhet_B));
        when(orgenhetService.getEnheterByLeder("B1")).thenReturn(List.of(orgenhet_B1));
        when(orgenhetService.getEnheterByLeder("B11")).thenReturn(List.of(orgenhet_B11));
        when(orgenhetService.getEnheterByLeder("C")).thenReturn(List.of(orgenhet_C));
        when(orgenhetService.getEnheterByLeder("C1")).thenReturn(List.of(orgenhet_C1));
        when(orgenhetService.getEnheterByLeder("C11")).thenReturn(List.of(orgenhet_C11));
        when(orgenhetService.getEnheterByLeder("C2")).thenReturn(List.of(orgenhet_C2));
        when(orgenhetService.getEnheterByLeder("C22")).thenReturn(List.of(orgenhet_C22));
        when(orgenhetService.getEnheterByLeder("D")).thenReturn(List.of(orgenhet_D));
        when(orgenhetService.getEnheterByLeder("D1")).thenReturn(List.of(orgenhet_D1));
        when(orgenhetService.getEnheterByLeder("E")).thenReturn(List.of(orgenhet_E));
        when(orgenhetService.getEnheterByLeder("E1")).thenReturn(List.of(orgenhet_E1));
        when(orgenhetService.getEnheterByLeder("H1")).thenReturn(List.of(orgenhet_helfo));
        when(orgenhetService.getEnheterByLeder("S1")).thenReturn(List.of(orgenhet_skatt));
        when(orgenhetService.getEnheterByLeder("R1")).thenReturn(List.of(orgenhet_riksrev));
    }

    private void setupBasisOrganisering() {

        OrganiseringState organisering_A_X = OrganiseringState.builder()
            .fid("FID_A_X")
            .id("A")
            .navn("OrgEnhet A")
            .idOver("X")
            .navnOver("OrgEnhet X")
            .gyldigFom(LocalDate.now().minusDays(5))
            .gyldigTom(null)
            .build();

        OrganiseringState organisering_A1_A = OrganiseringState.builder()
            .fid("FID_A1_A")
            .id("A1")
            .navn("OrgEnhet A1")
            .idOver("A")
            .navnOver("OrgEnhet A")
            .gyldigFom(LocalDate.now().minusDays(5))
            .gyldigTom(null)
            .build();

        OrganiseringState organisering_A11_A1 = OrganiseringState.builder()
            .fid("FID_A11_A1")
            .id("A11")
            .navn("OrgEnhet A11")
            .idOver("A1")
            .navnOver("OrgEnhet A1")
            .gyldigFom(LocalDate.now().minusDays(5))
            .gyldigTom(null)
            .build();


        OrganiseringState organisering_B_A = OrganiseringState.builder()
            .fid("FID_B_A")
            .id("B")
            .navn("OrgEnhet B")
            .idOver("A")
            .navnOver("OrgEnhet A")
            .gyldigFom(LocalDate.now().minusDays(5))
            .gyldigTom(null)
            .build();

        OrganiseringState organisering_B1_B = OrganiseringState.builder()
            .fid("FID_B1_B")
            .id("B1")
            .navn("OrgEnhet B1")
            .idOver("B")
            .navnOver("OrgEnhet B")
            .gyldigFom(LocalDate.now().minusDays(5))
            .gyldigTom(null)
            .build();

        OrganiseringState organisering_B11_B1 = OrganiseringState.builder()
            .fid("FID_B11_B1")
            .id("B11")
            .navn("OrgEnhet B11")
            .idOver("B1")
            .navnOver("OrgEnhet B1")
            .gyldigFom(LocalDate.now().minusDays(5))
            .gyldigTom(null)
            .build();

        OrganiseringState organisering_C_A = OrganiseringState.builder()
            .fid("FID_C_A")
            .id("C")
            .navn("OrgEnhet C")
            .idOver("A")
            .navnOver("OrgEnhet A")
            .gyldigFom(LocalDate.now().minusDays(5))
            .gyldigTom(null)
            .build();

        OrganiseringState organisering_C1_C = OrganiseringState.builder()
            .fid("FID_C1_C")
            .id("C1")
            .navn("OrgEnhet C1")
            .idOver("C")
            .navnOver("OrgEnhet C")
            .gyldigFom(LocalDate.now().minusDays(5))
            .gyldigTom(null)
            .build();

        OrganiseringState organisering_C11_C1 = OrganiseringState.builder()
            .fid("FID_C11_C1")
            .id("C11")
            .navn("OrgEnhet C11")
            .idOver("C1")
            .navnOver("OrgEnhet C1")
            .gyldigFom(LocalDate.now().minusDays(5))
            .gyldigTom(null)
            .build();

        OrganiseringState organisering_C2_C = OrganiseringState.builder()
            .fid("FID_C2_C")
            .id("C2")
            .navn("OrgEnhet C2")
            .idOver("C")
            .navnOver("OrgEnhet C")
            .gyldigFom(LocalDate.now().minusDays(5))
            .gyldigTom(null)
            .build();

        OrganiseringState organisering_C22_C2 = OrganiseringState.builder()
            .fid("FID_C22_C2")
            .id("C22")
            .navn("OrgEnhet C22")
            .idOver("C2")
            .navnOver("OrgEnhet C2")
            .gyldigFom(LocalDate.now().minusDays(5))
            .gyldigTom(null)
            .build();

        OrganiseringState organisering_D_A = OrganiseringState.builder()
            .fid("FID_D_A")
            .id("D")
            .navn("OrgEnhet D")
            .idOver("A")
            .navnOver("OrgEnhet A")
            .gyldigFom(LocalDate.now().minusDays(5))
            .gyldigTom(null)
            .build();

        OrganiseringState organisering_D1_D = OrganiseringState.builder()
            .fid("FID_D1_D")
            .id("D1")
            .navn("OrgEnhet D1")
            .idOver("D")
            .navnOver("OrgEnhet D")
            .gyldigFom(LocalDate.now().minusDays(5))
            .gyldigTom(null)
            .build();

        OrganiseringState organisering_E_A = OrganiseringState.builder()
            .fid("FID_E_A")
            .id("E")
            .navn("OrgEnhet E")
            .idOver("A")
            .navnOver("OrgEnhet A")
            .gyldigFom(LocalDate.now().minusDays(5))
            .gyldigTom(null)
            .build();

        OrganiseringState organisering_E1_E = OrganiseringState.builder()
            .fid("FID_E1_E")
            .id("E1")
            .navn("OrgEnhet E1")
            .idOver("E")
            .navnOver("OrgEnhet E")
            .gyldigFom(LocalDate.now().minusDays(5))
            .gyldigTom(null)
            .build();

        when(organiseringStateRepository.getActiveOrganisering()).thenReturn(Map.ofEntries(
            Map.entry("FID_A_X", organisering_A_X),
            Map.entry("FID_A1_A", organisering_A1_A),
            Map.entry("FID_A11_A1", organisering_A11_A1),
            Map.entry("FID_B_A", organisering_B_A),
            Map.entry("FID_B1_B", organisering_B1_B),
            Map.entry("FID_B11_B1", organisering_B11_B1),
            Map.entry("FID_C_A", organisering_C_A),
            Map.entry("FID_C1_C", organisering_C1_C),
            Map.entry("FID_C11_C1", organisering_C11_C1),
            Map.entry("FID_C2_C", organisering_C2_C),
            Map.entry("FID_C22_C2", organisering_C22_C2),
            Map.entry("FID_D_A", organisering_D_A),
            Map.entry("FID_D1_D", organisering_D1_D),
            Map.entry("FID_E_A", organisering_E_A),
            Map.entry("FID_E1_E", organisering_E1_E)
        ));

        lenient().when(organiseringStateRepository.findActiveById("A")).thenReturn(Optional.of(organisering_A_X));
        lenient().when(organiseringStateRepository.findActiveById("A1")).thenReturn(Optional.of(organisering_A1_A));
        lenient().when(organiseringStateRepository.findActiveById("A11")).thenReturn(Optional.of(organisering_A11_A1));
        lenient().when(organiseringStateRepository.findActiveById("B")).thenReturn(Optional.of(organisering_B_A));
        lenient().when(organiseringStateRepository.findActiveById("B1")).thenReturn(Optional.of(organisering_B1_B));
        lenient().when(organiseringStateRepository.findActiveById("B11")).thenReturn(Optional.of(organisering_B11_B1));
        lenient().when(organiseringStateRepository.findActiveById("C")).thenReturn(Optional.of(organisering_C_A));
        lenient().when(organiseringStateRepository.findActiveById("C1")).thenReturn(Optional.of(organisering_C1_C));
        lenient().when(organiseringStateRepository.findActiveById("C11")).thenReturn(Optional.of(organisering_C11_C1));
        lenient().when(organiseringStateRepository.findActiveById("C2")).thenReturn(Optional.of(organisering_C2_C));
        lenient().when(organiseringStateRepository.findActiveById("C22")).thenReturn(Optional.of(organisering_C22_C2));
        lenient().when(organiseringStateRepository.findActiveById("D")).thenReturn(Optional.of(organisering_D_A));
        lenient().when(organiseringStateRepository.findActiveById("D1")).thenReturn(Optional.of(organisering_D1_D));
        lenient().when(organiseringStateRepository.findActiveById("E")).thenReturn(Optional.of(organisering_E_A));
        lenient().when(organiseringStateRepository.findActiveById("E1")).thenReturn(Optional.of(organisering_E1_E));
    }

    private void setupBasisLedere() {
        DvhLeder leder_A = DvhLeder.builder()
            .navIdent("A")
            .orgEnhetNr("A")
            .orgenhetNavn("OrgEnhet A")
            .orgNiv("NAV")
            .build();

        DvhLeder leder_A1 = DvhLeder.builder()
            .navIdent("A1")
            .orgEnhetNr("A1")
            .orgenhetNavn("OrgEnhet A1")
            .orgNiv("ORGNIV2")
            .build();

        DvhLeder leder_A11 = DvhLeder.builder()
            .navIdent("A11")
            .orgEnhetNr("A11")
            .orgenhetNavn("OrgEnhet A11")
            .orgNiv("ORGNIV3")
            .build();

        DvhLeder leder_B = DvhLeder.builder()
            .navIdent("B")
            .orgEnhetNr("B")
            .orgenhetNavn("OrgEnhet B")
            .orgNiv("ORGNIV3")
            .build();

        DvhLeder leder_B1 = DvhLeder.builder()
            .navIdent("B1")
            .orgEnhetNr("B1")
            .orgenhetNavn("OrgEnhet B1")
            .orgNiv("ORGNIV4")
            .build();

        DvhLeder leder_B11 = DvhLeder.builder()
            .navIdent("B1")
            .orgEnhetNr("B11")
            .orgenhetNavn("OrgEnhet B11")
            .orgNiv("ORGENHET")
            .build();

        DvhLeder leder_C = DvhLeder.builder()
            .navIdent("C")
            .orgEnhetNr("C")
            .orgenhetNavn("OrgEnhet C")
            .orgNiv("ORGNIV3")
            .build();

        DvhLeder leder_C1 = DvhLeder.builder()
            .navIdent("C")
            .orgEnhetNr("C1")
            .orgenhetNavn("OrgEnhet C1")
            .orgNiv("ORGNIV4")
            .build();

        DvhLeder leder_C11 = DvhLeder.builder()
            .navIdent("C3")
            .orgEnhetNr("C11")
            .orgenhetNavn("OrgEnhet C11")
            .orgNiv("ORGENHET")
            .build();

        DvhLeder leder_C2 = DvhLeder.builder()
            .navIdent("C2")
            .orgEnhetNr("C2")
            .orgenhetNavn("OrgEnhet C2")
            .orgNiv("ORGNIV4")
            .build();

        DvhLeder leder_C22 = DvhLeder.builder()
            .navIdent("C2")
            .orgEnhetNr("C22")
            .orgenhetNavn("OrgEnhet C22")
            .orgNiv("ORGENHET")
            .build();

        DvhLeder leder_D = DvhLeder.builder()
            .navIdent("D")
            .orgEnhetNr("D")
            .orgenhetNavn("OrgEnhet D")
            .orgNiv("ORGNIV3")
            .build();

        DvhLeder leder_D1 = DvhLeder.builder()
            .navIdent("D")
            .orgEnhetNr("D1")
            .orgenhetNavn("OrgEnhet D1")
            .orgNiv("ORGENHET")
            .build();

        DvhLeder leder_E = DvhLeder.builder()
            .navIdent("D")
            .orgEnhetNr("E")
            .orgenhetNavn("OrgEnhet E")
            .orgNiv("ORGNIV3")
            .build();

        DvhLeder leder_E1 = DvhLeder.builder()
            .navIdent("E1")
            .orgEnhetNr("E1")
            .orgenhetNavn("OrgEnhet E1")
            .orgNiv("ORGENHET")
            .build();

        DvhLeder leder_HELFO = DvhLeder.builder()
            .navIdent("H1")
            .orgEnhetNr("HELFO")
            .orgenhetNavn("OrgEnhet HELFO")
            .orgNiv("NAV")
            .build();

        DvhLeder leder_SKATT = DvhLeder.builder()
            .navIdent("S1")
            .orgEnhetNr("SKATT")
            .orgenhetNavn("OrgEnhet SKATT")
            .orgNiv("NAV")
            .build();

        DvhLeder leder_RIKSREV = DvhLeder.builder()
            .navIdent("R1")
            .orgEnhetNr("RIKSREV")
            .orgenhetNavn("OrgEnhet RIKSREV")
            .orgNiv("NAV")
            .build();

        when(dvhRepository.getLedere()).thenReturn(List.of(
            leder_A, leder_A1, leder_A11, leder_B, leder_B1, leder_B11, leder_C, leder_C1, leder_C11, leder_C2, leder_C22, leder_D, leder_D1, leder_E, leder_E1, leder_HELFO, leder_SKATT, leder_RIKSREV
        ));
    }

    private void setupBasisOrgtilknytninger() {
        OrgtilknytningState ot_B11_B110001 = OrgtilknytningState.builder()
            .fid("FID_B11_B110001")
            .navident("B110001")
            .orgEnhetId("B11")
            .gyldigFom(LocalDate.now().minusDays(5))
            .gyldigTom(null)
            .build();

        OrgtilknytningState ot_B11_B110002 = OrgtilknytningState.builder()
            .fid("FID_B11_B110002")
            .navident("B110002")
            .orgEnhetId("B11")
            .gyldigFom(LocalDate.now().minusDays(5))
            .gyldigTom(null)
            .build();

        OrgtilknytningState ot_C11_C110001 = OrgtilknytningState.builder()
            .fid("FID_C11_C10001")
            .navident("C110001")
            .orgEnhetId("C11")
            .gyldigFom(LocalDate.now().minusDays(5))
            .gyldigTom(null)
            .build();

        OrgtilknytningState ot_C22_C220001 = OrgtilknytningState.builder()
            .fid("FID_C22_C220001")
            .navident("C220001")
            .orgEnhetId("C22")
            .gyldigFom(LocalDate.now().minusDays(5))
            .gyldigTom(null)
            .build();

        OrgtilknytningState ot_D1_D100001 = OrgtilknytningState.builder()
            .fid("FID_D1_D100001")
            .navident("D100001")
            .orgEnhetId("D1")
            .gyldigFom(LocalDate.now().minusDays(5))
            .gyldigTom(null)
            .build();

        OrgtilknytningState ot_E1_E100001 = OrgtilknytningState.builder()
            .fid("FID_E1_E100001")
            .navident("E100001")
            .orgEnhetId("E1")
            .gyldigFom(LocalDate.now().minusDays(5))
            .gyldigTom(null)
            .build();

        OrgtilknytningState ot_HELFO_H100001 = OrgtilknytningState.builder()
            .fid("FID_HELFO_H100001")
            .navident("H100001")
            .orgEnhetId("dummyHelfoId")
            .gyldigFom(LocalDate.now().minusDays(10))
            .gyldigTom(null)
            .build();

        OrgtilknytningState ot_SKATT_S100001 = OrgtilknytningState.builder()
            .fid("FID_SKATT_S100001")
            .navident("S100001")
            .orgEnhetId("dummySkattId")
            .gyldigFom(LocalDate.now().minusDays(10))
            .gyldigTom(null)
            .build();

        OrgtilknytningState ot_RIKSREV_R100001 = OrgtilknytningState.builder()
            .fid("FID_RIKSREV_R100001")
            .navident("R100001")
            .orgEnhetId("dummyRiksrevisjonId")
            .gyldigFom(LocalDate.now().minusDays(10))
            .gyldigTom(null)
            .build();

        lenient().when(orgtilknytningStateRepository.findByNavident("B110001")).thenReturn(List.of(ot_B11_B110001));
        lenient().when(orgtilknytningStateRepository.findByNavident("B110002")).thenReturn(List.of(ot_B11_B110002));
        lenient().when(orgtilknytningStateRepository.findByNavident("C110001")).thenReturn(List.of(ot_C11_C110001));
        lenient().when(orgtilknytningStateRepository.findByNavident("C220001")).thenReturn(List.of(ot_C22_C220001));
        lenient().when(orgtilknytningStateRepository.findByNavident("D100001")).thenReturn(List.of(ot_D1_D100001));
        lenient().when(orgtilknytningStateRepository.findByNavident("E100001")).thenReturn(List.of(ot_E1_E100001));
        lenient().when(orgtilknytningStateRepository.findByNavident("H100001")).thenReturn(List.of(ot_HELFO_H100001));
        lenient().when(orgtilknytningStateRepository.findByNavident("S100001")).thenReturn(List.of(ot_SKATT_S100001));
        lenient().when(orgtilknytningStateRepository.findByNavident("R100001")).thenReturn(List.of(ot_RIKSREV_R100001));
    }

    private void setupRessursState() {
        var hr = RessursState.builder()
            .fid("FID_HR")
            .personident("HR")
            .navident("HR")
            .sektor(Sektor.NAV_STATLIG)
            .visningsnavn(null)
            .sluttdato(null)
            .startdato(LocalDate.now().minusYears(1))
            .primaryFysiskBrukersted(null)
            .fysiskeBrukersteder(List.of("D1"))
            .build();

        var ekstern = RessursState.builder()
            .fid("FID_EKSTERN")
            .personident("EKSTERN")
            .navident("EKSTERN")
            .sektor(Sektor.EKSTERN)
            .visningsnavn(null)
            .sluttdato(null)
            .startdato(LocalDate.now().minusYears(1))
            .primaryFysiskBrukersted(null)
            .fysiskeBrukersteder(List.of("D1"))
            .build();

        var helfo = RessursState.builder()
            .fid("FID_HELFO")
            .personident("098765431234")
            .navident("H100001")
            .sektor(Sektor.EKSTERN)
            .visningsnavn(null)
            .sluttdato(null)
            .startdato(LocalDate.now().minusYears(1))
            .primaryFysiskBrukersted(null)
            .fysiskeBrukersteder(List.of("HELFO"))
            .build();

        var skatt = RessursState.builder()
            .fid("FID_SKATT")
            .personident("12345678901")
            .navident("S100001")
            .sektor(Sektor.EKSTERN)
            .visningsnavn(null)
            .sluttdato(null)
            .startdato(LocalDate.now().minusYears(1))
            .primaryFysiskBrukersted(null)
            .fysiskeBrukersteder(List.of("SKATT"))
            .build();

        var riksrevisjonen = RessursState.builder()
            .fid("FID_RIKSREV")
            .personident("12345678902")
            .navident("R100001")
            .sektor(Sektor.NAV_STATLIG)
            .visningsnavn(null)
            .sluttdato(null)
            .startdato(LocalDate.now().minusYears(1))
            .primaryFysiskBrukersted(null)
            .fysiskeBrukersteder(List.of("EKSTERN"))
            .build();

        lenient().when(ressursStateRepository.findByNavident("HR")).thenReturn(Optional.of(hr));
        lenient().when(ressursStateRepository.findByNavident("EKSTERN")).thenReturn(Optional.of(ekstern));
        lenient().when(ressursStateRepository.findByNavident("H100001")).thenReturn(Optional.of(helfo));
        lenient().when(ressursStateRepository.findByNavident("S100001")).thenReturn(Optional.of(skatt));
        lenient().when(ressursStateRepository.findByNavident("R100001")).thenReturn(Optional.of(riksrevisjonen));
    }

    @Getter
    @Builder
    public static class MockOrgenhet implements IOrgenhet {
        String fid;
        String id;
        String agressoId;
        String agressoOrgenhetType;
        String navn;
        String orgNiv;
        LocalDate gyldigFom;
        LocalDate gyldigTom;
        NomNivaa nomNivaa;
        OrgEnhetsType orgEnhetsType;
        String uniqueAgressoId;
        String overordnetOrgEnhetFid;
    }
}
