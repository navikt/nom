package no.nav.nom.apps.api.orgenhet.api.app;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.graphql.common.SecurityUtil;
import no.nav.nom.apps.api.organisering.eventsourcing.producer.OrganiseringEventCommandService;
import no.nav.nom.apps.api.organisering.eventsourcing.producer.commands.OpprettOrganiseringCmd;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.orgenhet.api.app.dto.OpprettOrgEnhetRequest;
import no.nav.nom.apps.api.orgenhet.api.app.dto.OpprettOrgEnhetResponse;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.OrgEnhetCommandService;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.OrgEnhetIdGenerator;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.commands.EndreOrgEnhetsType;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.commands.OpprettOrgEnhet;
import no.nav.nom.apps.api.orgenhet.exceptions.DuplicateOrgEnhetNameException;
import no.nav.nom.apps.api.orgenhet.exceptions.OrgEnhetNotFoundException;
import no.nav.nom.apps.api.orgenhetleder.eventsourcing.producer.OrgEnhetLederCommandService;
import no.nav.nom.apps.api.orgenhetleder.eventsourcing.producer.commands.OppdaterOrgEnhetLederStartdato;
import no.nav.nom.apps.api.orgenhetleder.eventsourcing.producer.commands.OpprettOrgEnhetLeder;
import no.nav.nom.apps.api.ressurs.RessursService;
import no.nav.nom.apps.api.ressurs.api.exceptions.RessursNotFoundException;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.Origination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static no.nav.nom.apps.api.graphql.common.SecurityUtil.getAuthenticatedNavIdent;
import static no.nav.nom.libs.utils.common.util.DateUtil.isActive;

@Slf4j
@Service
public class OrgEnhetRestService {

    private final static Logger logger = LoggerFactory.getLogger(OrgEnhetRestService.class);
    private final OrgEnhetIdGenerator orgEnhetIdGenerator;
    private final OrgenhetService orgenhetService;
    private final RessursService ressursService;
    private final OrgEnhetCommandService orgEnhetCommandService;
    private final OrganiseringEventCommandService organiseringEventCommandService;
    private final OrgEnhetLederCommandService orgEnhetLederCommandService;

    public OrgEnhetRestService(
        OrgEnhetIdGenerator orgEnhetIdGenerator,
        OrgenhetService orgenhetService,
        RessursService ressursService,
        OrgEnhetCommandService orgEnhetCommandService,
        OrganiseringEventCommandService organiseringEventCommandService,
        OrgEnhetLederCommandService orgEnhetLederCommandService
    ) {
        this.orgEnhetIdGenerator = orgEnhetIdGenerator;
        this.orgenhetService = orgenhetService;
        this.ressursService = ressursService;
        this.orgEnhetCommandService = orgEnhetCommandService;
        this.organiseringEventCommandService = organiseringEventCommandService;
        this.orgEnhetLederCommandService = orgEnhetLederCommandService;
    }


    public OpprettOrgEnhetResponse opprettOrgEnhet(OpprettOrgEnhetRequest opprettOrgEnhetRequest) {
        var overordnetOrgEnhet = orgenhetService.getById(opprettOrgEnhetRequest.overordnetOrgEnhetId())
            .orElseThrow(() -> OrgEnhetNotFoundException.id(opprettOrgEnhetRequest.overordnetOrgEnhetId()));

        var ressursState = ressursService.findByNavident(opprettOrgEnhetRequest.lederRessursNavident())
            .filter(r -> isActive(r.startdato(), r.sluttdato()))
            .orElseThrow(() -> RessursNotFoundException.navident(opprettOrgEnhetRequest.lederRessursNavident()));

        if (orgenhetService.activeOrFutureWithEqualName(opprettOrgEnhetRequest.orgEnhetNavn())) {
            throw DuplicateOrgEnhetNameException.name(opprettOrgEnhetRequest.orgEnhetNavn());
        }

        InputMetadata inputMetadata = new InputMetadata(
            "REST-API",
            "opprettOrgEnhet",
            opprettOrgEnhetRequest,
            new Origination(
                getAuthenticatedNavIdent(),
                SecurityUtil.getAuthenticatedAzpName()
            )
        );

        final var opprettOrgEnhet = new OpprettOrgEnhet(
            orgEnhetIdGenerator.newId(),
            opprettOrgEnhetRequest.orgEnhetNavn(),
            opprettOrgEnhetRequest.gyldigFom().isAfter(LocalDate.now()) ? LocalDate.now() : opprettOrgEnhetRequest.gyldigFom(),
            overordnetOrgEnhet.getFid(),
            ressursState.fid(),
            inputMetadata
        );

        try {
            var orgEnhetState = orgEnhetCommandService.opprettOrgEnhetSync(opprettOrgEnhet);

            final var endreOrgEnhetsType = new EndreOrgEnhetsType(
                opprettOrgEnhetRequest.orgEnhetsType(),
                inputMetadata
            );

            orgEnhetCommandService.endreOrgEnhetsType(orgEnhetState.fid(), endreOrgEnhetsType);

            final var opprettOrganisering = new OpprettOrganiseringCmd(
                orgEnhetState.id(),
                orgEnhetState.navn(),
                overordnetOrgEnhet.getId(),
                overordnetOrgEnhet.getNavn(),
                orgEnhetState.gyldigFom(),
                orgEnhetState.gyldigTom(),
                inputMetadata
            );

            organiseringEventCommandService.opprettOrganisering(opprettOrganisering);

            final var opprettOrgEnhetLeder = new OpprettOrgEnhetLeder(
                ressursState.navident(),
                orgEnhetState.id(),
                inputMetadata
            );

            String fid = orgEnhetLederCommandService.opprettOrgEnhetLeder(opprettOrgEnhetLeder);

            final var oppdaterOrgEnhetLederStartdato = new OppdaterOrgEnhetLederStartdato(
                orgEnhetState.gyldigFom(),
                inputMetadata
            );

            orgEnhetLederCommandService.oppdaterOrgEnhetLederStartdato(fid, oppdaterOrgEnhetLederStartdato);

            return new OpprettOrgEnhetResponse(
                orgEnhetState.fid(),
                orgEnhetState.id(),
                orgEnhetState.navn(),
                opprettOrganisering.idOver(),
                opprettOrganisering.navnOver(),
                orgEnhetState.gyldigFom(),
                orgEnhetState.gyldigTom(),
                ressursState.navident(),
                ressursState.visningsnavn().getFormattedVisningsnavn(),
                opprettOrgEnhetRequest.firesiffer(),
                orgEnhetState.orgEnhetsType()
            );
        } catch (ExecutionException | TimeoutException | InterruptedException e) {
            throw new RuntimeException(e);
        }

    }
}
