package no.nav.nom.libs.state.orgtilknytning;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.contract.nom.Tilknytningtype;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.libs.state.common.AbstractNomStateListener;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.PartitionOffset;
import org.springframework.kafka.annotation.TopicPartition;

import java.util.Optional;

@Slf4j
public class NomStateOrgtilknytningListener extends AbstractNomStateListener<OrgtilknytningState> {
    private final OrgtilknytningStateRepository orgtilknytningStateRepository;

    public NomStateOrgtilknytningListener(String stateTopic, OrgtilknytningStateRepository orgtilknytningStateRepository, String listenerId) {
        super(stateTopic, listenerId);
        this.orgtilknytningStateRepository = orgtilknytningStateRepository;
    }

    @KafkaListener(
        id = "#{orgtilknytningStateListenerId}",
        containerFactory = "nomStateOrgtilknytningKafkaListenerContainerFactory",
        topicPartitions = @TopicPartition(
            topic = "${nom.libs.state.orgtilknytning.state-topic}", partitionOffsets = @PartitionOffset(partition = "0", initialOffset = "0")
        )
    )
    public void onMessage(ConsumerRecords<String, OrgtilknytningState> records) {
        super.onMessage(records);
    }

    @Override
    protected void registerMessageWithState(String key, OrgtilknytningState orgtilknytningState) {
        if (orgtilknytningState.orgtilknytningType().isPresent() && orgtilknytningState.orgtilknytningType().get() == Tilknytningtype.MIDLERTIDLIG) {
            orgtilknytningState = new OrgtilknytningState(
                orgtilknytningState.fid(),
                orgtilknytningState.navident(),
                orgtilknytningState.orgEnhetId(),
                orgtilknytningState.sektor(),
                orgtilknytningState.erDagligOppfolging(),
                orgtilknytningState.gyldigFom(),
                orgtilknytningState.gyldigTom(),
                Optional.of(Tilknytningtype.MIDLERTIDIG)
            );
        }
        orgtilknytningStateRepository.update(key, orgtilknytningState);
    }

    @Override
    protected void doneReadToEndOnStartup() {
        log.info("Number of Orgtilknytnings FID's read on startup: {}", orgtilknytningStateRepository.count());
    }
}
