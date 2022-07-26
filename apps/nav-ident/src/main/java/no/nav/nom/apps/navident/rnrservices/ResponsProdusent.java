package no.nav.nom.apps.navident.rnrservices;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.navident.common.cache.RnRMdcCache;
import no.nav.nom.apps.navident.domain.IdentType;
import no.nav.nom.apps.navident.domain.Navident;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class ResponsProdusent {
    final KafkaTemplate<String, Need> kafkaTemplate;
    private final Map<String, Navident> navidenter;
    private final RnRMdcCache mdcCache;

    @Value("${app.kafka.topic.rnr.name}")  private String topic;

    @KafkaListener(
            id = "ResponsProdusent",
            topics = "${app.kafka.topic.rnr.name}")
    public void listen(Need need) {
        mdcCache.loadMdc(need.getUuid());

        log.debug("Got Need %s".formatted(need.maskedToString()));

        if (need.responsProdusentVisited || !need.personOppslagVisited || (need.eksisterendeId == null && !need.idGeneratorVisited) ) {
            log.debug("Nothing to do - skipping message");
            return;
        }
        String id = null;
        HttpStatus code = null;
        String feilmelding = null;
        if (need.eksisterendeId != null) {
            id = need.eksisterendeId;
            var ident = navidenter.get(need.eksisterendeId);
            if (need.identType == IdentType.STANDARD && (need.pdlPersonider == null || need.pdlPersonider.isEmpty())) {
                code = HttpStatus.NOT_FOUND;
                feilmelding = "Angitt personid for STANDARD ident finnes ifra før, men finnes ikke i PDL eller PDL oppslag feilet";
            } else {
                code = HttpStatus.OK;
            }
        } else if (need.identType == IdentType.STANDARD && (need.pdlPersonider == null || need.pdlPersonider.isEmpty())) {
            code = HttpStatus.NOT_FOUND;
            id = need.generertId;
            feilmelding = "Angitt personid for STANDARD ident finnes ikke ifra før, og finnes heller ikke i PDL eller PDL oppslag feilet";
        } else if (need.generertId == null && !need.pdlPersonider.isEmpty()) {
            code = HttpStatus.INTERNAL_SERVER_ERROR;
            feilmelding = "Ukjent feil - Angitt personid hverken ikke funnet som eksisterende navident ei heller generert ny";
        } else {
            code = HttpStatus.CREATED;
            id = need.generertId;
        }


        Respons respons = Respons.builder()
                .httpStatus(code)
                .body(
                    Respons.Body.builder()
                            .navident(id)
                            .feilmelding(feilmelding)
                            .build()
                )
                .build();

        need.respons = respons;

        // Need er oppdatert - publiser på nytt
        need.responsProdusentVisited = true;
        need.visited++;
        kafkaTemplate.send(topic, need);

        log.debug("Publisert Need melding: %s".formatted(need.maskedToString()));

        MDC.clear();
    }
}
