package no.nav.nom.apps.api.rest.omada;

import no.nav.nom.apps.api.TestTokensGenerator;
import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.dvh.dto.DvhLeder;
import no.nav.nom.apps.api.dvh.dto.DvhTelefon;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.orgenhet.implementations.OrgEnhet;
import no.nav.nom.apps.api.remedy.RemedyEnhet;
import no.nav.nom.apps.api.remedy.RemedyEnhetMapperService;
import no.nav.nom.apps.api.rest.RestTestBase;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.contract.nom.state.OrganiseringState;
import no.nav.nom.libs.state.organisering.OrganiseringStateRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class OmadaControllerTest extends RestTestBase {

    private static final String NOM_ID = "ab123c";
    private static final String NOM_ID_OVER = "ef567g";
    private static final String NORG_ID = "4321";
    private static final String ORG_ENHET_NAVN = "Min avdeling";
    private static final String AGRESSO_ID = "1234567";
    private static final String AGRESSO_ORG_NIVAA = "ORGENHET";
    private static final String NAVIDENT = "Z123456";
    private static final String TELEFONNUMMER = "87654321";


    @MockitoBean
    private OrganiseringStateRepository organiseringStateRepositoryMock;

    @MockitoBean
    private RemedyEnhetMapperService remedyEnhetMapperServiceMock;

    @MockitoBean
    private OrgenhetService orgenhetServiceMock;

    @MockitoBean
    private DvhRepository dvhRepositoryMock;

    @Autowired
    TestTokensGenerator testToken;

    @Test
    @DisplayName("Henter informasjon Omada trenger for orgEnhet")
    void should_get_orgenhet_information() {
        when(organiseringStateRepositoryMock.findActiveById(NOM_ID))
            .thenReturn(Optional.ofNullable(getOrganisering()));

        when(remedyEnhetMapperServiceMock.findByNomId(NOM_ID))
            .thenReturn(Optional.ofNullable(getRemedyEnhet()));

        when(orgenhetServiceMock.getById(NOM_ID))
            .thenReturn(Optional.of(getOrgEnhet()));

        when(dvhRepositoryMock.getLederByAgressoIdAndNivAndNavn(AGRESSO_ID, AGRESSO_ORG_NIVAA, ORG_ENHET_NAVN))
            .thenReturn(getLedere());


        webTestClient.get()
            .uri("/api/v1/omada/orgEnhet/{id}", NOM_ID)
            .headers(httpHeaders -> httpHeaders.setBearerAuth(testToken.queryOmadaClient()))
            .exchange()
            .expectStatus().isOk()
            .expectBody(OmadaOrgEnhetInfoDTO.class)
            .value(orgenhetDTO -> {
                assertThat(orgenhetDTO.id()).isEqualTo(NOM_ID);
                assertThat(orgenhetDTO.orgEnhetOver()).isEqualTo(NOM_ID_OVER);
                assertThat(orgenhetDTO.norgId()).isEqualTo(NORG_ID);
                assertThat(orgenhetDTO.leder()).isEqualTo(NAVIDENT);
            });
    }

    @Test
    @DisplayName("Returnerer 401 når request ikke har gyldig token")
    void should_return_401_when_request_does_not_have_valid_token() {
        webTestClient.get()
            .uri("/api/v1/omada/orgEnhet/{id}", NOM_ID)
            .exchange()
            .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("Returnerer 404 hvis orgEnhet ikke finnes")
    void should_return_404_when_orgEnhet_is_not_found() {
        when(orgenhetServiceMock.getById(NOM_ID)).thenReturn(Optional.empty());

        webTestClient.get()
            .uri("/api/v1/omada/orgEnhet/{id}", NOM_ID)
            .headers(httpHeaders -> httpHeaders.setBearerAuth(testToken.queryOmadaClient()))
            .exchange()
            .expectStatus().isNotFound();
    }

    @Test
    @DisplayName("Returnerer null for felt dersom data for denne orgEnheten ikke finnes")
    void should_return_null_if_data_is_not_found() {
        when(organiseringStateRepositoryMock.findActiveById(NOM_ID))
            .thenReturn(Optional.empty());

        when(remedyEnhetMapperServiceMock.findByNomId(NOM_ID))
            .thenReturn(Optional.empty());

        when(orgenhetServiceMock.getById(NOM_ID))
            .thenReturn(Optional.of(getOrgEnhet()));

        when(dvhRepositoryMock.getLederByAgressoIdAndNivAndNavn(AGRESSO_ID, AGRESSO_ORG_NIVAA, ORG_ENHET_NAVN))
            .thenReturn(Collections.emptyList());


        webTestClient.get()
            .uri("/api/v1/omada/orgEnhet/{id}", NOM_ID)
            .headers(httpHeaders -> httpHeaders.setBearerAuth(testToken.queryOmadaClient()))
            .exchange()
            .expectStatus().isOk()
            .expectBody(OmadaOrgEnhetInfoDTO.class)
            .value(orgenhetDTO -> {
                assertThat(orgenhetDTO.orgEnhetOver()).isEqualTo(null);
                assertThat(orgenhetDTO.leder()).isEqualTo(null);
                assertThat(orgenhetDTO.norgId()).isEqualTo(null);
            });
    }

    @Test
    @DisplayName("Hente telefonnummer for en ressurs")
    void should_return_phone_number_for_ressurs() {
        when(dvhRepositoryMock.getTelefonNrListByNavIdent(NAVIDENT))
            .thenReturn(List.of(getTelefonNummer()));

        webTestClient.get()
            .uri("/api/v1/omada/telefonnummer/{navIdent}", NAVIDENT)
            .headers(httpHeaders -> httpHeaders.setBearerAuth(testToken.queryOmadaClient()))
            .exchange()
            .expectStatus().isOk();
    }

    @Test
    @DisplayName("Første telefonnummer returneres for en ressurs")
    void should_return_first_phone_number_for_ressurs() {
        var anotherPhoneNumber = DvhTelefon.builder().navIdent(NAVIDENT).nummer("11223344").build();
        when(dvhRepositoryMock.getTelefonNrListByNavIdent(NAVIDENT))
            .thenReturn(List.of(getTelefonNummer(), anotherPhoneNumber));

        webTestClient.get()
            .uri("/api/v1/omada/telefonnummer/{navIdent}", NAVIDENT)
            .headers(httpHeaders -> httpHeaders.setBearerAuth(testToken.queryOmadaClient()))
            .exchange()
            .expectStatus().isOk()
            .expectBody(OmadaRessursTelefonDTO.class)
            .value(ressursTelefonDTO -> assertThat(ressursTelefonDTO.telefonNummer()).isEqualTo(TELEFONNUMMER));
    }

    @Test
    @DisplayName("Returnerer 404 når ingen telefonnummer finnes for ressurs")
    void should_return_404_when_no_phone_number_is_found() {
        when(dvhRepositoryMock.getTelefonNrListByNavIdent(NAVIDENT))
            .thenReturn(Collections.emptyList());

        webTestClient.get()
            .uri("/api/v1/omada/telefonnummer/{navIdent}", NAVIDENT)
            .headers(httpHeaders -> httpHeaders.setBearerAuth(testToken.queryOmadaClient()))
            .exchange()
            .expectStatus().isNotFound();
    }


    private OrgEnhet getOrgEnhet() {
        return new OrgEnhet(OrgEnhetState.builder()
            .id(NOM_ID)
            .navn(ORG_ENHET_NAVN)
            .agressoId(Optional.of(AGRESSO_ID))
            .agressoOrgNivaa(Optional.of(AGRESSO_ORG_NIVAA))
            .build());
    }

    private RemedyEnhet getRemedyEnhet() {
        return RemedyEnhet.builder()
            .remedyEnhetId(NORG_ID)
            .nomId(NOM_ID)
            .build();
    }

    private OrganiseringState getOrganisering() {
        return OrganiseringState.builder()
            .id(NOM_ID)
            .idOver(NOM_ID_OVER)
            .build();
    }

    private List<DvhLeder> getLedere() {
        return List.of(DvhLeder.builder()
            .navIdent(NAVIDENT)
            .orgEnhetNr(NOM_ID)
            .build()
        );
    }

    private DvhTelefon getTelefonNummer() {
        return DvhTelefon.builder()
            .navIdent(NAVIDENT)
            .nummer(TELEFONNUMMER)
            .build();
    }
}