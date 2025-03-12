package no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon;

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
import org.springframework.stereotype.Service;
import org.springframework.messaging.handler.annotation.Header;

@Service
public class NomRemedyIntegrasjonKafkaListenerNew {
    private final static Logger log = LoggerFactory.getLogger(NomRemedyIntegrasjonKafkaListenerNew.class);
    private final NomStateRessursListener nomStateRessursListener;
    private final NomStateOrgtilknytningListener nomStateOrgtilknytningListener;
    private final NomStateMasteredRessursListener nomStateMasteredRessursListener;
    private final MasteredRessursStateRepository masteredRessursStateRepository;

    public NomRemedyIntegrasjonKafkaListenerNew(
        NomStateRessursListener nomStateRessursListener,
        NomStateOrgtilknytningListener nomStateOrgtilknytningListener,
        NomStateMasteredRessursListener nomStateMasteredRessursListener,
        MasteredRessursStateRepository masteredRessursStateRepository
    ) {
        this.nomStateRessursListener = nomStateRessursListener;
        this.nomStateOrgtilknytningListener = nomStateOrgtilknytningListener;
        this.nomStateMasteredRessursListener = nomStateMasteredRessursListener;
        this.masteredRessursStateRepository = masteredRessursStateRepository;
    }

    @KafkaListener(
        topics = "${app.kafka.ressurs.state-topic}",
        autoStartup = "${app.kafka.ressurs.auto-startup}",
        containerFactory = "nomStateRessursKafkaListenerContainerFactory"
    )
    public void ressursStateListener(RessursState ressursState, @Header(KafkaHeaders.RECEIVED_KEY) String key) {
        waitForNomStates();
        log.info("Logget ressursStateFid: {}", key);
        if (isNomMastered(ressursState.navident())) {
            log.info("Logget mastered ressursStateFid: {}", key);
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
        if (isNomMastered(orgtilknytningState.navident())) {
            log.info("Logget mastered orgtilknytningStateFid: {}", key);
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
