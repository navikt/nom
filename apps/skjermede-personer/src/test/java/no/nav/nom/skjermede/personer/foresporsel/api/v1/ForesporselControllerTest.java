package no.nav.nom.skjermede.personer.foresporsel.api.v1;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import lombok.RequiredArgsConstructor;
import lombok.val;
import no.nav.nom.skjermede.personer.JWTTokenProvderTestConfig;
import no.nav.nom.skjermede.personer.common.NomRessurserService;
import no.nav.nom.skjermede.personer.common.security.TokenUtil;
import no.nav.nom.skjermede.personer.foresporsel.Foresporsel;
import no.nav.nom.skjermede.personer.foresporsel.ForesporselRepository;
import no.nav.nom.skjermede.personer.foresporsel.GraphService;
import no.nav.nom.skjermede.personer.foresporsel.azure.TokenService;
import no.nav.nom.skjermede.personer.foresporsel.api.v1.dto.TilknyttetForesporselDTO;
import no.nav.nom.skjermede.personer.skjerming.SkjermetPersonRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static no.nav.nom.skjermede.personer.foresporsel.api.v1.ForesporselController.ANSATT_NOT_ACTIVE_MESSAGE;
import static no.nav.nom.skjermede.personer.foresporsel.api.v1.ForesporselController.TILKNYTTET_FORESPORSEL_NOT_FOUND_MESSAGE;
import static no.nav.nom.skjermede.personer.foresporsel.api.v1.ForesporselTestUtils.TILKNYTTET_RESSURS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@RequiredArgsConstructor
@WebMvcTest(controllers = {ForesporselController.class, JWTTokenProvderTestConfig.class})
public class ForesporselControllerTest {

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ForesporselRepository foresporselRepository;

    @MockBean
    private NomRessurserService nomRessurserService;

    @MockBean
    private SkjermetPersonRepository skjermetPersonRepository;

    @MockBean
    private TokenUtil tokenUtil;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RestTemplate restTemplate;

    @MockBean
    private Counter counter;

    @MockBean
    private GraphService graphService;

    @MockBean
    private TokenService tokenService;

    @MockBean
    private OAuth2AuthorizedClientService oAuth2AuthorizedClientService;

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

