package no.nav.nom.apps.api.statistikk.api;

import no.nav.nom.apps.api.TestTokensGenerator;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.OrgEnhetOverordnetResolver;
import no.nav.nom.apps.api.orgenhet.OrgEnhetUnderordnetResolver;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.orgenhet.api.app.OrgEnhetMapper;
import no.nav.nom.apps.api.orgenhet.api.app.dto.OrgEnhet;
import no.nav.nom.apps.api.remedy.RemedyEnhet;
import no.nav.nom.apps.api.remedy.RemedyEnhetMapperService;
import no.nav.nom.apps.api.rest.RestTestBase;
import no.nav.nom.contract.nom.IdentType;
import no.nav.nom.contract.nom.NomNivaa;
import no.nav.nom.contract.nom.OrgEnhetsType;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepository;
import no.nav.nom.libs.state.ressurs.NomStateRessursListener;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static no.nav.nom.apps.api.common.TestData.orgEnhetState;
import static no.nav.nom.apps.api.common.TestData.ressursState;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class StatistikkControllerTest extends RestTestBase {

    @Autowired
    StatistikkRestService statistikkRestService;

    @MockitoBean
    NomStateRessursListener nomStateRessursListener;

    @MockitoBean
    RessursStateRepository ressursStateRepository;

    @MockitoBean
    OrgEnhetStateRepository orgEnhetStateRepository;

    @MockitoBean
    OrgenhetService orgenhetService;

    @MockitoBean
    RemedyEnhetMapperService remedyEnhetMapperService;

    @MockitoBean
    OrgEnhetUnderordnetResolver underordnetResolver;

    @MockitoBean
    OrgEnhetOverordnetResolver overordnetResolver;

    @Autowired
    OrgEnhetMapper orgEnhetMapper;

    @Autowired
    TestTokensGenerator testToken;

    @BeforeEach
    void setInitialLoadCompleteAndEnableUnleashAndClearData() {
        when(nomStateRessursListener.hasReadToEndOnStartup()).thenReturn(true);
        when(unleash.isEnabled(any())).thenReturn(true);
        clearOrgtilknytninger();
        ressursStateRepository.clear();
    }

    //@Test
    @DisplayName("Test hentGlobalStatistikk")
    public void testHentGlobalStatistikk() {
        var Iorgenheter = getAllIOrgEnheter().stream().toList();
        var firstOrgEnhet = getAllOrgEnheter().getFirst();
        when(ressursStateRepository.findAllActiveRessurser()).thenReturn(mapOfActiveRessurser());
        when(orgenhetService.getAllEnheterMedOrganisering()).thenReturn(getAllIOrgEnheter());
        when(orgenhetService.getAllOrgEnheterThatCanHaveRessurser()).thenReturn(getAllIOrgEnheter());
        when(remedyEnhetMapperService.findByNomId("orgEnhet123")).thenReturn(Optional.of(new RemedyEnhet("1234", "12345000", "ORGNIV", "orgEnhet123")));
        when(overordnetResolver.findOverordnetOrgEnhet("fid-123")).thenReturn(Optional.empty());
        when(orgEnhetStateRepository.findByFid("fid-123")).thenReturn(Optional.of(toppNivaaOrgEnhetState()));
        when(underordnetResolver.findUnderordnedeOrgEnheter("fid-123")).thenReturn(List.of(toppNivaaOrgEnhetState()));
        when(orgEnhetMapper.mapToOrgEnhet(Iorgenheter.getFirst())).thenReturn(firstOrgEnhet);

        var expectedGlobalStatistikkResponse = new GlobalStatistikkResponse(
            new RessursStatistikk(6, 2, 2, 2),
            0,
            6,
            0,
            0,
            0,
            0,
            0,
            1,
            4
        );

        var responseBody = getGlobalStatistikk(
            testToken.nomAdminUser()
        );

        assertThat(responseBody).isEqualTo(expectedGlobalStatistikkResponse);
    }

    private Map<String, RessursState> mapOfActiveRessurser() {
        return Map.of(
            "A123456",
            ressursState().toBuilder().navident("A123456").sektor(Sektor.NAV_STATLIG).startdato(Optional.of(LocalDate.now().minusMonths(1))).build(),
            "B123456",
            ressursState().toBuilder().navident("B123456").sektor(Sektor.NAV_STATLIG).startdato(Optional.of(LocalDate.now().minusMonths(1))).build(),
            "C123456",
            ressursState().toBuilder().navident("C123456").sektor(Sektor.NAV_KOMMUNAL).startdato(Optional.of(LocalDate.now().minusMonths(1))).build(),
            "D123456",
            ressursState().toBuilder().navident("D123456").sektor(Sektor.NAV_KOMMUNAL).startdato(Optional.of(LocalDate.now().minusMonths(1))).build(),
            "E123456",
            ressursState().toBuilder().navident("E123456").sektor(Sektor.EKSTERN).startdato(Optional.of(LocalDate.now().minusMonths(1))).build(),
            "F123456",
            ressursState().toBuilder().navident("F123456").sektor(Sektor.EKSTERN).startdato(Optional.of(LocalDate.now().minusMonths(1))).build(),
            "G123456",
            ressursState().toBuilder()
                .navident("G123456")
                .sektor(Sektor.NAV_STATLIG)
                .startdato(Optional.of(LocalDate.now().minusMonths(1)))
                .identType(IdentType.ROBOT)
                .build(),
            "H123456",
            ressursState().toBuilder()
                .navident("H123456")
                .sektor(Sektor.EKSTERN)
                .startdato(Optional.of(LocalDate.now().minusMonths(1)))
                .identType(IdentType.VIKAFOSSEN)
                .build()
        );
    }

    private Collection<IOrgenhet> getAllIOrgEnheter() {
        var orgenhetState = List.of(
            orgEnhetState().toBuilder()
                .fid("fid-123")
                .id("orgEnhet123")
                .navn("Test enhet123")
                .gyldigFom(LocalDate.now().minusMonths(1))
                .gyldigTom(Optional.empty())
                .overordnetOrgEnhetFid(Optional.empty())
                .lederFid(Optional.empty())
                .orgEnhetsType(Optional.empty())
                .nomNivaa(Optional.of(NomNivaa.LINJEENHET))
                .build(),
            orgEnhetState().toBuilder()
                .fid("fid-456")
                .id("orgEnhet456")
                .navn("Test enhet456")
                .gyldigFom(LocalDate.now().minusMonths(1))
                .gyldigTom(Optional.empty())
                .overordnetOrgEnhetFid(Optional.of("fid-123"))
                .lederFid(Optional.empty())
                .orgEnhetsType(Optional.of(OrgEnhetsType.FYLKE))
                .nomNivaa(Optional.of(NomNivaa.DRIFTSENHET))
                .build(),
            orgEnhetState().toBuilder()
                .fid("fid-789")
                .id("orgEnhet789")
                .navn("Test enhet789")
                .gyldigFom(LocalDate.now().minusMonths(1))
                .gyldigTom(Optional.empty())
                .overordnetOrgEnhetFid(Optional.of("fid-456"))
                .lederFid(Optional.empty())
                .orgEnhetsType(Optional.of(OrgEnhetsType.NAV_KONTOR))
                .nomNivaa(Optional.of(NomNivaa.ARBEIDSOMRAADE))
                .build(),
            orgEnhetState().toBuilder()
                .fid("fid-101112")
                .id("orgEnhet101112")
                .navn("Test enhet101112")
                .gyldigFom(LocalDate.now().minusMonths(1))
                .gyldigTom(Optional.empty())
                .overordnetOrgEnhetFid(Optional.of("fid-456"))
                .lederFid(Optional.empty())
                .orgEnhetsType(Optional.of(OrgEnhetsType.NAV_KONTOR))
                .nomNivaa(Optional.of(NomNivaa.ARBEIDSOMRAADE))
                .build(),
            orgEnhetState().toBuilder()
                .fid("fid-131415")
                .id("orgEnhet131415")
                .navn("Test enhet131415")
                .gyldigFom(LocalDate.now().minusMonths(1))
                .gyldigTom(Optional.empty())
                .overordnetOrgEnhetFid(Optional.of("fid-456"))
                .lederFid(Optional.empty())
                .orgEnhetsType(Optional.of(OrgEnhetsType.NAV_KONTOR))
                .nomNivaa(Optional.of(NomNivaa.ARBEIDSOMRAADE))
                .build(),
            orgEnhetState().toBuilder()
                .fid("fid-161718")
                .id("orgEnhet161718")
                .navn("Test enhet161718")
                .gyldigFom(LocalDate.now().minusMonths(1))
                .gyldigTom(Optional.empty())
                .overordnetOrgEnhetFid(Optional.of("fid-456"))
                .lederFid(Optional.empty())
                .orgEnhetsType(Optional.of(OrgEnhetsType.NAV_KONTOR))
                .nomNivaa(Optional.of(NomNivaa.ARBEIDSOMRAADE))
                .build()
        );

        return orgenhetState.stream().map(this::map).toList();
    }

    private IOrgenhet map(OrgEnhetState orgEnhetState) {
        return new no.nav.nom.apps.api.orgenhet.implementations.OrgEnhet(orgEnhetState);
    }

    private List<OrgEnhet> getAllOrgEnheter() {
        return List.of(
            new OrgEnhet(
                "orgEnhet123",
                "fid-123",
                "agressoId123",
                "orgNiv123",
                "agressoOrgenhetType123",
                "Test enhet123",
                LocalDate.now().minusMonths(1),
                null,
                NomNivaa.LINJEENHET,
                null,
                "1234",
                null,
                List.of("OrgEnhet456"),
                List.of(),
                List.of()
            ),
            new OrgEnhet(
                "orgEnhet456",
                "fid-456",
                "agressoId456",
                "orgNiv456",
                "agressoOrgenhetType456",
                "Test enhet456",
                LocalDate.now().minusMonths(1),
                null,
                NomNivaa.DRIFTSENHET,
                OrgEnhetsType.FYLKE,
                "1234",
                "fid-123",
                List.of("orgEnhet789", "orgEnhet101112", "orgEnhet131415", "orgEnhet161718"),
                List.of(),
                List.of()
            ),
            new OrgEnhet(
                "orgEnhet789",
                "fid-789",
                "agressoId789",
                "orgNiv789",
                "agressoOrgenhetType789",
                "Test enhet789",
                LocalDate.now().minusMonths(1),
                null,
                NomNivaa.ARBEIDSOMRAADE,
                OrgEnhetsType.NAV_KONTOR,
                "1234",
                "fid-456",
                List.of(),
                List.of(),
                List.of()
            ),
            new OrgEnhet(
                "orgEnhet101112",
                "fid-101112",
                "agressoId101112",
                "orgNiv101112",
                "agressoOrgenhetType101112",
                "Test enhet101112",
                LocalDate.now().minusMonths(1),
                null,
                NomNivaa.ARBEIDSOMRAADE,
                OrgEnhetsType.NAV_KONTOR,
                "1234",
                "fid-456",
                List.of(),
                List.of(),
                List.of()
            ),
            new OrgEnhet(
                "orgEnhet131415",
                "fid-131415",
                "agressoId131415",
                "orgNiv131415",
                "agressoOrgenhetType131415",
                "Test enhet131415",
                LocalDate.now().minusMonths(1),
                null,
                NomNivaa.ARBEIDSOMRAADE,
                OrgEnhetsType.NAV_KONTOR,
                "1234",
                "fid-456",
                List.of(),
                List.of(),
                List.of()
            ),
            new OrgEnhet(
                "orgEnhet161718",
                "fid-161718",
                "agressoId161718",
                "orgNiv161718",
                "agressoOrgenhetType161718",
                "Test enhet161718",
                LocalDate.now().minusMonths(1),
                null,
                NomNivaa.ARBEIDSOMRAADE,
                OrgEnhetsType.NAV_KONTOR,
                "1234",
                "fid-456",
                List.of(),
                List.of(),
                List.of()
            )
        );
    }

    private OrgEnhetState toppNivaaOrgEnhetState() {
        return new OrgEnhetState(
            "fid-123",
            "orgEnhet123",
            "Test enhet123",
            LocalDate.now().minusMonths(1),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty()
        );
    }

    private GlobalStatistikkResponse getGlobalStatistikk(
        String accessToken
    ) {
        return webTestClient
            .get()
            .uri("/app/statistikk")
            .headers(httpHeaders -> {
                httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                httpHeaders.setBearerAuth(accessToken);
            })
            .exchange()
            .expectStatus().is2xxSuccessful()
            .expectBody(GlobalStatistikkResponse.class)
            .returnResult().getResponseBody();
    }
}
