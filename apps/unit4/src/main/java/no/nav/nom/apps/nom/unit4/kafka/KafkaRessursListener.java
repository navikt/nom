package no.nav.nom.apps.nom.unit4.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.nom.unit4.unit4.Unit4Client;
import no.nav.nom.apps.nom.unit4.unit4.Unit4RessursDto;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaRessursListener {

    private final Unit4Client unit4Client;

    @KafkaListener(id="test", topics = "nom-ressurser", autoStartup = "true")
    public void listener(NomRessursDto record, @Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) String uuid) throws IllegalArgumentException {
        log.debug("Received message uuid: {}", uuid);

        try {
            unit4Client.createRessursByProxy(new Unit4RessursDto(record));
        }
        catch (Exception ex){

        }
    }
}
