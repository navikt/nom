package no.nav.nom.apps.api.eventsourcing.producer;

import no.nav.nom.apps.api.eventsourcing.producer.commands.OpphorRessursCommand;
import no.nav.nom.apps.api.eventsourcing.producer.commands.OpprettRessursCommand;
import no.nav.nom.apps.api.eventsourcing.state.EventAggregateListener;
import no.nav.nom.contract.nom.eventsource.Event;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.RessursOpprettetEventV1;
import no.nav.nom.contract.nom.eventsource.RessursSluttetEventV1;
import no.nav.nom.contract.nom.state.RessursState;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;

import java.util.UUID;

@Service
public class RessursCommadService {

    private final EventProducer eventProducer;
    private final EventAggregateListener eventAggregateListener;

    public RessursCommadService(EventProducer eventProducer, EventAggregateListener eventAggregateListener) {
        this.eventProducer = eventProducer;
        this.eventAggregateListener = eventAggregateListener;
    }

    public ListenableFuture<SendResult<String, Event>> createRessursEvent(OpprettRessursCommand opprettRessursCommand, InputMetadata inputMetadata){
        // TODO validate command
        return eventProducer.sendMessage(
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

    public ListenableFuture<SendResult<String, Event>> avsluttRessursEvent(OpphorRessursCommand opprettRessursCommand, InputMetadata inputMetadata){
        // TODO validate command
        return eventProducer.sendMessage(
                new RessursSluttetEventV1(
                        new RessursSluttetEventV1.RessursSluttetEventData(
                                opprettRessursCommand.sluttdato()
                        ),
                        inputMetadata
                )
        );
    }

    public RessursState createRessursSync(OpprettRessursCommand opprettRessursCommand, InputMetadata inputMetadata){
        final var fid = UUID.randomUUID();
        final var response = eventAggregateListener.waitForAggregate(fid);

        eventProducer.sendMessage(
                new RessursOpprettetEventV1(
                        fid,
                        new RessursOpprettetEventV1.RessursRegisteredEventData(
                                opprettRessursCommand.personident(),
                                opprettRessursCommand.navident(),
                                opprettRessursCommand.sektor(),
                                opprettRessursCommand.visningsnavn()),
                        inputMetadata
                )
        );
        return response.block();
    }

}
