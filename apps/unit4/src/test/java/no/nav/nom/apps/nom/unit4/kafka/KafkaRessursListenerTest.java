package no.nav.nom.apps.nom.unit4.kafka;

import no.nav.nom.apps.nom.unit4.SecurityConfig;
import no.nav.nom.apps.nom.unit4.kafka.utils.KafkaUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles({"test", "log-console"})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@EnableAutoConfiguration(exclude = {SecurityAutoConfiguration.class})
@EnableKafka
@EmbeddedKafka(partitions = 1, topics = {"nom-ressurser"}, controlledShutdown = true)
class KafkaRessursListenerTest {

    @Autowired
    KafkaRessursListener kafkaRessursListener;

    @Autowired
    KafkaUtils kafkaUtils;

    @Test
    public void ReadMessage() {
        kafkaUtils.send("key1", dummyRessurs());
    }

    private NomRessursDto dummyRessurs() {
        NomRessursDto ressurs = NomRessursDto.builder()
                .personident("01019912345")
                .navident("A000001")
                .visningsfornavn("Tom")
                .visningsetternavn("Bombadil")
                .kjoenn("M")
                .sektor("STATLIG")
                .build();
        return ressurs;
    }

}