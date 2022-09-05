package no.nav.nom.apps.nom.unit4.kafka;

import lombok.RequiredArgsConstructor;
import no.nav.nom.apps.nom.unit4.unit4.Unit4Client;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KafkaRessursListener {

    private final Unit4Client unit4Client;

    @KafkaListener(topics = "${app.ressurs.topic}")
    public void onMessage(ConsumerRecord<String, NomRessursDto> record) {

    }

}
