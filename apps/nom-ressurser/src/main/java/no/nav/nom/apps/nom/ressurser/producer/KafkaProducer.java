package no.nav.nom.apps.nom.ressurser.producer;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
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

    private final MeterRegistry meterRegistry;
    private final KafkaTemplate<String, RessursDto> kafkaTemplate;

    @Value("${app.kafka.topic.ressurs.name}")
    private String ressursTopic;

    @Timed(value = "kafka.messages.produced.timed", description = "Time spent sending kafka messages", extraTags = {"topic", "aapen-nom-ressurs-v1"})
    public void send(RessursDto ressurs) {
        kafkaTemplate.send(ressursTopic, ressurs.getPersonident(), ressurs);
        meterRegistry.counter("kafka.messages.produced", "topic", ressursTopic).increment();
    }
}
