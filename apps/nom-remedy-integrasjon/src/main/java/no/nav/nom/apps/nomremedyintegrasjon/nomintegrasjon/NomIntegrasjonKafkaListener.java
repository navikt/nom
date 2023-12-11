package no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon;

import io.getunleash.Unleash;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningEvent;
import no.nav.nom.contract.nom.eventsource.RessursEvent;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

@Service
public class NomIntegrasjonKafkaListener {

    private final Unleash unleash;
    private final NomIntegrasjonService nomIntegrasjonService;

    public NomIntegrasjonKafkaListener(Unleash unleash, NomIntegrasjonService nomIntegrasjonService) {
        this.unleash = unleash;
        this.nomIntegrasjonService = nomIntegrasjonService;
    }

    @KafkaListener(
        topics = "${app.kafka-topics.ressurs-eventsource.topic-name}",
        autoStartup = "${app.kafka-topics.ressurs-eventsource.auto-startup}",
        containerFactory = "ressursEventKafkaListenerContainerFactory"
    )
    public void ressursEventListener(RessursEvent ressursEvent, @Header(KafkaHeaders.RECEIVED_KEY) String recordId) {
        if (isWriteToRemedyEnabled()) {

            putMDCContext(recordId, ressursEvent.getClass().getSimpleName());

            nomIntegrasjonService.handleRessursEvent(ressursEvent);
        }
    }

    @KafkaListener(
        topics = "${app.kafka-topics.orgtilknytning-eventsource.topic-name}",
        autoStartup = "${app.kafka-topics.orgtilknytning-eventsource.auto-startup}",
        containerFactory = "orgtilknytningEventKafkaListenerContainerFactory"
    )
    public void orgtilknytningEventListener(OrgtilknytningEvent orgtilknytningEvent, @Header(KafkaHeaders.RECEIVED_KEY) String recordId) {
        if (isWriteToRemedyEnabled()) {

            putMDCContext(recordId, orgtilknytningEvent.getClass().getSimpleName());

            nomIntegrasjonService.handleOrgtilknytningEvent(orgtilknytningEvent);
        }
    }

    private void putMDCContext(String recordId, String eventType) {
        MDC.put("record_id", recordId);
        MDC.put("event_type", eventType);
    }

    private boolean isWriteToRemedyEnabled() {
        return unleash.isEnabled("nom-remedy-integrasjon.write_to_remedy");
    }
}
