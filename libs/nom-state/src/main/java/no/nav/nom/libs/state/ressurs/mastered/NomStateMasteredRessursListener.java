package no.nav.nom.libs.state.ressurs.mastered;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.libs.state.common.AbstractNomStateListener;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.PartitionOffset;
import org.springframework.kafka.annotation.TopicPartition;

import java.time.LocalDateTime;

@Slf4j
public class NomStateMasteredRessursListener extends AbstractNomStateListener<LocalDateTime> {
    private final MasteredRessursStateRepository masteredRessursStateRepository;

    public NomStateMasteredRessursListener(String stateTopic, MasteredRessursStateRepository masteredRessursStateRepository, String listenerId) {
        super(stateTopic, listenerId);
        this.masteredRessursStateRepository = masteredRessursStateRepository;
    }

    @KafkaListener(
        id = "#{masteredRessursStateListenerId}",
        containerFactory = "nomStateMasteredRessursKafkaListenerContainerFactory",
        topicPartitions = @TopicPartition(
            topic = "${nom.libs.state.mastered-ressurs.state-topic}", partitionOffsets = @PartitionOffset(partition = "0", initialOffset = "0")
        )
    )
    public void onMessage(ConsumerRecords<String, LocalDateTime> records) {
        super.onMessage(records);
    }

    @Override
    protected void registerMessageWithState(String navident, LocalDateTime since) {
        masteredRessursStateRepository.update(navident, since);
    }

    @Override
    protected void doneReadToEndOnStartup() {
        log.info("Number of MasteredRessurs navidents read on startup: {}", masteredRessursStateRepository.count());
    }
}
