package no.nav.nom.apps.api.orgenhet.api;

import no.nav.nom.apps.api.TestTokensGenerator;
import no.nav.nom.apps.api.dvh.dto.DvhLeder;
import no.nav.nom.apps.api.orgenhet.api.app.SetOrgEnhetsTypeRequest;
import no.nav.nom.apps.api.orgenhet.api.app.dto.OrgEnhetMedLedernavn;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.OrgEnhetCommandService;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.commands.EndreOrgEnhetsType;
import no.nav.nom.apps.api.rest.RestTestBase;
import no.nav.nom.contract.nom.OrgEnhetsType;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.contract.nom.state.OrganiseringState;
import no.nav.nom.contract.nom.state.RessursState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static no.nav.nom.apps.api.common.TestData.organiseringState;
import static no.nav.nom.apps.api.common.TestData.ressursState;
import static no.nav.nom.apps.api.orgenhet.OrgenhetTestUtils.orgEnhetStateBuilderWithDefaultValues;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class OrgEnhetControllerTest extends RestTestBase {

    @MockitoBean
    OrgEnhetCommandService orgEnhetCommandService;

    @Autowired
    TestTokensGenerator testToken;

    @Captor
    ArgumentCaptor<EndreOrgEnhetsType> endreOrgEnhetsTypeArgumentCaptor;

    @BeforeEach
    public void init() {
        orgEnhetStateRepository.clear();
    }

    @Test
    public void should_set_new_orgEnhetsType() {

        var existingOrgEnhet = orgEnhetStateBuilderWithDefaultValues().toBuilder()
                .orgEnhetsType(Optional.empty())
                .build();

        var setOrgEnhetsTypeRequest = setOrgEnhetsTypeRequest().toBuilder()
                .nomId(existingOrgEnhet.id())
                .orgEnhetsType(OrgEnhetsType.ARBEIDSLIVSSENTER)
                .build();

        orgEnhetStateRepository.update(existingOrgEnhet);

        postOrgenhetType(testToken.nomAdminUser(), setOrgEnhetsTypeRequest)
                .expectStatus().isOk();

        verify(orgEnhetCommandService, times(1))
                .endreOrgEnhetsType(eq(existingOrgEnhet.fid()), endreOrgEnhetsTypeArgumentCaptor.capture());

        assertThat(endreOrgEnhetsTypeArgumentCaptor.getValue().orgEnhetsType())
                .contains(setOrgEnhetsTypeRequest.getOrgEnhetsType());
    }

    @Test
    public void should_not_send_EndreOrgEnhetsType_event_when_existing_value_is_same_as_new() {

        var existingOrgenhetsType = OrgEnhetsType.FYLKE;

        var existingOrgEnhet = orgEnhetStateBuilderWithDefaultValues().toBuilder()
                .orgEnhetsType(Optional.of(existingOrgenhetsType))
                .build();

        orgEnhetStateRepository.update(existingOrgEnhet);

        var requestBody = setOrgEnhetsTypeRequest().toBuilder()
                .nomId(existingOrgEnhet.id())
                .orgEnhetsType(existingOrgenhetsType)
                .build();

        postOrgenhetType(testToken.nomAdminUser(), requestBody)
                .expectStatus().isOk();

        verify(orgEnhetCommandService, times(0)).endreOrgEnhetsType(any(), any());
    }

    @Test
    public void should_return_404_when_org_enhet_not_found() {

        postOrgenhetType(testToken.nomAdminUser(), setOrgEnhetsTypeRequest())
                .expectStatus().isNotFound();

        verify(orgEnhetCommandService, times(0))
                .endreOrgEnhetsType(any(), any());

    }

    @Test
    public void should_return_403_for_users_without_access() {
        postOrgenhetType(testToken.noScopesUser(), setOrgEnhetsTypeRequest())
                .expectStatus().isForbidden();
    }

    @Test
    public void should_return_orgEnheter_that_has_parent_as_orgEnheter_with_manager_navIdenter() {
        String orgEnhetId = "abc123d";
        String orgEnhetOverId = "efg456h";

        OrgEnhetState orgEnhet = orgEnhetStateBuilderWithDefaultValues().toBuilder()
                .id(orgEnhetId)
                .gyldigFom(LocalDate.now().minusMonths(6))
                .agressoOrgNivaa(Optional.of("ORGENHET"))
                .build();
        
        OrganiseringState organisering = organiseringState().toBuilder()
                .id(orgEnhetId)
                .idOver(orgEnhetOverId)
                .build();
        
        orgEnhetStateRepository.update(orgEnhet);
        organiseringStateRepository.update(organisering);

        RessursState lederRessurs = ressursState();
        ressursStateRepository.update(lederRessurs);

        @SuppressWarnings("OptionalGetWithoutIsPresent")
        DvhLeder dvhLeder = DvhLeder.builder()
                .orgEnhetNr(orgEnhet.agressoId().get())
                .orgNiv(orgEnhet.agressoOrgNivaa().get())
                .orgenhetNavn(orgEnhet.navn())
                .navIdent(lederRessurs.navident())
                .build();
        dvhRepository.saveLedere(List.of(dvhLeder));


        List<OrgEnhetMedLedernavn> result = webTestClient.get()
                .uri("/app/orgenheter/kanHaRessurser")
                .headers(httpHeaders -> httpHeaders.setBearerAuth(testToken.noScopesUser()))
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBodyList(OrgEnhetMedLedernavn.class)
                .returnResult().getResponseBody();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().ledere()).isEqualTo(List.of(lederRessurs.visningsnavn().getFormattedVisningsnavn()));
    }


    private WebTestClient.ResponseSpec postOrgenhetType(String accessToken, SetOrgEnhetsTypeRequest requestBody) {
        return webTestClient.post()
                .uri("/app/orgenheter/orgEnhetsType")
                .headers(httpHeaders -> httpHeaders.setBearerAuth(accessToken))
                .bodyValue(requestBody)
                .exchange();
    }

    private SetOrgEnhetsTypeRequest setOrgEnhetsTypeRequest() {
        return new SetOrgEnhetsTypeRequest("yobrah", OrgEnhetsType.ARBEIDSLIVSSENTER);
    }

}
