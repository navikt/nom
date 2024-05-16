package no.nav.nom.apps.api.authorize.redigereressurs;

import lombok.Builder;
import lombok.Getter;
import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.dvh.dto.DvhLeder;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.libs.state.organisering.OrganiseringStateRepository;
import no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateRepository;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import no.nav.nom.contract.nom.NomNivaa;
import no.nav.nom.contract.nom.OrgEnhetsType;
import no.nav.nom.contract.nom.state.OrganiseringState;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.contract.nom.state.RessursState;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

abstract public class MockBasisTestOrganisering {
    @MockBean
    OrgtilknytningStateRepository orgtilknytningStateRepository;
    @MockBean
    OrganiseringStateRepository organiseringStateRepository;
    @MockBean
    RessursStateRepository ressursStateRepository;
    @MockBean
    OrgenhetService orgenhetService;
    @MockBean
    DvhRepository dvhRepository;
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
        IOrgenhet orgenhet_A = MockOrgenhet.builder()
            .fid("FID_A")
            .id("A")
            .navn("OrgEnhet A")
            .nomNivaa(Optional.of(NomNivaa.DRIFTSENHET))
            .agressoId("A")
            .orgNiv("NAV")
            .build();

        IOrgenhet orgenhet_B = MockOrgenhet.builder()
            .fid("FID_B")
            .id("B")
            .navn("OrgEnhet B")
            .nomNivaa(Optional.of(NomNivaa.ARBEIDSOMRAADE))
            .agressoId("B")
            .orgNiv("ORGNIV3")
            .build();

        IOrgenhet orgenhet_B1 = MockOrgenhet.builder()
            .fid("FID_B1")
            .id("B1")
            .navn("OrgEnhet B1")
            .nomNivaa(Optional.empty())
            .agressoId("B1")
            .orgNiv("ORGNIV4")
            .build();

        IOrgenhet orgenhet_B11 = MockOrgenhet.builder()
            .fid("FID_B11")
            .id("B11")
            .navn("OrgEnhet B11")
            .nomNivaa(Optional.empty())
            .agressoId("B11")
            .orgNiv("ORGENHET")
            .build();

        IOrgenhet orgenhet_C = MockOrgenhet.builder()
            .fid("FID_C")
            .id("C")
            .navn("OrgEnhet C")
            .nomNivaa(Optional.of(NomNivaa.ARBEIDSOMRAADE))
            .agressoId("C")
            .orgNiv("ORGNIV3")
            .build();

        IOrgenhet orgenhet_C1 = MockOrgenhet.builder()
            .fid("FID_C1")
            .id("C1")
            .navn("OrgEnhet C1")
            .nomNivaa(Optional.empty())
            .agressoId("C1")
            .orgNiv("ORGNIV4")
            .build();

        IOrgenhet orgenhet_C11 = MockOrgenhet.builder()
            .fid("FID_C11")
            .id("C11")
            .navn("OrgEnhet C11")
            .nomNivaa(Optional.empty())
            .agressoId("C11")
            .orgNiv("ORGENHET")
            .build();

        IOrgenhet orgenhet_C2 = MockOrgenhet.builder()
            .fid("FID_C2")
            .id("C2")
            .navn("OrgEnhet C2")
            .nomNivaa(Optional.empty())
            .agressoId("C2")
            .orgNiv("ORGNIV4")
            .build();

        IOrgenhet orgenhet_C22 = MockOrgenhet.builder()
            .fid("FID_C22")
            .id("C22")
            .navn("OrgEnhet C22")
            .nomNivaa(Optional.empty())
            .agressoId("C22")
            .orgNiv("ORGENHET")
            .build();

        IOrgenhet orgenhet_D = MockOrgenhet.builder()
            .fid("FID_D")
            .id("D")
            .navn("OrgEnhet D")
            .nomNivaa(Optional.of(NomNivaa.ARBEIDSOMRAADE))
            .agressoId("D")
            .orgNiv("ORGNIV3")
            .build();

        IOrgenhet orgenhet_D1 = MockOrgenhet.builder()
            .fid("FID_D1")
            .id("D1")
            .navn("OrgEnhet D1")
            .nomNivaa(Optional.empty())
            .agressoId("D1")
            .orgNiv("ORGENHET")
            .build();

        IOrgenhet orgenhet_E = MockOrgenhet.builder()
            .fid("FID_E")
            .id("E")
            .navn("OrgEnhet E")
            .nomNivaa(Optional.of(NomNivaa.ARBEIDSOMRAADE))
            .agressoId("E")
            .orgNiv("ORGNIV3")
            .build();

