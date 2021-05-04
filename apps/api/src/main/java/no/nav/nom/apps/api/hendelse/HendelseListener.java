package no.nav.nom.apps.api.hendelse;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.common.AppProperties;
import no.nav.nom.apps.api.common.KafkaOffsetPostProcessor;
import no.nav.nom.apps.api.domain.OrgenhetRepository;
import no.nav.nom.apps.api.domain.RessursRepository;
import no.nav.nom.contract.nom.hendelse.HendelseTo;
import no.nav.nom.contract.nom.hendelse.Hendelsestyper;
import no.nav.nom.libs.utils.common.kafka.KafkaUtils;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.PartitionOffset;
import org.springframework.kafka.annotation.TopicPartition;
import org.springframework.kafka.listener.ConsumerSeekAware;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static no.nav.nom.libs.utils.common.metrics.MetricUtils.KAFKA_CONSUMER_TIMED;
import static no.nav.nom.libs.utils.common.metrics.MetricUtils.KEY;

@Slf4j
@Component
@RequiredArgsConstructor
public class HendelseListener implements ConsumerSeekAware {

    private final AtomicBoolean initialLoadComplete = new AtomicBoolean(false);
    private final AppProperties properties;
    private final ObjectMapper objectMapper;
    private final KafkaOffsetPostProcessor kafkaOffsetPostProcessor;
    private final Counter kafkaHendelseReadCounter;

    private final OrgenhetRepository orgenhetRepository;
    private final RessursRepository ressursRepository;

    @Timed(value = KAFKA_CONSUMER_TIMED, extraTags = {KEY, "hendelse"}, percentiles = {.99, .75, .50})
    @KafkaListener(groupId = "${app.kafka.group-id-prefix}-hendelse", topicPartitions = @TopicPartition(topic = "${app.hendelse.topic}", partitionOffsets = @PartitionOffset(partition = "0", initialOffset = "0")))
    public void onMessage(ConsumerRecords<String, String> records, Consumer<String, String> consumer) {
        var hendelser = StreamSupport.stream(records.spliterator(), false)
                .map(this::convert)
                .collect(Collectors.groupingBy(HendelseTo::getType));

        ressursRepository.save(hendelser.getOrDefault(Hendelsestyper.RESSURS_V1.getKey(), List.of()));
        orgenhetRepository.save(hendelser.getOrDefault(Hendelsestyper.ORGENHET_V1.getKey(), List.of()));

        KafkaUtils.verifyUpToDate(consumer, properties.getHendelse().getTopic(), initialLoadComplete);
        kafkaHendelseReadCounter.increment(hendelser.values().stream().mapToLong(Collection::size).sum());
    }

    @SneakyThrows
    private HendelseTo convert(ConsumerRecord<String, String> consumerRecord) {
        return objectMapper.readValue(consumerRecord.value(), HendelseTo.class);
    }

    @Override
    public void onPartitionsAssigned(Map<org.apache.kafka.common.TopicPartition, Long> assignments, ConsumerSeekCallback callback) {
        // Første startup vil køen være tom
        if (kafkaOffsetPostProcessor.getHendelseOffset().get() == 0) {
            log.info("Hendelse offset 0, setter initialLoadComplete");
            initialLoadComplete.set(true);
        }
        assignments.keySet().forEach(p -> callback.seekToBeginning(p.topic(), p.partition()));
    }

    public boolean getInitialLoadStatus() {
        return initialLoadComplete.get();
    }
}
