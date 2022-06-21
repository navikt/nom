package no.nav.nom.apps.navident.rnrservices.util;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.navident.rnrservices.Need;
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

    public boolean personOppslagVisited() {
        return needsRead.stream().anyMatch(n -> n.isPersonOppslagVisited());
    }

    public Need getNeed_personOppslagVisited() {
        return needsRead.stream().filter(n -> n.isPersonOppslagVisited()).findFirst().get();
    }

    public boolean pdlOppslagVisited() {
        return needsRead.stream().anyMatch(n -> n.isPdlOppslagVisited());
    }

    public Need getNeed_pdlOppslagVisited() {
        return needsRead.stream().filter(n -> n.isPdlOppslagVisited()).findFirst().get();
    }

    public boolean idGeneratorVisited() {
        return needsRead.stream().anyMatch(n -> n.isIdGeneratorVisited());
    }

    public Need getNeed_idGeneratorVisited() {
        return needsRead.stream().filter(n -> n.isIdGeneratorVisited()).findFirst().get();
    }

    public boolean responsProdusentVisited() {
        return needsRead.stream().anyMatch(n -> n.isResponsProdusentVisited());
    }

    public Need getNeed_responsProdusentVisited() {
        return needsRead.stream().filter(n -> n.isResponsProdusentVisited()).findFirst().get();
    }
}
