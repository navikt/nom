package no.nav.nom.apps.nom.unit4.kafka;

import no.nav.nom.apps.nom.unit4.kafka.utils.KafkaUtils;
import no.nav.nom.apps.nom.unit4.unit4.Unit4Client;
import no.nav.nom.apps.nom.unit4.unit4.Unit4ConflictException;
import no.nav.nom.apps.nom.unit4.unit4.Unit4RequestException;
import no.nav.nom.apps.nom.unit4.unit4.dto.Unit4RessursDto;
import no.nav.nom.contract.nom.eventsource.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
        String personident = "04129980932";

        kafkaUtils.send("key1", createOpprettRessursEvent(personident, "dev-gcp:org:nom-ui2"));

        verify(unit4Client, times(1))
                .createRessursByProxy(
                    argThat((Unit4RessursDto dto) -> dto.getPersonalInformation().getNationalIdentityNo().equals(personident))
                );
    }

    @Test
    public void basicMessageUnit4AllOk_NOT_CALLED() {
        String personident = "04129980932";
        kafkaUtils.send("key1", createOpprettRessursEvent(personident, "NOT_OK"));

        verifyNoInteractions(unit4Client);
    }

    @Test
    public void messageUnit4_CONFLICT(CapturedOutput output) throws Exception{
        String personident = "04129980932";

        doThrow(new Unit4ConflictException(""))
                .when(unit4Client)
                .createRessursByProxy(any());

        kafkaUtils.send("key1", createOpprettRessursEvent(personident, "dev-gcp:org:nom-ui2"));

        assertTrue(output.getOut().contains("Ressurs finnes i Unit4."));
    }

    @Test
    public void messageUnit4_RequestException(CapturedOutput output) throws Exception{
        doThrow(new Unit4RequestException(""))
                .when(unit4Client)
                .createRessursByProxy(any());

        Exception exception = assertThrows(RuntimeException.class, () -> {
            kafkaUtils.send("key1", createOpprettRessursEvent("04129980932", "dev-gcp:org:nom-ui2"));
        });
        assertTrue(output.getOut().contains("Bailer ut"));
    }

    private RessursEvent createOpprettRessursEvent(String personident, String initiatedFrom){
        return new RessursOpprettetEventV1(
                "fid",
                new RessursOpprettetEventV1.RessursOpprettetEventData(
                        personident,
                        Sektor.NAV_STATLIG,
                        new Visningsnavn(
                                "VisningsFornavn",
                                Optional.empty(),
                                "VisningsEtternavn"
                        )
                ),
                new Event.Metadata(
                        new InputMetadata("Test", "test", null,
                                new Origination("Test", initiatedFrom)),
                        LocalDateTime.now()
                ),
                new RessursOpprettetEventV1.RessursOpprettetAdditionalData(
                        Optional.of("A123456"),
                        Optional.of("MANN"),
                        Optional.of(new RessursOpprettetEventV1.RessursOpprettetAdditionalData.FolkeregistrertNavn(
                                "Fornavn",
                                null,
                                "Etternavn"
                        ))
                )
        );
    }
}