package no.nav.nom.skjermede.personer.skjerming.api.v1;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import no.nav.nom.skjermede.personer.common.TokenUtil;
import no.nav.nom.skjermede.personer.skjerming.SkjermetPerson;
import no.nav.nom.skjermede.personer.skjerming.SkjermetPersonRepository;
import no.nav.nom.skjermede.personer.skjerming.api.v1.dto.SkjermetPersonResponseDTO;
import no.nav.security.token.support.test.spring.TokenGeneratorController;
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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@RequiredArgsConstructor
@WebMvcTest(controllers = {SkjermedePersonerController.class, TokenGeneratorController.class})
public class SkjermedePersonerControllerTest {

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SkjermetPersonRepository repository;

    @MockBean
    private TokenUtil tokenUtil;

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testHentSkjermingtUnauthorized() throws Exception {
        when(repository.findDistinctByNavidentAndSkjermetTomNull(TestUtils.NAVIDENT)).thenReturn(Optional.of(TestUtils.skjermetPerson()));
        when(tokenUtil.isAuthorized(anyString())).thenReturn(Boolean.FALSE);

        mockMvc.perform(requestBuilder(HttpMethod.GET, TestUtils.NAVIDENT)).andExpect(status().isUnauthorized());
    }

    @Test
    public void testHentSkjermingByNavident() throws Exception {
        when(tokenUtil.getSubject()).thenReturn(TestUtils.PERSONIDENT);
        when(tokenUtil.isAuthorized(TestUtils.PERSONIDENT)).thenReturn(Boolean.TRUE);
        when(repository.findDistinctByNavidentAndSkjermetTomNull(TestUtils.NAVIDENT)).thenReturn(Optional.of(TestUtils.skjermetPerson()));

        mockMvc.perform(requestBuilder(HttpMethod.GET, TestUtils.NAVIDENT)).andExpect(status().isOk());
    }

    @Test
    public void testHentSkjermingByNavidentNotFound() throws Exception {
        when(repository.findDistinctByNavidentAndSkjermetTomNull(TestUtils.NAVIDENT)).thenReturn(Optional.ofNullable(null));

        mockMvc.perform(requestBuilder(HttpMethod.GET, TestUtils.NAVIDENT)).andExpect(status().isNotFound());
    }

    @Test
    public void testHentSkjermedeFamiliemedlemerUnauthorized() throws Exception {
        when(tokenUtil.isAuthorized(TestUtils.PERSONIDENT)).thenReturn(Boolean.TRUE);

        mockMvc.perform(requestBuilder(HttpMethod.GET, SkjermingConstants.FAMILIEMEDLEMER)).andExpect(status().isUnauthorized());
    }

    @Test
    public void testHentSkjermedeFamiliemedlemer() throws Exception {
        when(repository.findAllByTilknyttetRessurs(TestUtils.PERSONIDENT)).thenReturn(Arrays.asList(TestUtils.skjermetPerson()));
        when(tokenUtil.isAuthorized(TestUtils.PERSONIDENT)).thenReturn(Boolean.TRUE);
        when(tokenUtil.getSubject()).thenReturn(TestUtils.PERSONIDENT);

        final String response = mockMvc.perform(requestBuilder(HttpMethod.GET, SkjermingConstants.FAMILIEMEDLEMER))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertEquals(objectMapper.readValue(response, new TypeReference<List<SkjermetPersonResponseDTO>>() {}).size(), 1);
    }

    @Test
    public void testHentSkjermingerForAnsattUnauthorized() throws Exception {
        when(tokenUtil.getSubject()).thenReturn(Strings.EMPTY);
        when(tokenUtil.isAuthorized(TestUtils.PERSONIDENT)).thenReturn(Boolean.FALSE);

        mockMvc.perform(requestBuilder(HttpMethod.GET, SkjermingConstants.BY_ANSATT)).andExpect(status().isUnauthorized());
    }

    @Test
    public void testHentSkjermingerForAnsatt() throws Exception {
        when(tokenUtil.getSubject()).thenReturn(TestUtils.PERSONIDENT);
        when(tokenUtil.isAuthorized(TestUtils.PERSONIDENT)).thenReturn(Boolean.TRUE);

        mockMvc.perform(requestBuilder(HttpMethod.GET, SkjermingConstants.BY_ANSATT)).andExpect(status().isOk());
    }

    @Test
    public void testOpprettSkjermingDuplicate() throws Exception {
        when(tokenUtil.getSubject()).thenReturn(TestUtils.PERSONIDENT);
        when(tokenUtil.isAuthorized(TestUtils.PERSONIDENT)).thenReturn(Boolean.TRUE);
        when(repository.existsByPersonidentAndSkjermetTomNull(TestUtils.PERSONIDENT)).thenReturn(Boolean.TRUE);

        mockMvc.perform(requestBuilder(HttpMethod.POST)
                .content(objectMapper.writeValueAsString(TestUtils.skjermetPersonRequest())))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testOpprettSkjermingUnauthorized() throws Exception {
        when(tokenUtil.getSubject()).thenReturn(Strings.EMPTY);
        when(tokenUtil.isAuthorized(TestUtils.PERSONIDENT)).thenReturn(Boolean.FALSE);

        mockMvc.perform(requestBuilder(HttpMethod.POST)
                .content(objectMapper.writeValueAsString(TestUtils.skjermetPersonRequest())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testOpprettSkjerming() throws Exception {
        when(tokenUtil.getSubject()).thenReturn(TestUtils.PERSONIDENT);
        when(tokenUtil.isAuthorized(TestUtils.PERSONIDENT)).thenReturn(Boolean.TRUE);
        when(repository.existsByPersonidentAndSkjermetTomNull(anyString())).thenReturn(Boolean.FALSE);
        when(repository.save(any(SkjermetPerson.class))).thenReturn(TestUtils.skjermetPerson());

        mockMvc.perform(requestBuilder(HttpMethod.POST)
                .content(objectMapper.writeValueAsString(TestUtils.skjermetPersonRequest())))
                .andExpect(status().isCreated());
    }

    private String getBearerToken(final String subject) throws Exception {
        final String token = mockMvc
                .perform(get(String.format("/local/jwt?subject=%s", TestUtils.PERSONIDENT)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        return String.format("Bearer %s", token);
    }

    private MockHttpServletRequestBuilder requestBuilder(final HttpMethod method, final String ...requestPath) throws Exception {
        final Path path = requestPath.length == 0
                ? Paths.get(SkjermingConstants.BASE_URL)
                : Paths.get(SkjermingConstants.BASE_URL, requestPath);

        return (method == HttpMethod.GET ? get(path.toString()) : post(path.toString()))
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, getBearerToken(TestUtils.PERSONIDENT));
    }
}
