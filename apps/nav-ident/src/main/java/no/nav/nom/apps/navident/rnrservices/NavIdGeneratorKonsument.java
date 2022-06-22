package no.nav.nom.apps.navident.rnrservices;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.navident.idgenerator.NavidGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NavIdGeneratorKonsument {
    final KafkaTemplate<String, Need> kafkaTemplate;
    private final NavidGenerator idGenerator;

    @Value("${app.kafka.topic.rnr.name}")  private String topic;

    @KafkaListener(
            id = "NavIdGeneratorKonsument",
            topics = "${app.kafka.topic.rnr.name}")
    public void listen(Need need) {
        log.debug("Got Need %s".formatted(need.maskedToString()));

        if (need.idGeneratorVisited || !need.personOppslagVisited || need.eksisterendeId != null) {
            log.debug("Nothing to do - skipping message");
            return;
        }

        need.setGenerertId(idGenerator.generate(need.idPrefix));

        // Need er oppdatert - publiser på nytt
        need.idGeneratorVisited = true;
        need.visited++;
        kafkaTemplate.send(topic, need);

        log.debug("Publisert Need melding: %s".formatted(need.maskedToString()));
    }
}
