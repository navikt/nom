package no.nav.nom.apps.nom.ressurser.kafka.producer;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KafkaProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public void sendMelding(String topic, String key, String data){
        kafkaTemplate.send(topic, key, data);
    }
}
