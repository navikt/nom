package no.nav.nom.apps.api.orgtilknytning.dvh;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.implementations.OrgenhetService;
import no.nav.nom.apps.api.orgtilknytning.OrgtilknytningStateService;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.OrgtilknytningCommandService;
import no.nav.nom.apps.api.remedy.RemedyEnhetMapperService;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EksterneRessurserProcessor {

    private final RemedyEnhetMapperService remedyEnhetMapperService;
    private final OrgtilknytningStateService orgtilknytningStateService;
    private final OrgenhetService orgenhetService;
    private final OrgtilknytningCommandService orgtilknytningCommandService;

    public EksterneRessurserProcessor(RemedyEnhetMapperService remedyEnhetMapperService, OrgtilknytningStateService orgtilknytningStateService,
                                      OrgenhetService orgenhetService,
                                      OrgtilknytningCommandService orgtilknytningCommandService
    ) {
        this.remedyEnhetMapperService = remedyEnhetMapperService;
        this.orgtilknytningStateService = orgtilknytningStateService;
        this.orgenhetService = orgenhetService;
        this.orgtilknytningCommandService = orgtilknytningCommandService;
    }

    public void process(DvhEksternRessurs eksternRessurs) {
        log.debug("Prosesserer ekstern ressurs {}\n{}", eksternRessurs.navident(), eksternRessurs.payload());
    }
}
