package no.nav.nom.apps.api.ressurs.eventsourcing.producer;

import io.getunleash.Unleash;
import no.nav.nom.apps.api.common.PrefixedUlidCreator;
import no.nav.nom.apps.api.pdl.Person;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OppdaterFysiskeBrukerstederCommand;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OppdaterPrimaryTelefonnummerCommand;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OppdaterPersonaliaCommand;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OppdaterPersonidentCommand;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OppdaterSektorCommand;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OppdaterSluttdatoCommand;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OppdaterStartdatoCommand;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OpphorRessursCommand;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OpprettRessursCommand;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.SlettRessursCommand;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.RessursEvent;
import no.nav.nom.contract.nom.eventsource.RessursFysiskeBrukerstederOppdatertEventV1;
import no.nav.nom.contract.nom.eventsource.RessursPrimaryTelefonnummerOppdatertEventV1;
import no.nav.nom.contract.nom.eventsource.RessursOpprettetEventV1;
import no.nav.nom.contract.nom.eventsource.RessursPersonaliaOppdatertV1;
import no.nav.nom.contract.nom.eventsource.RessursPersonidentOppdatertV1;
import no.nav.nom.contract.nom.eventsource.RessursSektorOppdatertV1;
import no.nav.nom.contract.nom.eventsource.RessursSluttdatoOppdatertEventV1;
import no.nav.nom.contract.nom.eventsource.RessursSluttetEventV1;
import no.nav.nom.contract.nom.eventsource.RessursStartDatoOppdatertV1;
import no.nav.nom.contract.nom.eventsource.RessursTekniskSlettetEventV1;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.ressurs.NomStateRessursListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


@Service
public class RessursCommandService {

    private final Logger log = LoggerFactory.getLogger(RessursCommandService.class);
    private final RessursEventProducer eventProducer;
    private final NomStateRessursListener nomStateRessursListener;
    private final Unleash unleash;

    public RessursCommandService(
        RessursEventProducer eventProducer,
        NomStateRessursListener nomStateRessursListener, Unleash unleash
    ) {
        this.eventProducer = eventProducer;
        this.nomStateRessursListener = nomStateRessursListener;
        this.unleash = unleash;
    }

    public CompletableFuture<SendResult<String, RessursEvent>> createRessursEvent(OpprettRessursCommand opprettRessursCommand) {
        // TODO validate command
        return createRessurs(PrefixedUlidCreator.createRessursUlid(), opprettRessursCommand);
    }

