package no.nav.nom.apps.api.statistikk;

import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.orgenhet.implementations.OrgEnhet;
import no.nav.nom.apps.api.statistikk.api.GlobalStatistikkResponse;
import no.nav.nom.apps.api.statistikk.api.RessursStatistikk;
import no.nav.nom.apps.api.statistikk.api.StatistikkRestService;
import no.nav.nom.contract.nom.IdentType;
import no.nav.nom.contract.nom.NomNivaa;
import no.nav.nom.contract.nom.OrgEnhetsType;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static no.nav.nom.apps.api.common.TestData.orgEnhetState;
import static no.nav.nom.apps.api.common.TestData.ressursState;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class StatistikkRestServiceTest {

    @Mock
    RessursStateRepository ressursStateRepository;

    @Mock
    OrgenhetService orgenhetService;

    @InjectMocks
    StatistikkRestService statistikkRestService;

    @BeforeEach
    void setInitialLoadCompleteAndEnableUnleashAndClearData() {
        ressursStateRepository.clear();
    }

    @Test
    @DisplayName("Test hentGlobalStatistikk")
    public void testHentGlobalStatistikk() {
        when(ressursStateRepository.findAllActiveRessurser()).thenReturn(mapOfActiveRessurser());
        when(orgenhetService.getAllEnheterMedOrganisering()).thenReturn(getAllOrgEnheter());
        when(orgenhetService.getAllOrgEnheterThatCanHaveRessurser()).thenReturn(getAllOrgEnheter());

        var expectedGlobalStatistikkResponse = new GlobalStatistikkResponse(
            new RessursStatistikk(6, 2, 2, 2),
            0,
            7,
            0,
            0,
            0,
            0,
            0,
            1,
            4
        );

        var responseBody = statistikkRestService.hentGlobalStatistikk();

        assertThat(responseBody).isEqualTo(expectedGlobalStatistikkResponse);
    }

    private Map<String, RessursState> mapOfActiveRessurser() {
        return Map.of(
            "A123456", ressursState().toBuilder().navident("A123456").sektor(Sektor.NAV_STATLIG).startdato(LocalDate.now().minusYears(1)).build(),
            "B123456", ressursState().toBuilder().navident("B123456").sektor(Sektor.NAV_STATLIG).startdato(LocalDate.now().minusYears(1)).build(),
            "C123456", ressursState().toBuilder().navident("C123456").sektor(Sektor.NAV_KOMMUNAL).startdato(LocalDate.now().minusYears(1)).build(),
            "D123456", ressursState().toBuilder().navident("D123456").sektor(Sektor.NAV_KOMMUNAL).startdato(LocalDate.now().minusYears(1)).build(),
            "E123456", ressursState().toBuilder().navident("E123456").sektor(Sektor.EKSTERN).startdato(LocalDate.now().minusYears(1)).build(),
            "F123456", ressursState().toBuilder().navident("F123456").sektor(Sektor.EKSTERN).startdato(LocalDate.now().minusYears(1)).build(),
            "G123456", ressursState().toBuilder().navident("G123456").sektor(Sektor.NAV_STATLIG).identType(IdentType.ROBOT).build(),
            "H123456", ressursState().toBuilder().navident("H123456").sektor(Sektor.EKSTERN).identType(IdentType.VIKAFOSSEN).build()
        );
    }

    private Collection<IOrgenhet> getAllOrgEnheter() {

        var orgenhetState = List.of(
            orgEnhetState().toBuilder()
                .fid("fid-123")
                .id("orgEnhet123")
                .navn("Test enhet123")
                .gyldigFom(LocalDate.now().minusMonths(1))
                .gyldigTom(null)
                .overordnetOrgEnhetFid(null)
                .lederFid(null)
                .orgEnhetsType(null)
                .nomNivaa(NomNivaa.LINJEENHET)
                .build(),
            orgEnhetState().toBuilder()
                .fid("fid-456")
                .id("orgEnhet456")
                .navn("Test enhet456")
                .gyldigFom(LocalDate.now().minusMonths(1))
                .gyldigTom(null)
                .overordnetOrgEnhetFid(null)
                .lederFid(null)
                .orgEnhetsType(OrgEnhetsType.FYLKE)
                .nomNivaa(NomNivaa.DRIFTSENHET)
                .build(),
            orgEnhetState().toBuilder()
                .fid("fid-789")
                .id("orgEnhet789")
                .navn("Test enhet789")
                .gyldigFom(LocalDate.now().minusMonths(1))
                .gyldigTom(null)
                .overordnetOrgEnhetFid(null)
                .lederFid(null)
                .orgEnhetsType(OrgEnhetsType.NAV_KONTOR)
                .nomNivaa(NomNivaa.ARBEIDSOMRAADE)
                .build(),
            orgEnhetState().toBuilder()
                .fid("fid-101112")
                .id("orgEnhet101112")
                .navn("Test enhet101112")
                .gyldigFom(LocalDate.now().minusMonths(1))
                .gyldigTom(null)
                .overordnetOrgEnhetFid(null)
                .lederFid(null)
                .orgEnhetsType(OrgEnhetsType.NAV_KONTOR)
                .nomNivaa(NomNivaa.ARBEIDSOMRAADE)
                .build(),
            orgEnhetState().toBuilder()
                .fid("fid-131415")
                .id("orgEnhet131415")
                .navn("Test enhet131415")
                .gyldigFom(LocalDate.now().minusMonths(1))
                .gyldigTom(null)
                .overordnetOrgEnhetFid(null)
                .lederFid(null)
                .orgEnhetsType(OrgEnhetsType.NAV_KONTOR)
                .nomNivaa(NomNivaa.ARBEIDSOMRAADE)
                .build(),
            orgEnhetState().toBuilder()
                .fid("fid-161718")
                .id("orgEnhet161718")
                .navn("Test enhet161718")
                .gyldigFom(LocalDate.now().minusMonths(1))
                .gyldigTom(null)
                .overordnetOrgEnhetFid(null)
                .lederFid(null)
                .orgEnhetsType(OrgEnhetsType.NAV_KONTOR)
                .nomNivaa(NomNivaa.ARBEIDSOMRAADE)
                .build(),
            orgEnhetState().toBuilder()
                .fid("fid-192021")
                .id("orgEnhet192021")
                .navn("Test enhet192021")
                .gyldigFom(LocalDate.now().minusMonths(1))
                .gyldigTom(null)
                .overordnetOrgEnhetFid(null)
                .lederFid(null)
                .orgEnhetsType(OrgEnhetsType.NAV_KONTOR)
                .nomNivaa(null)
                .build()
        );

        return orgenhetState.stream().map(this::map).toList();
    }

    private IOrgenhet map(OrgEnhetState orgEnhetState) {
        return new OrgEnhet(orgEnhetState);
    }
}