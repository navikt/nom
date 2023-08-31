package no.nav.nom.apps.navident.rnrservices;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.navident.common.cache.RnRClientTokenCache;
import no.nav.nom.apps.navident.common.cache.RnRMdcCache;
import no.nav.nom.contract.nom.IdentType;
import no.nav.nom.apps.navident.domain.Navident;
import no.nav.nom.apps.navident.pdl.PdlService;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class PdlOppslag {
    private final Map<String, Navident> navidenter;
    final KafkaTemplate<String, Need> kafkaTemplate;
    private final PdlService pdlService;
    private final RnRMdcCache mdcCache;
    private final RnRClientTokenCache cache;


    @Value("${app.kafka.topic.rnr.name}")  private String topic;

    @KafkaListener(
            id = "PdlOppslagNeedListener",
            topics = "${app.kafka.topic.rnr.name}")
    public void listen(Need need) {
        mdcCache.loadMdc(need.getUuid());

        log.debug("Got Need %s".formatted(need.maskedToString()));

        if ( need.pdlOppslagVisited || need.identType != IdentType.STANDARD) {
            log.debug("Nothing to do - skipping message");
            return;
        }

        // Since this is async from request there is no SecurityContext.Authentication present.
        // This is needed in Pdl authentication
        cache.setCachedTokedBackOnSecurityContext(need.getUuid());

        List<String> pdlIder = pdlService.getKnownPersonider(need.personid);

        if (pdlIder == null || pdlIder.size() == 0) {
            log.error("PDL kjenner ikke til personId som er angitt, eller oppslag feilet");
            pdlIder = Collections.emptyList();
        } else {
            need.setPdlGjeldendePersonid(pdlIder.get(0));   // Ta den første i listen. Skal være sortert slik at gjeldende folkeregistrert id er først
        }
        need.setPdlPersonider(pdlIder);

        // Need er oppdatert - publiser på nytt
        need.pdlOppslagVisited = true;
        need.visited++;
        kafkaTemplate.send(topic, need);

        log.debug("Publisert Need melding: %s".formatted(need.maskedToString()));
        MDC.clear();
    }
}
