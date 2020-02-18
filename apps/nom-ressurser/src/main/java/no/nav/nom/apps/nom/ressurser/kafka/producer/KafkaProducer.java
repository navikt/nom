package no.nav.nom.apps.nom.ressurser.kafka.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.contract.nom.dto.RessursDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducer {

    @Value("${kafka.topic.ressurs.name}")
    private String ressursTopic;

    private final KafkaTemplate<String, RessursDto> kafkaTemplate;

    public void sendRessursMelding(RessursDto ressurs){
        kafkaTemplate.send(ressursTopic, ressurs.getPersonident(), ressurs);
    }
}
