package no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.common.PrefixedUlidCreator;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.AvvikleOrgtilknytning;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.EndreDagligOppfolgingOrgtilknytning;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.EndreOrgtilknytningType;
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
                opprettOrgtilknytning.gyldigFom()
            ),
            opprettOrgtilknytning.inputMetadata(),
            opprettOrgtilknytning.additionalAgressoData()
        ));
    }

    public CompletableFuture<SendResult<String, OrgtilknytningEvent>> endreDagligOppfolgingOrgtilknytning(
        String fid,
        EndreDagligOppfolgingOrgtilknytning primaeOrgtilknytningEndret
    ) {
        return eventProducer.sendMessage(new OrgtilknytningDagligOppfolgingEndretEventV1(
            fid,
            new OrgtilknytningDagligOppfolgingEndretEventV1.OrgtilknytningDagligOppfolgingEndretEventData(
                primaeOrgtilknytningEndret.navident(),
                primaeOrgtilknytningEndret.orgenhetId(),
                primaeOrgtilknytningEndret.gyldigFom(),
                primaeOrgtilknytningEndret.gyldigTom(),
                primaeOrgtilknytningEndret.dagligOppfolging()
            ),
            primaeOrgtilknytningEndret.inputMetadata(),
            primaeOrgtilknytningEndret.additionalAgressoData()
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

    public CompletableFuture<SendResult<String, OrgtilknytningEvent>> endreOrgtilknytningType(String fid, EndreOrgtilknytningType endreOrgtilknytningType) {
        return eventProducer.sendMessage(
            new OrgtilknytningTilknytningtypeEndretEventV1(
                fid,
                new OrgtilknytningTilknytningtypeEndretEventV1.OrgtilknytningTilknytningtypeEndretEventData(
                    endreOrgtilknytningType.orgtilknytningType()
                ),
                endreOrgtilknytningType.inputMetadata()
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
}
