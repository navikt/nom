package no.nav.nom.apps.nomremedyintegrasjon;

import no.nav.nom.contract.nom.eventsource.RessursEvent;
import no.nav.nom.contract.nom.eventsource.RessursOpprettetEventV1;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

@Service
public class NomIntegrasjonKafkaListener {

    private final Logger logger = LoggerFactory.getLogger(NomIntegrasjonKafkaListener.class);

    @KafkaListener(topics = "${app.kafka-topics.ressurs-eventsource}")
    public void listener(RessursEvent record, @Header(KafkaHeaders.RECEIVED_KEY) String recordId) {
        if (record instanceof RessursOpprettetEventV1) {
            logger.info("Received: " + record.getFid());
        }
    }
}
