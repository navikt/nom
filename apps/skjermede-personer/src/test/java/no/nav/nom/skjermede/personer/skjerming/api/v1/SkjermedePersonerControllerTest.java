package no.nav.nom.skjermede.personer.skjerming.api.v1;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.val;
import no.nav.nom.skjermede.personer.JWTTokenProvderTestConfig;
import no.nav.nom.skjermede.personer.common.NomRessurserService;
import no.nav.nom.skjermede.personer.common.security.TokenUtil;
import no.nav.nom.skjermede.personer.skjerming.SkjermetPerson;
import no.nav.nom.skjermede.personer.skjerming.SkjermetPersonRepository;
import no.nav.nom.skjermede.personer.skjerming.api.v1.dto.SkjermetPersonResponseDTO;
import org.apache.logging.log4j.util.Strings;
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
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@RequiredArgsConstructor
@WebMvcTest(controllers = {SkjermedePersonerController.class, JWTTokenProvderTestConfig.class})
public class SkjermedePersonerControllerTest {

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SkjermetPersonRepository repository;

    @MockBean
    private NomRessurserService nomRessurserService;

    @MockBean
    private TokenUtil tokenUtil;

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testHentSkjermingUnauthorized() throws Exception {
        when(repository.getSkjermetPersonByPersonident(SkjermingTestUtils.PERSONIDENT)).thenReturn(Optional.of(SkjermingTestUtils.skjermetPerson()));
        when(tokenUtil.isAuthorized()).thenReturn(Boolean.FALSE);

        mockMvc.perform(requestBuilder(HttpMethod.GET)).andExpect(status().isUnauthorized());
    }

    @Test
    public void testHentSkjerming() throws Exception {
        when(tokenUtil.getSubject()).thenReturn(SkjermingTestUtils.PERSONIDENT);
        when(tokenUtil.isAuthorized()).thenReturn(Boolean.TRUE);
        when(repository.getSkjermetPersonByPersonident(SkjermingTestUtils.PERSONIDENT)).thenReturn(Optional.of(SkjermingTestUtils.skjermetPerson()));

        mockMvc.perform(requestBuilder(HttpMethod.GET)).andExpect(status().isOk());
    }

    @Test
    public void testHentSkjermingNotFound() throws Exception {
        when(tokenUtil.getSubject()).thenReturn(SkjermingTestUtils.PERSONIDENT);
        when(tokenUtil.isAuthorized()).thenReturn(Boolean.TRUE);
        when(repository.getSkjermetPersonByPersonident(SkjermingTestUtils.PERSONIDENT)).thenReturn(Optional.ofNullable(null));

        mockMvc.perform(requestBuilder(HttpMethod.GET)).andExpect(status().isOk()).andExpect(jsonPath("$").doesNotExist());
    }

    @Test
    public void testHentSkjermedeFamiliemedlemerUnauthorized() throws Exception {
        when(tokenUtil.isAuthorized()).thenReturn(Boolean.FALSE);
        mockMvc.perform(requestBuilder(HttpMethod.GET, SkjermedePersonerController.FAMILIEMEDLEMMER)).andExpect(status().isUnauthorized());
    }

    @Test
    public void testHentSkjermedeFamiliemedlemer() throws Exception {
        when(nomRessurserService.getInternRessursByPersonident(anyString(), anyBoolean())).thenReturn(SkjermingTestUtils.ressurs());
        when(repository.findAllByTilknyttetRessurs(SkjermingTestUtils.NAVIDENT)).thenReturn(Arrays.asList(SkjermingTestUtils.skjermetPerson()));
        when(tokenUtil.isAuthorized()).thenReturn(Boolean.TRUE);
        when(tokenUtil.getSubject()).thenReturn(SkjermingTestUtils.PERSONIDENT);

        val response = mockMvc.perform(requestBuilder(HttpMethod.GET, SkjermedePersonerController.FAMILIEMEDLEMMER))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertEquals(objectMapper.readValue(response, new TypeReference<List<SkjermetPersonResponseDTO>>() {}).size(), 1);
    }

    @Test
    public void testHentSkjermingerForAnsattUnauthorized() throws Exception {
        when(tokenUtil.getSubject()).thenReturn(Strings.EMPTY);
        when(tokenUtil.isAuthorized()).thenReturn(Boolean.FALSE);

        mockMvc.perform(requestBuilder(HttpMethod.GET, SkjermedePersonerController.ALLE)).andExpect(status().isUnauthorized());
    }

