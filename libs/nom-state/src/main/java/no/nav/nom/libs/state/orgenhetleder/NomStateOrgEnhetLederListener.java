package no.nav.nom.libs.state.orgenhetleder;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.contract.nom.state.OrgEnhetLederState;
import no.nav.nom.libs.state.common.AbstractNomStateListener;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.PartitionOffset;
import org.springframework.kafka.annotation.TopicPartition;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;

import java.util.List;
import java.util.Map;

@Slf4j
public class NomStateOrgEnhetLederListener extends AbstractNomStateListener<OrgEnhetLederState> {
    private final OrgEnhetLederStateRepository orgEnhetLederStateRepository;

    public NomStateOrgEnhetLederListener(String stateTopic, String listenerId, OrgEnhetLederStateRepository orgEnhetLederStateRepository) {
        super(stateTopic, listenerId);
        this.orgEnhetLederStateRepository = orgEnhetLederStateRepository;
    }

    @KafkaListener(
        id = "#{orgEnhetLederStateListenerId}",
        containerFactory = "nomStateOrgEnhetLederKafkaListenerContainerFactory",
        topicPartitions = @TopicPartition(
            topic = "${nom.libs.state.orgenhet-leder.state-topic}", partitionOffsets = @PartitionOffset(partition = "0", initialOffset = "0")
        )
    )
    public void onMessage(List<OrgEnhetLederState> events, @Header(KafkaHeaders.RECEIVED_KEY) List<String> keys, @Header(KafkaHeaders.BATCH_CONVERTED_HEADERS) List<Map<String, Object>> headers) {
        super.onMessageWithHeaders(events, keys, headers);
    }

    @Override
    protected void registerMessageWithState(String key, OrgEnhetLederState orgEnhetLederEvent) {
        orgEnhetLederStateRepository.update(key, orgEnhetLederEvent);
    }

    @Override
    protected void doneReadToEndOnStartup() {
        log.info("Number of OrgEnhetLeder read on startup: {}", orgEnhetLederStateRepository.count());
    }
}
