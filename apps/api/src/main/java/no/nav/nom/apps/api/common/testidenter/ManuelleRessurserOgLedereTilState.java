package no.nav.nom.apps.api.common.testidenter;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.lucenesearch.LuceneService;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.orgenhetleder.OrgEnhetLederService;
import no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateRepository;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class ManuelleRessurserOgLedereTilState {
    final ManuelleRessurserOgLedereProperties cfg;
    final RessursStateRepository nomStateRessursRepository;
    final OrgtilknytningStateRepository nomStateOrgtilknytningRepository;
    final OrgenhetService orgenhetService;
    final OrgEnhetLederService orgEnhetLederService;
    final DvhRepository dvhRepository;
    final LuceneService luceneService;

    public ManuelleRessurserOgLedereTilState(
        ManuelleRessurserOgLedereProperties cfg,
        @Qualifier("nomStateRessursRepository") RessursStateRepository nomStateRessursRepository,
        @Qualifier("nomStateOrgtilknytningRepository") OrgtilknytningStateRepository nomStateOrgtilknytningRepository,
        OrgenhetService orgenhetService, OrgEnhetLederService orgEnhetLederService, DvhRepository dvhRepository,
        LuceneService luceneService
    ) {
        this.cfg = cfg;
        this.nomStateRessursRepository = nomStateRessursRepository;
        this.nomStateOrgtilknytningRepository = nomStateOrgtilknytningRepository;
        this.orgenhetService = orgenhetService;
        this.orgEnhetLederService = orgEnhetLederService;
        this.dvhRepository = dvhRepository;
        this.luceneService = luceneService;
    }

    public void oppdater() {
        if (cfg.enabled()) {
            log.info("Oppdaterer manuelle ressurser og ledere");
            cfg.ressurser().forEach(r -> {
                if (r.navident() != null && r.orgtilknytning() != null) {
                    nomStateRessursRepository.update(r.asState());
                    nomStateOrgtilknytningRepository.update(r.orgtilknytningFid(), r.asOrgtilknytningState());
                    luceneService.invalidateRessurs(r.navident());
                } else {
                    log.warn("Manuell ressurs mangler navident eller orgtilknytning: {}", r);
                }
            });
            cfg.ledere().forEach(l -> {
                if (l.navident() != null && l.orgenhet() != null) {
                    orgEnhetLederService.persisterLedereForOrgenheter(List.of(l.asDvhLeder(orgenhetService)));
                } else {
                    log.warn("Manuell leder mangler navident eller orgenhet: {}", l);
                }
            });
        }
    }
}
