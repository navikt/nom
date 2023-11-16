package no.nav.nom.apps.api.ressurs.api.app;

import com.fasterxml.jackson.databind.JsonNode;
import no.nav.nom.apps.api.graphql.common.SecurityUtil;
import no.nav.nom.apps.api.nik.NIKClient;
import no.nav.nom.apps.api.nik.OpprettNavidentRequest;
import no.nav.nom.apps.api.nik.OpprettNavidentResponse;
import no.nav.nom.apps.api.orgenhet.eventsourcing.state.OrgEnhetStateRepository;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.OrgtilknytningCommandService;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.AvvikleOrgtilknytning;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.EndreDagligOppfolgingOrgtilknytning;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.OpprettOrgtilknytning;
import no.nav.nom.apps.api.pdl.PdlClient;
import no.nav.nom.apps.api.ressurs.api.dto.Ressurs;
import no.nav.nom.apps.api.ressurs.api.dto.RessursLeder;
import no.nav.nom.apps.api.ressurs.api.exceptions.InvalidSektorException;
import no.nav.nom.apps.api.ressurs.email.RessursOpprettetEmailException;
import no.nav.nom.apps.api.ressurs.email.RessursOpprettetEmailService;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.RessursCommadService;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OppdaterPersonaliaCommand;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OpphorRessursCommand;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OpprettRessursCommand;
import no.nav.nom.apps.api.ressurs.api.exceptions.RessursNotFoundException;
import no.nav.nom.apps.api.ressurs.eventsourcing.state.RessursStateRepository;
import no.nav.nom.apps.api.ressurs.masterdata.NomMastered;
import no.nav.nom.apps.api.topicreader.KafkaTopicReader;
import no.nav.nom.apps.api.topicreader.ReadTopicRequest;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.Origination;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.contract.nom.state.RessursState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static no.nav.nom.apps.api.graphql.common.SecurityUtil.getAuthenticatedNavIdent;

@Service
public class RessursRestService {

    private final static Logger log = LoggerFactory.getLogger(RessursRestService.class);

    private final RessursStateRepository ressursStateRepository;
    private final RessursCommadService ressursCommadService;
    private final OrgtilknytningCommandService orgtilknytningCommandService;
    private final NIKClient nikClient;
    private final PdlClient pdlClient;
    private final OrgEnhetStateRepository orgEnhetStateRepository;
    private final RessursOpprettetEmailService ressursOpprettetEmailService;
    private final RessursContactDetailsResolver ressursContactDetailsResolver;
    private final RessursLedereResolver ressursLedereResolver;
    private final KafkaTopicReader kafkaTopicReader;
    private final NomMastered nomMastered;

    public RessursRestService(
        RessursStateRepository ressursStateRepository,
        RessursCommadService ressursCommadService,
        OrgtilknytningCommandService orgtilknytningCommandService,
        NIKClient nikClient,
        PdlClient pdlClient,
        OrgEnhetStateRepository orgEnhetStateRepository,
        RessursOpprettetEmailService ressursOpprettetEmailService,
        RessursContactDetailsResolver ressursContactDetailsResolver,
        RessursLedereResolver ressursLedereResolver,
        KafkaTopicReader kafkaTopicReader,
        NomMastered nomMastered
    ) {
        this.ressursStateRepository = ressursStateRepository;
        this.ressursCommadService = ressursCommadService;
        this.orgtilknytningCommandService = orgtilknytningCommandService;
        this.nikClient = nikClient;
        this.pdlClient = pdlClient;
        this.orgEnhetStateRepository = orgEnhetStateRepository;
        this.ressursOpprettetEmailService = ressursOpprettetEmailService;
        this.kafkaTopicReader = kafkaTopicReader;
        this.ressursContactDetailsResolver = ressursContactDetailsResolver;
        this.ressursLedereResolver = ressursLedereResolver;
        this.nomMastered = nomMastered;
    }

    public Ressurs findRessursByNavident(String navident) {
        var ressursState = findRessursStateByNavident(navident);
        return enrichRessursStateWithDvhData(ressursState);
    }

    private RessursState findRessursStateByNavident(String navident) {
        return ressursStateRepository.findByNavident(navident).orElseThrow(() -> RessursNotFoundException.navident(navident));
    }

    public Ressurs findRessursByFid(String fid) {
        var ressursState = ressursStateRepository.findByFid(fid).orElseThrow(() -> RessursNotFoundException.fid(fid));
        return enrichRessursStateWithDvhData(ressursState);
    }

    private Ressurs enrichRessursStateWithDvhData(RessursState ressursState) {
        var telefoner = ressursContactDetailsResolver.fetchTelefoner(ressursState.navident());
        var epost = ressursContactDetailsResolver.fetchEmail(ressursState.navident());

        return new Ressurs(
            ressursState,
            telefoner,
            epost.orElse(null)
        );
    }

    public void updateRessursNavn(String navident, UpdateVisningsnavnRequest updateVisningsnavnRequest) {
        var ressursState = findRessursStateByNavident(navident);

        if (ressursState.sektor() != Sektor.EKSTERN){
            throw new InvalidSektorException("Kun eksterne ressurser kan endres");
        }

        var oppdaterPersonaliaCommand = new OppdaterPersonaliaCommand(
            ressursState.fid(),
            new Visningsnavn(updateVisningsnavnRequest.fornavn(), updateVisningsnavnRequest.mellomnavn(), updateVisningsnavnRequest.etternavn())
        );

        var inputMetadata = new InputMetadata("REST", "updateRessursNavn", updateVisningsnavnRequest, new Origination(
            getAuthenticatedNavIdent(),
            SecurityUtil.getAuthenticatedAzpName()
        ));

        ressursCommadService.oppdaterPersonaliaEvent(oppdaterPersonaliaCommand, inputMetadata);
        nomMastered.markRessursAsMasteredInNom(navident);
    }

