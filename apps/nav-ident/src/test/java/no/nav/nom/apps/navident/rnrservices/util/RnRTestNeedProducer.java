package no.nav.nom.apps.navident.rnrservices.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.navident.domain.IdentType;
import no.nav.nom.apps.navident.rnrservices.Need;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RnRTestNeedProducer {
    final KafkaTemplate<String, Need> kafkaTemplate;

    @Value("${test.rnr-topic}")
    private String topic;

    public void publish(Need need) {
        kafkaTemplate.send(topic, need);
        log.debug("Publisert Need melding: %s".formatted(need.maskedToString()));
    }
}
