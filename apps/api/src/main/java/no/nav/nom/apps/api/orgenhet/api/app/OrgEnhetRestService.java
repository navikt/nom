package no.nav.nom.apps.api.orgenhet.api.app;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.graphql.common.SecurityUtil;
import no.nav.nom.apps.api.organisering.eventsourcing.producer.OrganiseringEventCommandService;
import no.nav.nom.apps.api.organisering.eventsourcing.producer.commands.AvsluttOrganiseringCmd;
import no.nav.nom.apps.api.organisering.eventsourcing.producer.commands.EndreOrganiseringCmd;
import no.nav.nom.apps.api.organisering.eventsourcing.producer.commands.KorrigerOrganiseringNavn;
import no.nav.nom.apps.api.organisering.eventsourcing.producer.commands.OpprettOrganiseringCmd;
import no.nav.nom.apps.api.organisering.exceptions.OrganiseringNotFoundException;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.orgenhet.api.app.dto.AvvikleOrgEnhetRequest;
import no.nav.nom.apps.api.orgenhet.api.app.dto.KorrigerOrgEnhetNavnRequest;
import no.nav.nom.apps.api.orgenhet.api.app.dto.OpprettOrgEnhetRequest;
import no.nav.nom.apps.api.orgenhet.api.app.dto.OpprettOrgEnhetResponse;
import no.nav.nom.apps.api.orgenhet.api.app.dto.OrgEnhet;
import no.nav.nom.apps.api.orgenhet.api.app.dto.OvrigeEndringerOrgEnhetRequest;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.OrgEnhetCommandService;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.OrgEnhetIdGenerator;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.commands.AvvikleOrgEnhet;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.commands.EndreNomNivaa;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.commands.EndreOrgEnhetsType;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.commands.KorrigerGyldigFom;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.commands.KorrigerOrgEnhetNavn;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.commands.OpprettOrgEnhet;
import no.nav.nom.apps.api.orgenhet.exceptions.DuplicateOrgEnhetNameException;
import no.nav.nom.apps.api.orgenhet.exceptions.OrgEnhetNotFoundException;
import no.nav.nom.apps.api.orgenhetleder.eventsourcing.producer.OrgEnhetLederCommandService;
import no.nav.nom.apps.api.orgenhetleder.eventsourcing.producer.commands.OppdaterOrgEnhetLederStartdato;
import no.nav.nom.apps.api.orgenhetleder.eventsourcing.producer.commands.OpprettOrgEnhetLeder;
import no.nav.nom.apps.api.ressurs.RessursService;
import no.nav.nom.apps.api.ressurs.api.exceptions.RessursNotFoundException;
import no.nav.nom.apps.api.rest.common.StandardResponse;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.Origination;
import no.nav.nom.libs.state.organisering.OrganiseringStateRepository;
import org.apache.commons.text.WordUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static no.nav.nom.apps.api.graphql.common.SecurityUtil.getAuthenticatedNavIdent;
import static no.nav.nom.libs.utils.common.util.DateUtils.isPeriodActive;

@Slf4j
@Service
public class OrgEnhetRestService {

    private final OrgEnhetIdGenerator orgEnhetIdGenerator;
    private final OrgenhetService orgenhetService;
    private final OrgEnhetMapper orgEnhetMapper;
    private final RessursService ressursService;
    private final OrgEnhetCommandService orgEnhetCommandService;
    private final OrganiseringEventCommandService organiseringEventCommandService;
    private final OrgEnhetLederCommandService orgEnhetLederCommandService;
    private final OrganiseringStateRepository organiseringStateRepository;

