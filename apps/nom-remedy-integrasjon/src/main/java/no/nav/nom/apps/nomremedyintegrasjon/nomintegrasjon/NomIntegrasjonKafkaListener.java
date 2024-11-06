package no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon;

import no.nav.nom.contract.nom.eventsource.Event;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningEvent;
import no.nav.nom.contract.nom.eventsource.RessursEvent;
import no.nav.nom.libs.state.orgenhet.NomStateOrgEnhetListener;
import no.nav.nom.libs.state.orgtilknytning.NomStateOrgtilknytningListener;
import no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateRepository;
import no.nav.nom.libs.state.ressurs.NomStateRessursListener;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import no.nav.nom.libs.state.ressurs.mastered.MasteredRessursStateRepository;
import no.nav.nom.libs.state.ressurs.mastered.NomStateMasteredRessursListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

@Service
public class NomIntegrasjonKafkaListener {

    private final NomIntegrasjonService nomIntegrasjonService;
    private final MasteredRessursStateRepository masteredRessursStateRepository;
    private final RessursStateRepository ressursStateRepository;
    private final OrgtilknytningStateRepository orgtilknytningStateRepository;
    private final NomStateMasteredRessursListener nomStateMasteredRessursListener;
    private final NomStateRessursListener nomStateRessursListener;
    private final NomStateOrgtilknytningListener nomStateOrgtilknytningListener;
    private final NomStateOrgEnhetListener nomStateOrgEnhetListener;

    private final Logger logger = LoggerFactory.getLogger(NomIntegrasjonKafkaListener.class);

    public NomIntegrasjonKafkaListener(
        NomIntegrasjonService nomIntegrasjonService,
        MasteredRessursStateRepository masteredRessursStateRepository,
        RessursStateRepository ressursStateRepository,
        OrgtilknytningStateRepository orgtilknytningStateRepository,
        NomStateMasteredRessursListener nomStateMasteredRessursListener,
        NomStateRessursListener nomStateRessursListener,
        NomStateOrgtilknytningListener nomStateOrgtilknytningListener,
        NomStateOrgEnhetListener nomStateOrgEnhetListener
    ) {
        this.nomIntegrasjonService = nomIntegrasjonService;
        this.masteredRessursStateRepository = masteredRessursStateRepository;
        this.ressursStateRepository = ressursStateRepository;
        this.orgtilknytningStateRepository = orgtilknytningStateRepository;
        this.nomStateMasteredRessursListener = nomStateMasteredRessursListener;
        this.nomStateRessursListener = nomStateRessursListener;
        this.nomStateOrgtilknytningListener = nomStateOrgtilknytningListener;
        this.nomStateOrgEnhetListener = nomStateOrgEnhetListener;
    }

    @KafkaListener(
        topics = "${app.kafka-topics.ressurs-eventsource.topic-name}",
        autoStartup = "${app.kafka-topics.ressurs-eventsource.auto-startup}",
        containerFactory = "ressursEventKafkaListenerContainerFactory"
    )
    public void ressursEventListener(RessursEvent ressursEvent, @Header(KafkaHeaders.RECEIVED_KEY) String recordId) {
        logger.info("Waiting for NOM states to be ready");
        waitForNomStates();
        logger.info("Received ressurs event with fid {}", ressursEvent.getFid());

        putMDCContext(recordId, ressursEvent.getClass().getSimpleName());

        if (isNomOriginationOrNomMastered(ressursEvent)) {
            nomIntegrasjonService.handleRessursEvent(ressursEvent);
        } else {
            logger.info("Skipping event with fid {} since it is not originated from nom", ressursEvent.getFid());
        }

        MDC.clear();
    }

