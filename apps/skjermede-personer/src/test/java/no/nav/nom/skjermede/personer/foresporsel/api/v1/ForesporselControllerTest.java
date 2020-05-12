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
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.client.RestTemplate;

import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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

    @MockBean
    private RestTemplate restTemplate;

    @Test
    public void testOpprettForesporsel() throws Exception {
        Foresporsel foresporsel = ForesporselTestUtils.foresporsel();
        when(tokenUtil.getSubject()).thenReturn(foresporsel.getPersonident());
        when(tokenUtil.isAuthorized()).thenReturn(Boolean.TRUE);
        when(foresporselRepository.existsByPersonidentAndBekreftetFalse(foresporsel.getPersonident())).thenReturn(Boolean.FALSE);
        when(foresporselRepository.save(any(Foresporsel.class))).thenReturn(foresporsel);

        mockMvc.perform(requestBuilder(HttpMethod.POST)
                .content(objectMapper.writeValueAsString(ForesporselTestUtils.foresporselRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.opprettetDato", is(foresporsel.getOpprettetDato().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))));
    }

    @Test
    public void testHentForesporsel() throws Exception {
        Foresporsel foresporsel = ForesporselTestUtils.foresporsel();
        when(tokenUtil.getSubject()).thenReturn(foresporsel.getPersonident());
        when(tokenUtil.isAuthorized()).thenReturn(Boolean.TRUE);
        when(foresporselRepository.findByPersonidentAndBekreftetFalse(ForesporselTestUtils.PERSONIDENT))
                .thenReturn(Optional.of(foresporsel));

        mockMvc.perform(requestBuilder(HttpMethod.GET))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.opprettetDato", is(foresporsel.getOpprettetDato().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))));
    }

    @Test
    public void testBekreftForesporsel() throws Exception {
        UUID uuid = ForesporselTestUtils.foresporsel().getFunksjonellId();
        when(foresporselRepository.findByFunksjonellIdAndBekreftetFalse(uuid))
                .thenReturn(Optional.of(ForesporselTestUtils.foresporsel()));
        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), any(Class.class))).thenReturn(ForesporselTestUtils.ressurs());

        mockMvc.perform(requestBuilder(HttpMethod.GET, String.format("bekreft?id=%s", uuid)))
                .andExpect(status().isOk())
                .andExpect(content().string("Bekreftelse av skjerming registrert"));
    }

    @Test
    public void testBekreftForesporselAnsattUgyldig() throws Exception {
        UUID uuid = ForesporselTestUtils.foresporsel().getFunksjonellId();
        when(foresporselRepository.findByFunksjonellIdAndBekreftetFalse(uuid))
                .thenReturn(Optional.of(ForesporselTestUtils.foresporsel()));
        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), any(Class.class))).thenReturn(new ResponseEntity(HttpStatus.OK));

        mockMvc.perform(requestBuilder(HttpMethod.GET, String.format("bekreft?id=%s", uuid)))
                .andExpect(status().isOk())
                .andExpect(content().string("Ugyldig operasjon"));
    }

    @Test
    public void testBekreftForesporselFunksjonellIdUgyldig() throws Exception {
        UUID uuid = ForesporselTestUtils.foresporsel().getFunksjonellId();
        when(foresporselRepository.findByFunksjonellIdAndBekreftetFalse(uuid))
                .thenReturn(Optional.of(ForesporselTestUtils.foresporsel()));

        mockMvc.perform(requestBuilder(HttpMethod.GET, String.format("bekreft?id=%s", UUID.randomUUID())))
                .andExpect(status().isOk())
                .andExpect(content().string("Ugyldig operasjon"));
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
