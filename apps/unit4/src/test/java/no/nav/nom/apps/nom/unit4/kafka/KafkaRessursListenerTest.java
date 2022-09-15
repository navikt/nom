package no.nav.nom.apps.nom.unit4.kafka;

import no.nav.nom.apps.nom.unit4.kafka.utils.KafkaUtils;
import no.nav.nom.apps.nom.unit4.unit4.Unit4Client;
import no.nav.nom.apps.nom.unit4.unit4.Unit4ConflictException;
import no.nav.nom.apps.nom.unit4.unit4.Unit4RequestException;
import no.nav.nom.apps.nom.unit4.unit4.dto.Unit4RessursDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ActiveProfiles({"test", "log-console"})
@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = {"nom-ressurser"}, controlledShutdown = true)
@ExtendWith(OutputCaptureExtension.class)
class KafkaRessursListenerTest {

    @Autowired
    KafkaUtils kafkaUtils;

    @MockBean
    Unit4Client unit4Client;

    @Test
    public void basicMessageUnit4AllOk() throws Exception{
        KafkaRessursDto ressurs = dummyRessurs();
        kafkaUtils.send("key1", ressurs);

        verify(unit4Client, times(1))
                .createRessursByProxy(
                    argThat((Unit4RessursDto dto) -> dto.getPersonalInformation().getNationalIdentityNo().equals(ressurs.getPersonident()))
                );
    }

    @Test
    public void messageUnit4_CONFLICT(CapturedOutput output) throws Exception{
        KafkaRessursDto ressurs = dummyRessurs();

        doThrow(new Unit4ConflictException(""))
                .when(unit4Client)
                .createRessursByProxy(any());

        kafkaUtils.send("key1", ressurs);

        assertTrue(output.getOut().contains("Ressurs finnes i Unit4."));
    }

    @Test
    public void messageUnit4_RequestException(CapturedOutput output) throws Exception{
        KafkaRessursDto ressurs = dummyRessurs();

        doThrow(new Unit4RequestException(""))
                .when(unit4Client)
                .createRessursByProxy(any());

        Exception exception = assertThrows(RuntimeException.class, () -> {
            kafkaUtils.send("key1", ressurs);
        });
        assertTrue(output.getOut().contains("Bailer ut"));
    }

    private KafkaRessursDto dummyRessurs() {
        KafkaRessursDto ressurs = KafkaRessursDto.builder()
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