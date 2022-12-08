package no.nav.nom.apps.api.orgenhet.eventsourcing.producer;

import no.nav.nom.apps.api.common.PrefixedUlidCreator;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.commands.AvvikleOrgEnhet;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.commands.OpprettOrgEnhet;
import no.nav.nom.contract.nom.eventsource.OrgEnhetAvvikletEventV1;
import no.nav.nom.contract.nom.eventsource.OrgEnhetEvent;
import no.nav.nom.contract.nom.eventsource.OrgEnhetOpprettetEventV1;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;

@Service
public class OrgEnhetCommandService {

    private final OrgEnhetEventProducer eventProducer;

    public OrgEnhetCommandService(OrgEnhetEventProducer eventProducer) {
        this.eventProducer = eventProducer;
    }

    public String createOrgEnhet(OpprettOrgEnhet opprettOrgEnhet) {
        String fid = PrefixedUlidCreator.createOrgEnhetUlid();
        createOrgEnhet(fid, opprettOrgEnhet);
        return fid;
    }

    private ListenableFuture<SendResult<String, OrgEnhetEvent>> createOrgEnhet(String fid, OpprettOrgEnhet opprettOrgEnhet) {
        return eventProducer.sendMessage(new OrgEnhetOpprettetEventV1(
                fid,
                new OrgEnhetOpprettetEventV1.OrgEnhetOpprettetEventData(
                        opprettOrgEnhet.id(),
                        opprettOrgEnhet.navn(),
                        opprettOrgEnhet.gyldigFom(),
                        opprettOrgEnhet.overordnetOrgEnhetFid(),
                        opprettOrgEnhet.lederRessursFid(),
                        opprettOrgEnhet.orgEnhetsKategori()
                ),
                opprettOrgEnhet.inputMetadata()
        ));
    }

    public ListenableFuture<SendResult<String, OrgEnhetEvent>> terminateOrgEnhet(String fid, AvvikleOrgEnhet avvikleOrgEnhet) {
        return eventProducer.sendMessage(new OrgEnhetAvvikletEventV1(
                fid,
                new OrgEnhetAvvikletEventV1.OrgEnhetAvvikletEventData(
                        avvikleOrgEnhet.gyldigTom()
                ),
                avvikleOrgEnhet.inputMetadata()
        ));
    }

}
