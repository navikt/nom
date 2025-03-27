package no.nav.nom.apps.admin.sanitycheck.service;

import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.Tilknytningtype;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;
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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static no.nav.nom.apps.admin.PeriodeUtil.today;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RessursSanityCheckServiceTest {

    private static final String ID_ORGENHET_1 = "ab123c";
    private static final String ID_ORGENHET_2 = "de456f";
    private static final String FID_ORGTILKNYTNING_1 = "orgtilknytning-1";
    private static final String FID_ORGTILKNYTNING_2 = "orgtilknytning-2";

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
    void should_include_cases_where_sluttdato_is_different(LocalDate sluttDato, LocalDate gyldigTom, int expected) {
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


        var ulikeDatoer = service.ressurserMedUlikOrgTilknytningSluttDato();

        assertThat(ulikeDatoer.size()).isEqualTo(1);
    }

    @Test
    @DisplayName("Filtere bort orgtilknytning.tomDato > 01.01.2099 i sjekk for ulik sluttdato")
    void should_remove_ot_far_in_the_future_when_comparing_to_ressurs() {
        RessursState ressurs = getMockRessursEkstern();

        Map<String, RessursState> ressursStateMap = new HashMap<>();
        ressursStateMap.put(ressurs.navident(), ressurs);
        when(ressursStateRepositoryMock.findAllActiveRessurser()).thenReturn(ressursStateMap);

        var otLangtFremITid = getMockOrgtilknytningEkstern().toBuilder()
                .gyldigFom(LocalDate.of(2024, 1, 1))
                .gyldigTom(Optional.of(LocalDate.of(2099, 1, 1)))
                .build();
        when(orgtilknytningStateRepositoryMock.findByNavident(ressurs.navident())).thenReturn(List.of(otLangtFremITid));


        var ulikeDatoer = service.ressurserMedUlikOrgTilknytningSluttDato();

        assertThat(ulikeDatoer.size()).isEqualTo(0);
    }

    @Test
    @DisplayName("Kun overlappende DOL rapporteres")
    void should_find_overlapping_dol() {
        RessursState ressurs = getMockRessursEkstern();
        when(ressursStateRepositoryMock.findAllActiveRessurser()).thenReturn(getRessursStateMap(ressurs));

        LocalDate fomDato = today().minusMonths(6);
        LocalDate tomDato = today().minusMonths(3);
        LocalDate tomDatoDOLto = tomDato.plusDays(7);

        var forsteDOL = getMockOrgtilknytningEkstern().toBuilder()
                .gyldigFom(fomDato)
                .gyldigTom(Optional.of(tomDato))
                .orgEnhetId(ID_ORGENHET_1)
                .build();
        var andreDOL = getMockOrgtilknytningEkstern().toBuilder()
                .fid(FID_ORGTILKNYTNING_2)
                .gyldigFom(fomDato)
                .gyldigTom(Optional.of(tomDatoDOLto))
                .orgEnhetId(ID_ORGENHET_1)
                .build();
        var tredjeDOL = getMockOrgtilknytningEkstern().toBuilder()
                .fid("orgtilnytning-3")
                .gyldigFom(today().minusMonths(1))
                .gyldigTom(Optional.of(today().plusYears(1)))
                .orgEnhetId(ID_ORGENHET_2)
                .build();
        when(orgtilknytningStateRepositoryMock.findByNavident(ressurs.navident())).thenReturn(List.of(forsteDOL, andreDOL, tredjeDOL));

        var ressurserMedFlereDOL = service.ressursMedFlereEnnEnDOL();

        assertThat(ressurserMedFlereDOL.size()).isEqualTo(1);
        assertThat(ressurserMedFlereDOL.getFirst().navident()).isEqualTo(getMockRessursEkstern().navident());
        assertThat(ressurserMedFlereDOL.getFirst().otErMedOverlapp()).contains(ID_ORGENHET_1, fomDato.toString(), tomDato.toString(), tomDatoDOLto.toString());
        assertThat(ressurserMedFlereDOL.getFirst().otErMedOverlapp()).contains(FID_ORGTILKNYTNING_1, FID_ORGTILKNYTNING_2);
        assertThat(ressurserMedFlereDOL.getFirst().otErMedOverlapp()).doesNotContain(ID_ORGENHET_2);
    }

    @DisplayName("Alle ulike kombinasjoner skal håndteres ved sjekk for DOL overlapp")
    @ParameterizedTest(name = "DOL 1: [{0} - {1}] DOL 2: [{2} - {3}]  - Forventet antall avvik: {4}")
    @MethodSource("overlappendeDOLTestCases")
    void should_handle_all_possible_cases_of_DOL_overlap(LocalDate startDato1, LocalDate sluttDato1, LocalDate startDato2, LocalDate sluttDato2, int forventetAntallOverlapp) {
        RessursState ressurs = getMockRessursEkstern();
        when(ressursStateRepositoryMock.findAllActiveRessurser()).thenReturn(getRessursStateMap(ressurs));
        
        var forsteDOL = getMockOrgtilknytningEkstern().toBuilder()
                .gyldigFom(startDato1)
                .gyldigTom(Optional.of(sluttDato1))
                .orgEnhetId(ID_ORGENHET_1)
                .build();
        var andreDOL = getMockOrgtilknytningEkstern().toBuilder()
                .fid(FID_ORGTILKNYTNING_2)
                .gyldigFom(startDato2)
                .gyldigTom(Optional.of(sluttDato2))
                .orgEnhetId(ID_ORGENHET_1)
                .build();

        when(orgtilknytningStateRepositoryMock.findByNavident(ressurs.navident())).thenReturn(List.of(forsteDOL, andreDOL));

        var ressurserMedFlereDOL = service.ressursMedFlereEnnEnDOL();

        assertThat(ressurserMedFlereDOL.size()).isEqualTo(forventetAntallOverlapp);
    }

    private static Stream<Arguments> overlappendeDOLTestCases() {
        LocalDate today = LocalDate.now();
        return Stream.of(
                Arguments.of(today.minusYears(1), today.minusMonths(6), today.minusMonths(6).plusDays(1), today.plusYears(1), 0),
                Arguments.of(today.minusYears(1), today, today, today.plusYears(1), 1),
                Arguments.of(today.minusYears(1), today, today.minusMonths(1), today.plusYears(2), 1),
                Arguments.of(today.minusYears(1), today, today.minusYears(1).plusMonths(1), today.minusMonths(1), 1),
                Arguments.of(today.minusYears(1), today, today.plusDays(1), today.plusYears(1), 0)
        );
    }

    @Test
    @DisplayName("Bruker med DOL som ikke overlapper tas ikke med")
    void should_not_include_non_overlapping_dol() {
        RessursState ressurs = getMockRessursEkstern();
        when(ressursStateRepositoryMock.findAllActiveRessurser()).thenReturn(getRessursStateMap(ressurs));

        var forsteDOL = getMockOrgtilknytningEkstern().toBuilder()
                .gyldigFom(today().minusMonths(6).plusDays(1))
                .gyldigTom(Optional.of(today().minusMonths(3)))
                .orgEnhetId(ID_ORGENHET_1)
                .build();
        var andreDOL = getMockOrgtilknytningEkstern().toBuilder()
                .gyldigFom(today().minusMonths(3).plusDays(1))
                .gyldigTom(Optional.of(today().plusYears(1)))
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
                .gyldigFom(today().minusMonths(3))
                .gyldigTom(Optional.of(today().plusYears(1)))
                .erDagligOppfolging(true)
                .orgEnhetId(ID_ORGENHET_1)
                .build();
        var andreDOL = getMockOrgtilknytningEkstern().toBuilder()
                .gyldigFom(today().minusMonths(2))
                .gyldigTom(Optional.of(today().plusYears(1).minusMonths(3)))
                .erDagligOppfolging(true)
                .orgEnhetId(ID_ORGENHET_1)
                .build();
        var annenOT = getMockOrgtilknytningEkstern().toBuilder()
                .gyldigFom(today().minusMonths(3))
                .gyldigTom(Optional.of(today().plusYears(1)))
                .erDagligOppfolging(false)
                .orgEnhetId(ID_ORGENHET_2)
                .build();
        when(orgtilknytningStateRepositoryMock.findByNavident(ressurs.navident())).thenReturn(List.of(forsteDOL, andreDOL, annenOT));

        var ressurserMedFlereDOL = service.ressursMedFlereEnnEnDOL();

        assertThat(ressurserMedFlereDOL.size()).isEqualTo(1);
    }

    @Test
    @DisplayName("OTer med gyldigTom eldre enn 6 måneder tas ikke med")
    void should_not_include_OTs_which_expired_more_than_6_months_ago() {
        RessursState ressurs = getMockRessursEkstern();
        when(ressursStateRepositoryMock.findAllActiveRessurser()).thenReturn(getRessursStateMap(ressurs));

        // DOL 1 og 2 overlapper, men er eldre enn 6 mnd.
        var utloptDOL1 = getMockOrgtilknytningEkstern().toBuilder()
                .gyldigFom(LocalDate.now().minusYears(1))
                .gyldigTom(Optional.of(LocalDate.now().minusYears(1).plusMonths(3)))
                .erDagligOppfolging(true)
                .orgEnhetId(ID_ORGENHET_1)
                .build();
        var utloptDOL2 = getMockOrgtilknytningEkstern().toBuilder()
                .gyldigFom(LocalDate.now().minusYears(1).plusDays(7))
                .gyldigTom(Optional.of(LocalDate.now().minusYears(1).plusMonths(3)))
                .erDagligOppfolging(true)
                .orgEnhetId(ID_ORGENHET_1)
                .build();
        var gyldigDOL = getMockOrgtilknytningEkstern().toBuilder()
                .gyldigFom(LocalDate.now().minusYears(1).plusMonths(4))
                .gyldigTom(Optional.of(LocalDate.now().plusYears(1)))
                .erDagligOppfolging(true)
                .orgEnhetId(ID_ORGENHET_1)
                .build();
        when(orgtilknytningStateRepositoryMock.findByNavident(ressurs.navident())).thenReturn(List.of(utloptDOL1, utloptDOL2, gyldigDOL));

        var ressurserMedFlereDOL = service.ressursMedFlereEnnEnDOL();

        assertThat(ressurserMedFlereDOL).isEmpty();
    }


    @Test
    @DisplayName("getSisteOrgtilknytninger skal returnere siste OT for hver ressurs")
    void should_return_latest_orgtilknytning_for_each_ressurs() {
        RessursState ressurs1 = getMockRessursEkstern();
        RessursState ressurs2 = getMockRessursEkstern().toBuilder().navident("X654321").build();

        Map<String, Collection<OrgtilknytningState>> alleOrgTilknytninger = new HashMap<>();
        alleOrgTilknytninger.put(ressurs1.navident(),
            List.of(
                getMockOrgtilknytningEkstern().toBuilder()
                    .gyldigTom(Optional.of(LocalDate.now().minusMonths(2)))
                    .build(),
                getMockOrgtilknytningEkstern().toBuilder()
                    .gyldigTom(Optional.of(LocalDate.now().minusMonths(1)))
                    .build(),
                getMockOrgtilknytningEkstern().toBuilder()
                    .gyldigTom(Optional.of(LocalDate.MAX))
                    .build()
            ));
        alleOrgTilknytninger.put(ressurs2.navident(),
            List.of(
                getMockOrgtilknytningEkstern().toBuilder()
                    .gyldigTom(Optional.empty())
                    .build()
            ));

        var sisteOrgtilknytninger = service.getSisteOrgtilknytninger(alleOrgTilknytninger);

        assertThat(sisteOrgtilknytninger.size()).isEqualTo(2);
        assertThat(sisteOrgtilknytninger.get(ressurs1.navident()).size()).isEqualTo(1);
        assertThat(sisteOrgtilknytninger.get(ressurs2.navident()).size()).isEqualTo(1);

        assertThat(sisteOrgtilknytninger
            .get(ressurs1.navident())
            .stream()
            .findFirst().get().gyldigTom()).isEqualTo(LocalDate.MAX);

        assertThat(sisteOrgtilknytninger
            .get(ressurs2.navident())
            .stream()
            .findFirst().get().gyldigTom()).isEmpty();
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
                .fid(FID_ORGTILKNYTNING_1)
                .navident("X123456")
                .orgEnhetId("TestId")
                .erDagligOppfolging(true)
                .gyldigFom(LocalDate.of(2024, 1, 1))
                .gyldigTom(Optional.of(LocalDate.of(2024, 12, 31)))
                .orgtilknytningType(Optional.of(Tilknytningtype.KONSULENT))
                .build();
    }

}