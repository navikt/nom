package no.nav.nom.apps.api.orgenhet.api.app;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.graphql.common.SecurityUtil;
import no.nav.nom.apps.api.organisering.eventsourcing.producer.OrganiseringEventCommandService;
import no.nav.nom.apps.api.organisering.eventsourcing.producer.commands.EndreOrganiseringCmd;
import no.nav.nom.apps.api.organisering.eventsourcing.producer.commands.OpprettOrganiseringCmd;
import no.nav.nom.apps.api.organisering.exceptions.OrganiseringNotFoundException;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.orgenhet.api.app.dto.OppdaterOrgEnhetRequest;
import no.nav.nom.apps.api.orgenhet.api.app.dto.OpprettOrgEnhetRequest;
import no.nav.nom.apps.api.orgenhet.api.app.dto.OpprettOrgEnhetResponse;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.OrgEnhetCommandService;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.OrgEnhetIdGenerator;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.commands.EndreOrgEnhetsType;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.commands.OppdaterOrgEnhetNavnCmd;
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
import no.nav.nom.libs.state.organisering.OrganiseringStateRepository;
import org.apache.commons.text.WordUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static no.nav.nom.apps.api.graphql.common.SecurityUtil.getAuthenticatedNavIdent;
import static no.nav.nom.libs.utils.common.util.DateUtil.isActive;

@Slf4j
@Service
public class OrgEnhetRestService {

    private final OrgEnhetIdGenerator orgEnhetIdGenerator;
    private final OrgenhetService orgenhetService;
    private final RessursService ressursService;
    private final OrgEnhetCommandService orgEnhetCommandService;
    private final OrganiseringEventCommandService organiseringEventCommandService;
    private final OrgEnhetLederCommandService orgEnhetLederCommandService;
    private final OrganiseringStateRepository organiseringStateRepository;

    public OrgEnhetRestService(
        OrgEnhetIdGenerator orgEnhetIdGenerator,
        OrgenhetService orgenhetService,
        RessursService ressursService,
        OrgEnhetCommandService orgEnhetCommandService,
        OrganiseringEventCommandService organiseringEventCommandService,
        OrgEnhetLederCommandService orgEnhetLederCommandService,
        OrganiseringStateRepository organiseringStateRepository
    ) {
        this.orgEnhetIdGenerator = orgEnhetIdGenerator;
        this.orgenhetService = orgenhetService;
        this.ressursService = ressursService;
        this.orgEnhetCommandService = orgEnhetCommandService;
        this.organiseringEventCommandService = organiseringEventCommandService;
        this.orgEnhetLederCommandService = orgEnhetLederCommandService;
        this.organiseringStateRepository = organiseringStateRepository;
    }


    public OpprettOrgEnhetResponse opprettOrgEnhet(OpprettOrgEnhetRequest opprettOrgEnhetRequest) {
        log.info("OpprettOrgEnhetRequest: {}", opprettOrgEnhetRequest);
        var overordnetOrgEnhet = orgenhetService.getById(opprettOrgEnhetRequest.overordnetOrgEnhetId())
            .orElseThrow(() -> OrgEnhetNotFoundException.id(opprettOrgEnhetRequest.overordnetOrgEnhetId()));

        var ressursState = ressursService.findByNavident(opprettOrgEnhetRequest.lederRessursNavident())
            .filter(r -> isActive(r.startdato(), r.sluttdato()))
            .orElseThrow(() -> RessursNotFoundException.navident(opprettOrgEnhetRequest.lederRessursNavident()));

        if (orgenhetService.activeOrFutureWithEqualName(opprettOrgEnhetRequest.navn())) {
            throw DuplicateOrgEnhetNameException.name(opprettOrgEnhetRequest.navn());
        }

        String navn = WordUtils.capitalize(opprettOrgEnhetRequest.navn());

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
            navn,
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

    public String oppdaterOrgEnhet(OppdaterOrgEnhetRequest oppdaterOrgEnhetRequest) {
        List<String> oppdaterteElementer = new ArrayList<>();

        var orgEnhetState = orgenhetService.getById(oppdaterOrgEnhetRequest.id()).orElseThrow(() -> OrgEnhetNotFoundException.id(oppdaterOrgEnhetRequest.id()));
        var organiseringState = organiseringStateRepository.findActiveById(orgEnhetState.getId()).orElseThrow(() -> OrganiseringNotFoundException.id(orgEnhetState.getId()));

        String navn = WordUtils.capitalize(oppdaterOrgEnhetRequest.navn());

        boolean hasChange = false;

        InputMetadata inputMetadata = new InputMetadata(
            "REST-API",
            "oppdaterOrgEnhet",
            oppdaterOrgEnhetRequest,
            new Origination(
                getAuthenticatedNavIdent(),
                SecurityUtil.getAuthenticatedAzpName()
            )
        );

        if (!Objects.equals(oppdaterOrgEnhetRequest.navn(), orgEnhetState.getNavn())) {
            if (orgenhetService.activeOrFutureWithEqualName(oppdaterOrgEnhetRequest.navn())) {
                throw DuplicateOrgEnhetNameException.name(oppdaterOrgEnhetRequest.navn());
            }
            hasChange = true;
            var oppdaterNavn = new OppdaterOrgEnhetNavnCmd(
                navn,
                inputMetadata
            );

            orgEnhetCommandService.oppdaterNavnOrgEnhet(orgEnhetState.getFid(), oppdaterNavn);
            oppdaterteElementer.add(String.format("OrgEnhet har endret navn fra %s til %s", orgEnhetState.getNavn(), navn));
        }

        if (hasChange) {
            final var opprettOrganisering = new EndreOrganiseringCmd(
                orgEnhetState.getId(),
                navn,
                organiseringState.idOver(),
                organiseringState.navnOver(),
                orgEnhetState.getGyldigFom(),
                orgEnhetState.getGyldigTom(),
                inputMetadata
            );

            organiseringEventCommandService.endreOrganisering(organiseringState.fid(), opprettOrganisering);
        }

        StringBuilder builder = new StringBuilder();
        oppdaterteElementer.forEach(result -> builder.append(result).append("\\n"));

        return builder.toString();
    }
}
