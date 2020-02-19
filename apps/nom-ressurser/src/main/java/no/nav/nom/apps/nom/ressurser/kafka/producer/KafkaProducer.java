package no.nav.nom.apps.nom.ressurser.kafka.producer;

import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.contract.nom.dto.RessursDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaProducer {

    @Value("${kafka.topic.ressurs.name}")
    private String ressursTopic;

    private final KafkaProducerMetrics kafkaProducerMetrics;

    private final KafkaTemplate<String, RessursDto> kafkaTemplate;

    @Timed(value = "kafka.messages.produced.timed", description = "Time spent sending kafka messages", extraTags = {"topic", "aapen-nom-ressurs-v1"})
    public void sendRessursMelding(RessursDto ressurs){
        kafkaTemplate.send(ressursTopic, ressurs.getPersonident(), ressurs);
        kafkaProducerMetrics.sendMeldingMetric(ressursTopic);
    }
}
