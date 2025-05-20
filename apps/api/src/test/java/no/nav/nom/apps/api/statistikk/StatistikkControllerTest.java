package no.nav.nom.apps.api.statistikk;

import no.nav.nom.apps.api.TestTokensGenerator;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.orgenhet.implementations.OrgEnhet;
import no.nav.nom.apps.api.rest.RestTestBase;
import no.nav.nom.apps.api.statistikk.api.GlobalStatistikkResponse;
import no.nav.nom.apps.api.statistikk.api.RessursStatistikk;
import no.nav.nom.contract.nom.IdentType;
import no.nav.nom.contract.nom.NomNivaa;
import no.nav.nom.contract.nom.OrgEnhetsType;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.ressurs.NomStateRessursListener;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

public class StatistikkControllerTest extends RestTestBase {

    @MockitoBean
    NomStateRessursListener nomStateRessursListener;

    @MockitoBean
    RessursStateRepository ressursStateRepository;

    @MockitoBean
    OrgenhetService orgenhetService;

    @Autowired
    TestTokensGenerator testToken;

    @BeforeEach
    void setInitialLoadCompleteAndEnableUnleashAndClearData() {
        when(nomStateRessursListener.hasReadToEndOnStartup()).thenReturn(true);
        when(unleash.isEnabled(any())).thenReturn(true);
        clearOrgtilknytninger();
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
            "A123456", ressursState().toBuilder().navident("A123456").sektor(Sektor.NAV_STATLIG).startdato(Optional.of(LocalDate.now().minusYears(1))).build(),
            "B123456", ressursState().toBuilder().navident("B123456").sektor(Sektor.NAV_STATLIG).startdato(Optional.of(LocalDate.now().minusYears(1))).build(),
            "C123456", ressursState().toBuilder().navident("C123456").sektor(Sektor.NAV_KOMMUNAL).startdato(Optional.of(LocalDate.now().minusYears(1))).build(),
            "D123456", ressursState().toBuilder().navident("D123456").sektor(Sektor.NAV_KOMMUNAL).startdato(Optional.of(LocalDate.now().minusYears(1))).build(),
            "E123456", ressursState().toBuilder().navident("E123456").sektor(Sektor.EKSTERN).startdato(Optional.of(LocalDate.now().minusYears(1))).build(),
            "F123456", ressursState().toBuilder().navident("F123456").sektor(Sektor.EKSTERN).startdato(Optional.of(LocalDate.now().minusYears(1))).build(),
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
        return new OrgEnhet(orgEnhetState);
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