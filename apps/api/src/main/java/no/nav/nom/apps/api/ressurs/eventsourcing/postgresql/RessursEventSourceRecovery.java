package no.nav.nom.apps.api.ressurs.eventsourcing.postgresql;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.contract.nom.eventsource.RessursEvent;
import no.nav.nom.libs.utils.common.platform.LeaderElection;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.task.TaskExecutor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.concurrent.ListenableFuture;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.springframework.transaction.TransactionDefinition.ISOLATION_READ_COMMITTED;

@Service
@Slf4j
public class RessursEventSourceRecovery {
    @Value("${app.kafka.ressurs.recoverRessursEventSource:false}") private boolean doRecover;
    private final KafkaTemplate<String, RessursEvent> ressursEventProducerTemplate;
    private final RessursEventSourceRepository ressursEventSourceRepository;
    private final ObjectMapper objectMapper;
    private final LeaderElection leaderElection;

    private final PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;
    private final TaskExecutor taskExecutor;
    public RessursEventSourceRecovery(
            KafkaTemplate<String, RessursEvent> ressursEventProducerTemplate,
            RessursEventSourceRepository ressursEventSourceRepository,
            ObjectMapper objectMapper,
            LeaderElection leaderElection,
            PlatformTransactionManager transactionManager, TaskExecutor taskExecutor
    ) {
        this.ressursEventProducerTemplate = ressursEventProducerTemplate;
        this.ressursEventSourceRepository = ressursEventSourceRepository;
        this.objectMapper = objectMapper;
        this.leaderElection = leaderElection;
        this.transactionManager = transactionManager;
        this.taskExecutor = taskExecutor;
    }

    private ListenableFuture<SendResult<String, RessursEvent>> sendMessage(String eventKey, RessursEvent event) {
        return ressursEventProducerTemplate.sendDefault(eventKey, event);
    }

    @Transactional(readOnly = true)
    public void recoverFromDB() {
        log.info("Starting ressurs recovery of eventsource from DB to topic {}", ressursEventProducerTemplate.getDefaultTopic());
        AtomicInteger count = new AtomicInteger();
        try (Stream<RessursEventSource> eventSource = ressursEventSourceRepository.findAllBy()) {
            eventSource.forEach( es -> {
                String eventKey = es.getEventKey();
                try {
                    RessursEvent event = objectMapper.treeToValue(es.getPayload(), RessursEvent.class);
                    sendMessage(eventKey, event);
                } catch (JsonProcessingException e) {
                    log.error("Failed to convert DB backed-up Ressurs EventSource with key - %s".formatted(eventKey), e);
                }
                count.getAndIncrement();
                if (count.get() % 5000 == 0) {
                    log.info("Processing ressurs recovery of eventsource from DB. Recovered {} events", count);
                }
            });
        }
        log.info("Done ressurs recovery of eventsource from DB. Recoverd {} events", count.get());
    }

    @EventListener
    public void triggerRecoverAfterStartup(ApplicationReadyEvent event) {
        log.info("Application startup event in ressurs recovery of eventsource with recoverRessursEventSource={}", doRecover);
        if (doRecover) {
            taskExecutor.execute(() -> {
                // wait a bit for new leader election after redeploy
                try {
                    TimeUnit.MINUTES.sleep(5);
                } catch (Exception ignored) {
                }
                transactionTemplate = new TransactionTemplate(transactionManager);
                transactionTemplate.setIsolationLevel(ISOLATION_READ_COMMITTED);
                transactionTemplate.execute( status -> {
                    triggerRecoveryIfLeader();
                    return "done";
                });
            });
        }
    }

    @Transactional
    public void triggerRecoveryIfLeader() {
        log.info("Trigger for ressurs recovery of eventsource with leader={}", leaderElection.isLeader());
        if (leaderElection.isLeader()) recoverFromDB();
    }
}
