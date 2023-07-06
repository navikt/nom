package no.nav.nom.apps.navident.api;

import com.google.gson.Gson;
import com.thomaskasene.wiremock.junit.WireMockStubs;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.navident.IdentService;
import no.nav.nom.apps.navident.RsaStub;
import no.nav.nom.apps.navident.api.dto.NavidentRequestDTO;
import no.nav.nom.apps.navident.domain.IdentType;
import no.nav.nom.apps.navident.rnrservices.Respons;
import no.nav.nom.libs.utils.common.mdc.RestMdcInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@ActiveProfiles("test")
@WireMockStubs(RsaStub.class)
@WebMvcTest(NavidentController.class)
@ContextConfiguration(classes = {NavidentController.class, RestMdcInterceptor.class})
class NavidentControllerWebMvcTest {
    @Autowired
    private MockMvc mvc;

    @MockBean
    IdentService identService;

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

        return post(NavidentController.BASE_URL)
                .with(jwt()) // Lager en JWT som will passere alle SecurityFilters
                .contentType(MediaType.APPLICATION_JSON)
                .content(reqJson);
    }
}