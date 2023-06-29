package no.nav.nom.apps.api;

import lombok.val;
import no.nav.nom.apps.api.ressurs.eventsourcing.state.RessursStateListener;
import no.nav.nom.contract.nom.state.RessursState;
import org.apache.kafka.clients.admin.RecordsToDelete;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringSerializer;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class KafkaUtils {

    private final String ressursStateTopic;
    private final EmbeddedKafkaBroker embeddedKafkaBroker;
    @SpyBean
    private final RessursStateListener ressursStateListener;
    private final KafkaTemplate<String, RessursState> ressursStateKafkaTemplate;

    @Autowired
    public KafkaUtils(
            EmbeddedKafkaBroker embeddedKafkaBroker,
            RessursStateListener ressursStateListener,
            @Value("${app.kafka.ressurs.stateTopic}") String ressursStateTopic
    ) {
        this.embeddedKafkaBroker = embeddedKafkaBroker;
        this.ressursStateListener = ressursStateListener;
        this.ressursStateTopic = ressursStateTopic;
        ressursStateKafkaTemplate = ressursStateKafkaTemplate();
    }

    public synchronized void send(String key, RessursState ressursState) {
        CountDownLatch latch = new CountDownLatch(1);

        Mockito.doAnswer(invocation -> {
            invocation.callRealMethod();
            latch.countDown();
            return null;
        }).when(ressursStateListener).onMessage(Mockito.any(), Mockito.any());


        ressursStateKafkaTemplate.sendDefault(key, ressursState);
        try {
            if (!latch.await(900, TimeUnit.SECONDS)) {
                throw new RuntimeException("Invocation of kafka onMessage never occoured or never exited");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void clearKafka() {
        embeddedKafkaBroker.doWithAdmin(adminClient -> {
            try {
                Map<TopicPartition, RecordsToDelete> collect = adminClient.listPartitionReassignments()
                        .reassignments()
                        .get(10, TimeUnit.SECONDS)
                        .keySet().stream()
                        .collect(Collectors.toMap(Function.identity(), topicPartition -> RecordsToDelete.beforeOffset(Long.MAX_VALUE)));

                adminClient.deleteRecords(collect).all().get();
            } catch (Exception exception) {
                throw new RuntimeException("Failed to clear topic", exception);
            }
        });
    }

    private KafkaTemplate<String, RessursState> ressursStateKafkaTemplate() {
        Map<String, Object> producerProperties = KafkaTestUtils.producerProps(embeddedKafkaBroker);
        producerProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        ProducerFactory<String, RessursState> producerFactory = new DefaultKafkaProducerFactory<>(producerProperties);
        val kafkaTemplate = new KafkaTemplate<>(producerFactory);
        kafkaTemplate.setDefaultTopic(ressursStateTopic);
        return kafkaTemplate;
    }
}
