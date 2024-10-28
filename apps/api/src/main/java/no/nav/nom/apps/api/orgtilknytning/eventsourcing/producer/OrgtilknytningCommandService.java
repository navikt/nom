package no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.common.PrefixedUlidCreator;
import no.nav.nom.apps.api.graphql.common.SecurityUtil;
import no.nav.nom.apps.api.orgtilknytning.api.email.OTEndretData;
import no.nav.nom.apps.api.orgtilknytning.api.email.OrgtilknytningEndretEmailService;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.AvvikleOrgtilknytning;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.EndreDagligOppfolgingOrgtilknytning;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.EndreDagligOppfolgingOrgtilknytningCommand;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.EndreOrgtilknytningDatoCommand;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.EndreTilknytningtypeCommand;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.FjernOrgtilknytning;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.EndreOrgtilknytningDato;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.OpprettOrgtilknytning;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningAvsluttetEventV1;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningDatoEndretEventV2;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningEvent;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningFjernetEventV1;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningOpprettetEventV1;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningDagligOppfolgingEndretEventV1;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningTilknytningtypeEndretEventV1;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class OrgtilknytningCommandService {
    private final no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.OrgtilknytningEventProducer eventProducer;

    private final OrgtilknytningEndretEmailService orgtilknytningEndretEmailService;

    public OrgtilknytningCommandService(no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.OrgtilknytningEventProducer eventProducer,
                                        OrgtilknytningEndretEmailService orgtilknytningEndretEmailService) {
        this.eventProducer = eventProducer;
        this.orgtilknytningEndretEmailService = orgtilknytningEndretEmailService;
    }

    private void addToOTEndretDataEmailQueue(String navident, String fid, OTEndretData.OTEndretDataType eventType) {
        String changedByNavIdent = SecurityUtil.getAuthenticatedNavIdent();
        orgtilknytningEndretEmailService.addToOrgtilknytningEndringerQueue(
            navident,
            OTEndretData.builder()
                .fid(fid)
                .navident(navident)
                .changedByNavIdent(changedByNavIdent)
                .eventType(eventType)
                .timestamp(DateTimeFormatter.ofPattern("dd.MM.YYYY").format(LocalDate.now()))
                .build()
        );
    }

    public String opprettOrgtilknytning(OpprettOrgtilknytning opprettOrgtilknytning) {
        String fid = PrefixedUlidCreator.createOrgtilknytningUlid();
        opprettOrgtilknytning(fid, opprettOrgtilknytning);
        return fid;
    }

    private CompletableFuture<SendResult<String, OrgtilknytningEvent>> opprettOrgtilknytning(String fid, OpprettOrgtilknytning opprettOrgtilknytning) {
        //update OTendretData for e-post varsel
        addToOTEndretDataEmailQueue(opprettOrgtilknytning.navident(), fid, OTEndretData.OTEndretDataType.OT_NY);
        return eventProducer.sendMessage(new OrgtilknytningOpprettetEventV1(
            fid,
            new OrgtilknytningOpprettetEventV1.OrgtilknytningOpprettetEventData(
                opprettOrgtilknytning.navident(),
                opprettOrgtilknytning.orgenhetId(),
                opprettOrgtilknytning.gyldigFom()
            ),
            opprettOrgtilknytning.inputMetadata(),
            opprettOrgtilknytning.additionalAgressoData()
        ));
    }

    // TODO: Fjern denne metoden og bruk metoden endreDagligOppfolgingOrgtilknytning(EndreDagligOppfolgingOrgtilknytningCommand endreDagligOppfolgingOrgtilknytningCommand) istedet.
    // Grunnen til dette er at det er enklere å lage tester med ArgumentCaptor når fid er inkludert i Command-objektet
    public CompletableFuture<SendResult<String, OrgtilknytningEvent>> endreDagligOppfolgingOrgtilknytning(
        String fid,
        EndreDagligOppfolgingOrgtilknytning endreDagligOppfolgingOrgtilknytning
    ) {

        return endreDagligOppfolgingOrgtilknytning(
            EndreDagligOppfolgingOrgtilknytningCommand.builder()
                .fid(fid)
                .navident(endreDagligOppfolgingOrgtilknytning.navident())
                .orgenhetId(endreDagligOppfolgingOrgtilknytning.orgenhetId())
                .gyldigFom(endreDagligOppfolgingOrgtilknytning.gyldigFom())
                .gyldigTom(endreDagligOppfolgingOrgtilknytning.gyldigTom())
                .erDagligOppfolging(endreDagligOppfolgingOrgtilknytning.dagligOppfolging())
                .inputMetadata(endreDagligOppfolgingOrgtilknytning.inputMetadata())
                .additionalAgressoData(endreDagligOppfolgingOrgtilknytning.additionalAgressoData())
                .build()
        );
    }

    public CompletableFuture<SendResult<String, OrgtilknytningEvent>> endreDagligOppfolgingOrgtilknytning(
        EndreDagligOppfolgingOrgtilknytningCommand endreDagligOppfolgingOrgtilknytningCommand
    ) {
        //update OTendretData for e-post varsel
        addToOTEndretDataEmailQueue(endreDagligOppfolgingOrgtilknytningCommand.navident(), endreDagligOppfolgingOrgtilknytningCommand.fid(), OTEndretData.OTEndretDataType.OT_ENDRET);
        return eventProducer.sendMessage(new OrgtilknytningDagligOppfolgingEndretEventV1(
            endreDagligOppfolgingOrgtilknytningCommand.fid(),
            new OrgtilknytningDagligOppfolgingEndretEventV1.OrgtilknytningDagligOppfolgingEndretEventData(
                endreDagligOppfolgingOrgtilknytningCommand.navident(),
                endreDagligOppfolgingOrgtilknytningCommand.orgenhetId(),
                endreDagligOppfolgingOrgtilknytningCommand.gyldigFom(),
                endreDagligOppfolgingOrgtilknytningCommand.gyldigTom(),
                endreDagligOppfolgingOrgtilknytningCommand.erDagligOppfolging()
            ),
            endreDagligOppfolgingOrgtilknytningCommand.inputMetadata(),
            endreDagligOppfolgingOrgtilknytningCommand.additionalAgressoData()
        ));
    }

    public CompletableFuture<SendResult<String, OrgtilknytningEvent>> avvikleOrgtilknytning(String fid, AvvikleOrgtilknytning avvikleOrgtilknytning) {
        return eventProducer.sendMessage(new OrgtilknytningAvsluttetEventV1(
            fid,
            new OrgtilknytningAvsluttetEventV1.OrgtilknytningAvsluttetEventData(
                avvikleOrgtilknytning.navident(),
                avvikleOrgtilknytning.orgenhetId(),
                avvikleOrgtilknytning.gyldigTom()
            ),
            avvikleOrgtilknytning.inputMetadata(),
            avvikleOrgtilknytning.additionalAgressoData()
        ));
    }

    // Alle verdier i OrgtilknytningDatoEndretEventData objektet erstatter eksisterende verdier. Også tomme.
    public CompletableFuture<SendResult<String, OrgtilknytningEvent>> endreOrgtilknytningDato(String fid, EndreOrgtilknytningDato endreOrgtilknytningDato) {
        //update OTendretData for e-post varsel
        addToOTEndretDataEmailQueue(endreOrgtilknytningDato.navident(), fid, OTEndretData.OTEndretDataType.OT_ENDRET);
        return eventProducer.sendMessage(
            new OrgtilknytningDatoEndretEventV2(
                fid,
                new OrgtilknytningDatoEndretEventV2.OrgtilknytningDatoEndretEventData(
                    endreOrgtilknytningDato.gyldigFom(),
                    endreOrgtilknytningDato.gyldigTom()
                ),
                endreOrgtilknytningDato.inputMetadata(),
                endreOrgtilknytningDato.additionalAgressoData()
            ));
    }


    public CompletableFuture<SendResult<String, OrgtilknytningEvent>> endreOrgtilknytningDato(EndreOrgtilknytningDatoCommand endreOrgtilknytningDatoCommand) {
        //update OTendretData for e-post varsel
        addToOTEndretDataEmailQueue(orgtilknytningEndretEmailService.getNavIdentFromOrgtilknytningState(endreOrgtilknytningDatoCommand.fid()),
            endreOrgtilknytningDatoCommand.fid(), OTEndretData.OTEndretDataType.OT_ENDRET);
        return eventProducer.sendMessage(
            new OrgtilknytningDatoEndretEventV2(
                endreOrgtilknytningDatoCommand.fid(),
                new OrgtilknytningDatoEndretEventV2.OrgtilknytningDatoEndretEventData(
                    endreOrgtilknytningDatoCommand.gyldigFom(),
                    endreOrgtilknytningDatoCommand.gyldigTom()
                ),
                endreOrgtilknytningDatoCommand.inputMetadata(),
                null
            ));
    }

    public CompletableFuture<SendResult<String, OrgtilknytningEvent>> endreTilknytningtype(EndreTilknytningtypeCommand endreTilknytningTypeCommand) {
        //update OTendretData for e-post varsel
        addToOTEndretDataEmailQueue(orgtilknytningEndretEmailService.getNavIdentFromOrgtilknytningState(endreTilknytningTypeCommand.fid()),
            endreTilknytningTypeCommand.fid(), OTEndretData.OTEndretDataType.OT_ENDRET);

        return eventProducer.sendMessage(
            new OrgtilknytningTilknytningtypeEndretEventV1(
                endreTilknytningTypeCommand.fid(),
                new OrgtilknytningTilknytningtypeEndretEventV1.OrgtilknytningTilknytningtypeEndretEventData(
                    endreTilknytningTypeCommand.tilknytningtype()
                ),
                endreTilknytningTypeCommand.inputMetadata()
            ));
    }

    public CompletableFuture<SendResult<String, OrgtilknytningEvent>> fjernOrgtilknytning(String fid, FjernOrgtilknytning fjernOrgtilknytning) {
        //update OTendretData for e-post varsel
        addToOTEndretDataEmailQueue(fjernOrgtilknytning.navident(),
            fid, OTEndretData.OTEndretDataType.OT_FJERNET);
        return eventProducer.sendMessage(new OrgtilknytningFjernetEventV1(
            fid,
            new OrgtilknytningFjernetEventV1.OrgtilknytningFjernetEventData(
                fjernOrgtilknytning.navident(),
                fjernOrgtilknytning.orgenhetId(),
                fjernOrgtilknytning.gyldigFom(),
                fjernOrgtilknytning.gyldigTom()
            ),
            fjernOrgtilknytning.inputMetadata(),
            fjernOrgtilknytning.additionalAgressoData()
        ));
    }
}
