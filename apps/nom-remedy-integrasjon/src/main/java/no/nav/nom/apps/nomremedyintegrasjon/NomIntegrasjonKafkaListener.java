package no.nav.nom.apps.nomremedyintegrasjon;

import io.getunleash.Unleash;
import no.nav.nom.contract.nom.eventsource.RessursEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

@Service
public class NomIntegrasjonKafkaListener {

    private final Logger logger = LoggerFactory.getLogger(NomIntegrasjonKafkaListener.class);

    private final Unleash unleash;

    public NomIntegrasjonKafkaListener(Unleash unleash) {
        this.unleash = unleash;
    }

    @KafkaListener(topics = "${app.kafka-topics.ressurs-eventsource}")
    public void listener(RessursEvent ressursEvent, @Header(KafkaHeaders.RECEIVED_KEY) String recordId) {
        String msg = "Received: " + ressursEvent.getFid();
        if (isWriteToRemedyEnabled()) {
            logger.info(msg + " writing to remedy is enabled");
        }
        else {
            logger.info(msg + " writing to remedy is not enabled");
        }
    }

    private boolean isWriteToRemedyEnabled() {
        return unleash.isEnabled("nom-remedy-integrasjon.write_to_remedy");
    }
}
