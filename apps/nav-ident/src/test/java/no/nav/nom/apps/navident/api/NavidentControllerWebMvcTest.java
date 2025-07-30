package no.nav.nom.apps.navident.api;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.navident.IdentService;
import no.nav.nom.apps.navident.api.dto.NavidentRequestDTO;
import no.nav.nom.apps.navident.eventsource.EventSourceProducer;
import no.nav.nom.apps.navident.rnrservices.Respons;
import no.nav.nom.contract.nom.IdentType;
import no.nav.nom.libs.test.MockOAuth2ServerInitializer;
import no.nav.nom.libs.utils.common.mdc.RestMdcInterceptor;
import no.nav.security.mock.oauth2.MockOAuth2Server;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import java.time.Instant;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@ActiveProfiles("test")
@ContextConfiguration(classes = {NavidentController.class, RestMdcInterceptor.class}, initializers = MockOAuth2ServerInitializer.class)
@WebMvcTest(NavidentController.class)
class NavidentControllerWebMvcTest {

    @Autowired
    MockOAuth2Server mockOAuth2Server;

    @MockitoBean
    OAuth2AuthorizedClientManager oAuth2AuthorizedClientManager;

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    IdentService identService;

    @MockitoBean
    EventSourceProducer eventSourceProducer;

    @BeforeEach
    void setup() {
        OAuth2AuthorizedClient oAuth2AuthorizedClient = mock(OAuth2AuthorizedClient.class);
        when(oAuth2AuthorizedClientManager.authorize(any())).thenReturn(oAuth2AuthorizedClient);
        when(oAuth2AuthorizedClient.getAccessToken()).thenReturn(
                new OAuth2AccessToken(
                        OAuth2AccessToken.TokenType.BEARER,
                        "tokenValue",
                        Instant.now(),
                        Instant.now().plusSeconds(1000L)
                )
        );
    }

    @Test
    public void createNavident() throws Exception {
        String personId = "01019012345";
        char prefix = 'X';
        IdentType type = IdentType.STANDARD;

        String generertNavIdent = "X123456";
        Respons rnrResponse = Respons.builder()
                .body(Respons.Body.builder()
                        .navident(generertNavIdent)
                        .build()
                )
                .httpStatus(CREATED)
                .build();

        when(identService.bestillIdent(personId, type, prefix)).thenReturn(rnrResponse);

        mvc.perform(
                        getValidRequest(personId, type, String.valueOf(prefix))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.navident", is(generertNavIdent)))
                .andDo(result -> log.debug("Response body: %s".formatted(result.getResponse().getContentAsString())))
                .andDo(MockMvcResultHandlers.print())
        ;
    }

    private MockHttpServletRequestBuilder getValidRequest(String personId, IdentType type, String prefix) {
        NavidentRequestDTO req = NavidentRequestDTO.builder()
                .personid(personId)
                .identType(type)
                .identPrefix(prefix)
                .build();
        Gson gson = new Gson();
        String reqJson = gson.toJson(req);
        log.debug("Request body: %s".formatted(reqJson));

        return post(NavidentController.BASE_URL + "/create")
                .with(jwt()) // Lager en JWT som will passere alle SecurityFilters
                .contentType(MediaType.APPLICATION_JSON)
                .content(reqJson);
    }
}