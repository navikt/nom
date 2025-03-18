package no.nav.nom.apps.admin.events.ressurs.api;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.admin.dto.ressurs.RessursStateKafkaDTO;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.PartitionOffset;
import org.springframework.kafka.annotation.TopicPartition;
import org.springframework.kafka.event.ListenerContainerIdleEvent;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
public class RessursApiStateListener {
    private final AtomicBoolean hasReadToEndOnStartup = new AtomicBoolean(false);
    private final RessursApiStateRepository ressursApiStateRepository;
    private final String listenerId;

    public RessursApiStateListener(RessursApiStateRepository ressursApiStateRepository, String apiStateRessursUniqueListenerId) {
        this.ressursApiStateRepository = ressursApiStateRepository;
        this.listenerId = apiStateRessursUniqueListenerId;
    }

    @KafkaListener(
        containerFactory = "genericApiEventKafkaListenerContainerFactory",
        id = "#{apiStateRessursUniqueListenerId}",
        topicPartitions = @TopicPartition(
            topic = "${app.kafka.ressurs.apiEventTopic}", partitionOffsets = @PartitionOffset(partition = "0", initialOffset = "0")
        )
    )
    public void onMessage(ConsumerRecords<String, RessursStateKafkaDTO> records) {
        List<RessursStateKafkaDTO> ressursStateTopic = new ArrayList<>();
        records.forEach(record -> ressursStateTopic.add(
            RessursStateKafkaDTO.builder()
                .key(record.key())
                .timestamp(LocalDateTime.ofInstant(Instant.ofEpochMilli(record.timestamp()), ZoneId.systemDefault()))
                .personident(record.value().personident())
                .navident(record.value().navident())
                .visningsnavn(record.value().visningsnavn())
                .primaryTelefonnummer(record.value().primaryTelefonnummer())
                .sektor(record.value().sektor())
                .orgtilknytninger(record.value().orgtilknytninger())
                .startdato(record.value().startdato())
                .sluttdato(record.value().sluttdato())
            .build()));
        ressursApiStateRepository.addRessursStateTopicEvents(ressursStateTopic);
    }

    @EventListener
    public void hasReadToEndOnStartupEventHandler(ListenerContainerIdleEvent event) {
        if (hasReadToEndOnStartup.get()) {
            return;
        }

        log.debug("Idle event received for listener {}. Waiting for listener {}", event.getListenerId(), listenerId);
        if (event.getListenerId().startsWith(listenerId)) {
            log.info(
                "All Ressurs StateTopic events has now been read on startup. Number of RessursStateTopic messages consumed: {}", ressursApiStateRepository.count()
            );
            hasReadToEndOnStartup.set(true);

            // Logg info
            var numWithoutSluttdato =
                ressursApiStateRepository.getLatestRessursStatePerNavident().values()
                    .stream()
                    .filter(ressurs -> ressurs.sluttdato().isEmpty() || ressurs.sluttdato().get().isBefore(LocalDate.now()))
                    .count();
            log.info("Ressurs API topic - Total num of Ressurser: {} - Number of Ressurser without sluttdato: {}", ressursApiStateRepository.countDistinctNavident(), numWithoutSluttdato);
        }
    }
}
