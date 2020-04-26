package no.nav.nom.skjermede.personer.foresporsel.api.v1;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.val;
import no.nav.nom.skjermede.personer.common.JWTTokenProviderConfig;
import no.nav.nom.skjermede.personer.common.TokenUtil;
import no.nav.nom.skjermede.personer.foresporsel.Foresporsel;
import no.nav.nom.skjermede.personer.foresporsel.ForesporselConstants;
import no.nav.nom.skjermede.personer.foresporsel.ForesporselController;
import no.nav.nom.skjermede.personer.foresporsel.ForesporselRepository;
import no.nav.nom.skjermede.personer.skjerming.SkjermetPersonRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.nio.file.Paths;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@RequiredArgsConstructor
@WebMvcTest(controllers = {ForesporselController.class, JWTTokenProviderConfig.class})
public class ForesporselControllerTest {

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ForesporselRepository foresporselRepository;

    @MockBean
    private SkjermetPersonRepository skjermetPersonRepository;

    @MockBean
    private TokenUtil tokenUtil;

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testOpprettForesporsel() throws Exception {
        when(tokenUtil.getSubject()).thenReturn(ForesporselTestUtils.PERSONIDENT);
        when(tokenUtil.isAuthorized()).thenReturn(Boolean.TRUE);
        when(foresporselRepository.existsByPersonidentAndBekreftetFalse(ForesporselTestUtils.PERSONIDENT)).thenReturn(Boolean.FALSE);
        when(foresporselRepository.save(any(Foresporsel.class))).thenReturn(ForesporselTestUtils.foresporsel());

        mockMvc.perform(requestBuilder(HttpMethod.POST)
                .content(objectMapper.writeValueAsString(ForesporselTestUtils.foresporselRequest())))
                .andExpect(status().isCreated());
    }

    @Test
    public void testHentForesporsel() throws Exception {
        when(tokenUtil.getSubject()).thenReturn(ForesporselTestUtils.PERSONIDENT);
        when(tokenUtil.isAuthorized()).thenReturn(Boolean.TRUE);
        when(foresporselRepository.findByPersonidentAndBekreftetFalse(ForesporselTestUtils.PERSONIDENT))
                .thenReturn(Optional.of(ForesporselTestUtils.foresporsel()));

        mockMvc.perform(requestBuilder(HttpMethod.GET)).andExpect(status().isOk());
    }

    private MockHttpServletRequestBuilder requestBuilder(final HttpMethod method, final String ...requestPath) throws Exception {
        val path = requestPath.length == 0
                ? Paths.get(ForesporselConstants.BASE_URL)
                : Paths.get(ForesporselConstants.BASE_URL, requestPath);

        return (method == HttpMethod.GET ? get(path.toString()) : method == HttpMethod.POST ? post(path.toString()) : put(path.toString()))
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, getBearerToken(ForesporselTestUtils.PERSONIDENT));
    }

    private String getBearerToken(final String subject) throws Exception {
        final String token = mockMvc
                .perform(get(String.format("/local/jwt?subject=%s", ForesporselTestUtils.PERSONIDENT)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        return String.format("Bearer %s", token);
    }
}
