package no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.repository.aggregatestate.AggregateState;
import no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.repository.aggregatestate.AggregateStateRepository;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.orgtilknytning.NomStateOrgtilknytningListener;
import no.nav.nom.libs.state.ressurs.NomStateRessursListener;
import no.nav.nom.libs.state.ressurs.mastered.MasteredRessursStateRepository;
import no.nav.nom.libs.state.ressurs.mastered.NomStateMasteredRessursListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class NomRemedyIntegrasjonKafkaListenerNew {
    private final static Logger log = LoggerFactory.getLogger(NomRemedyIntegrasjonKafkaListenerNew.class);
    private final ObjectMapper objectMapper;
    private final NomStateRessursListener nomStateRessursListener;
    private final NomStateOrgtilknytningListener nomStateOrgtilknytningListener;
    private final NomStateMasteredRessursListener nomStateMasteredRessursListener;
    private final MasteredRessursStateRepository masteredRessursStateRepository;
    private final AggregateStateRepository aggregateStateRepository;

    public NomRemedyIntegrasjonKafkaListenerNew(
        ObjectMapper objectMapper,
        NomStateRessursListener nomStateRessursListener,
        NomStateOrgtilknytningListener nomStateOrgtilknytningListener,
        NomStateMasteredRessursListener nomStateMasteredRessursListener,
        MasteredRessursStateRepository masteredRessursStateRepository,
        AggregateStateRepository aggregateStateRepository
    ) {
        this.objectMapper = objectMapper;
        this.nomStateRessursListener = nomStateRessursListener;
        this.nomStateOrgtilknytningListener = nomStateOrgtilknytningListener;
        this.nomStateMasteredRessursListener = nomStateMasteredRessursListener;
        this.masteredRessursStateRepository = masteredRessursStateRepository;
        this.aggregateStateRepository = aggregateStateRepository;
    }

    @KafkaListener(
        topics = "${app.kafka.ressurs.state-topic}",
        autoStartup = "${app.kafka.ressurs.auto-startup}",
        containerFactory = "nomStateRessursKafkaListenerContainerFactory"
    )
    public void ressursStateListener(RessursState ressursState, @Header(KafkaHeaders.RECEIVED_KEY) String key) {
        waitForNomStates();
        log.info("Logget ressursStateFid: {}", key);
        var navident = ressursState.navident();
        if (isNomMastered(navident)) {
            log.info("Logget mastered ressursStateFid: {}", key);
            var allRowsByStateFid = aggregateStateRepository.findByStateFid(ressursState.fid());
            if (allRowsByStateFid.isEmpty()) {
                aggregateStateRepository.save(AggregateState.builder()
                    .navident(navident)
                    .createdAt(LocalDateTime.now())
                    .stateFid(ressursState.fid())
                    .stateType(ressursState.getClass().getSimpleName())
                    .build());
            }
        }
    }

    @KafkaListener(
        topics = "${app.kafka.orgtilknytning.state-topic}",
        autoStartup = "${app.kafka.orgtilknytning.auto-startup}",
        containerFactory = "nomStateOrgtilknytningKafkaListenerContainerFactory"
    )
    public void orgtilknytningStateListener(OrgtilknytningState orgtilknytningState, @Header(KafkaHeaders.RECEIVED_KEY) String key) {
        waitForNomStates();
        log.info("Logget orgtilknytningStateFid: {}", key);
        var navident = orgtilknytningState.navident();
        if (isNomMastered(navident) && orgtilknytningState.erDagligOppfolging()) {
            log.info("Logget mastered orgtilknytningStateFid: {}", key);
            var masseflyttetFraFid = orgtilknytningState.masseflyttetFraFid().orElse(null);
            var allRowsByNavidentAndMasseflyttetFraFid = aggregateStateRepository.findByNavidentAndMasseflyttetFraFid(navident, masseflyttetFraFid);
            if (allRowsByNavidentAndMasseflyttetFraFid.isEmpty()) {
                aggregateStateRepository.save(AggregateState.builder()
                    .navident(navident)
                    .createdAt(LocalDateTime.now())
                    .stateFid(orgtilknytningState.fid())
                    .stateType(orgtilknytningState.getClass().getSimpleName())
                    .masseflyttetFraFid(orgtilknytningState.masseflyttetFraFid().orElse(null))
                    .masseflytting(orgtilknytningState.masseflyttetFraFid().isPresent())
                    .build());
            }
        }
    }

    /*
    @KafkaListener(
        id = "NomRemedyIntegrasjonOrgtilknytningEventListenerNew",
        topics = "${app.kafka-topics.orgtilknytning-eventsource.topic-name}",
        autoStartup = "${app.kafka-topics.orgtilknytning-eventsource.auto-startup}",
        containerFactory = "orgtilknytningEventKafkaListenerContainerFactory"
    )
    public void orgtilknytningEventListener(OrgtilknytningEvent orgtilknytningEvent, @Header(KafkaHeaders.RECEIVED_KEY) String key) {
        log.info("Logget orgtilknytningEventFid: {}", key);
        if (orgtilknytningEvent instanceof OrgtilknytningOpprettetEventV1 opprettetEvent) {
            log.info("Logget opprettet orgtilknytningEventFid: {}", opprettetEvent.getFid());
            if (opprettetEvent.getMetadata().inputMetadata().inputInterfaceMethodName().equals("masseflytting") && erMasseflytting(opprettetEvent.getMetadata().inputMetadata())) {
                var navident = opprettetEvent.getOrgtilknytningOpprettetEventData().navident();
                var masseflyttetFraFid = opprettetEvent.getOrgtilknytningOpprettetEventData().masseflyttetFraFid();
                var allRowsByStateFid = aggregateStateRepository.findByNavidentAndMasseflyttetFraFid(navident, masseflyttetFraFid);
                if (allRowsByStateFid.isEmpty()) {
                    aggregateStateRepository.save(AggregateState.builder()
                        .navident(navident)
                        .createdAt(LocalDateTime.now())
                        .stateType(opprettetEvent.getClass().getSimpleName())
                        .masseflyttetFraFid(masseflyttetFraFid)
                        .masseflytting(true)
                        .build());
                } else {
                    allRowsByStateFid.forEach(row -> aggregateStateRepository.save(row.toBuilder().masseflytting(true).build()));
                }
            }
        }
    }*/

    private void waitForNomStates() {
        while (!isInitialStateReady()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                log.error("Thread interrupted while waiting for NomStateMasteredRessursListener/NomStateRessursListener to read to end on startup", e);
            }
        }
    }

    private Boolean isNomMastered(String navident) {
        return masteredRessursStateRepository.isMastered(navident);
    }

    private boolean isInitialStateReady() {
        return nomStateRessursListener.hasReadToEndOnStartup() && nomStateOrgtilknytningListener.hasReadToEndOnStartup() && nomStateMasteredRessursListener.hasReadToEndOnStartup();
    }

    private boolean erMasseflytting(InputMetadata inputMetadata) {
        Map<String, Object> inputInterfaceMethodArguments = objectMapper.convertValue(inputMetadata.inputInterfaceMethodArguments(), new TypeReference<>() {});

        return inputInterfaceMethodArguments.containsKey("beholdTilganger") ? (boolean) inputInterfaceMethodArguments.get("beholdTilganger") : (boolean) inputInterfaceMethodArguments.get(
            "beholdTilgang");
    }
}
