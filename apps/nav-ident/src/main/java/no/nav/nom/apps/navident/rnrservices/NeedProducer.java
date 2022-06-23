package no.nav.nom.apps.navident.rnrservices;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.navident.domain.IdentType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
public class NeedProducer {
    final KafkaTemplate<String, Need> kafkaTemplate;

    @Value("${app.kafka.topic.rnr.name}")  private String topic;


    public void produceNeed(char idPrefix, String personid, IdentType type) {
        produceNeed(idPrefix, personid, type, LocalDate.now());

    }

    public void produceNeed(char idPrefix, String personid, IdentType type, LocalDate fraDato) {
        Need need = Need.builder()
                .idPrefix(idPrefix)
                .personid(personid)
                .identType(type)
                .build();
        kafkaTemplate.send(topic, need);

        log.debug("Publisert Need melding: %s".formatted(need.maskedToString()));
    }
}
