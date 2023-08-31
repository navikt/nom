package no.nav.nom.apps.api.orgenhet.api;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.OrgEnhetCommandService;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.commands.EndreOrgEnhetsType;
import no.nav.nom.apps.api.rest.RestTestBase;
import no.nav.nom.contract.nom.OrgEnhetsType;
import no.nav.nom.libs.test.SecurityTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import java.util.Optional;
import static no.nav.nom.apps.api.orgenhet.OrgenhetTestUtils.orgEnhetStateBuilderWithDefaultValues;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class OrgEnhetControllerTest extends RestTestBase {

    public static final String ORGENHETSTYPE_URL = "/app/orgenhet/orgEnhetsType";
    @MockBean
    OrgEnhetCommandService orgEnhetCommandService;

    @Autowired
    private SecurityTestUtils securityTestUtils;

    @Captor
    ArgumentCaptor<EndreOrgEnhetsType> endreOrgEnhetsTypeArgumentCaptor;

    @BeforeEach
    public void init() {
        orgEnhetStateRepository.clear();
    }

    @Test
    public void should_set_new_orgEnhetsType() {

        var existingOrgEnhet = orgEnhetStateBuilderWithDefaultValues()
            .orgEnhetsType(Optional.empty())
            .build();

        var setOrgEnhetsTypeRequest = new SetOrgEnhetsTypeRequest(existingOrgEnhet.id(), OrgEnhetsType.ARBEIDSLIVSSENTER);

        orgEnhetStateRepository.putEventAggregate(existingOrgEnhet);

        webTestClient.post()
            .uri(ORGENHETSTYPE_URL)
            .headers(httpHeaders -> httpHeaders.setBearerAuth(securityTestUtils.createToken(
                validJwtWithAzpClaim("admin", "admin")))
            )
            .bodyValue(setOrgEnhetsTypeRequest)
            .exchange()
            .expectStatus().isOk();

        verify(orgEnhetCommandService, times(1))
            .endreOrgEnhetsType(eq(existingOrgEnhet.fid()), endreOrgEnhetsTypeArgumentCaptor.capture());

        assertThat(endreOrgEnhetsTypeArgumentCaptor.getValue().orgEnhetsType())
            .contains(setOrgEnhetsTypeRequest.getOrgEnhetsType());
    }

    @Test
    public void should_not_send_EndreOrgEnhetsType_event_when_existing_value_is_same_as_new() {

        var existingOrgenhetsType = OrgEnhetsType.FYLKE;

        var existingOrgEnhet = orgEnhetStateBuilderWithDefaultValues()
            .orgEnhetsType(Optional.of(existingOrgenhetsType))
            .build();

        orgEnhetStateRepository.putEventAggregate(existingOrgEnhet);

        webTestClient.post()
            .uri(ORGENHETSTYPE_URL)
            .headers(httpHeaders -> httpHeaders.setBearerAuth(securityTestUtils.createToken(
                validJwtWithAzpClaim("admin", "admin")))
            )
            .bodyValue(new SetOrgEnhetsTypeRequest(
                existingOrgEnhet.id(),
                existingOrgenhetsType
            ))
            .exchange()
            .expectStatus().isOk();

        verify(orgEnhetCommandService, times(0)).endreOrgEnhetsType(any(), any());
    }

    @Test
    public void should_return_404_when_org_enhet_not_found() {

        webTestClient.post()
            .uri(ORGENHETSTYPE_URL)
            .headers(httpHeaders -> httpHeaders.setBearerAuth(securityTestUtils.createToken(
                validJwtWithAzpClaim("admin", "admin"))
            ))
            .bodyValue(new SetOrgEnhetsTypeRequest("yobrah", OrgEnhetsType.ARBEIDSLIVSSENTER))
            .exchange()
            .expectStatus().isNotFound();

        verify(orgEnhetCommandService, times(0))
            .endreOrgEnhetsType(any(), any());

    }

    @Test
    public void should_return_403_for_users_without_access() {

        webTestClient.post()
            .uri(ORGENHETSTYPE_URL)
            .headers(httpHeaders -> httpHeaders.setBearerAuth(securityTestUtils.createToken(
                validJwtWithAzpClaim("noAdmin", "no-admin"))
            ))
            .bodyValue(new SetOrgEnhetsTypeRequest("yobrah", OrgEnhetsType.ARBEIDSLIVSSENTER))
            .exchange()
            .expectStatus().isForbidden();

    }

}
