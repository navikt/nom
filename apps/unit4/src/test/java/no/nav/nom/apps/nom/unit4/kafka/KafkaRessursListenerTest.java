package no.nav.nom.apps.nom.unit4.kafka;

import com.github.tomakehurst.wiremock.WireMockServer;
import no.nav.nom.apps.nom.unit4.kafka.utils.KafkaUtils;
import no.nav.nom.apps.nom.unit4.unit4.Unit4Client;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ActiveProfiles({"test", "log-console"})
@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = {"nom-ressurser"}, controlledShutdown = true)
class KafkaRessursListenerTest {

    @Autowired
    KafkaUtils kafkaUtils;
    WireMockServer wireMockServer = new WireMockServer();

    @MockBean
    Unit4Client unit4Client;

    @Test
    public void readMessage() throws Exception{
        kafkaUtils.send("key1", dummyRessurs());

//        when(unit4Client.createRessursByProxy(any())).thenReturn()
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