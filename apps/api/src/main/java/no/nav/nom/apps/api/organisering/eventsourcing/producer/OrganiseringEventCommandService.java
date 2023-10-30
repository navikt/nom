package no.nav.nom.apps.api.organisering.eventsourcing.producer;

import no.nav.nom.apps.api.common.PrefixedUlidCreator;
import no.nav.nom.apps.api.organisering.eventsourcing.producer.commands.OpprettOrganisering;
import no.nav.nom.contract.nom.eventsource.OrganiseringOpprettetEventV1;
import org.springframework.stereotype.Service;

@Service
public class OrganiseringEventCommandService {

    private final OrganiseringEventProducer eventProducer;


    public OrganiseringEventCommandService(OrganiseringEventProducer eventProducer) {
        this.eventProducer = eventProducer;
    }

    public String opprettOrganisering(OpprettOrganisering opprettOrganisering) {
        String fid = PrefixedUlidCreator.createOrganiseringUlid();
        eventProducer.sendMessage(getOpprettEvent(fid, opprettOrganisering));
        return fid;
    }

    private OrganiseringOpprettetEventV1 getOpprettEvent(String fid, OpprettOrganisering opprettOrganisering) {
        return new OrganiseringOpprettetEventV1(
            fid,
            new OrganiseringOpprettetEventV1.OrganiseringOpprettetEventData(
                opprettOrganisering.agressoIdOver(),
                opprettOrganisering.agressoIdUnder(),
                opprettOrganisering.orgNivOver(),
                opprettOrganisering.orgNivUnder(),
                opprettOrganisering.navnOver(),
                opprettOrganisering.navnUnder(),
                opprettOrganisering.gyldigFom(),
                opprettOrganisering.gyldigTom()
            ),
            opprettOrganisering.inputMetadata(),
            null
        );
    }

}
