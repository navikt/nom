package no.nav.nom.apps.nom.unit4.kafka;

import no.nav.nom.apps.nom.unit4.TestBase;
import no.nav.nom.apps.nom.unit4.kafka.utils.KafkaUtils;
import no.nav.nom.apps.nom.unit4.unit4.Unit4Client;
import no.nav.nom.apps.nom.unit4.unit4.Unit4RequestException;
import no.nav.nom.contract.nom.AnsettelsesType;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.eventsource.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@ExtendWith(OutputCaptureExtension.class)
class KafkaRessursDltListenerTest extends TestBase {

    @Autowired
    KafkaUtils kafkaUtils;

    @MockitoBean
    Unit4Client unit4Client;

    @MockitoSpyBean
    private KafkaRessursDltListener kafkaRessursDltListener;


    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    public void messageUnit4_RequestException(CapturedOutput output) throws Exception{
        CountDownLatch latch = new CountDownLatch(1);

        Mockito.doAnswer(invocation -> {
            invocation.callRealMethod();
            latch.countDown();
            return null;
        }).when(kafkaRessursDltListener).listener(Mockito.any(), Mockito.any());

        doThrow(new Unit4RequestException("Test Failed due to Request Error"))
            .when(unit4Client)
            .createRessurs(any());

        var ressursEvent = createOpprettRessursEvent("04129980932", "dev-gcp:org:nom-ui2", null);
        kafkaUtils.send("key1", ressursEvent);

        latch.await(4, TimeUnit.SECONDS);

        assertTrue(output.getOut().contains("KafkaRessursDltListener -- Ubehandlet Unit4 Exception! På meldings-key:"));
    }

    private RessursEvent createOpprettRessursEvent(String personident, String initiatedFrom, AnsettelsesType ansettelsesType){
        return new RessursOpprettetEventV1(
                "fid",
                new RessursOpprettetEventV1.RessursOpprettetEventData(
                        personident,
                        Sektor.NAV_STATLIG,
                        new Visningsnavn(
                                "VisningsFornavn",
                                Optional.empty(),
                                "VisningsEtternavn"
                        ),
                    Optional.empty(),
                    List.of("2021"),
                    "2021"
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
                        )),
                        Optional.of(LocalDate.now()),
                        Optional.of(LocalDate.now()),
                        Optional.of("0101001"),
                        Optional.ofNullable(ansettelsesType)
                )
        );
    }
}