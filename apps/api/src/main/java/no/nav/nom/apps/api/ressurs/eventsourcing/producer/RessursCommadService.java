package no.nav.nom.apps.api.ressurs.eventsourcing.producer;

import no.nav.nom.apps.api.common.PrefixedUlidCreator;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OpphorRessursCommand;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OpprettRessursCommand;
import no.nav.nom.apps.api.ressurs.eventsourcing.state.EventAggregateListener;
import no.nav.nom.contract.nom.eventsource.Event;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.RessursOpprettetEventV1;
import no.nav.nom.contract.nom.eventsource.RessursSluttetEventV1;
import no.nav.nom.contract.nom.state.RessursState;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;

import java.util.Optional;

@Service
public class RessursCommadService {

    private final EventProducer eventProducer;
    private final EventAggregateListener eventAggregateListener;

    public RessursCommadService(EventProducer eventProducer, EventAggregateListener eventAggregateListener) {
        this.eventProducer = eventProducer;
        this.eventAggregateListener = eventAggregateListener;
    }

    public ListenableFuture<SendResult<String, Event>> createRessursEvent(OpprettRessursCommand opprettRessursCommand){
        // TODO validate command
        return createRessurs(PrefixedUlidCreator.createRessursUlid(), opprettRessursCommand);
    }

    public ListenableFuture<SendResult<String, Event>> avsluttRessursEvent(OpphorRessursCommand opprettRessursCommand, InputMetadata inputMetadata){
        // TODO validate command
        return eventProducer.sendMessage(
                new RessursSluttetEventV1(
                        opprettRessursCommand.fid(),
                        new RessursSluttetEventV1.RessursSluttetEventData(
                                opprettRessursCommand.sluttdato()
                        ),
                        inputMetadata
                )
        );
    }

    public RessursState createRessursSync(OpprettRessursCommand opprettRessursCommand){
        final var fid = PrefixedUlidCreator.createRessursUlid();
        final var response = eventAggregateListener.waitForAggregate(fid);
        createRessurs(fid, opprettRessursCommand);

        return response.block();
    }

    private ListenableFuture<SendResult<String, Event>> createRessurs(String fid, OpprettRessursCommand opprettRessursCommand){
        return eventProducer.sendMessage(
                new RessursOpprettetEventV1(
                        fid,
                        new RessursOpprettetEventV1.RessursOpprettetEventData(
                                opprettRessursCommand.personident(),
                                opprettRessursCommand.sektor(),
                                opprettRessursCommand.visningsnavn()),
                        opprettRessursCommand.inputMetadata(),
                        new RessursOpprettetEventV1.RessursOpprettetAdditionalData(
                                Optional.ofNullable(opprettRessursCommand.navident()),
                                opprettRessursCommand.pdlPerson().map(r -> r.getKjonn().name()),
                                opprettRessursCommand.pdlPerson().map(pdlPerson ->
                                        new RessursOpprettetEventV1.RessursOpprettetAdditionalData.FolkeregistrertNavn(
                                                pdlPerson.getFornavn(),
                                                pdlPerson.getMellomnavn(),
                                                pdlPerson.getEtternavn()
                                        )
                                )
                        )
                )
        );
    }

}
