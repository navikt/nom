package no.nav.nom.apps.nom.unit4.unit4;

import com.thomaskasene.wiremock.junit.WireMockStubs;
import lombok.SneakyThrows;
import no.nav.nom.apps.nom.unit4.unit4.dto.Unit4RessursDto;
import no.nav.nom.libs.test.RsaStub;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ActiveProfiles({"test", "log-console"})
@WireMockStubs(RsaStub.class)
@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = {"nom-ressurser"}, controlledShutdown = true)
class Unit4ClientTest {


    @Autowired
    Unit4Client unit4Client;

    @Test
    @SneakyThrows
    void createRessursByProxy_OK() {
        unit4Client.createRessursByProxy(dummyRessurs("010199000001"));
    }

    @Test
    void createRessursByProxy_CONFLICT() {
        assertThrows(Unit4ConflictException.class, () -> unit4Client.createRessursByProxy(dummyRessurs("010199000002")));
    }

    private Unit4RessursDto dummyRessurs(String personId) {
        return Unit4RessursDto.builder()
                .personId(personId)
                .build();
    }
}