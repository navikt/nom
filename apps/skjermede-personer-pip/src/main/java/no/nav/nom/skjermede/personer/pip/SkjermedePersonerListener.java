package no.nav.nom.skjermede.personer.pip;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.contract.skjermede.dto.SkjermetPersonDTO;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.errors.RetriableException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.PartitionOffset;
import org.springframework.kafka.annotation.TopicPartition;
import org.springframework.kafka.listener.ConsumerSeekAware;
import org.springframework.kafka.listener.ConsumerSeekAware.ConsumerSeekCallback;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@AllArgsConstructor
@Slf4j
public class SkjermedePersonerListener implements ConsumerSeekAware {

    private final Cache cache;

    @KafkaListener(groupId = "skjermede-personer-pip", topicPartitions = @TopicPartition(topic = "aapen-skjermede-personer-v1"))
    public void onMessage(ConsumerRecord<String, SkjermetPersonDTO> record) {
        try {
            cache.store(record.key(), new Skjermingsperiode(record.value().getSkjermetFra(), record.value().getSkjermetTil()));
            log.info("Received offset " + record.offset());
        } catch (RetriableException e) {
            // no-op
        } catch (RuntimeException e) {
            log.error("Error fetching event with key: " + record.key() + ". SkjermetFra: " + record.value().getSkjermetFra() + ", skjermetTil: " + record
                    .value().getSkjermetTil() + ". Exception: " + e);
        }
    }

    @Override
    public void onPartitionsAssigned(Map<org.apache.kafka.common.TopicPartition, Long> assignments, ConsumerSeekCallback callback) {
        assignments.keySet().forEach(p -> callback.seekToBeginning(p.topic(), p.partition()));
    }
}