    @Test
    public void testHentSkjermingerForAnsatt() throws Exception {
        when(tokenUtil.getSubject()).thenReturn(SkjermingTestUtils.PERSONIDENT);
        when(tokenUtil.isAuthorized()).thenReturn(Boolean.TRUE);

        mockMvc.perform(requestBuilder(HttpMethod.GET, SkjermedePersonerController.ALLE)).andExpect(status().isOk());
    }

    @Test
    public void testOpprettSkjermingDuplicate() throws Exception {
        when(tokenUtil.getSubject()).thenReturn(SkjermingTestUtils.PERSONIDENT);
        when(tokenUtil.isAuthorized()).thenReturn(Boolean.TRUE);
        when(repository.getSkjermetPersonByPersonident(SkjermingTestUtils.PERSONIDENT)).thenReturn(Optional.of(SkjermingTestUtils.skjermetPerson()));

        mockMvc.perform(requestBuilder(HttpMethod.POST)).andExpect(status().isBadRequest());
    }

    @Test
    public void testOpprettSkjermingUnauthorized() throws Exception {
        when(tokenUtil.getSubject()).thenReturn(SkjermingTestUtils.PERSONIDENT);
        when(tokenUtil.isAuthorized()).thenReturn(Boolean.FALSE);

        mockMvc.perform(requestBuilder(HttpMethod.POST)).andExpect(status().isUnauthorized());
    }

    @Test
    public void testOpprettSkjerming() throws Exception {
        when(tokenUtil.getSubject()).thenReturn(SkjermingTestUtils.PERSONIDENT);
        when(tokenUtil.isAuthorized()).thenReturn(Boolean.TRUE);
        when(repository.getSkjermetPersonByPersonident(SkjermingTestUtils.PERSONIDENT)).thenReturn(Optional.empty());
        when(nomRessurserService.getInternRessursByPersonident(anyString(), anyBoolean())).thenReturn(SkjermingTestUtils.ressurs());
        when(repository.save(any(SkjermetPerson.class))).thenReturn(SkjermingTestUtils.skjermetPerson());

        mockMvc.perform(requestBuilder(HttpMethod.POST)).andExpect(status().isCreated());
    }

    @Test
    public void testOpphorSkjermingNotFound() throws Exception {
        when(tokenUtil.getSubject()).thenReturn(SkjermingTestUtils.PERSONIDENT);
        when(tokenUtil.isAuthorized()).thenReturn(Boolean.TRUE);
        when(repository.getSkjermetPersonByPersonident(SkjermingTestUtils.PERSONIDENT)).thenReturn(Optional.empty());

        mockMvc.perform(requestBuilder(HttpMethod.PUT)).andExpect(status().isNotFound());
    }

    @Test
    public void testOpphorSkjermingUnauthorized() throws Exception {
        when(tokenUtil.getSubject()).thenReturn(SkjermingTestUtils.PERSONIDENT);
        when(tokenUtil.isAuthorized()).thenReturn(Boolean.FALSE);

        mockMvc.perform(requestBuilder(HttpMethod.PUT)).andExpect(status().isUnauthorized());
    }

    @Test
    public void testOpphorSkjerming() throws Exception {
        when(tokenUtil.getSubject()).thenReturn(SkjermingTestUtils.PERSONIDENT);
        when(tokenUtil.isAuthorized()).thenReturn(Boolean.TRUE);
        when(repository.getSkjermetPersonByPersonident(SkjermingTestUtils.PERSONIDENT)).thenReturn(Optional.of(SkjermingTestUtils.skjermetPerson()));
        when(repository.save(any(SkjermetPerson.class))).thenReturn(SkjermingTestUtils.skjermetPerson());

        mockMvc.perform(requestBuilder(HttpMethod.PUT)).andExpect(status().isOk());
    }

    private String getBearerToken(final String subject) throws Exception {
        final String token = mockMvc
                .perform(get(String.format("/local/jwt?subject=%s", SkjermingTestUtils.PERSONIDENT)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        return String.format("Bearer %s", token);
    }

    private MockHttpServletRequestBuilder requestBuilder(final HttpMethod method, final String ...requestPath) throws Exception {
        val path = UriComponentsBuilder.newInstance()
                .path(SkjermedePersonerController.BASE_URL)
                .pathSegment(requestPath)
                .build().toString();

        return (method == HttpMethod.GET ? get(path.toString()) : method == HttpMethod.POST ? post(path.toString()) : put(path.toString()))
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, getBearerToken(SkjermingTestUtils.PERSONIDENT));
    }
}
