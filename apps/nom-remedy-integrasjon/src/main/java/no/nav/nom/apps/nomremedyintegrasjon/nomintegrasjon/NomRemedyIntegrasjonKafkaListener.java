package no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon;

import no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.repository.aggregatestate.AggregateState;
import no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.repository.aggregatestate.AggregateStateRepository;
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

@Service
public class NomRemedyIntegrasjonKafkaListener {
    private final static Logger log = LoggerFactory.getLogger(NomRemedyIntegrasjonKafkaListener.class);
    private final NomStateRessursListener nomStateRessursListener;
    private final NomStateOrgtilknytningListener nomStateOrgtilknytningListener;
    private final NomStateMasteredRessursListener nomStateMasteredRessursListener;
    private final MasteredRessursStateRepository masteredRessursStateRepository;
    private final AggregateStateRepository aggregateStateRepository;

    public NomRemedyIntegrasjonKafkaListener(
        NomStateRessursListener nomStateRessursListener,
        NomStateOrgtilknytningListener nomStateOrgtilknytningListener,
        NomStateMasteredRessursListener nomStateMasteredRessursListener,
        MasteredRessursStateRepository masteredRessursStateRepository,
        AggregateStateRepository aggregateStateRepository
    ) {
        this.nomStateRessursListener = nomStateRessursListener;
        this.nomStateOrgtilknytningListener = nomStateOrgtilknytningListener;
        this.nomStateMasteredRessursListener = nomStateMasteredRessursListener;
        this.masteredRessursStateRepository = masteredRessursStateRepository;
        this.aggregateStateRepository = aggregateStateRepository;
    }

    @KafkaListener(
        topics = "${app.kafka.ressurs.state-topic}",
        autoStartup = "${app.kafka.ressurs.auto-startup}",
        containerFactory = "nomRessursStateKafkaListenerContainerFactory"
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
                    .masseflyttetFraFid(null)
                    .masseflytting(false)
                    .build());
            } else {
                aggregateStateRepository.save(allRowsByStateFid.getFirst().toBuilder().delayedUntil(null).build());
            }
        }
    }

    @KafkaListener(
        topics = "${app.kafka.orgtilknytning.state-topic}",
        autoStartup = "${app.kafka.orgtilknytning.auto-startup}",
        containerFactory = "nomOrgtilknytningStateKafkaListenerContainerFactory"
    )
    public void orgtilknytningStateListener(OrgtilknytningState orgtilknytningState, @Header(KafkaHeaders.RECEIVED_KEY) String key) {
        waitForNomStates();
        log.info("Logget orgtilknytningStateFid: {}", key);
        var navident = orgtilknytningState.navident();
        if (isNomMastered(navident) && orgtilknytningState.erDagligOppfolging()) {
            log.info("Logget mastered orgtilknytningStateFid: {}", key);
            var masseflyttetFraFid = orgtilknytningState.masseflyttetFraFid();
            var stateFid = orgtilknytningState.fid();
            var allRowsByNavidentAndMasseflyttetFraFid = aggregateStateRepository.findAggregateStatesByStateFidAndMasseflyttetFraFid(stateFid, masseflyttetFraFid);
            if (allRowsByNavidentAndMasseflyttetFraFid.isEmpty()) {
                aggregateStateRepository.save(AggregateState.builder()
                    .navident(navident)
                    .createdAt(LocalDateTime.now())
                    .stateFid(stateFid)
                    .stateType(orgtilknytningState.getClass().getSimpleName())
                    .masseflyttetFraFid(masseflyttetFraFid)
                    .masseflytting(orgtilknytningState.masseflyttetFraFid() != null)
                    .build());
            } else {
                aggregateStateRepository.save(allRowsByNavidentAndMasseflyttetFraFid.getFirst().toBuilder().delayedUntil(null).build());
            }
        }
    }

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
        return nomStateRessursListener.hasReadToEndOnStartup()
            && nomStateOrgtilknytningListener.hasReadToEndOnStartup()
            && nomStateMasteredRessursListener.hasReadToEndOnStartup();
    }
}
