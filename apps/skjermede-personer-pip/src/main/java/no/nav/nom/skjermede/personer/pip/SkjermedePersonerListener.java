package no.nav.nom.skjermede.personer.pip;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.contract.skjermede.dto.SkjermetPersonDTO;
import no.nav.nom.skjermede.personer.pip.config.AppProperties;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.errors.RetriableException;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.TopicPartition;
import org.springframework.kafka.listener.ConsumerSeekAware;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static no.nav.nom.libs.utils.common.kafka.KafkaUtils.verifyUpToDate;

@Component
@AllArgsConstructor
@Slf4j
public class SkjermedePersonerListener implements ConsumerSeekAware {

    public static final String CACHE = "cache-source";

    private final AppProperties appProperties;
    private final AtomicBoolean initialLoadComplete = new AtomicBoolean(false);
    private final ApplicationEventPublisher appEventPublisher;

    private final Cache cache;

    @KafkaListener(groupId = "skjermede-personer-pip", topicPartitions = @TopicPartition(topic = "${app.skjermedePersoner.topicName}", partitions = "0"))
    public void onMessage(ConsumerRecord<String, SkjermetPersonDTO> record, Consumer<String, String> consumer) {
        try {
            cache.store(record.key(), new Skjermingsperiode(record.value().getSkjermetFra(), record.value().getSkjermetTil()));
            log.info("Received offset " + record.offset());
            verifyUpToDate(consumer, appProperties.getSkjermedePersoner().getTopicName(), initialLoadComplete, () -> {
                AvailabilityChangeEvent.publish(appEventPublisher, CACHE, ReadinessState.ACCEPTING_TRAFFIC);
                log.info("Changing to ReadinessState.ACCEPTING_TRAFFIC");
            });
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
