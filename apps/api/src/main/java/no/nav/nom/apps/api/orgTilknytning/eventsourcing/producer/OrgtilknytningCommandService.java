package no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.common.PrefixedUlidCreator;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.AvvikleOrgtilknytning;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.FjernOrgtilknytning;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.KorrigerOrgtilknytningDato;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.OpprettOrgtilknytning;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.EndrePrimaeOrgtilknytning;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningAvsluttetEventV1;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningDatoEndretEventV1;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningEvent;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningFjernetEventV1;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningOpprettetEventV1;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningPrimaerEndretEventV1;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;

@Slf4j
@Service
public class OrgtilknytningCommandService {

    private final no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.OrgtilknytningEventProducer eventProducer;
    private final ObjectMapper objectMapper;
    public OrgtilknytningCommandService(no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.OrgtilknytningEventProducer eventProducer, ObjectMapper objectMapper) {
        this.eventProducer = eventProducer;
        this.objectMapper = objectMapper;
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
                opprettOrgtilknytning.orgEnhetId(),
                opprettOrgtilknytning.gyldigFom()
            ),
            opprettOrgtilknytning.inputMetadata(),
            opprettOrgtilknytning.additionalAgressoData()
        ));
    }

    private ListenableFuture<SendResult<String, OrgtilknytningEvent>> endrePrimaerOrgtilknytning(String fid, EndrePrimaeOrgtilknytning primaeOrgtilknytningEndret) {
        return eventProducer.sendMessage(new OrgtilknytningPrimaerEndretEventV1(
            fid,
            new OrgtilknytningPrimaerEndretEventV1.OrgtilknytningPrimaerEndretEventData(
                primaeOrgtilknytningEndret.navident(),
                primaeOrgtilknytningEndret.orgEnhetId(),
                primaeOrgtilknytningEndret.gyldigFom(),
                primaeOrgtilknytningEndret.gyldigTom(),
                primaeOrgtilknytningEndret.primaer()
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
                avvikleOrgtilknytning.orgEnhetId(),
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
                korrigerOrgtilknytningDato.orgEnhetId(),
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
                fjernOrgtilknytning.orgEnhetId(),
                fjernOrgtilknytning.gyldigFom(),
                fjernOrgtilknytning.gyldigTom()
            ),
            fjernOrgtilknytning.inputMetadata(),
            fjernOrgtilknytning.additionalAgressoData()
        ));
    }
}
