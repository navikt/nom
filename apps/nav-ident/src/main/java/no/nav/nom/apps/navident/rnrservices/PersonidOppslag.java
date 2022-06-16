package no.nav.nom.apps.navident.rnrservices;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.navident.domain.IdentType;
import no.nav.nom.apps.navident.domain.Navident;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
@Slf4j
public class PersonidOppslag {
    private final Map<String, Navident> navidenter;
    final KafkaTemplate<String, Need> kafkaTemplate;

    @Value("${app.kafka.topic.rnr.name}")  private String topic;

    // Oppdatert Need melding med info om eksisterende NavIdent for angitt personid
    // Kryss med alle personid'er fra PDL.
    // Finnes ikke NavIdent ifra før sett need.eksisterendeId til tom streng
    // Finnes den fra før sett den til å være id'en til eksisterende NavIdent

    @KafkaListener(
            id = "PersonidOppslagNeedListener",
            topics = "${app.kafka.topic.rnr.name}")
    public void listen(Need need) {
        log.debug("PersonidOppslagNeedListener: Got Need for %s".formatted(need.maskedToString()));

        if (
                (need.eksisterendeId != null) // Vi har allerede svart
                || (need.identType == IdentType.STANDARD && (need.pdlPersonider == null || need.pdlPersonider.size() == 0)) // Vi må vente på pdl sine personid'er
        ) {
            log.debug("Nothing to do - skipping message");
            return;
        }


        Stream<Navident> eksisterendeNavidenter;
        if (need.identType != IdentType.STANDARD) {
            eksisterendeNavidenter = navidenter.values().stream().filter(ident -> ident.is(need.personid));
        } else {
            // Finn alle NavIdenter hvor en eller flere av Need's pdlPersonid'er finnes i NavIdent.allePersonid'er
            eksisterendeNavidenter = navidenter.values().stream().filter(ident -> need.pdlPersonider.stream().anyMatch(pdlId -> ident.is(pdlId)));

            // TODO: Nå har vi både NavIdenter.allePersonid'er og nye pdlPersonid'er -> Oppdater Domene via EventSource med korrekt sett av personid'er
        }

        if (eksisterendeNavidenter.count() == 0) {
            need.setEksisterendeId("");
        } else if (eksisterendeNavidenter.count() == 1) {
            need.setEksisterendeId(eksisterendeNavidenter.findFirst().get().getNavident());
        } else { // Found multiple for same personId
            log.error("Det finnes flere NavIdenter for samme personid. Plukker den som har nyeste benyttet dato i sine perioder");
            need.setEksisterendeId(
                    eksisterendeNavidenter.sorted((id1, id2) -> id2.senestePeriodeDato().compareTo(id2.senestePeriodeDato()))
                            .findFirst().get().getNavident()
            );
        }

        // Need er oppdatert - publiser på nytt
        need.visited++;
        kafkaTemplate.send(topic, need);

        log.debug("Publisert Need melding: %s".formatted(need.maskedToString()));
    }
}
