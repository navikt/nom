package no.nav.nom.apps.api.organisering.eventsourcing.producer;

import no.nav.nom.apps.api.common.PrefixedUlidCreator;
import no.nav.nom.apps.api.organisering.eventsourcing.producer.commands.OpprettOrganisering;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.contract.nom.eventsource.OrganiseringOpprettetEventV1;
import org.springframework.stereotype.Service;

@Service
public class OrganiseringEventCommandService {

    private final OrgenhetService orgenhetService;


    private final OrganiseringEventProducer eventProducer;


    public OrganiseringEventCommandService(OrgenhetService orgenhetService, OrganiseringEventProducer eventProducer) {
        this.orgenhetService = orgenhetService;
        this.eventProducer = eventProducer;
    }

    public String opprettOrganisering(OpprettOrganisering opprettOrganisering) {
        String fid = PrefixedUlidCreator.createOrganiseringUlid();
        eventProducer.sendMessage(getOpprettEvent(fid, opprettOrganisering));
        return fid;
    }

    private OrganiseringOpprettetEventV1 getOpprettEvent(String fid, OpprettOrganisering opprettOrganisering) {

        String nomIdOver = orgenhetService.getEnhetByAgressoIdNivaAndName(
            opprettOrganisering.agressoIdOver(),
            opprettOrganisering.orgNivOver(),
            opprettOrganisering.navnOver()
        ).map(IOrgenhet::getId).orElse("");

        String nomIdUnder = orgenhetService.getEnhetByAgressoIdNivaAndName(
            opprettOrganisering.agressoIdUnder(),
            opprettOrganisering.orgNivUnder(),
            opprettOrganisering.navnUnder()
        ).map(IOrgenhet::getId).orElse("");


        return new OrganiseringOpprettetEventV1(
            fid,
            new OrganiseringOpprettetEventV1.OrganiseringOpprettetEventData(
                nomIdOver,
                nomIdUnder,
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
