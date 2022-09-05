package no.nav.nom.apps.nom.unit4.kafka;

import no.nav.nom.apps.nom.unit4.kafka.utils.KafkaUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles({"test", "log-console"})
@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = {"nom-ressurser"}, controlledShutdown = true)
class KafkaRessursListenerTest {

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