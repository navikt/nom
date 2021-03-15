package no.nav.nom.skjermede.personer.pip;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = ApplicationStarter.class,
        properties = { "spring.kafka.consumer.client-id=ITPipControllerTest" })
@ActiveProfiles("local")
@AutoConfigureMockMvc
public class ITPipControllerTest {
    @Autowired
    private Cache cache;

    @Autowired
    private MockMvc mockMvc;

    /*@Test
    void get_skjermet_status_for_ikke_skjermet_personident() throws Exception {
        mockMvc.perform(
                get("/skjermet")
                .param("personident", "01019012345")
        )
        .andExpect(status().isOk())
        .andExpect(content().string(Boolean.FALSE.toString()))
        .andDo(print());
    }

    @Test
    void get_skjermet_status_for_skjermet_personident() throws Exception {
        cache.store("02019012345", new Skjermingsperiode(LocalDateTime.now().minusYears(1)));

        mockMvc.perform(
                get("/skjermet")
                        .param("personident", "02019012345")
        )
                .andExpect(status().isOk())
                .andExpect(content().string(Boolean.TRUE.toString()))
                .andDo(print());
    }*/
}