        IOrgenhet orgenhet_E1 = MockOrgenhet.builder()
            .fid("FID_E1")
            .id("E1")
            .navn("OrgEnhet E1")
            .nomNivaa(Optional.empty())
            .agressoId("E1")
            .orgNiv("ORGENHET")
            .build();

        lenient().when(orgenhetService.getById("A")).thenReturn(Optional.of(orgenhet_A));
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

        when(orgenhetService.getAllEnheterMedOrganisering()).thenReturn(List.of(
            orgenhet_A, orgenhet_B, orgenhet_B1, orgenhet_B11, orgenhet_C, orgenhet_C1, orgenhet_C11,
            orgenhet_C2, orgenhet_C22, orgenhet_D, orgenhet_D1, orgenhet_E, orgenhet_E1
        ));
    }

    private void setupBasisOrganisering() {
        OrganiseringState organisering_B_A = OrganiseringState.builder()
            .fid("FID_B_A")
            .id("B")
            .navn("OrgEnhet B")
            .idOver("A")
            .navnOver("OrgEnhet A")
            .gyldigFom(LocalDate.now().minusDays(5))
            .gyldigTom(Optional.empty())
            .build();

        OrganiseringState organisering_B1_B = OrganiseringState.builder()
            .fid("FID_B1_B")
            .id("B1")
            .navn("OrgEnhet B1")
            .idOver("B")
            .navnOver("OrgEnhet B")
            .gyldigFom(LocalDate.now().minusDays(5))
            .gyldigTom(Optional.empty())
            .build();

        OrganiseringState organisering_B11_B1 = OrganiseringState.builder()
            .fid("FID_B11_B1")
            .id("B11")
            .navn("OrgEnhet B11")
            .idOver("B1")
            .navnOver("OrgEnhet B1")
            .gyldigFom(LocalDate.now().minusDays(5))
            .gyldigTom(Optional.empty())
            .build();

        OrganiseringState organisering_C_A = OrganiseringState.builder()
            .fid("FID_C_A")
            .id("C")
            .navn("OrgEnhet C")
            .idOver("A")
            .navnOver("OrgEnhet A")
            .gyldigFom(LocalDate.now().minusDays(5))
            .gyldigTom(Optional.empty())
            .build();

        OrganiseringState organisering_C1_C = OrganiseringState.builder()
            .fid("FID_C1_C")
            .id("C1")
            .navn("OrgEnhet C1")
            .idOver("C")
            .navnOver("OrgEnhet C")
            .gyldigFom(LocalDate.now().minusDays(5))
            .gyldigTom(Optional.empty())
            .build();

        OrganiseringState organisering_C11_C1 = OrganiseringState.builder()
            .fid("FID_C11_C1")
            .id("C11")
            .navn("OrgEnhet C11")
            .idOver("C1")
            .navnOver("OrgEnhet C1")
            .gyldigFom(LocalDate.now().minusDays(5))
            .gyldigTom(Optional.empty())
            .build();

        OrganiseringState organisering_C2_C = OrganiseringState.builder()
            .fid("FID_C2_C")
            .id("C2")
            .navn("OrgEnhet C2")
            .idOver("C")
            .navnOver("OrgEnhet C")
            .gyldigFom(LocalDate.now().minusDays(5))
            .gyldigTom(Optional.empty())
            .build();

        OrganiseringState organisering_C22_C2 = OrganiseringState.builder()
            .fid("FID_C22_C2")
            .id("C22")
            .navn("OrgEnhet C22")
            .idOver("C2")
            .navnOver("OrgEnhet C2")
            .gyldigFom(LocalDate.now().minusDays(5))
            .gyldigTom(Optional.empty())
            .build();

        OrganiseringState organisering_D_A = OrganiseringState.builder()
            .fid("FID_D_A")
            .id("D")
            .navn("OrgEnhet D")
            .idOver("A")
            .navnOver("OrgEnhet A")
            .gyldigFom(LocalDate.now().minusDays(5))
            .gyldigTom(Optional.empty())
            .build();

        OrganiseringState organisering_D1_D = OrganiseringState.builder()
            .fid("FID_D1_D")
            .id("D1")
            .navn("OrgEnhet D1")
            .idOver("D")
            .navnOver("OrgEnhet D")
            .gyldigFom(LocalDate.now().minusDays(5))
            .gyldigTom(Optional.empty())
            .build();

        OrganiseringState organisering_E_A = OrganiseringState.builder()
            .fid("FID_E_A")
            .id("E")
            .navn("OrgEnhet E")
            .idOver("A")
            .navnOver("OrgEnhet A")
            .gyldigFom(LocalDate.now().minusDays(5))
            .gyldigTom(Optional.empty())
            .build();

        OrganiseringState organisering_E1_E = OrganiseringState.builder()
            .fid("FID_E1_E")
            .id("E1")
            .navn("OrgEnhet E1")
            .idOver("E")
            .navnOver("OrgEnhet E")
            .gyldigFom(LocalDate.now().minusDays(5))
            .gyldigTom(Optional.empty())
            .build();

        when(organiseringStateRepository.getActiveOrganisering()).thenReturn(Map.ofEntries(
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
    }

    private void setupBasisLedere() {
        DvhLeder leder_A = DvhLeder.builder()
            .navIdent("A")
            .orgEnhetNr("A")
            .orgenhetNavn("OrgEnhet A")
            .orgNiv("NAV")
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

        when(dvhRepository.getLedere()).thenReturn(List.of(
            leder_A, leder_B, leder_B1, leder_B11, leder_C, leder_C1, leder_C11, leder_C2, leder_C22, leder_D, leder_D1, leder_E, leder_E1
        ));
    }

    private void setupBasisOrgtilknytninger() {
        OrgtilknytningState ot_B11_B110001 = OrgtilknytningState.builder()
            .fid("FID_B11_B110001")
            .navident("B110001")
            .orgEnhetId("B11")
            .gyldigFom(LocalDate.now().minusDays(5))
            .gyldigTom(Optional.empty())
            .build();

        OrgtilknytningState ot_B11_B110002 = OrgtilknytningState.builder()
            .fid("FID_B11_B110002")
            .navident("B110002")
            .orgEnhetId("B11")
            .gyldigFom(LocalDate.now().minusDays(5))
            .gyldigTom(Optional.empty())
            .build();

        OrgtilknytningState ot_C11_C110001 = OrgtilknytningState.builder()
            .fid("FID_C11_C10001")
            .navident("C110001")
            .orgEnhetId("C11")
            .gyldigFom(LocalDate.now().minusDays(5))
            .gyldigTom(Optional.empty())
            .build();

        OrgtilknytningState ot_C22_C220001 = OrgtilknytningState.builder()
            .fid("FID_C22_C220001")
            .navident("C220001")
            .orgEnhetId("C22")
            .gyldigFom(LocalDate.now().minusDays(5))
            .gyldigTom(Optional.empty())
            .build();

        OrgtilknytningState ot_D1_D100001 = OrgtilknytningState.builder()
            .fid("FID_D1_D100001")
            .navident("D100001")
            .orgEnhetId("D1")
            .gyldigFom(LocalDate.now().minusDays(5))
            .gyldigTom(Optional.empty())
            .build();

        OrgtilknytningState ot_E1_E100001 = OrgtilknytningState.builder()
            .fid("FID_E1_E100001")
            .navident("E100001")
            .orgEnhetId("E1")
            .gyldigFom(LocalDate.now().minusDays(5))
            .gyldigTom(Optional.empty())
            .build();

        lenient().when(orgtilknytningStateRepository.findByNavident("B110001")).thenReturn(List.of(ot_B11_B110001));
        lenient().when(orgtilknytningStateRepository.findByNavident("B110002")).thenReturn(List.of(ot_B11_B110002));
        lenient().when(orgtilknytningStateRepository.findByNavident("C110001")).thenReturn(List.of(ot_C11_C110001));
        lenient().when(orgtilknytningStateRepository.findByNavident("C220001")).thenReturn(List.of(ot_C22_C220001));
        lenient().when(orgtilknytningStateRepository.findByNavident("D100001")).thenReturn(List.of(ot_D1_D100001));
        lenient().when(orgtilknytningStateRepository.findByNavident("E100001")).thenReturn(List.of(ot_E1_E100001));
    }

    private void setupRessursState() {
        var hr = RessursState.builder()
            .fid("FID_HR")
            .personident("HR")
            .navident("HR")
            .sektor(null)
            .visningsnavn(null)
            .sluttdato(Optional.empty())
            .startdato(Optional.of(LocalDate.now().minusYears(1)))
            .primaryFysiskBrukersted(Optional.empty())
            .fysiskeBrukersteder(List.of("D1"))
            .build();

        lenient().when(ressursStateRepository.findByNavident("HR")).thenReturn(Optional.of(hr));
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
        Optional<LocalDate> gyldigTom;
        Optional<NomNivaa> nomNivaa;
        Optional<OrgEnhetsType> orgEnhetsType;
        String uniqueAgressoId;
        Optional<String> overordnetOrgEnhetFid;
    }
}
