package no.nav.nom.apps.sfintegrasjon.sfhendelse.api;

import no.nav.nom.apps.sfintegrasjon.config.SecurityConfig;
import no.nav.nom.apps.sfintegrasjon.sfhendelse.repository.RessursHendelseRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@Import(SecurityConfig.class)
@TestPropertySource(
    properties = {
        "NOM_ADMIN_GROUP_ID=test-admin-group",
        "ZALARIS_CLIENT_ID=test-zalaris-client",
        "AZURE_APP_TENANT_ID=test-tenant-id",
        "AZURE_APP_CLIENT_ID=test-client-id"
    }
)
@WebMvcTest(controllers = RessursHendelseController.class, excludeAutoConfiguration = OAuth2ClientAutoConfiguration.class)
public class RessursHendelseControllerTest {

    @MockitoBean
    private RessursHendelseRepository ressursHendelseRepository;

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void registrererRessursHendelse() throws Exception {
        var melding = """
                      {
                          "sporingsId": "123",
                          "endretAv": "A001001",
                          "endringshendelser": ["RessursOpprettet"],
                          "ressurs": {
                            "navident": "R001001"
                          }
                      }
                      """.trim();

        mockMvc.perform(post(RessursHendelseController.BASE_URL)
                .with(jwt().authorities(new SimpleGrantedAuthority("ADMIN")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(melding))
            .andExpect(MockMvcResultMatchers.status().isCreated());
    }

    @Test
    public void badRequestForManglendeRessurs() throws Exception {
        var melding = """
                      {
                          "ressurs": {
                              "navn": ""
                          }
                      }
                      """.trim();
        mockMvc.perform(post(RessursHendelseController.BASE_URL)
                .with(jwt().authorities(new SimpleGrantedAuthority("ADMIN")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(melding))
            .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }
}
