package no.nav.nom.apps.admin.sanitycheck.service;

import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.Tilknytningtype;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.organisering.OrganiseringStateRepository;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepository;
import no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateRepository;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RessursSanityCheckServiceTest {

    private static final String ID_ORGENHET_1 = "ab123c";
    private static final String ID_ORGENHET_2 = "de456f";

    @Mock
    private RessursStateRepository ressursStateRepositoryMock;

    @Mock
    private OrgEnhetStateRepository orgEnhetStateRepositoryMock;

    @Mock
    private OrganiseringStateRepository organiseringStateRepositoryMock;

    @Mock
    private OrgtilknytningStateRepository orgtilknytningStateRepositoryMock;


    private RessursSanityCheckService service;


    @BeforeEach
    void setUp() {
        service = new RessursSanityCheckService(
                ressursStateRepositoryMock,
                orgEnhetStateRepositoryMock,
                organiseringStateRepositoryMock,
                orgtilknytningStateRepositoryMock
        );
    }

    @DisplayName("Sjekk for ulik sluttdato ressurs og siste OT")
    @ParameterizedTest(name = "Ressurs sluttdato: {0}, OT gyldigTOM: {1} - forventet antall avvik: {2}")
    @MethodSource("ulikSluttdatoTestCases")
    void should_include_cases_whedsdsdsdsre_sluttdato_is_different(LocalDate sluttDato, LocalDate gyldigTom, int expected) {
        RessursState ressurs = getMockRessursEkstern().toBuilder()
                .sluttdato(Optional.ofNullable(sluttDato))
                .build();
        Map<String, RessursState> ressursStateMap = new HashMap<>();
        ressursStateMap.put(ressurs.navident(), ressurs);
        when(ressursStateRepositoryMock.findAllActiveRessurser()).thenReturn(ressursStateMap);

        var orgtilknytning = getMockOrgtilknytningEkstern().toBuilder()
                .gyldigTom(Optional.ofNullable(gyldigTom))
                .build();
        when(orgtilknytningStateRepositoryMock.findByNavident(ressurs.navident())).thenReturn(List.of(orgtilknytning));

        var ulikeDatoer = service.ressurserMedUlikOrgTilknytningSluttDato();

        assertThat(ulikeDatoer.size()).isEqualTo(expected);
    }

    private static Stream<Arguments> ulikSluttdatoTestCases() {
        return Stream.of(
                Arguments.of(LocalDate.of(2025, 6, 1), LocalDate.of(2025, 6, 1), 0),
                Arguments.of(LocalDate.of(2024, 12, 31), LocalDate.of(2025, 6, 1), 1),
                Arguments.of(null, LocalDate.of(2025, 6, 1), 1)
        );
    }

    @Test
    @DisplayName("Bruker siste OT når sjekker for ulik sluttdato")
    void should_use_latest_orgtilknytning_when_comparing_to_ressurs() {
        RessursState ressurs = getMockRessursEkstern();

        Map<String, RessursState> ressursStateMap = new HashMap<>();
        ressursStateMap.put(ressurs.navident(), ressurs);
        when(ressursStateRepositoryMock.findAllActiveRessurser()).thenReturn(ressursStateMap);

        var gammelOT = getMockOrgtilknytningEkstern().toBuilder()
                .gyldigFom(LocalDate.of(2024, 1, 1))
                .gyldigTom(Optional.of(LocalDate.of(2024, 6, 30)))
                .build();
        var sisteOT = getMockOrgtilknytningEkstern().toBuilder()
                .gyldigFom(LocalDate.of(2024, 7, 1))
                .gyldigTom(Optional.of(LocalDate.of(2025, 6, 1)))
                .build();
        when(orgtilknytningStateRepositoryMock.findByNavident(ressurs.navident())).thenReturn(List.of(gammelOT, sisteOT));


        var res = service.ressurserMedUlikOrgTilknytningSluttDato();

        assertThat(res.size()).isEqualTo(1);
    }

    @Test
    @DisplayName("Alle DOL rapporteres for bruker med overlappende DOL")
    void should_find_overlapping_dol() {
        RessursState ressurs = getMockRessursEkstern();
        when(ressursStateRepositoryMock.findAllActiveRessurser()).thenReturn(getRessursStateMap(ressurs));

        when(orgEnhetStateRepositoryMock.findById(ID_ORGENHET_1)).thenReturn(Optional.of(getMockOrgenhet()));
        OrgEnhetState orgEnhet2 = getMockOrgenhet().toBuilder().id(ID_ORGENHET_2).navn("IT-Utvikling 2").build();
        when(orgEnhetStateRepositoryMock.findById(ID_ORGENHET_2)).thenReturn(Optional.of(orgEnhet2));

        var forsteDOL = getMockOrgtilknytningEkstern().toBuilder()
                .gyldigFom(LocalDate.of(2024, 1, 1))
                .gyldigTom(Optional.of(LocalDate.of(2024, 6, 30)))
                .orgEnhetId(ID_ORGENHET_1)
                .build();
        var andreDOL = getMockOrgtilknytningEkstern().toBuilder()
                .gyldigFom(LocalDate.of(2024, 6, 30))
                .gyldigTom(Optional.of(LocalDate.of(2024, 12, 31)))
                .orgEnhetId(ID_ORGENHET_1)
                .build();
        var tredjeDOL = getMockOrgtilknytningEkstern().toBuilder()
                .gyldigFom(LocalDate.of(2025, 1, 1))
                .gyldigTom(Optional.of(LocalDate.of(2025, 12, 31)))
                .orgEnhetId(ID_ORGENHET_2)
                .build();
        when(orgtilknytningStateRepositoryMock.findByNavident(ressurs.navident())).thenReturn(List.of(forsteDOL, andreDOL, tredjeDOL));

        var ressurserMedFlereDOL = service.ressursMedFlereEnnEnDOL();

        assertThat(ressurserMedFlereDOL.size()).isEqualTo(3);
    }

    @Test
    @DisplayName("Bruker med DOL som ikke overlapper tas ikke med")
    void should_not_include_non_overlapping_dol() {
        RessursState ressurs = getMockRessursEkstern();
        when(ressursStateRepositoryMock.findAllActiveRessurser()).thenReturn(getRessursStateMap(ressurs));

        var forsteDOL = getMockOrgtilknytningEkstern().toBuilder()
                .gyldigFom(LocalDate.of(2024, 1, 1))
                .gyldigTom(Optional.of(LocalDate.of(2024, 6, 30)))
                .orgEnhetId(ID_ORGENHET_1)
                .build();
        var andreDOL = getMockOrgtilknytningEkstern().toBuilder()
                .gyldigFom(LocalDate.of(2024, 7, 1))
                .gyldigTom(Optional.of(LocalDate.of(2024, 12, 31)))
                .orgEnhetId(ID_ORGENHET_1)
                .build();
        when(orgtilknytningStateRepositoryMock.findByNavident(ressurs.navident())).thenReturn(List.of(forsteDOL, andreDOL));

        var ressurserMedFlereDOL = service.ressursMedFlereEnnEnDOL();

        assertThat(ressurserMedFlereDOL).isEmpty();
    }

    @Test
    @DisplayName("Overlapp mellom DOL og annen OT tas ikke med")
    void should_not_include_overlap_with_dol_and_other_orgtilknytning() {
        RessursState ressurs = getMockRessursEkstern();
        when(ressursStateRepositoryMock.findAllActiveRessurser()).thenReturn(getRessursStateMap(ressurs));

        var forsteDOL = getMockOrgtilknytningEkstern().toBuilder()
                .gyldigFom(LocalDate.of(2024, 1, 1))
                .gyldigTom(Optional.of(LocalDate.of(2024, 12, 31)))
                .erDagligOppfolging(true)
                .orgEnhetId(ID_ORGENHET_1)
                .build();
        var andreDOL = getMockOrgtilknytningEkstern().toBuilder()
                .gyldigFom(LocalDate.of(2024, 7, 1))
                .gyldigTom(Optional.of(LocalDate.of(2024, 12, 31)))
                .erDagligOppfolging(false)
                .orgEnhetId(ID_ORGENHET_1)
                .build();
        when(orgtilknytningStateRepositoryMock.findByNavident(ressurs.navident())).thenReturn(List.of(forsteDOL, andreDOL));

        var ressurserMedFlereDOL = service.ressursMedFlereEnnEnDOL();

        assertThat(ressurserMedFlereDOL).isEmpty();
    }

    private Map<String, RessursState> getRessursStateMap(RessursState ressurs) {
        Map<String, RessursState> ressursStateMap = new HashMap<>();
        ressursStateMap.put(ressurs.navident(), ressurs);
        return ressursStateMap;
    }

    private RessursState getMockRessursEkstern() {
        return RessursState.builder()
                .fid("R-FID")
                .personident("01234598765")
                .navident("X123456")
                .sektor(Sektor.EKSTERN)
                .visningsnavn(new Visningsnavn("Nav", Optional.empty(), "Navesen"))
                .primaryTelefonnummer(Optional.of("12345678"))
                .startdato(Optional.of(LocalDate.of(2024, 1, 1)))
                .sluttdato(Optional.of(LocalDate.of(2024, 12, 31)))
                .primaryFysiskBrukersted(Optional.of("2700"))
                .fysiskeBrukersteder(List.of("2700"))
                .build();
    }

    private OrgtilknytningState getMockOrgtilknytningEkstern() {
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

    private OrgEnhetState getMockOrgenhet() {
        return OrgEnhetState.builder()
                .fid("OH-FID")
                .id(ID_ORGENHET_1)
                .navn("IT-Utvikling 1")
                .gyldigFom(LocalDate.of(2020, 1, 1))
                .build();
    }
}