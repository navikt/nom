package no.nav.nom.apps.nom.ressurser.kafka.producer;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KafkaProducerMetrics {

    private final MeterRegistry meterRegistry;

    public void sendMeldingMetric(String topic){
        meterRegistry.counter("kafka.messages.produced", "topic", topic).increment();
    }
}
