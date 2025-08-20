package no.nav.nom.apps.admin.events.ressurs.api;

import no.nav.nom.apps.admin.dto.ressurs.RessursHendelseKafkaDTO;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.PartitionOffset;
import org.springframework.kafka.annotation.TopicPartition;
import org.springframework.kafka.event.ListenerContainerIdleEvent;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class RessursHendelseApiListener {
    private final Logger log = LoggerFactory.getLogger(RessursHendelseApiListener.class);

    private final AtomicBoolean hasReadToEndOnStartup = new AtomicBoolean(false);
    private final RessursHendelseApiRepository ressursHendelseApiRepository;
    private final String listenerId;

    public RessursHendelseApiListener(
            RessursHendelseApiRepository ressursHendelseApiRepository, String apiHendelseRessursUniqueListenerId) {
        this.ressursHendelseApiRepository = ressursHendelseApiRepository;
        this.listenerId = apiHendelseRessursUniqueListenerId;
    }

    
    @KafkaListener(
            containerFactory = "genericApiEventKafkaListenerContainerFactory",
            id = "#{apiHendelseRessursUniqueListenerId}",
            topicPartitions = @TopicPartition(
                    topic = "${app.kafka.ressurs-hendelser.apiEventTopic}", 
                    partitionOffsets = @PartitionOffset(partition = "0", initialOffset = "0")
            )
    )
    public void onMessage(ConsumerRecords<String, RessursHendelseKafkaDTO> records) {
        List<RessursHendelseKafkaDTO> ressursHendelser = new ArrayList<>();

        records.forEach(record -> {
            log.debug("Record received - Key: {} Value: {}", record.key(), record.value());
            ressursHendelser.add(
                    RessursHendelseKafkaDTO.builder()
                        .key(record.key())
                        .timestamp(LocalDateTime.ofInstant(Instant.ofEpochMilli(record.timestamp()), ZoneId.systemDefault()))
                        .fid(record.value().fid())
                        .navident(record.value().navident())
                        .visningsnavn(record.value().visningsnavn())
                        .startdato(record.value().startdato())
                        .sluttdato(record.value().sluttdato())
                        .sektor(record.value().sektor())
                        .orgtilknytning(record.value().orgtilknytning())
                        .build());
        });

        ressursHendelseApiRepository.add(ressursHendelser);
    }

    @EventListener
    public void hasReadToEndOnStartupEventHandler(ListenerContainerIdleEvent event) {
        if (hasReadToEndOnStartup.get()) {
            return;
        }

        log.debug("Idle event received for listener {}. Waiting for listener {}", event.getListenerId(), listenerId);
        if (event.getListenerId().startsWith(listenerId)) {
            log.info(
                    "All Ressurs Api Hendelse events has now been read on startup. Number of RessursApiHendelse messages consumed: {}", ressursHendelseApiRepository.count()
            );
            hasReadToEndOnStartup.set(true);
        }
    }
    
}
