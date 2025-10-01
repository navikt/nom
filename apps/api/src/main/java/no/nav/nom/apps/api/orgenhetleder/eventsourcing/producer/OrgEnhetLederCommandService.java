package no.nav.nom.apps.api.orgenhetleder.eventsourcing.producer;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.common.PrefixedUlidCreator;
import no.nav.nom.apps.api.orgenhetleder.eventsourcing.producer.commands.OppdaterOrgEnhetLederSluttdato;
import no.nav.nom.apps.api.orgenhetleder.eventsourcing.producer.commands.OppdaterOrgEnhetLederStartdato;
import no.nav.nom.apps.api.orgenhetleder.eventsourcing.producer.commands.OpprettOrgEnhetLeder;
import no.nav.nom.apps.api.orgenhetleder.eventsourcing.producer.commands.SlettOrgEnhetLeder;
import no.nav.nom.contract.nom.eventsource.orgenhetleder.OrgEnhetLederOpprettetEventV1;
import no.nav.nom.contract.nom.eventsource.orgenhetleder.OrgEnhetLederSlettetEventV1;
import no.nav.nom.contract.nom.eventsource.orgenhetleder.OrgEnhetLederSluttdatoOppdatertEventV1;
import no.nav.nom.contract.nom.eventsource.orgenhetleder.OrgEnhetLederStartdatoOppdatertEventV1;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class OrgEnhetLederCommandService {
    private final OrgEnhetLederEventProducer eventProducer;

    public OrgEnhetLederCommandService(OrgEnhetLederEventProducer eventProducer) {
        this.eventProducer = eventProducer;
    }

    public String opprettOrgEnhetLeder(OpprettOrgEnhetLeder opprettOrgEnhetLeder) {
        String fid = PrefixedUlidCreator.createLederUlid();
        opprettLeder(fid, opprettOrgEnhetLeder);
        return fid;
    }

    public void oppdaterOrgEnhetLederStartdato(String fid, OppdaterOrgEnhetLederStartdato oppdaterOrgEnhetLederStartdato) {
        oppdaterStardato(fid, oppdaterOrgEnhetLederStartdato);
    }

    public void oppdaterOrgEnhetLederSluttdato(String fid, OppdaterOrgEnhetLederSluttdato oppdaterOrgEnhetLederSluttdato) {
        oppdaterSluttdato(fid, oppdaterOrgEnhetLederSluttdato);
    }

    public void slettOrgEnhetLederState(SlettOrgEnhetLeder slettOrgEnhetLeder) {
        eventProducer.sendMessage(new OrgEnhetLederSlettetEventV1(
            slettOrgEnhetLeder.fid(),
            new OrgEnhetLederSlettetEventV1.OrgEnhetLederSlettetEventData(),
            slettOrgEnhetLeder.inputMetadata()
        ));
    }

    private void opprettLeder(
        String fid,
        OpprettOrgEnhetLeder opprettOrgEnhetLeder
    ) {
        eventProducer.sendMessage(new OrgEnhetLederOpprettetEventV1(
            fid,
            new OrgEnhetLederOpprettetEventV1.OrgEnhetLederOpprettetEventData(
                opprettOrgEnhetLeder.navident(),
                opprettOrgEnhetLeder.orgEnhetId()
            ),
            opprettOrgEnhetLeder.inputMetadata()
        ));
    }

    private void oppdaterStardato(
        String fid,
        OppdaterOrgEnhetLederStartdato oppdaterOrgEnhetLederStartdato
    ) {
        eventProducer.sendMessage(new OrgEnhetLederStartdatoOppdatertEventV1(
            fid,
            new OrgEnhetLederStartdatoOppdatertEventV1.OrgEnhetLederStartdatoOppdatertData(
                oppdaterOrgEnhetLederStartdato.startdato()
            ),
            oppdaterOrgEnhetLederStartdato.inputMetadata()
        ));
    }

    private void oppdaterSluttdato(
        String fid,
        OppdaterOrgEnhetLederSluttdato oppdaterOrgEnhetLederSluttdato
    ) {
        eventProducer.sendMessage(new OrgEnhetLederSluttdatoOppdatertEventV1(
            fid,
            new OrgEnhetLederSluttdatoOppdatertEventV1.OrgEnhetLederAvsluttetEventData(
                oppdaterOrgEnhetLederSluttdato.sluttdato()
            ),
            oppdaterOrgEnhetLederSluttdato.inputMetadata()
        ));
    }
}