    public RessursState createRessursSync(OpprettRessursCommand opprettRessursCommand) throws ExecutionException, InterruptedException, TimeoutException {
        boolean exceptionThrown = false;
        final var fid = PrefixedUlidCreator.createRessursUlid();
        final var response = nomStateRessursListener.waitForStateAfterEventCreation(fid);
        createRessurs(fid, opprettRessursCommand);

        try {
            return response.get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            exceptionThrown = true;
            nomStateRessursListener.removeWait(fid);
            log.error("Error waiting for ressurs to be created", e);
            throw e;
        } finally {
            if (exceptionThrown) {
                slettRessursCommand(new SlettRessursCommand(fid), opprettRessursCommand.inputMetadata());
            }
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

    public CompletableFuture<SendResult<String, RessursEvent>> oppdaterRessursSluttdatoEvent(OppdaterSluttdatoCommand oppdaterSluttdatoCommand) {
        return eventProducer.sendMessage(new RessursSluttdatoOppdatertEventV1(
            oppdaterSluttdatoCommand.fid(),
            new RessursSluttdatoOppdatertEventV1.RessursSluttdatoOppdatertEventData(oppdaterSluttdatoCommand.sluttdato()),
            oppdaterSluttdatoCommand.inputMetadata()
        ));
    }


    public CompletableFuture<SendResult<String, RessursEvent>> oppdaterPersonaliaEvent(
        OppdaterPersonaliaCommand oppdaterPersonaliaCommand,
        InputMetadata inputMetadata
    ) {
        // TODO validate command
        return eventProducer.sendMessage(new RessursPersonaliaOppdatertV1(
            oppdaterPersonaliaCommand.fid(),
            new RessursPersonaliaOppdatertV1.PersonaliaOppdatertEventData(
                oppdaterPersonaliaCommand.visningsnavn()
            ),
            inputMetadata
        ));
    }

    public CompletableFuture<SendResult<String, RessursEvent>> oppdatertPrimaryTelefonnummer(
        OppdaterPrimaryTelefonnummerCommand oppdaterPrimaryTelefonnummerCommand,
        InputMetadata inputMetadata
    ) {
        return eventProducer.sendMessage(new RessursPrimaryTelefonnummerOppdatertEventV1(
            oppdaterPrimaryTelefonnummerCommand.fid(),
            new RessursPrimaryTelefonnummerOppdatertEventV1.PrimaryTelefonnummerOppdatertEventData(oppdaterPrimaryTelefonnummerCommand.primaryTelefonnummer()),
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
                opprettRessursCommand.visningsnavn(),
                opprettRessursCommand.primaryTelefonnummer(),
                opprettRessursCommand.fysiskbrukerStedNr() == null ? Collections.emptyList() : List.of(opprettRessursCommand.fysiskbrukerStedNr()),
                opprettRessursCommand.fysiskbrukerStedNr()
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

    public CompletableFuture<SendResult<String, RessursEvent>> oppdaterPersonidentCommand(
        OppdaterPersonidentCommand oppdaterPersonidentCommand,
        InputMetadata inputMetadata
    ) {
        return eventProducer.sendMessage(new RessursPersonidentOppdatertV1(
            oppdaterPersonidentCommand.fid(),
            new RessursPersonidentOppdatertV1.PersonidentOppdatertEventData(
                oppdaterPersonidentCommand.personident()
            ),
            inputMetadata
        ));
    }


    // Dette er en administrativ kommando - brukes med care :-)
    public CompletableFuture<SendResult<String, RessursEvent>> slettRessursCommand(SlettRessursCommand slettRessursCommand, InputMetadata inputMetadata) {
        return eventProducer.sendMessage(new RessursTekniskSlettetEventV1(
            slettRessursCommand.fid(),
            new RessursTekniskSlettetEventV1.RessursTekniskSlettetEventData(),
            inputMetadata
        ));
    }

    public CompletableFuture<SendResult<String, RessursEvent>> endreStartRessursEvent(
        OppdaterStartdatoCommand oppdaterStartdatoCommand,
        InputMetadata inputMetadata
    ) {
        return eventProducer.sendMessage(new RessursStartDatoOppdatertV1(
            oppdaterStartdatoCommand.fid(),
            new RessursStartDatoOppdatertV1.StartDatoOppdatertEventData(oppdaterStartdatoCommand.startdato()),
            inputMetadata
        ));
    }

    public CompletableFuture<SendResult<String, RessursEvent>> oppdaterFysiskeBrukerstederCommand(
        OppdaterFysiskeBrukerstederCommand oppdaterFysiskeBrukerstederCommand, InputMetadata inputMetadata
    ) {
        return eventProducer.sendMessage(new RessursFysiskeBrukerstederOppdatertEventV1(
            oppdaterFysiskeBrukerstederCommand.fid(),
            RessursFysiskeBrukerstederOppdatertEventV1.RessursFysiskeBrukerstederOppdatertEventData.builder()
                .primaryFysiskBrukersted(oppdaterFysiskeBrukerstederCommand.primaryFysiskBrukersted())
                .fysiskeBrukersteder(oppdaterFysiskeBrukerstederCommand.fysiskeBrukersteder().stream().toList())
                .build(),
            inputMetadata
        ));
    }

    public CompletableFuture<SendResult<String, RessursEvent>> oppdaterRessursStartdatoEvent(
        OppdaterStartdatoCommand oppdaterStartdatoCommand,
        InputMetadata inputMetadata
    ) {
        return eventProducer.sendMessage(new RessursStartDatoOppdatertV1(
                oppdaterStartdatoCommand.fid(),
                new RessursStartDatoOppdatertV1.StartDatoOppdatertEventData(oppdaterStartdatoCommand.startdato())
                , inputMetadata
            )
        );
    }
}
