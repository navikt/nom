package no.nav.nom.apps.admin.events.ressurs.api;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.admin.dto.ressurs.RessursStateKafkaDTO;
import org.apache.kafka.clients.consumer.ConsumerRecords;
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
public class RessursApiStateV3Listener {
    private final AtomicBoolean hasReadToEndOnStartup = new AtomicBoolean(false);
    private final RessursApiStateV3Repository ressursApiStateRepository;
    private final String listenerId;

    public RessursApiStateV3Listener(RessursApiStateV3Repository ressursApiStateRepository, String apiStateRessursV3UniqueListenerId) {
        this.ressursApiStateRepository = ressursApiStateRepository;
        this.listenerId = apiStateRessursV3UniqueListenerId;
    }

    @KafkaListener(
        containerFactory = "genericApiEventKafkaListenerContainerFactory",
        id = "#{apiStateRessursV3UniqueListenerId}",
        topics = "${app.kafka.ressurs.apiEventTopicV3}"
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
                "All Ressurs StateTopic V3 events has now been read on startup by {}. Number of RessursStateTopic messages consumed: {}", listenerId, ressursApiStateRepository.count()
            );
            hasReadToEndOnStartup.set(true);

            // Logg info
            var numWithoutSluttdato =
                ressursApiStateRepository.getLatestRessursStatePerNavident().values()
                    .stream()
                    .filter(ressurs -> ressurs.sluttdato().isEmpty() || ressurs.sluttdato().get().isAfter(LocalDate.now()))
                    .count();

            var numWithOTWithoutSluttdato =
                ressursApiStateRepository.getLatestRessursStatePerNavident().values()
                    .stream()
                    .filter(ressurs -> !ressurs.orgtilknytninger().isEmpty())
                    .filter(ressurs -> ressurs.sluttdato().isEmpty() || ressurs.sluttdato().get().isAfter(LocalDate.now()))
                    .count();

            log.info("Ressurs API topic V3 - Total num of Ressurser: {} - Number of Ressurser without sluttdato: {} - With OT and without sluttdato {}", ressursApiStateRepository.countDistinctNavident(), numWithoutSluttdato, numWithOTWithoutSluttdato);
        }
    }
}
