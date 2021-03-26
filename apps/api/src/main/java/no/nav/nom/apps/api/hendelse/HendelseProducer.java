package no.nav.nom.apps.api.hendelse;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.common.AppProperties;
import no.nav.nom.contract.nom.hendelse.HendelseTo;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class HendelseProducer {

    private final AppProperties appProperties;
    private final ObjectMapper om;
    private final KafkaTemplate<String, String> template;
    private final Counter kafkaHendelseProducedCounter;

    @SneakyThrows
    public void produceHendelse(HendelseTo hendelseTo) {
        var topic = appProperties.getHendelse().getTopic();

        var key = hendelseTo.getId().toString();
        var message = om.writeValueAsString(hendelseTo);
        var record = new ProducerRecord<>(topic, key, message);
        template.send(record);
        kafkaHendelseProducedCounter.increment();
    }
}
