package no.nav.nom.skjermede.personer.pip;

import lombok.RequiredArgsConstructor;
import lombok.val;
import no.nav.nom.skjermede.personer.pip.api.PipController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.util.UriComponentsBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@RequiredArgsConstructor
@WebMvcTest(controllers = PipController.class)
class PipControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private Cache cache;

    /*@DisplayName("Tester gyldig scenario")
    @Test
    void scenario11() throws Exception{
        val path = UriComponentsBuilder.newInstance()
                .path("/skjermet")
                .build().toString();

        mockMvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content("{\"personident\": \"test\"}")).andExpect(status().isOk());
    }

    @DisplayName("Returnerer 401 når body er tom")
    @Test
    void scenario12() throws Exception{
        val path = UriComponentsBuilder.newInstance()
                .path("/skjermet")
                .build().toString();

        mockMvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isBadRequest());
    }

    @DisplayName("Returnerer 401 når body mangler")
    @Test
    void scenario13() throws Exception{
        val path = UriComponentsBuilder.newInstance()
                .path("/skjermet")
                .build().toString();

        mockMvc.perform(post(path)).andExpect(status().isBadRequest());
    }*/

}
