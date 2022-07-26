package no.nav.nom.apps.navident.rnrservices;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.navident.common.cache.RnRMdcCache;
import no.nav.nom.apps.navident.domain.IdentType;
import no.nav.nom.apps.navident.domain.Navident;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class PersonidOppslag {
    private final Map<String, Navident> navidenter;
    final KafkaTemplate<String, Need> kafkaTemplate;
    private final RnRMdcCache mdcCache;

    @Value("${app.kafka.topic.rnr.name}")  private String topic;

    // Oppdatert Need melding med info om eksisterende NavIdent for angitt personid
    // Kryss med alle personid'er fra PDL.
    // Finnes ikke NavIdent ifra før sett need.eksisterendeId til tom streng
    // Finnes den fra før sett den til å være id'en til eksisterende NavIdent

    @KafkaListener(
            id = "PersonidOppslagNeedListener",
            topics = "${app.kafka.topic.rnr.name}")
    public void listen(Need need) {
        mdcCache.loadMdc(need.getUuid());

        log.debug("Got Need %s".formatted(need.maskedToString()));

        if (
                (need.personOppslagVisited) // Vi har allerede svart
                || (need.identType == IdentType.STANDARD && !need.pdlOppslagVisited ) // Vi må vente på pdl sine personid'er
        ) {
            log.debug("Nothing to do - skipping message");
            return;
        }

        List<Navident> eksisterendeNavidenter;
        if (need.identType != IdentType.STANDARD || need.pdlPersonider == null || need.pdlPersonider.isEmpty()) {
            eksisterendeNavidenter = navidenter.values().stream().filter(ident -> ident.is(need.personid)).collect(Collectors.toList());
        } else {
            // Finn alle NavIdenter hvor en eller flere av Need's pdlPersonid'er finnes i NavIdent.allePersonid'er
            eksisterendeNavidenter = navidenter.values().stream().filter(ident -> need.pdlPersonider.stream().anyMatch(pdlId -> ident.is(pdlId))).collect(Collectors.toList());

            // TODO: Nå har vi både NavIdenter.allePersonid'er og nye pdlPersonid'er -> Oppdater Domene via EventSource med korrekt sett av personid'er
        }

        if (eksisterendeNavidenter.size() == 0) {
            log.debug("Ingen eksisterende NavIdent funnet");
        } else if (eksisterendeNavidenter.size() == 1) {
            need.setEksisterendeId(eksisterendeNavidenter.get(0).getNavident());
        } else { // Found multiple for same personId
            log.error("Det finnes flere NavIdenter, %s, for samme personid. Plukker den som har høyeste navident id".formatted(eksisterendeNavidenter.stream().map(Navident::getNavident).collect(Collectors.toList())));
            need.setEksisterendeId(
                    eksisterendeNavidenter.stream().sorted((id1, id2) -> id2.getNavident().compareTo(id2.getNavident()))
                            .findFirst().get().getNavident()
            );
        }

        // Need er oppdatert - publiser på nytt
        need.personOppslagVisited = true;
        need.visited++;
        kafkaTemplate.send(topic, need);

        log.debug("Publisert Need melding: %s".formatted(need.maskedToString()));

        MDC.clear();
    }
}
