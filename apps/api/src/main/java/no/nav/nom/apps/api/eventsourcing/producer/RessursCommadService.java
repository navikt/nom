package no.nav.nom.apps.api.eventsourcing.producer;

import no.nav.nom.apps.api.eventsourcing.producer.commands.OpprettRessursCommand;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.RessursOpprettetEventV1;
import org.springframework.stereotype.Service;

@Service
public class RessursCommadService {

    private final EventProducer eventProducer;

    public RessursCommadService(EventProducer eventProducer) {
        this.eventProducer = eventProducer;
    }

    public void createRessursEvent(OpprettRessursCommand opprettRessursCommand, InputMetadata inputMetadata){
        // TODO validate command
        eventProducer.sendMessage(
                new RessursOpprettetEventV1(
                        new RessursOpprettetEventV1.RessursRegisteredEventData(
                                opprettRessursCommand.personident(),
                                opprettRessursCommand.navident(),
                                opprettRessursCommand.sektor(),
                                opprettRessursCommand.visningsnavn()),
                        inputMetadata
                )
        );
    }

}
