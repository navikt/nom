package no.nav.nom.apps.nom.unit4.kafka;

import no.nav.nom.apps.nom.unit4.TestBase;
import no.nav.nom.apps.nom.unit4.kafka.utils.KafkaUtils;
import no.nav.nom.apps.nom.unit4.unit4.Unit4Client;
import no.nav.nom.apps.nom.unit4.unit4.Unit4ConflictException;
import no.nav.nom.apps.nom.unit4.unit4.Unit4RequestException;
import no.nav.nom.apps.nom.unit4.unit4.Unit4ServerException;
import no.nav.nom.apps.nom.unit4.unit4.dto.Unit4RessursDto;
import no.nav.nom.contract.nom.AnsettelsesType;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.eventsource.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(OutputCaptureExtension.class)
class KafkaRessursListenerTest extends TestBase {

    @Autowired
    KafkaUtils kafkaUtils;

    @MockitoBean
    Unit4Client unit4Client;

    @MockitoBean
    KafkaRessursDltProducer kafkaRessursDltProducer;

    @Test
    public void basicMessageUnit4AllOk() throws Exception {
        String personident = "04129980932";

        kafkaUtils.send("key1", createOpprettRessursEvent(personident, "dev-gcp:org:nom-ui2", null));

        verify(unit4Client, times(1))
                .createRessurs(
                        argThat((Unit4RessursDto dto) -> dto.getPersonalInformation().getNationalIdentityNo().equals(personident)
                                && dto.getRelatedValues().stream().filter(r -> r.getRelationId().equals("C8")).allMatch(r -> r.getRelatedValue().equals("FLON"))

                        ));
    }

    @Test
    public void shouldSetMidlertidigAnsettelsesForm() throws Exception {
        String personident = "04129980932";

        kafkaUtils.send("key1", createOpprettRessursEvent(personident, "dev-gcp:org:nom-ui2", AnsettelsesType.MIDLERTIDIG));

        verify(unit4Client, times(1))
                .createRessurs(
                        argThat((Unit4RessursDto dto) -> dto.getRelatedValues()
                                .stream().filter(r -> r.getRelationId().equals("C8")).allMatch(r -> r.getRelatedValue().equals("MIDL"))

                        ));
    }

    @Test
    public void basicMessageUnit4AllOk_NOT_CALLED() {
        String personident = "04129980932";
        kafkaUtils.send("key1", createOpprettRessursEvent(personident, "NOT_OK", null));

        verifyNoInteractions(unit4Client);
    }

    @Test
    public void messageUnit4_CONFLICT(CapturedOutput output) throws Exception {
        String personident = "04129980932";

        doThrow(new Unit4ConflictException(""))
                .when(unit4Client)
                .createRessurs(any());

        kafkaUtils.send("key1", createOpprettRessursEvent(personident, "dev-gcp:org:nom-ui2", null));

        assertTrue(output.getOut().contains("Ressurs finnes i Unit4."));
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    public void messageUnit4_ServerException(CapturedOutput output) throws Exception {
        doThrow(new Unit4ServerException(""))
                .when(unit4Client)
                .createRessurs(any());

        assertThrows(RuntimeException.class, () -> kafkaUtils.send("key1", createOpprettRessursEvent("04129980932", "dev-gcp:org:nom-ui2", null)));
        assertTrue(output.getOut().contains("Caused by: no.nav.nom.apps.nom.unit4.kafka.KafkaRessursBlockingException"));
    }

    @Test
    public void messageUnit4_RequestException_sends_to_dlt() throws Exception {
        doThrow(new Unit4RequestException(""))
                .when(unit4Client)
                .createRessurs(any());

        var ressursEvent = createOpprettRessursEvent("04129980932", "dev-gcp:org:nom-ui2", null);
        kafkaUtils.send("key1", ressursEvent);

        ArgumentCaptor<RessursEvent> arg = ArgumentCaptor.forClass(RessursEvent.class);
        ArgumentCaptor<String> argUuid = ArgumentCaptor.forClass(String.class);
        verify(kafkaRessursDltProducer, times(1)).send(arg.capture(), argUuid.capture());

        assertEquals(ressursEvent.getData(), arg.getValue().getData());
        assertEquals("key1", argUuid.getValue());
    }

    private RessursEvent createOpprettRessursEvent(String personident, String initiatedFrom, AnsettelsesType ansettelsesType) {
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