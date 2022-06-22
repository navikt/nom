package no.nav.nom.apps.navident.rnrservices;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.navident.domain.IdentType;
import no.nav.nom.apps.navident.domain.Navident;
import no.nav.nom.apps.navident.domain.Periode;
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

    @Value("${app.kafka.topic.rnr.name}")  private String topic;

    @KafkaListener(
            id = "ResponsProdusent",
            topics = "${app.kafka.topic.rnr.name}")
    public void listen(Need need) {
        log.debug("Got Need %s".formatted(need.maskedToString()));

        if (need.responsProdusentVisited || !need.personOppslagVisited || (need.eksisterendeId == null && !need.idGeneratorVisited) ) {
            log.debug("Nothing to do - skipping message");
            return;
        }
        String id = null;
        Periode periode = null;
        HttpStatus code = null;
        String feilmelding = null;
        if (need.eksisterendeId != null) {
            id = need.eksisterendeId;
            var ident = navidenter.get(need.eksisterendeId);
            var senestePeriode = ident.senestePeriode();
            if (senestePeriode.getTilOgMed() == null) {
                code = HttpStatus.CONFLICT;
                periode = senestePeriode.clone();
                feilmelding = "Navident for angitt personid er allerede aktiv";
            } else if (senestePeriode.getTilOgMed().isAfter(need.fraDato)) {
                code = HttpStatus.CONFLICT;
                periode = senestePeriode.clone();
                feilmelding = "Navident for angitt personid kan ikke settes aktiv fra angitt dato siden dette er i en allerede deaktivert periode";
            } else {
                code = HttpStatus.OK;
                periode = Periode.builder().fraOgMed(need.fraDato).build();
            }
        } else if (need.identType == IdentType.STANDARD && need.generertId != null && (need.pdlPersonider == null || need.pdlPersonider.isEmpty())) {
            code = HttpStatus.NOT_FOUND;
            id = need.generertId;
            periode = Periode.builder().fraOgMed(need.fraDato).build();
            feilmelding = "Angitt personid for STANDARD ident finnes ikke ifra før, og finnes ikke i PDL eller PDL oppslag feilet";
        } else if (need.generertId == null) {
            code = HttpStatus.INTERNAL_SERVER_ERROR;
            feilmelding = "Ukjent feil - hverken ikke funnet eksisterende navident ei heller generert ny";
        } else {
            code = HttpStatus.CREATED;
            periode = Periode.builder().fraOgMed(need.fraDato).build();
            id = need.generertId;
        }


        Respons respons = Respons.builder()
                .httpStatus(code)
                .body(
                    Respons.Body.builder()
                            .navident(id)
                            .aktivFra(periode != null ? periode.getFraOgMed() : null)
                            .aktivTil(periode != null ? periode.getTilOgMed() : null)
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
    }
}