    @Test
    public void testHentTilknyttetForesporsel() throws Exception {
        when(tokenUtil.getSubject()).thenReturn(ForesporselTestUtils.PERSONIDENT);
        when(tokenUtil.isAuthorized()).thenReturn(Boolean.TRUE);
        when(nomRessurserService.getInternRessursByPersonident(anyString(), anyBoolean())).thenReturn(ForesporselTestUtils.ressurs());
        val familie1 = ForesporselTestUtils.foresporsel();
        val familie2 = ForesporselTestUtils.foresporsel();
        familie2.setFornavn("Ola");
        when(foresporselRepository.findAllByTilknyttetRessurs(TILKNYTTET_RESSURS))
                .thenReturn(Arrays.asList(familie1, familie2));

        val response = mockMvc.perform(requestBuilder(HttpMethod.GET, "/tilknyttede"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        List<TilknyttetForesporselDTO> tilknyttedeForesporsler = objectMapper.readValue(response, new TypeReference<>() {
        });

        assertEquals(tilknyttedeForesporsler.size(), 2);
        assertEquals(tilknyttedeForesporsler.get(0).getFornavn(), "Jenny");
        assertEquals(tilknyttedeForesporsler.get(1).getFornavn(), "Ola");
    }

    @Test
    public void testHentTilknyttetForesporselAnsattUgyldig() throws Exception {
        when(tokenUtil.getSubject()).thenReturn(ForesporselTestUtils.PERSONIDENT);
        when(tokenUtil.isAuthorized()).thenReturn(Boolean.TRUE);
        when(nomRessurserService.getInternRessursByPersonident(anyString(), anyBoolean())).thenReturn(null);

        mockMvc.perform(requestBuilder(HttpMethod.GET, "/tilknyttede"))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
    }

    @Test
    public void testBekreftForesporsel() throws Exception {
        val uuid = ForesporselTestUtils.foresporsel().getFunksjonellId();
        when(foresporselRepository.findByFunksjonellIdAndBekreftetFalse(uuid))
                .thenReturn(Optional.of(ForesporselTestUtils.foresporsel()));
        when(nomRessurserService.getInternRessursByNavident(anyString(), anyBoolean()))
                .thenReturn(ForesporselTestUtils.ressurs());
        when(tokenService.getOboAccessToken()).thenReturn("");
        when(graphService.getNavident(anyString())).thenReturn(ForesporselTestUtils.TILKNYTTET_RESSURS);

        mockMvc.perform(requestBuilder(HttpMethod.GET, String.format("bekreft?id=%s", uuid)))
                .andExpect(status().isOk())
                .andExpect(content().string("Bekreftelse av skjerming registrert"));
    }

    @Test
    public void testBekreftTilknyttetForesporsel() throws Exception {
        when(tokenUtil.getSubject()).thenReturn(ForesporselTestUtils.PERSONIDENT);
        when(tokenUtil.isAuthorized()).thenReturn(Boolean.TRUE);
        when(nomRessurserService.getInternRessursByPersonident(anyString(), anyBoolean())).thenReturn(ForesporselTestUtils.ressurs());
        val foresporsel = ForesporselTestUtils.foresporsel();
        when(foresporselRepository.findByTilknyttetRessursAndFornavnAndEtternavnAndBekreftetFalse(TILKNYTTET_RESSURS, foresporsel.getFornavn(), foresporsel.getEtternavn())).thenReturn(Optional.of(foresporsel));

        val tilknyttetForesporsel = TilknyttetForesporselDTO.builder()
                .fornavn(foresporsel.getFornavn())
                .etternavn(foresporsel.getEtternavn())
                .opprettetDato(LocalDateTime.now())
                .build();

        mockMvc.perform(requestBuilder(HttpMethod.POST, "tilknyttede/bekreft")
                .content(objectMapper.writeValueAsString(tilknyttetForesporsel)))
                .andExpect(status().isOk());
    }

    @Test
    public void testBekreftTilknyttetForesporselNotGyldigAnsatt() throws Exception {
        when(tokenUtil.getSubject()).thenReturn(ForesporselTestUtils.PERSONIDENT);
        when(tokenUtil.isAuthorized()).thenReturn(Boolean.TRUE);
        when(nomRessurserService.getInternRessursByPersonident(anyString(), anyBoolean())).thenReturn(null);
        val foresporsel = ForesporselTestUtils.foresporsel();
        when(foresporselRepository.findByTilknyttetRessursAndFornavnAndEtternavnAndBekreftetFalse(TILKNYTTET_RESSURS, foresporsel.getFornavn(), foresporsel.getEtternavn())).thenReturn(Optional.of(foresporsel));

        val tilknyttetForesporsel = TilknyttetForesporselDTO.builder().fornavn(foresporsel.getFornavn()).etternavn(foresporsel.getEtternavn()).opprettetDato(LocalDateTime.now()).build();

        mockMvc.perform(requestBuilder(HttpMethod.POST, "tilknyttede/bekreft")
                .content(objectMapper.writeValueAsString(tilknyttetForesporsel)))
                .andExpect(status().isBadRequest())
                .andExpect(status().reason(ANSATT_NOT_ACTIVE_MESSAGE));
    }

    @Test
    public void testBekreftTilknyttetForesporselIngenAktiveForesporsel() throws Exception {
        when(tokenUtil.getSubject()).thenReturn(ForesporselTestUtils.PERSONIDENT);
        when(tokenUtil.isAuthorized()).thenReturn(Boolean.TRUE);
        when(nomRessurserService.getInternRessursByPersonident(anyString(), anyBoolean())).thenReturn(ForesporselTestUtils.ressurs());
        val foresporsel = ForesporselTestUtils.foresporsel();
        when(foresporselRepository.findByTilknyttetRessursAndFornavnAndEtternavnAndBekreftetFalse(TILKNYTTET_RESSURS, foresporsel.getFornavn(), foresporsel.getEtternavn())).thenReturn(Optional.empty());

        val tilknyttetForesporsel = TilknyttetForesporselDTO.builder().fornavn(foresporsel.getFornavn()).etternavn(foresporsel.getEtternavn()).opprettetDato(LocalDateTime.now()).build();

        mockMvc.perform(requestBuilder(HttpMethod.POST, "tilknyttede/bekreft")
                .content(objectMapper.writeValueAsString(tilknyttetForesporsel)))
                .andExpect(status().isNotFound())
                .andExpect(status().reason(TILKNYTTET_FORESPORSEL_NOT_FOUND_MESSAGE));
    }

    @Test
    public void testBekreftForesporselAnsattUgyldig() throws Exception {
        val uuid = ForesporselTestUtils.foresporsel().getFunksjonellId();
        when(foresporselRepository.findByFunksjonellIdAndBekreftetFalse(uuid))
                .thenReturn(Optional.of(ForesporselTestUtils.foresporsel()));
        when(nomRessurserService.getInternRessursByNavident(anyString(), anyBoolean())).thenReturn(null);
        when(tokenService.getOboAccessToken()).thenReturn("");
        when(graphService.getNavident(anyString())).thenReturn(ForesporselTestUtils.TILKNYTTET_RESSURS.substring(1));

        mockMvc.perform(requestBuilder(HttpMethod.GET, String.format("bekreft?id=%s", uuid)))
                .andExpect(status().isOk())
                .andExpect(content().string("Tilgang nektet"));
    }

    @Test
    public void testBekreftForesporselFunksjonellIdUgyldig() throws Exception {
        val uuid = ForesporselTestUtils.foresporsel().getFunksjonellId();
        when(foresporselRepository.findByFunksjonellIdAndBekreftetFalse(uuid))
                .thenReturn(Optional.of(ForesporselTestUtils.foresporsel()));
        when(tokenService.getOboAccessToken()).thenReturn("");
        when(graphService.getNavident(anyString())).thenReturn(ForesporselTestUtils.TILKNYTTET_RESSURS);

        mockMvc.perform(requestBuilder(HttpMethod.GET, String.format("bekreft?id=%s", UUID.randomUUID())))
                .andExpect(status().isOk())
                .andExpect(content().string("Ugyldig operasjon"));
    }

    @Test
    public void testSlettetForesporsel() throws Exception {
        when(tokenUtil.getSubject()).thenReturn(ForesporselTestUtils.PERSONIDENT);
        when(tokenUtil.isAuthorized()).thenReturn(Boolean.TRUE);
        when(foresporselRepository.findByPersonidentAndBekreftetFalse(ForesporselTestUtils.PERSONIDENT)).thenReturn(Optional.of(ForesporselTestUtils.foresporsel()));
        doNothing().when(foresporselRepository).delete(any(Foresporsel.class));

        mockMvc.perform(requestBuilder(HttpMethod.DELETE)).andExpect(status().isOk());
    }

    @Test
    public void testSlettetForesporselNotFound() throws Exception {
        when(tokenUtil.getSubject()).thenReturn(ForesporselTestUtils.PERSONIDENT);
        when(tokenUtil.isAuthorized()).thenReturn(Boolean.TRUE);
        when(foresporselRepository.findByPersonidentAndBekreftetFalse(ForesporselTestUtils.PERSONIDENT)).thenReturn(Optional.empty());
        doNothing().when(foresporselRepository).delete(any(Foresporsel.class));

        mockMvc.perform(requestBuilder(HttpMethod.DELETE)).andExpect(status().isNotFound());
    }

    private MockHttpServletRequestBuilder requestBuilder(final HttpMethod method, final String ...requestPath) throws Exception {
        val path = UriComponentsBuilder.newInstance()
                .path(ForesporselController.BASE_URL)
                .pathSegment(requestPath)
                .build().toString();

        return (method == HttpMethod.GET ? get(path) : method == HttpMethod.POST ? post(path) : method == HttpMethod.PUT ? put(path) : delete(path))
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