    public List<JsonNode> getRessursEvents(ReadTopicRequest readTopicRequest) {
        return kafkaTopicReader.getRessursEvents(readTopicRequest.maxRecords(), readTopicRequest.fromOffset(), readTopicRequest.partition());
    }

    public List<RessursLeder> getLedereForRessurs(String navident) {
        return ressursLedereResolver.ledere(navident);
    }

    public RessursState createRessursAndNotify(
        OpprettRessursRequest request
    ) {

        return ressursStateRepository.findByPersonident(request.getPersonident()).orElseGet(() -> {
            final var createRessursResponse = createRessurs(request);

            try {
                ressursOpprettetEmailService.notifyByEmail(createRessursResponse.navident(), request.getOrgEnhetId());
            } catch (RessursOpprettetEmailException exception) {
                //TODO: consider handling exception if email sending is important
                log.warn("Epost varsling om opprettelse av ressurs til orgenhet leder feilet. {}", exception.getMessage());
            }

            return createRessursResponse;
        });

    }

    private RessursState createRessurs(OpprettRessursRequest request) {

        final var opprettNavidentResponse = nikClient.opprettNavidentOnBehalfOfUser(
            OpprettNavidentRequest.builder()
                .personid(request.getPersonident())
                .identPrefix(request.getVisningsetternavn().substring(0, 1))
                .identType("STANDARD")
                .build()
        );

        if (!opprettNavidentResponse.getStatusCode().is2xxSuccessful()) {
            throw new ResponseStatusException(
                opprettNavidentResponse.getStatusCode(),
                Optional.ofNullable(opprettNavidentResponse.getBody())
                    .map(OpprettNavidentResponse::getFeilmelding)
                    .orElse("NIK connection error")
            );
        }

        if (opprettNavidentResponse.getBody() != null && opprettNavidentResponse.getBody().getFeilmelding() != null) {
            throw new ResponseStatusException(
                opprettNavidentResponse.getStatusCode(),
                opprettNavidentResponse.getBody().getFeilmelding()
            );
        }

        final var pdlPerson = pdlClient.hentPerson(request.getPersonident());

        if (request.getVisningsmellomnavn() != null && request.getVisningsmellomnavn().isBlank()) {
            request.setVisningsmellomnavn(null);
        }

        log.debug("createRessurs for {} - {}", opprettNavidentResponse.getBody().getNavident(), request.getVisningsetternavn());

        var metaData = new InputMetadata(
            "GraphQL",
            "opprettRessurs",
            request,
            new Origination(
                getAuthenticatedNavIdent(),
                SecurityUtil.getAuthenticatedAzpName()
            )
        );

        final var opprettRessursCommand = new OpprettRessursCommand(
            request.getPersonident(),
            opprettNavidentResponse.getBody().getNavident(),
            pdlPerson,
            request.getSektor(),
            new Visningsnavn(
                request.getVisningsfornavn(),
                Optional.ofNullable(request.getVisningsmellomnavn()),
                request.getVisningsetternavn()
            ),
            metaData,
            request.getStartDato(),
            Optional.ofNullable(request.getOrgEnhetId())
                .flatMap(orgEnhetStateRepository::findById)
                .flatMap(OrgEnhetState::agressoId)
                .orElse(null), // TODO: Validate not null in RessursCommandService
            request.getAnsettelsesType()
        );


        try {
            RessursState opprettetRessurs = ressursCommadService.createRessursSync(opprettRessursCommand);
            nomMastered.markRessursAsMasteredInNom(opprettetRessurs.navident());

            // If ressurs is EKSTERN we fire a separate "avsluttRessursEvent" so that other systems, such as skjerming, know the ressurs already has a sluttDato.
            if (request.getSektor() == Sektor.EKSTERN && request.getSluttDato() != null) {
                ressursCommadService.avsluttRessursEvent(
                    new OpphorRessursCommand(opprettetRessurs.fid(), request.getSluttDato()),
                    metaData
                );

                var orgTilknytningFid = orgtilknytningCommandService.opprettOrgtilknytning(
                    new OpprettOrgtilknytning(
                        opprettetRessurs.navident(),
                        request.getOrgEnhetId(),
                        request.getStartDato(),
                        metaData,
                        null
                    )
                );

                orgtilknytningCommandService.avvikleOrgtilknytning(
                    orgTilknytningFid,
                    new AvvikleOrgtilknytning(
                        opprettetRessurs.navident(),
                        request.getOrgEnhetId(),
                        request.getSluttDato(),
                        metaData,
                        null
                    )
                );

                orgtilknytningCommandService.endreDagligOppfolgingOrgtilknytning(
                    orgTilknytningFid,
                    new EndreDagligOppfolgingOrgtilknytning(
                        opprettetRessurs.navident(),
                        request.getOrgEnhetId(),
                        request.getStartDato(),
                        Optional.of(request.getSluttDato()),
                        true,
                        metaData,
                        null
                    )
                );
            }

            return opprettetRessurs;
        } catch (ExecutionException | TimeoutException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
