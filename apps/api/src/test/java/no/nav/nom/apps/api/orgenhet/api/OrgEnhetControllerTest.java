package no.nav.nom.apps.api.orgenhet.api;
import no.nav.nom.apps.api.TestTokensGenerator;
import no.nav.nom.apps.api.orgenhet.api.app.SetOrgEnhetsTypeRequest;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.OrgEnhetCommandService;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.commands.EndreOrgEnhetsType;
import no.nav.nom.apps.api.rest.RestTestBase;
import no.nav.nom.contract.nom.OrgEnhetsType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Optional;
import static no.nav.nom.apps.api.orgenhet.OrgenhetTestUtils.orgEnhetStateBuilderWithDefaultValues;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class OrgEnhetControllerTest extends RestTestBase {

    @MockBean
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

        orgEnhetStateRepository.putEventAggregate(existingOrgEnhet);

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

        orgEnhetStateRepository.putEventAggregate(existingOrgEnhet);

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
