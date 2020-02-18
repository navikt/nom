package no.nav.nom.apps.nom.ressurser.kafka.producer;

import lombok.RequiredArgsConstructor;
import no.nav.nom.contract.nom.dto.RessursDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KafkaProducer {

    @Value("${kafka.topic.ressurs.name}")
    private String ressursTopic;

    private final KafkaTemplate<String, RessursDto> kafkaTemplate;

    public void sendRessursMelding(RessursDto ressurs){
        kafkaTemplate.send(ressursTopic, ressurs.getPersonident(), ressurs);
    }
}
