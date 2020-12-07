package no.nav.nom.apps.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.val;
import no.nav.nom.apps.api.dvh.DvhStateListener;
import no.nav.nom.contract.dvh.DvhEvent;
import no.nav.nom.contract.dvh.Enhet;
import no.nav.nom.contract.dvh.Ressurs;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@Component
public class KafkaUtils {
    @Autowired
    private DvhStateListener dvhStateListener;

    @Autowired
    private ObjectMapper objectMapper;

    public void sendFromKafkaMock(Object... objects){

        val consumer = mock(Consumer.class);
        doReturn(Map.of(new TopicPartition("", 0), 0L)).when(consumer).endOffsets(any());
        doReturn(0L).when(consumer).position(any());

        val recordsAsJson = createConsumerRecords(List.of(objects).stream()
                .map(e -> {
                    return DvhEvent.builder()
                            .type(e instanceof Enhet ? Enhet.TYPE : Ressurs.TYPE)
                            .data(toJsonNode(e))
                            .build();
                })
                .map(this::toJson)
                .map(e -> createConsumerRecord("", e))
                .collect(Collectors.toUnmodifiableList()));

        dvhStateListener.onMessage(recordsAsJson, consumer);
    }

    private  <K, V> ConsumerRecord<K, V> createConsumerRecord(K key, V value) {
        return new ConsumerRecord<>("", 0, 0, key, value);
    }


    private  <K, V> ConsumerRecords<K, V> createConsumerRecords(List<ConsumerRecord<K, V>> records) {
        return new ConsumerRecords<>(Map.of(new TopicPartition("test", 123), records));
    }

    private JsonNode toJsonNode(Object object){
        return objectMapper.convertValue(object, JsonNode.class);
    }

    @SneakyThrows
    private String toJson(Object object){
        return objectMapper.writeValueAsString(object);
    }
}
