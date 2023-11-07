package no.nav.nom.apps.api.organisering.eventsourcing.producer;

import no.nav.nom.apps.api.common.PrefixedUlidCreator;
import no.nav.nom.apps.api.organisering.eventsourcing.producer.commands.EndreOrganiseringCmd;
import no.nav.nom.apps.api.organisering.eventsourcing.producer.commands.OpprettOrganiseringCmd;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.contract.nom.eventsource.OrganiseringEndretEventV1;
import no.nav.nom.contract.nom.eventsource.OrganiseringEvent;
import no.nav.nom.contract.nom.eventsource.OrganiseringOpprettetEventV1;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class OrganiseringEventCommandService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final OrgenhetService orgenhetService;


    private final OrganiseringEventProducer eventProducer;


    public OrganiseringEventCommandService(OrgenhetService orgenhetService, OrganiseringEventProducer eventProducer) {
        this.orgenhetService = orgenhetService;
        this.eventProducer = eventProducer;
    }

    public String opprettOrganisering(OpprettOrganiseringCmd opprettOrganiseringCmd) {
        String fid = PrefixedUlidCreator.createOrganiseringUlid();
        eventProducer.sendMessage(getOpprettEvent(fid, opprettOrganiseringCmd));
        logger.debug("Sendt opprettOrganisering: command={}", opprettOrganiseringCmd);
        return fid;
    }

    public void endreOrganisering(String fid, EndreOrganiseringCmd endreOrganiseringCmd) {
        logger.debug("Sendt endreOrganisering: fid={} command={}", fid, endreOrganiseringCmd);
        eventProducer.sendMessage(getEndreEvent(fid, endreOrganiseringCmd));
    }

    private OrganiseringOpprettetEventV1 getOpprettEvent(String fid, OpprettOrganiseringCmd opprettOrganiseringCmd) {

        String nomIdOver = orgenhetService.getEnhetByAgressoIdNivaAndName(
            opprettOrganiseringCmd.agressoIdOver(),
            opprettOrganiseringCmd.orgNivOver(),
            opprettOrganiseringCmd.navnOver()
        ).map(IOrgenhet::getId).orElse("");

        String nomIdUnder = orgenhetService.getEnhetByAgressoIdNivaAndName(
            opprettOrganiseringCmd.agressoIdUnder(),
            opprettOrganiseringCmd.orgNivUnder(),
            opprettOrganiseringCmd.navnUnder()
        ).map(IOrgenhet::getId).orElse("");


        return new OrganiseringOpprettetEventV1(
            fid,
            new OrganiseringOpprettetEventV1.OrganiseringOpprettetEventData(
                nomIdOver,
                nomIdUnder,
                opprettOrganiseringCmd.navnOver(),
                opprettOrganiseringCmd.navnUnder(),
                opprettOrganiseringCmd.gyldigFom(),
                opprettOrganiseringCmd.gyldigTom()
            ),
            opprettOrganiseringCmd.inputMetadata(),
            null
        );
    }

    private OrganiseringEvent getEndreEvent(String fid, EndreOrganiseringCmd endreOrganiseringCmd) {
        String nomIdOver = orgenhetService.getEnhetByAgressoIdNivaAndName(
            endreOrganiseringCmd.agressoIdOver(),
            endreOrganiseringCmd.orgNivOver(),
            endreOrganiseringCmd.navnOver()
        ).map(IOrgenhet::getId).orElse("");

        String nomIdUnder = orgenhetService.getEnhetByAgressoIdNivaAndName(
            endreOrganiseringCmd.agressoIdUnder(),
            endreOrganiseringCmd.orgNivUnder(),
            endreOrganiseringCmd.navnUnder()
        ).map(IOrgenhet::getId).orElse("");

        return new OrganiseringEndretEventV1(
            fid,
            new OrganiseringEndretEventV1.OrganiseringEndretEventData(
                nomIdOver,
                nomIdUnder,
                endreOrganiseringCmd.navnOver(),
                endreOrganiseringCmd.navnUnder(),
                endreOrganiseringCmd.gyldigFom(),
                endreOrganiseringCmd.gyldigTom()
            ),
            endreOrganiseringCmd.inputMetadata(),
            null
        );
    }

}
