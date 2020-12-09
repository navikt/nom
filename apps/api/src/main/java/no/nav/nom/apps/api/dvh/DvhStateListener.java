package no.nav.nom.apps.api.dvh;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.nom.apps.api.common.AppProperties;
import no.nav.nom.contract.dvh.DvhEvent;
import no.nav.nom.contract.dvh.Enhet;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.LivenessState;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.PartitionOffset;
import org.springframework.kafka.annotation.TopicPartition;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.StreamSupport;

import static no.nav.nom.libs.utils.common.kafka.KafkaUtils.findEndOffsetForPartition;
import static no.nav.nom.libs.utils.common.kafka.KafkaUtils.isAtOrAfterOffsetForPartition;
import static no.nav.nom.libs.utils.common.metrics.MetricUtils.KAFKA_CONSUMER_TIMED;
import static no.nav.nom.libs.utils.common.metrics.MetricUtils.KEY;

@Slf4j
@Component
@RequiredArgsConstructor
public class DvhStateListener {
    public static final String CACHE = "cache-source";
    private final AtomicBoolean initialLoadComplete = new AtomicBoolean(false);
    private final ApplicationEventPublisher appEventPublisher;
    private final AppProperties properties;
    private final DvhRepository repository;
    private final ObjectMapper objectMapper;

    @Timed(value = KAFKA_CONSUMER_TIMED, extraTags = {KEY, "dvhState"}, percentiles = {.99, .75, .50})
    @KafkaListener(groupId = "${app.kafka.group-id-prefix}-dvhState", topicPartitions = @TopicPartition(topic = "${app.dvh.topic}", partitionOffsets = @PartitionOffset(partition = "0", initialOffset = "0")))
    public void onMessage(ConsumerRecords<String, String> records, Consumer<String, String> consumer) {
        StreamSupport.stream(records.spliterator(), false)
                .map(this::convert)
                .filter(e -> {
                    if(e.getType().equals(Enhet.TYPE)) {
                        return e.getData().get("ORGNODE_TYPE").asText().equals("NAVENHET");
                    }
                    return true;
                })
                .forEach(e -> {
                    if(e.getType().equals(Enhet.TYPE)) {
                        repository.saveEnhet(new DvhOrganisasjonsenhet(e.getData()));
                        repository.saveOrganisering(new DvhOrganisering(e.getData()));
                        if(e.getData().hasNonNull("LEDER_RESOURCE_ID")) {
                            repository.saveOrgLeder(new DvhLeder(e.getData()));
                        }
                    } else{
                        repository.saveRessurs(new DvhRessurs(e.getData()));
                    }
                });

        verifyUpToDate(consumer);
    }

    @SneakyThrows
    private DvhEvent convert(ConsumerRecord<String, String> consumerRecord) {
        return objectMapper.readValue(consumerRecord.value(), DvhEvent.class);
    }

    private void verifyUpToDate(Consumer<String, String> consumer) {
        if (!initialLoadComplete.get()) {
            val endOffset = findEndOffsetForPartition(consumer, properties.getDvh().getTopic(), 0);
            val complete = isAtOrAfterOffsetForPartition(consumer, properties.getDvh().getTopic(), 0, endOffset);

            if (complete) {
                initialLoadComplete.set(true);
                AvailabilityChangeEvent.publish(appEventPublisher, CACHE, LivenessState.CORRECT);
                log.info("State restored from Kafka, changing to LivenessState.CORRECT. Total records persisted: {}", repository.enhetCount());
            }
        }
    }
}
