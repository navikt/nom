package no.nav.nom.apps.admin.sanitycheck.service;

import no.nav.nom.apps.admin.webclient.nomremedyapiclient.NomRemedyApiClient;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.Tilknytningtype;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
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
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrgtilknytningSanityCheckServiceTest {

    private static final String ORGENHET_ID = "ab123c";
    @Mock
    private OrgtilknytningStateRepository orgtilknytningStateRepository;

    @Mock
    private OrgEnhetStateRepository orgEnhetStateRepository;

    @Mock
    private RessursStateRepository ressursStateRepository;

    @Mock
    private NomRemedyApiClient nomRemedyApiClient;

    private OrgtilknytningSanityCheckService service;

    private static final LocalDate now = LocalDate.now();

    @BeforeEach
    void setUp() {
        service = new OrgtilknytningSanityCheckService(orgtilknytningStateRepository, orgEnhetStateRepository, nomRemedyApiClient, ressursStateRepository);
    }

    @Test
    void orgtilknytninger_som_ikke_overlapper_skal_ikke_returneres() {
        var orgTilknytning2 = orgTilknytning().toBuilder()
                .fid("orgtilknytning-123abc")
                .gyldigFom(now.plusYears(1).plusDays(1))
                .gyldigTom(now.plusYears(2))
                .build();
        when(orgtilknytningStateRepository.getAll()).thenReturn(List.of(orgTilknytning(), orgTilknytning2));

        var overlappendeOTer = service.orgtilknytningMedFlereOverlappendeDagligOppfolgingCurrentOgFuture();

        assertThat(overlappendeOTer).isEmpty();
    }

    @Test
    void orgtilknytninger_med_overlapp_skal_returneres() {
        var orgTilknytning2 = orgTilknytning().toBuilder()
                .fid("orgtilknytning-123abc")
                .gyldigFom(now.plusMonths(2))
                .gyldigTom(now.plusYears(2))
                .build();
        when(orgtilknytningStateRepository.getAll()).thenReturn(List.of(orgTilknytning(), orgTilknytning2));

        var overlappendeOTer = service.orgtilknytningMedFlereOverlappendeDagligOppfolgingCurrentOgFuture();

        assertThat(overlappendeOTer.size()).isEqualTo(1);
    }

    @Test
    void orgtilknytninger_som_ikke_er_daglig_oppfolging_skal_ikke_vaere_med_i_sammenligning() {
        // OT2 er identisk med OT1, men ikke DOL
        var orgTilknytning2 = orgTilknytning().toBuilder()
                .fid("orgtilknytning-123abc")
                .erDagligOppfolging(false)
                .build();
        when(orgtilknytningStateRepository.getAll()).thenReturn(List.of(orgTilknytning(), orgTilknytning2));

        var overlappendeOTer = service.orgtilknytningMedFlereOverlappendeDagligOppfolgingCurrentOgFuture();

        assertThat(overlappendeOTer).isEmpty();
    }

    @Test
    void orgtilknytninger_i_fortiden_skal_ikke_vaere_med_i_sammenligning() {
        var orgTilknytning2 = orgTilknytning().toBuilder()
                .fid("orgtilknytning-123abc")
                .gyldigFom(now.minusYears(1))
                .gyldigTom(now.minusYears(1).plusMonths(6))
                .build();
        when(orgtilknytningStateRepository.getAll()).thenReturn(List.of(orgTilknytning(), orgTilknytning2));

        var overlappendeOTer = service.orgtilknytningMedFlereOverlappendeDagligOppfolgingCurrentOgFuture();

        assertThat(overlappendeOTer).isEmpty();
    }

    @Test
    void alle_overlappende_orgtilknytninger_skal_returneres() {
        var orgTilknytning2 = orgTilknytning().toBuilder()
                .fid("orgtilknytning-123abc")
                .gyldigFom(now.plusMonths(2))
                .gyldigTom(now.plusYears(2))
                .build();
        var orgTilknytning3 = orgTilknytning().toBuilder()
                .fid("orgtilknytning-456def")
                .gyldigFom(now.plusMonths(4))
                .gyldigTom(now.plusYears(3))
                .build();
        when(orgtilknytningStateRepository.getAll()).thenReturn(List.of(orgTilknytning(), orgTilknytning2, orgTilknytning3));

        var overlappendeOTer = service.orgtilknytningMedFlereOverlappendeDagligOppfolgingCurrentOgFuture();

        assertThat(overlappendeOTer.size()).as("Alle overlappende OTer skal returneres").isEqualTo(2);
    }

    @Test
    void should_handle_null_value_in_orgtilknytningType() {
        var orgTilknytning2 = new OrgtilknytningState("orgtilknytning-123abc",
                "A123456",
                ORGENHET_ID,
                Sektor.NAV_STATLIG,
                true,
                LocalDate.now(),
                null,
                null,
                null,
            null);
        when(orgtilknytningStateRepository.getAll()).thenReturn(List.of(orgTilknytning(), orgTilknytning2));

        var duplikateOTer = service.duplikateOrgtilknytninger();

        assertThat(duplikateOTer).isEmpty();
    }

    @DisplayName("Duplikate orgtilknytninger")
    @ParameterizedTest(name = "Orgenhet {0}, sektor {1}, gyldigFom {2}, tilknytning {3} - forventet antall duplikater: {4}")
    @MethodSource("duplikateOrgtilknytningerTestCases")
    void orgtilknytninger_som_er_duplikate_skal_returneres(String orgEnhetId, Sektor sektor, LocalDate gyldigFom, Tilknytningtype tilknytningtype, int expected) {
        var orgTilknytning2 = orgTilknytning().toBuilder()
                .fid("orgtilknytning-123abc")
                .orgEnhetId(orgEnhetId)
                .sektor(sektor)
                .gyldigFom(gyldigFom)
                .orgtilknytningType(tilknytningtype)
                .build();
        when(orgtilknytningStateRepository.getAll()).thenReturn(List.of(orgTilknytning(), orgTilknytning2));

        var duplikateOTer = service.duplikateOrgtilknytninger();

        assertThat(duplikateOTer.size()).isEqualTo(expected);
    }

    // navIdent er lik i alle testcasene
    private static Stream<Arguments> duplikateOrgtilknytningerTestCases() {
        return Stream.of(
                Arguments.of(ORGENHET_ID, Sektor.NAV_STATLIG, now, Tilknytningtype.FAST, 1),
                Arguments.of("de456f", Sektor.NAV_STATLIG, now, Tilknytningtype.FAST, 0),
                Arguments.of(ORGENHET_ID, Sektor.NAV_KOMMUNAL, now, Tilknytningtype.FAST, 0),
                Arguments.of(ORGENHET_ID, Sektor.NAV_STATLIG, now.plusMonths(3), Tilknytningtype.FAST, 0),
                Arguments.of(ORGENHET_ID, Sektor.NAV_STATLIG, now, Tilknytningtype.MIDLERTIDIG, 0),
                Arguments.of(ORGENHET_ID, null, now, Tilknytningtype.FAST, 0)
        );
    }

    @DisplayName("Kun sjekke orgtilknytninger med gyldigTom nyere enn 6 mnd. for duplikater")
    @ParameterizedTest(name = "Dato {0}, antall duplikater returnert {1}")
    @MethodSource("lessThanSixMonthsTestCases")
    void orgtilknytninger_avsluttet_mer_enn_6_mnd_siden_skal_ikke_tas_med(LocalDate gyldigTom, int forventetAntall) {
        var orgTilknytning2 = orgTilknytning().toBuilder()
                .gyldigTom(gyldigTom)
                .build();
        when(orgtilknytningStateRepository.getAll()).thenReturn(List.of(orgTilknytning(), orgTilknytning2));

        var duplikateOTer = service.duplikateOrgtilknytninger();

        assertThat(duplikateOTer.size()).isEqualTo(forventetAntall);
    }

    private static Stream<Arguments> lessThanSixMonthsTestCases() {
        return Stream.of(
                Arguments.of(now.minusMonths(6).minusDays(1), 0),
                Arguments.of(now.minusMonths(5), 1)
        );
    }

    private OrgtilknytningState orgTilknytning() {
        return OrgtilknytningState.builder()
                .fid("orgtilknytning-acb123")
                .navident("A123456")
                .orgEnhetId(ORGENHET_ID)
                .sektor(Sektor.NAV_STATLIG)
                .erDagligOppfolging(true)
                .gyldigFom(now)
                .gyldigTom(now.plusYears(1))
                .orgtilknytningType(Tilknytningtype.FAST)
                .build();
    }
}