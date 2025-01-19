package no.nav.nom.libs.state.orgenhet;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.libs.state.common.AbstractNomStateListener;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.PartitionOffset;
import org.springframework.kafka.annotation.TopicPartition;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;

import java.util.List;
import java.util.Map;

@Slf4j
public class NomStateOrgEnhetListener extends AbstractNomStateListener<OrgEnhetState> {
    private final OrgEnhetStateRepository orgEnhetStateRepository;

    public NomStateOrgEnhetListener(String stateTopic, OrgEnhetStateRepository orgEnhetStateRepository, String listenerId) {
        super(stateTopic, listenerId);
        this.orgEnhetStateRepository = orgEnhetStateRepository;
    }

    @KafkaListener(
        id = "#{orgEnhetStateListenerId}",
        containerFactory = "nomStateOrgEnhetKafkaListenerContainerFactory",
        topicPartitions = @TopicPartition(
            topic = "${nom.libs.state.orgenhet.state-topic}", partitionOffsets = @PartitionOffset(partition = "0", initialOffset = "0")
        )
    )
    public void listen(List<OrgEnhetState> events, @Header(KafkaHeaders.RECEIVED_KEY) List<String> keys, @Header(KafkaHeaders.BATCH_CONVERTED_HEADERS) List<Map<String, Object>> headers) {
        super.onMessageWithHeaders(events, keys, headers);
    }

    @Override
    protected void registerMessageWithState(String key, OrgEnhetState orgEnhetState) {
        if (orgEnhetState.uniqueAgressoId().isPresent()) {
            orgEnhetStateRepository.update(orgEnhetState);
        } else { log.info("OrgEnhetState with key {} has no uniqueAgressoId - will not add to state", key); }
    }

    @Override
    protected void doneReadToEndOnStartup() {
        log.info("Number of OrgEnhets FID's read on startup: {}", orgEnhetStateRepository.count());
    }
}
