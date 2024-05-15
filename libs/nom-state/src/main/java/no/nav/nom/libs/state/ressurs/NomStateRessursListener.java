package no.nav.nom.libs.state.ressurs;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.common.AbstractNomStateListener;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.PartitionOffset;
import org.springframework.kafka.annotation.TopicPartition;

@Slf4j
public class NomStateRessursListener extends AbstractNomStateListener<RessursState> {
    private final RessursStateRepository ressursStateRepository;

    public NomStateRessursListener(String stateTopic, RessursStateRepository ressursStateRepository, String listenerId) {
        super(stateTopic, listenerId);
        this.ressursStateRepository = ressursStateRepository;
    }

    @KafkaListener(
        id = "#{ressursStateListenerId.toString()}",
        containerFactory = "nomStateRessursKafkaListenerContainerFactory",
        topicPartitions = @TopicPartition(
            topic = "${nom.libs.state.ressurs.state-topic}", partitionOffsets = @PartitionOffset(partition = "0", initialOffset = "0")
        )
    )
    public void onMessage(ConsumerRecords<String, RessursState> records) {
        super.onMessage(records);
    }

    @Override
    protected void registerMessageWithState(String key, RessursState ressursState) {
        if (ressursState == null) {
            ressursStateRepository.remove(key);
        } else {
            ressursStateRepository.update(ressursState);
        }
    }

    @Override
    protected void doneReadToEndOnStartup() {
        log.info("Number of Ressurs FID's read on startup: {}", ressursStateRepository.count());
    }
}
