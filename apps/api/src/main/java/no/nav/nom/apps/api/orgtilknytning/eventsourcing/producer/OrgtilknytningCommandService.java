package no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.common.PrefixedUlidCreator;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.AvvikleOrgtilknytning;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.EndreDagligOppfolgingOrgtilknytning;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.EndreDagligOppfolgingOrgtilknytningCommand;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.EndreFysiskeBrukerstederCommand;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.EndreOrgtilknytningDato;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.EndreOrgtilknytningDatoCommand;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.EndreOrgtilknytningSektorCommand;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.EndreTilknytningtypeCommand;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.FjernOrgtilknytning;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.OpprettOrgtilknytning;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningAvsluttetEventV1;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningDagligOppfolgingEndretEventV1;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningDatoEndretEventV2;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningEvent;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningFjernetEventV1;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningFysiskBrukerstedEndretEventV1;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningOpprettetEventV1;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningSektorEndretEventV1;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningTilknytningtypeEndretEventV1;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class OrgtilknytningCommandService {
    private final no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.OrgtilknytningEventProducer eventProducer;

    public OrgtilknytningCommandService(no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.OrgtilknytningEventProducer eventProducer) {
        this.eventProducer = eventProducer;
    }

    public String opprettOrgtilknytning(OpprettOrgtilknytning opprettOrgtilknytning) {
        String fid = PrefixedUlidCreator.createOrgtilknytningUlid();
        opprettOrgtilknytning(fid, opprettOrgtilknytning);
        return fid;
    }

    private CompletableFuture<SendResult<String, OrgtilknytningEvent>> opprettOrgtilknytning(String fid, OpprettOrgtilknytning opprettOrgtilknytning) {
        return eventProducer.sendMessage(new OrgtilknytningOpprettetEventV1(
            fid,
            new OrgtilknytningOpprettetEventV1.OrgtilknytningOpprettetEventData(
                opprettOrgtilknytning.navident(),
                opprettOrgtilknytning.orgenhetId(),
                opprettOrgtilknytning.sektor(),
                opprettOrgtilknytning.gyldigFom(),
                opprettOrgtilknytning.masseflyttetFraFid()
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
        return eventProducer.sendMessage(new OrgtilknytningDagligOppfolgingEndretEventV1(
            endreDagligOppfolgingOrgtilknytningCommand.fid(),
            new OrgtilknytningDagligOppfolgingEndretEventV1.OrgtilknytningDagligOppfolgingEndretEventData(
                endreDagligOppfolgingOrgtilknytningCommand.navident(),
                endreDagligOppfolgingOrgtilknytningCommand.orgenhetId(),
                endreDagligOppfolgingOrgtilknytningCommand.gyldigFom(),
                endreDagligOppfolgingOrgtilknytningCommand.gyldigTom(),
                endreDagligOppfolgingOrgtilknytningCommand.erDagligOppfolging(),
                endreDagligOppfolgingOrgtilknytningCommand.masseflyttetFraFid()
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
                avvikleOrgtilknytning.gyldigTom(),
                avvikleOrgtilknytning.masseflyttetFraFid()
            ),
            avvikleOrgtilknytning.inputMetadata(),
            avvikleOrgtilknytning.additionalAgressoData()
        ));
    }

    // Alle verdier i OrgtilknytningDatoEndretEventData objektet erstatter eksisterende verdier. Også tomme.
    public CompletableFuture<SendResult<String, OrgtilknytningEvent>> endreOrgtilknytningDato(String fid, EndreOrgtilknytningDato endreOrgtilknytningDato) {
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
        return eventProducer.sendMessage(
            new OrgtilknytningTilknytningtypeEndretEventV1(
                endreTilknytningTypeCommand.fid(),
                new OrgtilknytningTilknytningtypeEndretEventV1.OrgtilknytningTilknytningtypeEndretEventData(
                    endreTilknytningTypeCommand.tilknytningtype(),
                    endreTilknytningTypeCommand.masseflyttetFraFid()
                ),
                endreTilknytningTypeCommand.inputMetadata()
            ));
    }

    public CompletableFuture<SendResult<String, OrgtilknytningEvent>> fjernOrgtilknytning(String fid, FjernOrgtilknytning fjernOrgtilknytning) {
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

    public CompletableFuture<SendResult<String, OrgtilknytningEvent>> endreOrgtilknytningSektor(EndreOrgtilknytningSektorCommand endreOrgtilknytningSektorCommand) {
        return eventProducer.sendMessage(new OrgtilknytningSektorEndretEventV1(
            endreOrgtilknytningSektorCommand.fid(),
            new OrgtilknytningSektorEndretEventV1.OrgtilknytningSektorEndretEventData(
                endreOrgtilknytningSektorCommand.navident(),
                endreOrgtilknytningSektorCommand.orgenhetId(),
                endreOrgtilknytningSektorCommand.sektor()
            ),
            endreOrgtilknytningSektorCommand.inputMetadata(),
            endreOrgtilknytningSektorCommand.additionalAgressoData()
        ));
    }

    public CompletableFuture<SendResult<String, OrgtilknytningEvent>> endreFysiskeBrukersted(EndreFysiskeBrukerstederCommand endreFysiskeBrukerstederCommand) {
        return eventProducer.sendMessage(new OrgtilknytningFysiskBrukerstedEndretEventV1(
            endreFysiskeBrukerstederCommand.fid(),
            new OrgtilknytningFysiskBrukerstedEndretEventV1.OrgtilknytningPrimaryFysiskBrukerstedEventData(
                endreFysiskeBrukerstederCommand.navident(),
                endreFysiskeBrukerstederCommand.orgEnhetId(),
                endreFysiskeBrukerstederCommand.primaryFysiskBrukerstedNummer(),
                endreFysiskeBrukerstederCommand.additionalFysiskeBrukerstederNummer()
            ),
            endreFysiskeBrukerstederCommand.inputMetadata()
        ));
    }
}