    public OrgEnhetRestService(
        OrgEnhetIdGenerator orgEnhetIdGenerator,
        OrgenhetService orgenhetService,
        OrgEnhetMapper orgEnhetMapper,
        RessursService ressursService,
        OrgEnhetCommandService orgEnhetCommandService,
        OrganiseringEventCommandService organiseringEventCommandService,
        OrgEnhetLederCommandService orgEnhetLederCommandService,
        OrganiseringStateRepository organiseringStateRepository
    ) {
        this.orgEnhetIdGenerator = orgEnhetIdGenerator;
        this.orgenhetService = orgenhetService;
        this.orgEnhetMapper = orgEnhetMapper;
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
            .filter(r -> isPeriodActive(r.startdato(), r.sluttdato()))
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
            opprettOrgEnhetRequest.gyldigFom(),
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

    public StandardResponse korrigerOrgEnhetNavn(KorrigerOrgEnhetNavnRequest korrigerOrgEnhetNavnRequest) {
        if (korrigerOrgEnhetNavnRequest.navn().isBlank()) {
            throw new IllegalStateException("Navn kan ikke være blankt");
        }

        var orgEnhetState = orgenhetService.getById(korrigerOrgEnhetNavnRequest.id()).orElseThrow(() -> OrgEnhetNotFoundException.id(korrigerOrgEnhetNavnRequest.id()));
        var organiseringState = organiseringStateRepository.findActiveById(orgEnhetState.getId()).orElseThrow(() -> OrganiseringNotFoundException.id(orgEnhetState.getId()));

        String navn = WordUtils.capitalize(korrigerOrgEnhetNavnRequest.navn());

        InputMetadata inputMetadata = new InputMetadata(
            "REST-API",
            "korrigerOrgEnhetNavn",
            korrigerOrgEnhetNavnRequest,
            new Origination(
                getAuthenticatedNavIdent(),
                SecurityUtil.getAuthenticatedAzpName()
            )
        );

        if (!Objects.equals(korrigerOrgEnhetNavnRequest.navn(), orgEnhetState.getNavn())) {
            if (orgenhetService.activeOrFutureWithEqualName(korrigerOrgEnhetNavnRequest.navn())) {
                throw DuplicateOrgEnhetNameException.name(korrigerOrgEnhetNavnRequest.navn());
            }
            var oppdaterNavn = new KorrigerOrgEnhetNavn(
                navn,
                inputMetadata
            );

            orgEnhetCommandService.korrigerNavnOrgEnhet(orgEnhetState.getFid(), oppdaterNavn);
            organiseringEventCommandService.korrigerOrganiseringNavn(organiseringState.fid(), new KorrigerOrganiseringNavn(organiseringState.id(), navn, inputMetadata));
        }
        return StandardResponse.builder().message(String.format("Organisasjonsenhet med id %s har byttet navn fra %s til %s", orgEnhetState.getId(), orgEnhetState.getNavn(), navn)).build();
    }

    public String ovrigeEndringerOrgEnhet(OvrigeEndringerOrgEnhetRequest ovrigeEndringerOrgEnhetRequest) {
        List<String> oppdaterteElementer = new ArrayList<>();

        var orgEnhetState = orgenhetService.getById(ovrigeEndringerOrgEnhetRequest.id())
            .orElseThrow(() -> OrgEnhetNotFoundException.id(ovrigeEndringerOrgEnhetRequest.id()));

        InputMetadata inputMetadata = new InputMetadata(
            "REST-API",
            "overigeEndringerOrgEnhet",
            ovrigeEndringerOrgEnhetRequest,
            new Origination(
                getAuthenticatedNavIdent(),
                SecurityUtil.getAuthenticatedAzpName()
            )
        );

        if (!Objects.equals(ovrigeEndringerOrgEnhetRequest.gyldigFom(), orgEnhetState.getGyldigFom())) {
            var organiseringState = organiseringStateRepository.findActiveById(orgEnhetState.getId())
                .orElseThrow(() -> OrganiseringNotFoundException.id(orgEnhetState.getId()));

            orgEnhetCommandService.korrigerGyldigFom(orgEnhetState.getFid(), new KorrigerGyldigFom(
                ovrigeEndringerOrgEnhetRequest.gyldigFom(),
                inputMetadata
            ));

            final var opprettOrganisering = new EndreOrganiseringCmd(
                orgEnhetState.getId(),
                orgEnhetState.getNavn(),
                organiseringState.idOver(),
                organiseringState.navnOver(),
                ovrigeEndringerOrgEnhetRequest.gyldigFom(),
                orgEnhetState.getGyldigTom(),
                inputMetadata
            );

            organiseringEventCommandService.endreOrganisering(organiseringState.fid(), opprettOrganisering);
            oppdaterteElementer.add(String.format("OrgEnhet har endret gyldigFom fra %s til %s", orgEnhetState.getGyldigFom(), ovrigeEndringerOrgEnhetRequest.gyldigFom()));
        }

        if (!Objects.equals(ovrigeEndringerOrgEnhetRequest.orgEnhetsType(), orgEnhetState.getOrgEnhetsType())) {
            orgEnhetCommandService.endreOrgEnhetsType(orgEnhetState.getFid(), new EndreOrgEnhetsType(
                ovrigeEndringerOrgEnhetRequest.orgEnhetsType(),
                inputMetadata
            ));

            oppdaterteElementer.add(String.format("OrgEnhet har endret Enhets Type fra %s til %s", orgEnhetState.getOrgEnhetsType(), ovrigeEndringerOrgEnhetRequest.orgEnhetsType()));
        }

        if (!Objects.equals(ovrigeEndringerOrgEnhetRequest.nomNivaa(), orgEnhetState.getNomNivaa())) {
            orgEnhetCommandService.endreNomNivaa(orgEnhetState.getFid(), new EndreNomNivaa(
                ovrigeEndringerOrgEnhetRequest.nomNivaa(),
                inputMetadata
            ));

            oppdaterteElementer.add(String.format("OrgEnhet har endret NOM Nivå fra %s til %s", orgEnhetState.getNomNivaa(), ovrigeEndringerOrgEnhetRequest.nomNivaa()));
        }

        return String.join("\\n", oppdaterteElementer);
    }

    public StandardResponse avvikleOrgEnhet(String orgEnhetId, AvvikleOrgEnhetRequest avvikleOrgEnhetRequest) {

        var orgEnhet = orgenhetService.getById(orgEnhetId).map(orgEnhetMapper::mapToOrgEnhet).orElseThrow(() -> OrgEnhetNotFoundException.id(orgEnhetId));
        var overliggendeOrgEnhet = orgenhetService.getById(orgEnhet.overordnetEnhetId()).orElseThrow(() -> OrgEnhetNotFoundException.id(orgEnhetId));

        if (nonNull(overliggendeOrgEnhet.getGyldigTom()) && avvikleOrgEnhetRequest.gyldigTom().isAfter(overliggendeOrgEnhet.getGyldigTom())) {
            throw new IllegalStateException(
                String.format(
                    "Kan ikke sette avviklings dato til enhet %s (%s) til å være etter overenheten %s (%s) sin avviklingsdato",
                    orgEnhet.navn(),
                    avvikleOrgEnhetRequest.gyldigTom(),
                    overliggendeOrgEnhet.getNavn(),
                    overliggendeOrgEnhet.getGyldigTom())
            );
        }

        validerOrgtilknytningGyldigTom(orgEnhet, avvikleOrgEnhetRequest.gyldigTom());

        var underliggendeOrgEnheter = orgenhetService.getAllUnderliggendeOrgEnhetById(orgEnhetId).stream().map(orgEnhetMapper::mapToOrgEnhet).toList();

        underliggendeOrgEnheter.forEach(underliggendeOrgEnhet -> {
            if (isNull(underliggendeOrgEnhet.gyldigTom())) {
                throw new IllegalStateException(String.format("Underliggende enhet %s (%s) har ingen sluttdato", underliggendeOrgEnhet.navn(), underliggendeOrgEnhet.id()));
            }

            if (underliggendeOrgEnhet.gyldigTom().isAfter(avvikleOrgEnhetRequest.gyldigTom())) {
                throw new IllegalStateException(String.format("Underliggende enhet %s (%s) har sluttdato etter %s (%s)", underliggendeOrgEnhet.navn(), underliggendeOrgEnhet.id(), orgEnhet.navn(), orgEnhet.id()));
            }

            validerOrgtilknytningGyldigTom(underliggendeOrgEnhet, underliggendeOrgEnhet.gyldigTom());
        });

        InputMetadata inputMetadata = new InputMetadata(
            "REST-API",
            "avvikleOrgEnhet",
            Map.of("id", orgEnhetId, "value", avvikleOrgEnhetRequest),
            new Origination(
                getAuthenticatedNavIdent(),
                SecurityUtil.getAuthenticatedAzpName()
            )
        );

        orgEnhetCommandService.avvikleOrgEnhet(orgEnhet.fid(), new AvvikleOrgEnhet(avvikleOrgEnhetRequest.gyldigTom(), inputMetadata));


        var organisering = organiseringStateRepository.findActiveById(orgEnhetId).orElseThrow(() -> OrganiseringNotFoundException.id(orgEnhetId));
        organiseringEventCommandService.avsluttOrganisering(organisering.fid(), new AvsluttOrganiseringCmd(avvikleOrgEnhetRequest.gyldigTom(), inputMetadata));

        return StandardResponse.builder()
            .message(String.format("Enhet %s (%s) har fått satt sluttdato til %s", orgEnhet.navn(), orgEnhet.id(), avvikleOrgEnhetRequest.gyldigTom()))
            .build();
    }

    private void validerOrgtilknytningGyldigTom(OrgEnhet orgEnhet, LocalDate gyldigTom) {
        orgEnhet.ressursKoblinger().forEach(ressurs -> {
            String format = String.format(
                "En eller flere ressurser i enhet %s (%s) har tilknytning til enheten etter gitt dato %s",
                orgEnhet.id(),
                orgEnhet.navn(),
                gyldigTom
            );
            if (isNull(ressurs.gyldigTom())) {
                throw new IllegalStateException(format);
            }

            if (ressurs.gyldigTom().isAfter(gyldigTom)) {
                throw new IllegalStateException(format);
            }
        });
    }
}
