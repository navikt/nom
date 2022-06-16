package no.nav.nom.apps.navident.rnrservices;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class RnRTestListener {

    public final List<Need> needsRead = new ArrayList<>();

    @KafkaListener(
            id = "rnrTestListener",
            topics = "${app.kafka.topic.rnr.name}")
    public void listen(Need need) {
        needsRead.add(need);
        log.debug("Need consumed from rnr topic: %s".formatted(need));
    }
}
