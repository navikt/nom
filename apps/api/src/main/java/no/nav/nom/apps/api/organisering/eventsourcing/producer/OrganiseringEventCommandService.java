package no.nav.nom.apps.api.organisering.eventsourcing.producer;

import no.nav.nom.apps.api.common.PrefixedUlidCreator;
import no.nav.nom.apps.api.organisering.eventsourcing.producer.commands.EndreOrganiseringCmd;
import no.nav.nom.apps.api.organisering.eventsourcing.producer.commands.OpprettOrganiseringCmd;
import no.nav.nom.contract.nom.eventsource.OrganiseringEndretEventV1;
import no.nav.nom.contract.nom.eventsource.OrganiseringEvent;
import no.nav.nom.contract.nom.eventsource.OrganiseringOpprettetEventV1;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class OrganiseringEventCommandService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());


    private final OrganiseringEventProducer eventProducer;


    public OrganiseringEventCommandService(OrganiseringEventProducer eventProducer) {
        this.eventProducer = eventProducer;
    }

    public void opprettOrganisering(OpprettOrganiseringCmd opprettOrganiseringCmd) {
        String fid = PrefixedUlidCreator.createOrganiseringUlid();
        eventProducer.sendMessage(getOpprettEvent(fid, opprettOrganiseringCmd));
        logger.debug("Sendt opprettOrganisering: command={}", opprettOrganiseringCmd);
    }

    public void endreOrganisering(String fid, EndreOrganiseringCmd endreOrganiseringCmd) {
        logger.debug("Sendt endreOrganisering: fid={} command={}", fid, endreOrganiseringCmd);
        eventProducer.sendMessage(getEndreEvent(fid, endreOrganiseringCmd));
    }

    private OrganiseringOpprettetEventV1 getOpprettEvent(String fid, OpprettOrganiseringCmd opprettOrganiseringCmd) {
        return new OrganiseringOpprettetEventV1(
            fid,
            new OrganiseringOpprettetEventV1.OrganiseringOpprettetEventData(
                opprettOrganiseringCmd.id(),
                opprettOrganiseringCmd.navn(),
                opprettOrganiseringCmd.idOver(),
                opprettOrganiseringCmd.navnOver(),
                opprettOrganiseringCmd.gyldigFom(),
                opprettOrganiseringCmd.gyldigTom()
            ),
            opprettOrganiseringCmd.inputMetadata(),
            null
        );
    }

    private OrganiseringEvent getEndreEvent(String fid, EndreOrganiseringCmd endreOrganiseringCmd) {
        return new OrganiseringEndretEventV1(
            fid,
            new OrganiseringEndretEventV1.OrganiseringEndretEventData(
                endreOrganiseringCmd.id(),
                endreOrganiseringCmd.navn(),
                endreOrganiseringCmd.idOver(),
                endreOrganiseringCmd.navnOver(),
                endreOrganiseringCmd.gyldigFom(),
                endreOrganiseringCmd.gyldigTom()
            ),
            endreOrganiseringCmd.inputMetadata(),
            null
        );
    }

}
