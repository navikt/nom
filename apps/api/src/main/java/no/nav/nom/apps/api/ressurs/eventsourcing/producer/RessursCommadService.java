package no.nav.nom.apps.api.ressurs.eventsourcing.producer;

import no.nav.nom.apps.api.common.PrefixedUlidCreator;
import no.nav.nom.apps.api.pdl.Person;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OppdaterPersonaliaCommand;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OppdaterSektorCommand;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OpphorRessursCommand;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OpprettRessursCommand;
import no.nav.nom.apps.api.ressurs.eventsourcing.state.RessursStateListener;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.RessursEvent;
import no.nav.nom.contract.nom.eventsource.RessursOpprettetEventV1;
import no.nav.nom.contract.nom.eventsource.RessursPersonaliaOppdatertV1;
import no.nav.nom.contract.nom.eventsource.RessursSektorOppdatertV1;
import no.nav.nom.contract.nom.eventsource.RessursSluttetEventV1;
import no.nav.nom.contract.nom.state.RessursState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class RessursCommadService {

    private final Logger log = LoggerFactory.getLogger(RessursCommadService.class);
    private final RessursEventProducer eventProducer;
    private final RessursStateListener ressursStateListener;

    public RessursCommadService(
        RessursEventProducer eventProducer,
        RessursStateListener ressursStateListener
    ) {
        this.eventProducer = eventProducer;
        this.ressursStateListener = ressursStateListener;
    }

    public CompletableFuture<SendResult<String, RessursEvent>> createRessursEvent(OpprettRessursCommand opprettRessursCommand) {
        // TODO validate command
        return createRessurs(PrefixedUlidCreator.createRessursUlid(), opprettRessursCommand);
    }

    public RessursState createRessursSync(OpprettRessursCommand opprettRessursCommand) throws ExecutionException, InterruptedException, TimeoutException {
        final var fid = PrefixedUlidCreator.createRessursUlid();
        final var response = ressursStateListener.waitForAggregate(fid);
        createRessurs(fid, opprettRessursCommand);

        try{
            return response.get(10, TimeUnit.SECONDS);
        } catch (Exception e){
            ressursStateListener.removeWait(fid);
            log.error("Error waiting for ressurs to be created ", e);
            throw e;
        }
    }

    public CompletableFuture<SendResult<String, RessursEvent>> avsluttRessursEvent(OpphorRessursCommand opphorRessursCommand, InputMetadata inputMetadata) {
        // TODO validate command
        return eventProducer.sendMessage(new RessursSluttetEventV1(
                opphorRessursCommand.fid(),
                new RessursSluttetEventV1.RessursSluttetEventData(opphorRessursCommand.sluttdato()),
                inputMetadata
        ));
    }

    public CompletableFuture<SendResult<String, RessursEvent>> oppdaterPersonaliaEvent(OppdaterPersonaliaCommand oppdaterPersonaliaCommand, InputMetadata inputMetadata) {
        // TODO validate command
        return eventProducer.sendMessage(new RessursPersonaliaOppdatertV1(
                oppdaterPersonaliaCommand.fid(),
                new RessursPersonaliaOppdatertV1.PersonaliaOppdatertEventData(
                        oppdaterPersonaliaCommand.visningsnavn()
                ),
                inputMetadata
        ));
    }

    public CompletableFuture<SendResult<String, RessursEvent>> oppdaterSektorEvent(OppdaterSektorCommand oppdaterSektorCommand, InputMetadata inputMetadata) {
        return eventProducer.sendMessage(new RessursSektorOppdatertV1(
            oppdaterSektorCommand.fid(),
            new RessursSektorOppdatertV1.SektorOppdatertEventData(
                oppdaterSektorCommand.sektor()
            ),
            inputMetadata
        ));
    }


    private CompletableFuture<SendResult<String, RessursEvent>> createRessurs(String fid, OpprettRessursCommand opprettRessursCommand) {

        return eventProducer.sendMessage(new RessursOpprettetEventV1(
            fid,
            new RessursOpprettetEventV1.RessursOpprettetEventData(
                opprettRessursCommand.personident(),
                opprettRessursCommand.sektor(),
                opprettRessursCommand.visningsnavn()
                ),
                opprettRessursCommand.inputMetadata(),
                new RessursOpprettetEventV1.RessursOpprettetAdditionalData(
                        Optional.ofNullable(opprettRessursCommand.navident()),
                        opprettRessursCommand.pdlPerson().map(r -> r.getKjonn().name()),
                        opprettRessursCommand.pdlPerson()
                                .map(pdlPerson -> new RessursOpprettetEventV1.RessursOpprettetAdditionalData.FolkeregistrertNavn(
                                        pdlPerson.getFornavn(),
                                        pdlPerson.getMellomnavn(),
                                        pdlPerson.getEtternavn()
                                )),
                        opprettRessursCommand.pdlPerson().map(Person::getFoedselsdato),
                        Optional.ofNullable(opprettRessursCommand.startdato()),
                        Optional.ofNullable(opprettRessursCommand.orgEnhetId()),
                        Optional.ofNullable(opprettRessursCommand.ansettelsesType())
                )
        ));
    }

}
