package no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.common.PrefixedUlidCreator;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.AvvikleOrgtilknytning;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.EndreDagligOppfolgingOrgtilknytning;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.FjernOrgtilknytning;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.KorrigerOrgtilknytningDato;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.OpprettOrgtilknytning;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningAvsluttetEventV1;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningDatoEndretEventV1;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningEvent;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningFjernetEventV1;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningOpprettetEventV1;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningDagligOppfolgingEndretEventV1;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;

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

    private ListenableFuture<SendResult<String, OrgtilknytningEvent>> opprettOrgtilknytning(String fid, OpprettOrgtilknytning opprettOrgtilknytning) {
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

    public ListenableFuture<SendResult<String, OrgtilknytningEvent>> endreDagligOppfolgingOrgtilknytning(String fid, EndreDagligOppfolgingOrgtilknytning primaeOrgtilknytningEndret) {
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

    public ListenableFuture<SendResult<String, OrgtilknytningEvent>> avvikleOrgtilknytning(String fid, AvvikleOrgtilknytning avvikleOrgtilknytning) {
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

    // Alle verdier i KorrigerOrgEnhet objektet erstatter eksisterende verdier. Også tomme.
    public ListenableFuture<SendResult<String, OrgtilknytningEvent>> korrigerOrgtilknytningDato(String fid, KorrigerOrgtilknytningDato korrigerOrgtilknytningDato) {
        return eventProducer.sendMessage(new OrgtilknytningDatoEndretEventV1(
            fid,
            new OrgtilknytningDatoEndretEventV1.OrgtilknytningDatoEndretEventData(
                korrigerOrgtilknytningDato.navident(),
                korrigerOrgtilknytningDato.orgenhetId(),
                korrigerOrgtilknytningDato.gyldigFom(),
                korrigerOrgtilknytningDato.gyldigTom()
            ),
            korrigerOrgtilknytningDato.inputMetadata(),
            korrigerOrgtilknytningDato.additionalAgressoData()
        ));
    }

    public ListenableFuture<SendResult<String, OrgtilknytningEvent>> fjernOrgtilknytning(String fid, FjernOrgtilknytning fjernOrgtilknytning) {
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
