package no.nav.nom.apps.api.rest.admin;

import no.nav.nom.apps.api.TestTokensGenerator;
import no.nav.nom.apps.api.authorize.redigereressurs.MockBasisTestOrganisering;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.orgenhetleder.OrgEnhetLederService;
import no.nav.nom.apps.api.orgtilknytning.OrgtilknytningService;
import no.nav.nom.apps.api.rest.RestTestBase;
import no.nav.nom.contract.nom.state.OrgEnhetLederState;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class SanityCheckControllerTest extends RestTestBase {

    final String ORG_ENHET_NAVN_A = "OrgEnhet A";
    final String AGRESSO_ID_A = "A";
    final String AGRESSO_ORG_NIVAA_A = "NAV";

    final String ORG_ENHET_NAVN_B = "OrgEnhet B";
    final String AGRESSO_ID_B = "B";
    final String AGRESSO_ORG_NIVAA_B = "ORGNIV3";

    @Autowired
    TestTokensGenerator testToken;

    @MockitoBean
    OrgenhetService orgenhetServiceMock;

    @MockitoBean
    OrgEnhetLederService orgEnhetLederService;

    @MockitoBean
    OrgtilknytningService orgtilknytningServiceMock;

    @MockitoBean
    RessursStateRepository ressursStateRepository;

//    @Test
//    @DisplayName("Henter orgenheter uten leder og tom NAV hierarkiet")
//    void test_listOrgenheterUtenLeder_no_leaders_no_hierarchy() {
//
//        when(orgenhetServiceMock.getAllEnheterMedOrganisering())
//            .thenReturn(Collections.emptyList());
//        when(orgEnhetLederService.getAktivLederForOrgEnhet(any())).thenReturn(Optional.empty());
//
//        webTestClient.get()
//            .uri("/api/admin/SanityCheck/listOrgenheterUtenLeder")
//            .headers(httpHeaders -> httpHeaders.setBearerAuth(testToken.nomAdminUser()))
//            .exchange()
//            .expectStatus().isOk()
//            .expectBody(String.class)
//            .value(result -> assertThat(result).isNull());
//    }

//    @Test
//    @DisplayName("Henter orgenheter uten leder og NAV hierarkiet")
//    void test_listOrgenheterUtenLeder_no_leaders_with_hierarchy() {
//
//        when(orgenhetServiceMock.getAllEnheterMedOrganisering())
//            .thenReturn(getMockEnheterMedOrganisering());
//        when(orgEnhetLederService.getAktivLederForOrgEnhet(any())).thenReturn(Optional.empty());
//        webTestClient.get()
//            .uri("/api/admin/SanityCheck/listOrgenheterUtenLeder")
//            .headers(httpHeaders -> httpHeaders.setBearerAuth(testToken.nomAdminUser()))
//            .exchange()
//            .expectStatus().isOk()
//            .expectBody(String.class)
//            .value(result -> {
//                assertThat(result).isNotNull();
//                assertThat(result.contains(ORG_ENHET_NAVN_B)).isTrue();
//                assertThat(result.contains(ORG_ENHET_NAVN_A)).isTrue();
//             });
//    }

//    @Test
//    @DisplayName("Henter orgenheter med leder i NAV hierarkiet")
//    void test_listOrgenheterUtenLeder_with_leaders() {
//        when(orgenhetServiceMock.getAllEnheterMedOrganisering())
//            .thenReturn(getMockEnheterMedOrganisering());
//        // simulate that orgenhet B has a leader
//        when(orgEnhetLederService.getAktivLederForOrgEnhet("B")).thenReturn(getMockLederState());
//
//        webTestClient.get()
//            .uri("/api/admin/SanityCheck/listOrgenheterUtenLeder")
//            .headers(httpHeaders -> httpHeaders.setBearerAuth(testToken.nomAdminUser()))
//            .exchange()
//            .expectStatus().isOk()
//            .expectBody(String.class)
//            .value(result -> {
//                assertThat(result).isNotNull();
//                assertThat(result.contains(ORG_ENHET_NAVN_B)).isFalse();
//                assertThat(result.contains(ORG_ENHET_NAVN_A)).isTrue();
//            });
//    }

    private Optional<OrgEnhetLederState> getMockLederState() {
        return Optional.of(
            OrgEnhetLederState.builder()
                .fid("FID_1")
                .navident("JohnDoe")
                .orgEnhetId("A")
                .startdato(LocalDate.now().minusYears(1))
                .build()
        );
    }

    private Collection<IOrgenhet> getMockEnheterMedOrganisering() {
        IOrgenhet orgenhet_A = MockBasisTestOrganisering.MockOrgenhet.builder()
            .id(AGRESSO_ID_A)
            .navn(ORG_ENHET_NAVN_A)
            .agressoId(AGRESSO_ID_A)
            .uniqueAgressoId(AGRESSO_ID_A+"-"+ORG_ENHET_NAVN_A+"-"+AGRESSO_ORG_NIVAA_A)
            .orgNiv(AGRESSO_ORG_NIVAA_A)
            .gyldigTom(LocalDate.now().plusDays(1))
            .build();

        IOrgenhet orgenhet_B = MockBasisTestOrganisering.MockOrgenhet.builder()
            .id(AGRESSO_ID_B)
            .navn(ORG_ENHET_NAVN_B)
            .agressoId(AGRESSO_ID_B)
            .uniqueAgressoId(AGRESSO_ID_B+"-"+ORG_ENHET_NAVN_B+"-"+AGRESSO_ORG_NIVAA_B)
            .orgNiv(AGRESSO_ORG_NIVAA_B)
            .gyldigTom(LocalDate.now().plusDays(1))
            .build();
        return List.of(orgenhet_A, orgenhet_B);
    }
}
