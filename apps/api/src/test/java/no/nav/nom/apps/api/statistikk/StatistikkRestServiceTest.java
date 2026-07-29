package no.nav.nom.apps.api.statistikk;

import no.nav.nom.apps.api.common.AppProperties;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.orgenhet.api.app.OrgEnhetMapper;
import no.nav.nom.apps.api.orgenhet.implementations.OrgEnhet;
import no.nav.nom.apps.api.statistikk.api.GlobalStatistikkResponse;
import no.nav.nom.apps.api.statistikk.api.RessursStatistikk;
import no.nav.nom.apps.api.statistikk.api.StatistikkRestService;
import no.nav.nom.contract.nom.IdentType;
import no.nav.nom.contract.nom.NomNivaa;
import no.nav.nom.contract.nom.OrgEnhetsType;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateRepository;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

    @Mock
    OrgEnhetMapper orgEnhetMapper;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    AppProperties appProperties;

    @Mock
    OrgtilknytningStateRepository orgtilknytningStateRepository;

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
        when(orgenhetService.getAktiveEnheterIOrganisasjonen()).thenReturn(getAllOrgEnheter());
        when(orgenhetService.getAllOrgEnheterThatCanHaveRessurser()).thenReturn(getAllOrgEnheter());
        when(appProperties.orgenhet().helfoId()).thenReturn("helfo123");
        when(appProperties.orgenhet().riksrevisjonenId()).thenReturn("riksrevisjonen123");
        when(appProperties.orgenhet().vikafossenId()).thenReturn("vikafossen123");
        when(appProperties.orgenhet().skattId()).thenReturn("skatt123");

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

    @Test
    @DisplayName("hent statistikk for gitt enhet")
    public void testHentGlobalStatistikkForGittEnhet() {

        var orgenheterSelection = getAllOrgEnheter().subList(0, 3);

        when(orgenhetService.getById("orgEnhet123")).thenReturn(Optional.of(orgenheterSelection.get(0)));
        when(orgenhetService.getAllUnderliggendeOrgEnhetById("orgEnhet123")).thenReturn(List.of(orgenheterSelection.get(1), orgenheterSelection.get(2)));
        when(orgEnhetMapper.mapToOrgEnhet(Mockito.argThat(x -> x != null && x.getId()
            .equals("orgEnhet123")))).thenReturn(no.nav.nom.apps.api.orgenhet.api.app.dto.OrgEnhet.builder()
            .id("orgEnhet123")
            .ressursKoblinger(List.of(OrgtilknytningState.builder().navident("a").sektor(Sektor.NAV_STATLIG).build()))
            .build());

        when(orgEnhetMapper.mapToOrgEnhet(Mockito.argThat(oe -> oe != null && oe.getId()
            .equals("orgEnhet456")))).thenReturn(no.nav.nom.apps.api.orgenhet.api.app.dto.OrgEnhet.builder()
            .id("orgEnhet456")
            .ressursKoblinger(List.of(OrgtilknytningState.builder().navident("b").erDagligOppfolging(true).sektor(Sektor.NAV_KOMMUNAL).build()))
            .build());

        when(orgEnhetMapper.mapToOrgEnhet(Mockito.argThat(oe -> oe != null && oe.getId()
            .equals("orgEnhet789")))).thenReturn(no.nav.nom.apps.api.orgenhet.api.app.dto.OrgEnhet.builder()
            .id("orgEnhet789")
            .ressursKoblinger(List.of(
                OrgtilknytningState.builder().navident("b").sektor(Sektor.NAV_STATLIG).build(),
                OrgtilknytningState.builder().navident("c").sektor(Sektor.NAV_STATLIG).build(),
                OrgtilknytningState.builder().navident("r").sektor(Sektor.NAV_STATLIG).build()
            ))
            .build());

        when(ressursStateRepository.findByNavident(Mockito.argThat(x -> List.of("a", "b", "c").contains(x)))).thenReturn(Optional.of(RessursState.builder().identType(IdentType.STANDARD).build())
        );

        when(appProperties.orgenhet().helfoId()).thenReturn("helfo123");
        when(appProperties.orgenhet().riksrevisjonenId()).thenReturn("riksrevisjonen123");
        when(appProperties.orgenhet().vikafossenId()).thenReturn("vikafossen123");
        when(appProperties.orgenhet().skattId()).thenReturn("skatt123");


        when(ressursStateRepository.findByNavident("r")).thenReturn(
            Optional.of(RessursState.builder().identType(IdentType.ROBOT).build())
        );

        var exectedStatistikk = new RessursStatistikk(3, 2, 1, 0);

        var responseBody = statistikkRestService.getStatistikkForOrgEnhetCache("orgEnhet123");

        assertThat(responseBody.ressursStatistikk()).isEqualTo(exectedStatistikk);
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

    private List<IOrgenhet> getAllOrgEnheter() {

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