    @KafkaListener(
        topics = "${app.kafka-topics.orgtilknytning-eventsource.topic-name}",
        autoStartup = "${app.kafka-topics.orgtilknytning-eventsource.auto-startup}",
        containerFactory = "orgtilknytningEventKafkaListenerContainerFactory"
    )
    public void orgtilknytningEventListener(OrgtilknytningEvent orgtilknytningEvent, @Header(KafkaHeaders.RECEIVED_KEY) String recordId) {
        logger.info("Waiting for NOM states to be ready");
        waitForNomStates();
        logger.info("Received orgtilknytning event with fid {}", orgtilknytningEvent.getFid());

        putMDCContext(recordId, orgtilknytningEvent.getClass().getSimpleName());

        if (isNomOriginationOrNomMastered(orgtilknytningEvent)) {
            nomIntegrasjonService.handleOrgtilknytningEvent(orgtilknytningEvent);
        } else {
            logger.info("Skipping event with fid {} since it is not originated from nom", orgtilknytningEvent.getFid());
        }

        MDC.clear();
    }

    private void waitForNomStates() {
        while (!isInitialStateReady()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                logger.error("Thread interrupted while waiting for NomStateMasteredRessursListener/NomStateRessursListener to read to end on startup", e);
            }
        }
    }

    private boolean isInitialStateReady() {
        return nomStateMasteredRessursListener.hasReadToEndOnStartup()
            && nomStateRessursListener.hasReadToEndOnStartup()
            && nomStateOrgtilknytningListener.hasReadToEndOnStartup()
            && nomStateOrgEnhetListener.hasReadToEndOnStartup();
    }

    private void putMDCContext(String recordId, String eventType) {
        MDC.put("record_id", recordId);
        MDC.put("event_type", eventType);
    }

    private Boolean isNomOriginationOrNomMastered(Event event) {
        logger.info("Event with fid {} is a {}", event.getFid(), event.getClass().getSimpleName());

        if (event instanceof RessursEvent) {
            logger.info("Event {} is a RessursEvent", event.getFid());
            return ressursEventIsNomOriginationOrNomMastered((RessursEvent) event);
        } else if (event instanceof OrgtilknytningEvent) {
            logger.info("Event {} is a OrgtilknytningEvent", event.getFid());
            return orgtilknytningEventIsNomOriginationOrNomMastered((OrgtilknytningEvent) event);
        }
        return false;
    }

    private Boolean orgtilknytningEventIsNomOriginationOrNomMastered(OrgtilknytningEvent orgtilknytningEvent) {
        var orgtilknytningState = orgtilknytningStateRepository.findByFid(orgtilknytningEvent.getFid());

        int count = 0;
        while (orgtilknytningState.isEmpty() && count < 5) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                logger.error("Thread interrupted while waiting for orgtilknytningStateRepository to be ready", e);
            }
            count++;
            orgtilknytningState = orgtilknytningStateRepository.findByFid(orgtilknytningEvent.getFid());
        }

        return orgtilknytningState.map(state -> {
            logger.info("Checking if ressurs with fid {} navident {} and orgtilknytning {} is mastered", orgtilknytningEvent.getFid(), state.navident(), state.orgEnhetId());
            if (!masteredRessursStateRepository.isMastered(state.navident())) {
                return orgtilknytningEvent.getMetadata().inputMetadata().origination().initiatedFrom().contains(":nom-ui2");
            }
            return true;
        }).orElse(false);
    }

    private boolean ressursEventIsNomOriginationOrNomMastered(RessursEvent ressursEvent) {
        var ressursState = ressursStateRepository.findByFid(ressursEvent.getFid());

        int count = 0;
        while (ressursState.isEmpty() && count < 5) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                logger.error("Thread interrupted while waiting for ressursStateRepository to be ready", e);
            }
            count++;
            ressursState = ressursStateRepository.findByFid(ressursEvent.getFid());
        }
        return ressursState.map(state -> {
            logger.info("Checking if ressurs with fid {} and navident {} is mastered", ressursEvent.getFid(), state.navident());
            if (!masteredRessursStateRepository.isMastered(state.navident())) {
                logger.info("Ressurs with navident {} is not mastered", state.navident());
                return ressursEvent.getMetadata().inputMetadata().origination().initiatedFrom().contains(":nom-ui2");
            }
            logger.info("Ressurs with navident {} is mastered", state.navident());
            return true;
        }).orElse(false);
    }
}