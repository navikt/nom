package no.nav.nom.apps.api.ressurs.masterdata;


import lombok.extern.slf4j.Slf4j;
import no.nav.nom.libs.state.ressurs.mastered.MasteredRessursStateRepository;
import no.nav.nom.libs.utils.common.kafka.KafkaUtils;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.PartitionOffset;
import org.springframework.kafka.annotation.TopicPartition;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.StreamSupport;

@Slf4j
@Service
public class NomMasteredRessursListener {
    private final AtomicBoolean initialLoadComplete = new AtomicBoolean(false);
    private final String stateTopic;
    private final MasteredRessursStateRepository masteredRessursStateRepository;

    public NomMasteredRessursListener(
        @Value("${app.kafka.ressurs.nomMasteredRessursTopic}") String stateTopic,
        @Qualifier("OldNomMasteredRepository") MasteredRessursStateRepository masteredRessursStateRepository
    ) {
        this.stateTopic = stateTopic;
        this.masteredRessursStateRepository = masteredRessursStateRepository;
    }

    @KafkaListener(
        containerFactory = "nomMasteredRessursKafkaListenerContainerFactory",
        clientIdPrefix = "nomMasteredRessursListener",
        topicPartitions = @TopicPartition(
            topic = "${app.kafka.ressurs.nomMasteredRessursTopic}", partitionOffsets = @PartitionOffset(partition = "0", initialOffset = "0")
        )
    )
    public void onMessage(ConsumerRecords<String, LocalDateTime> records, Consumer<String, String> consumer) {
        StreamSupport.stream(records.spliterator(), false).forEach(it -> {
            processMessage(it.key(), it.value());
        });

        KafkaUtils.verifyUpToDate(
            consumer,
            stateTopic,
            initialLoadComplete,
            () -> log.info("Kafka nomMasteredRessurs listener initial load finished. {} ressurser", masteredRessursStateRepository.count())
        );
    }

    private void processMessage(String navident, LocalDateTime since) {
        masteredRessursStateRepository.update(navident, since);
    }

    public boolean initloadComplete() {
        return initialLoadComplete.get();
    }
}
