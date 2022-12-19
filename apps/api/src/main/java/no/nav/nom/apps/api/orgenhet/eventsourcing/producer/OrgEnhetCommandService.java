package no.nav.nom.apps.api.orgenhet.eventsourcing.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.nom.apps.api.common.PrefixedUlidCreator;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.commands.AvvikleOrgEnhet;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.commands.OpprettOrgEnhet;
import no.nav.nom.apps.api.orgenhet.implementations.dvh.v2.DvhOrgenhetV2;
import no.nav.nom.contract.nom.eventsource.OrgEnhetAvvikletEventV1;
import no.nav.nom.contract.nom.eventsource.OrgEnhetEvent;
import no.nav.nom.contract.nom.eventsource.OrgEnhetOpprettetEventV1;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;

@Service
public class OrgEnhetCommandService {

    private final OrgEnhetEventProducer eventProducer;
    private final ObjectMapper objectMapper;
    public OrgEnhetCommandService(OrgEnhetEventProducer eventProducer, ObjectMapper objectMapper) {
        this.eventProducer = eventProducer;
        this.objectMapper = objectMapper;
    }

    public String createOrgEnhet(OpprettOrgEnhet opprettOrgEnhet) {
        String fid = PrefixedUlidCreator.createOrgEnhetUlid();
        createOrgEnhet(fid, opprettOrgEnhet);
        return fid;
    }

    private ListenableFuture<SendResult<String, OrgEnhetEvent>> createOrgEnhet(String fid, OpprettOrgEnhet opprettOrgEnhet) {
        return eventProducer.sendMessage(getOpprettEvent(fid, opprettOrgEnhet));
    }

    private OrgEnhetOpprettetEventV1 getOpprettEvent(String fid, OpprettOrgEnhet opprettOrgEnhet) {
        OrgEnhetOpprettetEventV1.OrgEnhetOpprettetAdditionalData additionalData = null;
        if (opprettOrgEnhet.inputMetadata().inputInterfaceMethodName().equals("Datavarehus")) {
            var input = objectMapper.valueToTree(opprettOrgEnhet.inputMetadata().inputInterfaceMethodArguments());
            var dvhOrgenhet2 = new DvhOrgenhetV2(input);
            additionalData = new OrgEnhetOpprettetEventV1.OrgEnhetOpprettetAdditionalData(
                    dvhOrgenhet2.getAgressoId(),
                    dvhOrgenhet2.getNavn(),
                    dvhOrgenhet2.getOrgNiv(),
                    dvhOrgenhet2.getAgressoOrgenhetType(),
                    dvhOrgenhet2.getUniqueAgressoId()
            );
        }
        return new OrgEnhetOpprettetEventV1(
                fid,
                new OrgEnhetOpprettetEventV1.OrgEnhetOpprettetEventData(
                        opprettOrgEnhet.id(),
                        opprettOrgEnhet.navn(),
                        opprettOrgEnhet.gyldigFom(),
                        opprettOrgEnhet.overordnetOrgEnhetFid(),
                        opprettOrgEnhet.lederRessursFid(),
                        opprettOrgEnhet.orgEnhetsKategori()
                ),
                opprettOrgEnhet.inputMetadata(),
                additionalData
        );
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
