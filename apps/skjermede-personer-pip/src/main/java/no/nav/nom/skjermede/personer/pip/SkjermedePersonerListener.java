package no.nav.nom.skjermede.personer.pip;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.contract.skjermede.dto.SkjermetPersonDTO;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.errors.RetriableException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.PartitionOffset;
import org.springframework.kafka.annotation.TopicPartition;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@AllArgsConstructor
@Slf4j
public class SkjermedePersonerListener {

    private final Cache cache;
    private final MeterRegistry metric;

    @KafkaListener(groupId = "skjermede-personer-pip", topicPartitions = @TopicPartition(topic = "aapen-skjermede-personer-v1", partitionOffsets = @PartitionOffset(partition = "0", initialOffset = "0")))
    public void onMessage(ConsumerRecord<String, SkjermetPersonDTO> record) {
        try {
            cache.store(record.key(), new Skjermingsperiode(record.value().getSkjermetFra(), record.value().getSkjermetTil()));
            log.info("Received offset " + record.offset());
            metric.counter("events").increment();
        } catch(RetriableException e) {
            // no-op
        } catch(RuntimeException e) {
            log.error("Error fetching event with key: " + record.key() + ". SkjermetFra: " + record.value().getSkjermetFra() + ", skjermetTil: " + record
            .value().getSkjermetTil() + ". Exception: " + e);
        }
    }
}
