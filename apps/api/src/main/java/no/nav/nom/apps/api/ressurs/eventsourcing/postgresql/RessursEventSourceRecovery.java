package no.nav.nom.apps.api.ressurs.eventsourcing.postgresql;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.contract.nom.eventsource.RessursEvent;
import no.nav.nom.libs.utils.common.platform.LeaderElection;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
public class RessursEventSourceRecovery {
    @Value("${app.kafka.ressurs.recoverRessursEventSource:false}") private boolean doRecover;
    private final KafkaTemplate<String, RessursEvent> ressursEventProducerTemplate;
    private final RessursEventSourceRepository ressursEventSourceRepository;
    private final ObjectMapper objectMapper;
    private final LeaderElection leaderElection;
    public RessursEventSourceRecovery(
            @Qualifier("new_RessursEventSourceTopic") KafkaTemplate<String, RessursEvent> ressursEventProducerTemplate,
            RessursEventSourceRepository ressursEventSourceRepository,
            ObjectMapper objectMapper,
            LeaderElection leaderElection
    ) {
        this.ressursEventProducerTemplate = ressursEventProducerTemplate;
        this.ressursEventSourceRepository = ressursEventSourceRepository;
        this.objectMapper = objectMapper;
        this.leaderElection = leaderElection;
    }

    private ListenableFuture<SendResult<String, RessursEvent>> sendMessage(String eventKey, RessursEvent event) {
        return ressursEventProducerTemplate.sendDefault(eventKey, event);
    }

    private void recoverFromDB() {
        log.info("Starting recovery of eventsource from DB to topic {}", ressursEventProducerTemplate.getDefaultTopic());
        AtomicInteger count = new AtomicInteger();
        ressursEventSourceRepository.findAll().forEach( es -> {
            String eventKey = es.getEventKey();
            try {
                RessursEvent event = objectMapper.treeToValue(es.getPayload(), RessursEvent.class);
                sendMessage(eventKey, event);
            } catch (JsonProcessingException e) {
                log.error("Failed to convert DB backed-up Ressurs EventSource with key - %s".formatted(eventKey), e);
            }
            count.getAndIncrement();
        });
        log.info("Done recovery of eventsource from DB. Recoverd {} events", count);
    }

    @Scheduled(initialDelay = 180000, fixedDelayString = "PT02H") // Wait 3 min, then delay 2 hours before trying again
    public void triggerRecover() {
        if (leaderElection.isLeader() && doRecover) recoverFromDB();
    }
}
