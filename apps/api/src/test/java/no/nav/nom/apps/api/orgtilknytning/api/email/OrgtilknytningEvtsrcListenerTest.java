package no.nav.nom.apps.api.orgtilknytning.api.email;

import io.getunleash.Unleash;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningEvent;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningFjernetEventV1;
import no.nav.nom.contract.nom.eventsource.Origination;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrgtilknytningEvtsrcListenerTest {

    @Mock
    private OrgtilknytningEndretEmailService orgtilknytningEndretEmailService;

    @Mock
    private Environment env;

    @Mock
    private Unleash unleash;

    @InjectMocks
    private OrgtilknytningEvtsrcListener listener;

    @Test
    void slettingLeggesIkkeIKoenNaarToggleErAv() {
        listener.onMessage(records(slettetEvent("dev-gcp:org:nom-ui2")));

        verify(orgtilknytningEndretEmailService, never()).addToOrgtilknytningEndringerQueue(any(), any());
    }

    @Test
    void slettingFraAdminLeggesIKoenMedNavidentOgEnhetNaarToggleErPaa() {
        when(unleash.isEnabled(OrgtilknytningEndretEmailService.UNLEASH_DETALJERT_OT_EPOST)).thenReturn(true);

        listener.onMessage(records(slettetEvent("Admin swagger/rest")));

        verify(orgtilknytningEndretEmailService).addToOrgtilknytningEndringerQueue(eq("fid1"), argThat(data ->
            data.eventType() == OTEndretData.OTEndretDataType.OT_FJERNET
                && "A123456".equals(data.navident())
                && "enhet1".equals(data.orgEnhetId())
        ));
    }

    private static OrgtilknytningFjernetEventV1 slettetEvent(String initiatedFrom) {
        return new OrgtilknytningFjernetEventV1(
            "fid1",
            new OrgtilknytningFjernetEventV1.OrgtilknytningFjernetEventData("A123456", "enhet1", LocalDate.of(2026, 1, 1), null),
            new InputMetadata("test", "fjernOrgtilknytning", "fid1", new Origination("C123456", initiatedFrom)),
            null
        );
    }

    private static ConsumerRecords<String, String> records(OrgtilknytningEvent event) {
        var json = JsonMapper.builder().build().writerFor(OrgtilknytningEvent.class).writeValueAsString(event);
        var partition = new TopicPartition("org.nom.orgtilknytning-eventsource", 0);
        return new ConsumerRecords<>(Map.of(partition, List.of(new ConsumerRecord<>(partition.topic(), 0, 0L, "fid1", json))), Map.of());
    }
}
