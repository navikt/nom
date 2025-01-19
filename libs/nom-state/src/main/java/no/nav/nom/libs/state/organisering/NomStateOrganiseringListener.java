package no.nav.nom.libs.state.organisering;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.contract.nom.state.OrganiseringState;
import no.nav.nom.libs.state.common.AbstractNomStateListener;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.PartitionOffset;
import org.springframework.kafka.annotation.TopicPartition;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;

import java.util.List;
import java.util.Map;

@Slf4j
public class NomStateOrganiseringListener extends AbstractNomStateListener<OrganiseringState> {
    private final OrganiseringStateRepository organiseringStateRepository;

    public NomStateOrganiseringListener(String stateTopic, OrganiseringStateRepository organiseringStateRepository, String listenerId) {
        super(stateTopic, listenerId);
        this.organiseringStateRepository = organiseringStateRepository;
    }

    @KafkaListener(
        id = "#{organiseringStateListenerId}",
        containerFactory = "nomStateOrganiseringKafkaListenerContainerFactory",
        topicPartitions = @TopicPartition(
            topic = "${nom.libs.state.organisering.state-topic}", partitionOffsets = @PartitionOffset(partition = "0", initialOffset = "0")
        )
    )
    public void onMessage(List<OrganiseringState> events, @Header(KafkaHeaders.RECEIVED_KEY) List<String> keys, @Header(KafkaHeaders.BATCH_CONVERTED_HEADERS) List<Map<String, Object>> headers) {
        super.onMessageWithHeaders(events, keys, headers);
    }

    @Override
    protected void registerMessageWithState(String key, OrganiseringState organiseringState) {
        organiseringStateRepository.update(organiseringState);
    }

    @Override
    protected void doneReadToEndOnStartup() {
        log.info("Number of Organiserings FID's read on startup: {}", organiseringStateRepository.count());
    }
}
