package no.nav.nom.apps.navident.job;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.getunleash.Unleash;
import no.nav.nom.apps.navident.SpringBootTestBase;
import no.nav.nom.apps.navident.domain.Navident;
import no.nav.nom.apps.navident.eventsource.EventPayload;
import no.nav.nom.apps.navident.eventsource.EventSourceService;
import no.nav.nom.apps.navident.eventsource.SourceEvent;
import no.nav.nom.apps.navident.eventsource.SourceEventRepository;
import no.nav.nom.contract.nom.IdentType;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@EmbeddedKafka(partitions = 1, topics = {"embedded-test-topic"}, controlledShutdown = true)
public class ImportNavIdenterKafkaTest extends SpringBootTestBase {

    private static final String TOPIC = "embedded-test-topic";


    @MockBean
    protected Unleash unleash;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    ImportNavIdenter importNavIdenter;

    @MockBean
    EventSourceService eventSourceService;

    @MockBean
    SourceEventRepository sourceEventRepository;

    BlockingQueue<ConsumerRecord<String, SourceEvent>> recordsQueue;

    KafkaMessageListenerContainer<String, SourceEvent> container;

    @BeforeEach
    public void setUp() {
        // enable features for tests
        when(unleash.isEnabled(any())).thenReturn(true);

        Map<String, Object> consumerProps = new HashMap<>(KafkaTestUtils
            .consumerProps("consumerGroup", "false", embeddedKafkaBroker));

        DefaultKafkaConsumerFactory<String, SourceEvent> consumerFactory = new DefaultKafkaConsumerFactory<>(
            consumerProps,
            new StringDeserializer(),
            new JsonDeserializer<>(SourceEvent.class, objectMapper)
        );

        recordsQueue = new LinkedBlockingQueue<>();

        ContainerProperties containerProperties = new ContainerProperties(TOPIC);
        container = new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);
        container.setupMessageListener((MessageListener<String, SourceEvent>) recordsQueue::add);
        container.start();

        ContainerTestUtils.waitForAssignment(container, embeddedKafkaBroker.getPartitionsPerTopic());
    }

    @AfterEach
    public void tearDown() {
        container.stop();
    }

    @Test
    @DisplayName("Produserer endre en navident")
    void endre_en_navident() {
        when(eventSourceService.finnesNavIdent(any())).thenReturn(true);
        when(eventSourceService.getsNavIdent(anyString())).thenAnswer((Answer<Navident>) invocationOnMock -> {
            Object[] args = invocationOnMock.getArguments();
            return Navident.builder().navident((String) args[0]).identType(IdentType.STANDARD).build();
        });

        Resource resource = new ClassPathResource("NIK_identType-test.txt");
        importNavIdenter.importIdentTypeFromResourceFile(resource);
        Map<String, String> fileContents = importNavIdenter.readIdentTypeFromResourceFile(resource);
        await()
            .atMost(1, SECONDS)
            .pollInterval(10, MILLISECONDS)
            .until(() -> recordsQueue.size() == 20);

        // assert
        var endreNavIdentRecord = recordsQueue.remove();
        assertThat(endreNavIdentRecord.value()).isExactlyInstanceOf(SourceEvent.class);
        var endreMessage = (EventPayload) endreNavIdentRecord.value().getPayload();
        assertThat(endreMessage.getIdentType().toString()).isEqualTo(fileContents.get(endreMessage.getNavident()));
    }
}
