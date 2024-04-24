package no.nav.nom.apps.api.rest.admin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.nom.apps.api.TestTokensGenerator;
import no.nav.nom.apps.api.authorize.redigereressurs.MockBasisTestOrganisering;
import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.dvh.dto.DvhLeder;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.rest.RestTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.LocalDate;
import java.util.ArrayList;
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

    @MockBean
    private OrgenhetService orgenhetServiceMock;

    @MockBean
    private DvhRepository dvhRepositoryMock;

    @Test
    @DisplayName("Henter orgenheter uten leder og tom NAV hierarkiet")
    void test_listNAVOrgenheterUtenLeder_no_leaders_no_hierarchy() {

        when(orgenhetServiceMock.getAllEnheterMedOrganisering())
            .thenReturn(Collections.emptyList());
        when(dvhRepositoryMock.getLederByAgressoIdAndNivAndNavn(any(), any(), any()))
            .thenReturn(Collections.emptyList());
        webTestClient.get()
            .uri("/api/admin/SanityCheck/listNAVOrgenheterUtenLeder")
            .headers(httpHeaders -> httpHeaders.setBearerAuth(testToken.nomAdminUser()))
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class)
            .value(result -> assertThat(result).isNull());
    }

    @Test
    @DisplayName("Henter orgenheter uten leder og NAV hierarkiet")
    void test_listNAVOrgenheterUtenLeder_no_leaders_with_hierarchy() {

        when(orgenhetServiceMock.getAllEnheterMedOrganisering())
            .thenReturn(getMockEnheterMedOrganisering());
        when(dvhRepositoryMock.getLederByAgressoIdAndNivAndNavn(any(), any(), any()))
            .thenReturn(Collections.emptyList());
        webTestClient.get()
            .uri("/api/admin/SanityCheck/listNAVOrgenheterUtenLeder")
            .headers(httpHeaders -> httpHeaders.setBearerAuth(testToken.nomAdminUser()))
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class)
            .value(result -> {
                assertThat(result).isNotNull();
                assertThat(result.contains(ORG_ENHET_NAVN_B)).isTrue();
                assertThat(result.contains(ORG_ENHET_NAVN_A)).isTrue();
             });
    }

    @Test
    @DisplayName("Henter orgenheter med leder i NAV hierarkiet")
    void test_listNAVOrgenheterUtenLeder_with_leaders() throws JsonProcessingException {
        when(orgenhetServiceMock.getAllEnheterMedOrganisering())
            .thenReturn(getMockEnheterMedOrganisering());
        // simulate that orgenhet B has a leader
        when(dvhRepositoryMock.getLederByAgressoIdAndNivAndNavn(AGRESSO_ID_B, AGRESSO_ORG_NIVAA_B, ORG_ENHET_NAVN_B))
            .thenReturn(getMockLeaders());

        webTestClient.get()
            .uri("/api/admin/SanityCheck/listNAVOrgenheterUtenLeder")
            .headers(httpHeaders -> httpHeaders.setBearerAuth(testToken.nomAdminUser()))
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class)
            .value(result -> {
                assertThat(result).isNotNull();
                assertThat(result.contains(ORG_ENHET_NAVN_B)).isFalse();
                assertThat(result.contains(ORG_ENHET_NAVN_A)).isTrue();
            });
    }
    private List<DvhLeder> getMockLeaders() throws JsonProcessingException {
        String jsonStr = "{" +
            "\"LEDER_NAVIDENT\": \"JohnDoe\"," +
            "\"ORGNODE_KODE\": \"B\"," +
            "\"ORGNODE_NIVA\": \"ORGNIV3\"," +
            "\"ORGNODE_NAVN\": \"OrgEnhet B\"" +
            "}";
        DvhLeder leder = new DvhLeder(new ObjectMapper().readTree(jsonStr));
        return new ArrayList<>(){{add(leder);}};
    }

    private Collection<IOrgenhet> getMockEnheterMedOrganisering() {
        IOrgenhet orgenhet_A = MockBasisTestOrganisering.MockOrgenhet.builder()
            .id(AGRESSO_ID_A)
            .navn(ORG_ENHET_NAVN_A)
            .agressoId(AGRESSO_ID_A)
            .uniqueAgressoId(AGRESSO_ID_A+"-"+ORG_ENHET_NAVN_A+"-"+AGRESSO_ORG_NIVAA_A)
            .orgNiv(AGRESSO_ORG_NIVAA_A)
            .gyldigTom(Optional.of(LocalDate.now().plusDays(1)))
            .build();

        IOrgenhet orgenhet_B = MockBasisTestOrganisering.MockOrgenhet.builder()
            .id(AGRESSO_ID_B)
            .navn(ORG_ENHET_NAVN_B)
            .agressoId(AGRESSO_ID_B)
            .uniqueAgressoId(AGRESSO_ID_B+"-"+ORG_ENHET_NAVN_B+"-"+AGRESSO_ORG_NIVAA_B)
            .orgNiv(AGRESSO_ORG_NIVAA_B)
            .gyldigTom(Optional.of(LocalDate.now().plusDays(1)))
            .build();
        return List.of(orgenhet_A, orgenhet_B);
    }
}
