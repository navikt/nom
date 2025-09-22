package no.nav.nom.apps.api.rest.dolly;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.ressurs.api.app.OpprettRessursRequest;
import no.nav.nom.apps.api.ressurs.api.app.RessursRestService;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DollyAsyncOpprettRessurs {
    private final RessursRestService ressursRestService;

    public DollyAsyncOpprettRessurs(RessursRestService ressursRestService) {
        this.ressursRestService = ressursRestService;
    }

    @Async
    public void opprettRessurs(OpprettRessursRequest request, InputMetadata metaData) {
        log.info("Starter asynkron opprettelse av ressurs for: {} {}", request.visningsfornavn(), request.visningsetternavn());
        ressursRestService.createRessursAndNotify(request, metaData);
    }
}
