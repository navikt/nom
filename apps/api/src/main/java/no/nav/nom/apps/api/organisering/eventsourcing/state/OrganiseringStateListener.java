package no.nav.nom.apps.api.organisering.eventsourcing.state;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.contract.nom.state.OrganiseringState;
import no.nav.nom.libs.utils.common.kafka.KafkaUtils;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.PartitionOffset;
import org.springframework.kafka.annotation.TopicPartition;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.StreamSupport;

@Service
@Slf4j
public class OrganiseringStateListener {

    private final AtomicBoolean initialLoadComplete = new AtomicBoolean(false);
    private final String stateTopic;

    private final OrganiseringStateRepository repository;


    public OrganiseringStateListener(
        @Value("${app.kafka.organisering.stateTopic}") String stateTopic,
        OrganiseringStateRepository repository
    ) {
        this.stateTopic = stateTopic;
        this.repository = repository;
    }

    @KafkaListener(
        containerFactory = "organiseringStateKafkaListenerContainerFactory",
        clientIdPrefix = "organiseringStateListener",
        topicPartitions = @TopicPartition(
            topic = "${app.kafka.organisering.stateTopic}", partitionOffsets = @PartitionOffset(partition = "0", initialOffset = "0")
        )
    )
    public void onMessage(ConsumerRecords<String, OrganiseringState> records, Consumer<String, String> consumer) {
        StreamSupport.stream(records.spliterator(), false).forEach(it -> {
            var organiseringState = it.value();
            if(organiseringState.gyldigTom().isPresent() && organiseringState.gyldigTom().get().isBefore(LocalDate.now())){
                repository.removeEventAggregate(organiseringState);
            } else {
                repository.putEventAggregate(organiseringState);
            }
        });

        KafkaUtils.verifyUpToDate(consumer, stateTopic, initialLoadComplete,
            () -> log.info("Kafka OrganiseringState listener initial load finished - {} Organiseringer", repository.count())
        );
    }

}
