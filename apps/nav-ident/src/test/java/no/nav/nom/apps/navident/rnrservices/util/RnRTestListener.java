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

    public boolean personOppslagVisited(String personid) {
        return needsRead.stream().anyMatch(n -> n.isPersonOppslagVisited()  && n.getPersonid().equals(personid));
    }

    public Need getNeed_personOppslagVisited(String personid) {
        return needsRead.stream().filter(n -> n.isPersonOppslagVisited()  && n.getPersonid().equals(personid)).findFirst().get();
    }

    public boolean pdlOppslagVisited(String personid) {
        return needsRead.stream().anyMatch(n -> n.isPdlOppslagVisited()  && n.getPersonid().equals(personid));
    }

    public Need getNeed_pdlOppslagVisited(String personid) {
        return needsRead.stream().filter(n -> n.isPdlOppslagVisited()  && n.getPersonid().equals(personid)).findFirst().get();
    }

    public boolean idGeneratorVisited(String personid) {
        return needsRead.stream().anyMatch(n -> n.isIdGeneratorVisited()  && n.getPersonid().equals(personid));
    }

    public Need getNeed_idGeneratorVisited(String personid) {
        return needsRead.stream().filter(n -> n.isIdGeneratorVisited()  && n.getPersonid().equals(personid)).findFirst().get();
    }

    public boolean responsProdusentVisited(String personid) {
        return needsRead.stream().anyMatch(n -> n.isResponsProdusentVisited()  && n.getPersonid().equals(personid));
    }

    public Need getNeed_responsProdusentVisited(String personid) {
        return needsRead.stream().filter(n -> n.isResponsProdusentVisited() && n.getPersonid().equals(personid) ).findFirst().get();
    }
}
