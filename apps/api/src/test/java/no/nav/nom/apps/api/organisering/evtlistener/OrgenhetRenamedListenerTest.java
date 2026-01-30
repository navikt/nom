package no.nav.nom.apps.api.organisering.evtlistener;

import freemarker.template.Configuration;
import no.nav.nom.apps.api.clients.msgraph.MicrosoftGraphClient;
import no.nav.nom.apps.api.common.AppProperties;
import no.nav.nom.apps.api.dvh.DvhStartupLoadMonitor;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.remedy.RemedyEnhetMapperService;
import no.nav.nom.contract.nom.eventsource.Event;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.OrganiseringAvsluttetEventV1;
import no.nav.nom.contract.nom.eventsource.OrganiseringEvent;
import no.nav.nom.contract.nom.eventsource.Origination;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class OrgenhetRenamedListenerTest {

    @InjectMocks
    OrgenhetRenamedListener orgenhetRenamedListener;

    @Mock
    DvhStartupLoadMonitor dvhStartupLoadMonitor;
    @Mock
    OrgenhetService orgenhetService;
    @Mock
    RemedyEnhetMapperService remedyEnhetMapperService;
    @Mock
    Configuration freemarkerConfig;
    @Mock
    MicrosoftGraphClient microsoftGraphClient;
    @Mock
    AppProperties appProperties;
    @Mock
    JsonMapper jsonMapper;

    @Test
    public void testDelayedHandleAvslutningCall() throws InterruptedException {
        orgenhetRenamedListener.setDelay(TimeUnit.MILLISECONDS, 3);
        var AvslutningEvent = new OrganiseringAvsluttetEventV1(
                "FID_1",
                new OrganiseringAvsluttetEventV1.OrganiseringAvsluttetEventData(LocalDate.now()),
                new Event.Metadata(
                    new InputMetadata(
                        "TEST",
                        "TEST",
                        Map.of("id", "1", "navn", "Test Navn", "idOver", "0", "navnOver", "Overordnet Navn", "gyldigFom", "2023-01-01"),
                        new Origination("nom-api", "nom-api"))
                    , LocalDateTime.now()
            )
        );
        ConsumerRecord<String, OrganiseringEvent> eventRecord = new ConsumerRecord<>(
            "topic",
            0,
            0L,
            "key",
            AvslutningEvent
        );
        TopicPartition topicPartition = new TopicPartition("topic", 0);
        Map<TopicPartition, List<ConsumerRecord<String, OrganiseringEvent>>> records = Map.of(
            topicPartition, List.of(eventRecord)
        );

        orgenhetRenamedListener.onMessage(new ConsumerRecords<>(records));
        verify(jsonMapper, never()).valueToTree(anyMap());

        Thread.sleep(10);
        Mockito.reset(jsonMapper);

        orgenhetRenamedListener.onMessage(new ConsumerRecords<>(records));
        verify(jsonMapper, timeout(50)).valueToTree(anyMap());
    }
}