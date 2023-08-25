package no.nav.nom.apps.nom.unit4.unit4;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thomaskasene.wiremock.junit.WireMockStubs;
import lombok.SneakyThrows;
import no.nav.nom.libs.test.RsaStub;
import no.nav.nom.apps.nom.unit4.unit4.dto.Unit4RessursDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("test")
@WireMockStubs(RsaStub.class)
@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = {"nom-ressurser"}, controlledShutdown = true)
@ExtendWith(OutputCaptureExtension.class)
class Unit4ClientTest {

    @Autowired
    Unit4Client unit4Client;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    @SneakyThrows
    void createRessurs_OK() {
        unit4Client.createRessurs(dummyRessurs("010199000001"));
    }

    @Test
    void createRessurs_CONFLICT() {
        assertThrows(Unit4ConflictException.class, () -> unit4Client.createRessurs(dummyRessurs("010199000002")));
    }

    @Test
    void createRessurs_422_BadRequest() {
        assertThrows(Unit4RequestException.class, () -> unit4Client.createRessurs(dummyRessurs("010199000003")));
    }

    @Test
    void createRessurs_422_BadRequest_Logging(CapturedOutput output) {
        try {
            unit4Client.createRessurs(dummyRessurs("010199000003"));
        } catch (Exception ignored) {}
        assertTrue(output.getOut().contains("Leverandør-ID og ressurs-ID må være identiske, i henhold til systemparameteren HS_EQUAL_ID."));
    }

    @Test
    void createRessurs_422_BadRequest2() {
        assertThrows(Unit4RequestException.class, () -> unit4Client.createRessurs(dummyRessurs("010199000004")));
    }

    @Test
    @SneakyThrows
    void createRessurs_422_BadRequest2_Logging(CapturedOutput output) {
        try  {
            unit4Client.createRessurs(dummyRessurs("010199000004"));
        } catch (Exception ignored) {}
        assertTrue(output.getOut().contains("Obligatorisk relasjon for begrepet FELLES."));
    }

    private Unit4RessursDto dummyRessurs(String personId) {
        return Unit4RessursDto.builder()
                .personId(personId)
                .build();
    }
}