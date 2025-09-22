package no.nav.nom.apps.api.rest.dolly;

import no.nav.nom.apps.api.ressurs.api.app.OpprettRessursRequest;
import no.nav.nom.apps.api.ressurs.api.app.RessursRestService;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class DollyAsyncOpprettRessurs {
    private final RessursRestService ressursRestService;

    public DollyAsyncOpprettRessurs(RessursRestService ressursRestService) {
        this.ressursRestService = ressursRestService;
    }

    @Async
    public void opprettRessurs(OpprettRessursRequest request, InputMetadata metaData) {
        ressursRestService.createRessursAndNotify(request, metaData);
    }
}
