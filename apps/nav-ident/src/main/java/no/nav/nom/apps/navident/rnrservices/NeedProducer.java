package no.nav.nom.apps.navident.rnrservices;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.navident.domain.IdentType;
import no.nav.nom.apps.navident.domain.Personid;
import no.nav.nom.apps.navident.eventsource.SourceEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NeedProducer {
    final KafkaTemplate<String, Need> kafkaTemplate;


    @Value("${app.kafka.topic.rnr.name}")  private String topic;

    public void produceNeed(String personid, IdentType type) {
        Need need = Need.builder()
                .personid(personid)
                .identType(type)
                .build();
        kafkaTemplate.send(topic, need);

        log.debug("Publisert Need melding: %s".formatted(need.maskedToString()));
    }
}
