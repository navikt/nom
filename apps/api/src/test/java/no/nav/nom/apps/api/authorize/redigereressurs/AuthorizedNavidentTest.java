package no.nav.nom.apps.api.authorize.redigereressurs;

import lombok.Builder;
import lombok.Getter;
import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.dvh.dto.DvhLeder;
import no.nav.nom.apps.api.organisering.eventsourcing.state.OrganiseringStateRepository;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.state.OrgtilknytningStateRepository;
import no.nav.nom.apps.api.ressurs.eventsourcing.state.RessursStateRepository;
import no.nav.nom.contract.nom.NomNivaa;
import no.nav.nom.contract.nom.OrgEnhetsType;
import no.nav.nom.contract.nom.state.OrganiseringState;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.contract.nom.state.RessursState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthorizedNavidentTest {

    @Mock
    private OrgtilknytningStateRepository orgtilknytningStateRepository;
    @Mock
    private OrganiseringStateRepository organiseringStateRepository;
    @Mock
    RessursStateRepository ressursStateRepository;
    @Mock
    private OrgenhetService orgenhetService;
    @Mock
    private DvhRepository dvhRepository;

    @InjectMocks
    private AuthorizedNavident authorizedNavident;

    @Test
    @DisplayName("Verifiser at Leder B har auth til å redigere ressurser B110001 og B110002")
    void test_0001() {
        // Arrange
        setupBasisOrganisasjon();

        // Act
        boolean kanRedigereB110001 = authorizedNavident.kanBrukerRedigereRessurs("B", "B110001");
        boolean kanRedigereB110002 = authorizedNavident.kanBrukerRedigereRessurs("B", "B110002");
        // Assert
        assertTrue(kanRedigereB110001, "Leder B har ikke auth til å redigere ressurs B110001");
        assertTrue(kanRedigereB110002, "Leder B har ikke auth til å redigere ressurs B110002");
    }

    @Test
    @DisplayName("Verifiser at Leder A ikke har auth til å redigere ressurser B110001 og B110002")
    void test_0002() {
        // Arrange
        setupBasisOrganisasjon();

        // Act
        boolean kanRedigereB110001 = authorizedNavident.kanBrukerRedigereRessurs("A", "B110001");
        boolean kanRedigereB110002 = authorizedNavident.kanBrukerRedigereRessurs("A", "B110002");
        // Assert
        assertFalse(kanRedigereB110001, "Leder A har auth til å redigere ressurs B110001");
        assertFalse(kanRedigereB110002, "Leder A har auth til å redigere ressurs B110002");
    }

    @Test
    @DisplayName("Verifiser at Leder B ikke har auth til å redigere ressurser C110001")
    void test_0003() {
        // Arrange
        setupBasisOrganisasjon();
        // Act
        boolean kanRedigereC110001 = authorizedNavident.kanBrukerRedigereRessurs("B", "C110001");
        // Assert
        assertFalse(kanRedigereC110001, "Leder B har auth til å redigere ressurs C110001");
    }

    @Test
    @DisplayName("Verifiser at Leder C har auth til å redigere ressurser C110001 og C220001")
    void test_0004() {
        // Arrange
        setupBasisOrganisasjon();
        // Act
        boolean kanRedigereC110001 = authorizedNavident.kanBrukerRedigereRessurs("C", "C110001");
        boolean kanRedigereC220001 = authorizedNavident.kanBrukerRedigereRessurs("C", "C220001");
        // Assert
        assertTrue(kanRedigereC110001, "Leder C har ikke auth til å redigere ressurs C110001");
        assertTrue(kanRedigereC220001, "Leder C har ikke auth til å redigere ressurs C220001");
    }

    @Test
    @DisplayName("Verifiser at Leder C3 har auth til å redigere ressurs C110001 men ikke C220001")
    void test_0005() {
        // Arrange
        setupBasisOrganisasjon();
        // Act
        boolean kanRedigereC110001 = authorizedNavident.kanBrukerRedigereRessurs("C3", "C110001");
        boolean kanRedigereC220001 = authorizedNavident.kanBrukerRedigereRessurs("C3", "C220001");
        // Assert
        assertTrue(kanRedigereC110001, "Leder C3 har ikke auth til å redigere ressurs C110001");
        assertFalse(kanRedigereC220001, "Leder C3 har auth til å redigere ressurs C220001");
    }

    @Test
    @DisplayName("Verifiser at Leder C2 har auth til å redigere ressurs C220001 men ikke C110001")
    void test_0006() {
        // Arrange
        setupBasisOrganisasjon();
        // Act
        boolean kanRedigereC110001 = authorizedNavident.kanBrukerRedigereRessurs("C2", "C110001");
        boolean kanRedigereC220001 = authorizedNavident.kanBrukerRedigereRessurs("C2", "C220001");
        // Assert
        assertFalse(kanRedigereC110001, "Leder C2 har auth til å redigere ressurs C110001");
        assertTrue(kanRedigereC220001, "Leder C2 har ikke auth til å redigere ressurs C220001");
    }

    @Test
    @DisplayName("Verifiser at Leder D har auth til å redigere ressurs D100001 og E100001")
    void test_0008() {
        // Arrange
        setupBasisOrganisasjon();
        // Act
        boolean kanRedigereD100001 = authorizedNavident.kanBrukerRedigereRessurs("D", "D100001");
        boolean kanRedigereE100001 = authorizedNavident.kanBrukerRedigereRessurs("D", "E100001");
        // Assert
        assertTrue(kanRedigereD100001, "Leder D har ikke auth til å redigere ressurs D100001");
        assertTrue(kanRedigereE100001, "Leder D har ikke auth til å redigere ressurs E100001");
    }


    @Test
    @DisplayName("Verifiser at Leder A ikke har auth til å delegere rettighet til ressurs HR for enhet D1")
    void test_0009() {
        // Arrange
        setupBasisOrganisasjon();
        // Act
        Exception exception = assertThrows(NotAuthorizedToDelegateLeder.class, () ->
            authorizedNavident.addDelegertLederForOrgenhet("A", "HR", "D1")
        );
    }

    @Test
    @DisplayName("Verifiser at man ikke kan delegere rettighet til ressurs-redigering for enhet som ikke finnes")
    void test_0010() {
        // Arrange
        setupBasisOrganisasjon();
        // Act
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
            authorizedNavident.addDelegertLederForOrgenhet("A", "HR", "UKJENT")
        );
    }

    @Test
    @DisplayName("Verifiser at man ikke kan delegere rettighet til ressurs-redigering for enhet som ikke er arb.område eller lavere")
    void test_0011() {
        // Arrange
        setupBasisOrganisasjon();
        // Act
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
            authorizedNavident.addDelegertLederForOrgenhet("A", "HR", "A")
        );
    }

    @Test
    @DisplayName("Verifiser at man ikke kan delegere rettighet til ressurs-redigering for ressurs som ikke er aktiv")
    void test_0012() {
        // Arrange
        setupBasisOrganisasjon();
        // Act
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
            authorizedNavident.addDelegertLederForOrgenhet("D", "INGEN_ANSATT", "D1")
        );
    }

    @Test
    @DisplayName("Verifiser at man ikke kan delegere rettighet til seg selv")
    void test_0013() {
        // Arrange
        setupBasisOrganisasjon();
        // Act
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
            authorizedNavident.addDelegertLederForOrgenhet("D", "D", "D1")
        );
    }

    @Test
    @DisplayName("Verifiser at man kan delegere rettighet til sin egen enhet")
    void test_0014() {
        // Arrange
        setupBasisOrganisasjon();
        // Act
        boolean delegert = authorizedNavident.addDelegertLederForOrgenhet("D", "HR", "D");
        // Assert
        assertTrue(delegert, "Leder D Kunne ikke delegere rettighet til ressurs HR for enhet D");
    }

    @Test
    @DisplayName("Verifiser at man kan delegere rettighet til enhet under sin egen")
    void test_0015() {
        // Arrange
        setupBasisOrganisasjon();
        // Act
        boolean delegert = authorizedNavident.addDelegertLederForOrgenhet("D", "HR", "D1");
        // Assert
        assertTrue(delegert, "Leder D Kunne ikke delegere rettighet til ressurs HR for enhet D1");
    }

    @Test
    @DisplayName("Verifiser at delegert leder HR har auth til å redigere ressurs D100001 men ikke E100001")
    void test_0016() {
        // Arrange
        setupBasisOrganisasjon();
        authorizedNavident.addDelegertLederForOrgenhet("D", "HR", "D");

        // Act
        boolean kanRedigereD100001 = authorizedNavident.kanBrukerRedigereRessurs("HR", "D100001");
        boolean kanRedigereE100001 = authorizedNavident.kanBrukerRedigereRessurs("HR", "E100001");
        // Assert
        assertTrue(kanRedigereD100001, "Leder D har ikke auth til å redigere ressurs D100001");
        assertFalse(kanRedigereE100001, "Leder D har auth til å redigere ressurs E100001");
    }


    // Sett opp basic organisasjon aka eksemplet i https://navno.sharepoint.com/:w:/r/sites/NOM-Navorganisasjonsmaster/Shared%20Documents/General/NOM/Under%20arbeid/Tilgangsmodell%20for%20NOM%20-%20v001.docx?d=wcd1dae1bd7774e44b0fa9e340ffecdf9&csf=1&web=1&e=1T7uzW
    private void setupBasisOrganisasjon() {
        seutpBasisOrgenheter();
        setupBasisOrganisering();
        setupBasisLedere();
        setupBasisOrgtilknytninger();
        setupRessursState();

        authorizedNavident.oppdaterLedereForOrgenheter();

    }

    private void seutpBasisOrgenheter() {
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
    private static class MockOrgenhet implements IOrgenhet {
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