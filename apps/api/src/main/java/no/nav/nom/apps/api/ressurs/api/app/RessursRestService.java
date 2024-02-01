package no.nav.nom.apps.api.ressurs.api.app;

import com.fasterxml.jackson.databind.JsonNode;
import no.nav.nom.apps.api.graphql.common.SecurityUtil;
import no.nav.nom.apps.api.nik.NIKClient;
import no.nav.nom.apps.api.nik.OpprettNavidentRequest;
import no.nav.nom.apps.api.nik.OpprettNavidentResponse;
import no.nav.nom.apps.api.orgenhet.eventsourcing.state.OrgEnhetStateRepository;
import no.nav.nom.apps.api.orgtilknytning.api.app.OrgtilknytningRestService;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.OrgtilknytningCommandService;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.AvvikleOrgtilknytning;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.EndreDagligOppfolgingOrgtilknytning;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.EndreTilknytningtype;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.OpprettOrgtilknytning;
import no.nav.nom.apps.api.pdl.PdlClient;
import no.nav.nom.apps.api.ressurs.api.dto.Ressurs;
import no.nav.nom.apps.api.ressurs.api.dto.RessursLeder;
import no.nav.nom.apps.api.ressurs.api.exceptions.InvalidSektorException;
import no.nav.nom.apps.api.ressurs.email.RessursOpprettetEmailException;
import no.nav.nom.apps.api.ressurs.email.RessursOpprettetEmailService;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.RessursCommadService;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OppdaterFysiskeBrukerstederCommand;
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

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static no.nav.nom.apps.api.common.DateUtils.validateChronologicalDates;
import static no.nav.nom.apps.api.graphql.common.SecurityUtil.getAuthenticatedNavIdent;
import static no.nav.nom.apps.api.orgtilknytning.api.app.OrgtilknytningRestService.validateOrgtilknytningTypeSektorCombination;

@Service
public class RessursRestService {

    private final static Logger logger = LoggerFactory.getLogger(RessursRestService.class);

    private final RessursStateRepository ressursStateRepository;
    private final RessursCommadService ressursCommadService;
    private final OrgtilknytningCommandService orgtilknytningCommandService;
    private final OrgtilknytningRestService orgtilknytningRestService;
    private final NIKClient nikClient;
    private final PdlClient pdlClient;
    private final NomRemedyIntegrasjonClient nomRemedyIntegrasjonClient;
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
        OrgtilknytningRestService orgtilknytningRestService,
        NIKClient nikClient,
        PdlClient pdlClient,
        NomRemedyIntegrasjonClient nomRemedyIntegrasjonClient, OrgEnhetStateRepository orgEnhetStateRepository,
        RessursOpprettetEmailService ressursOpprettetEmailService,
        RessursContactDetailsResolver ressursContactDetailsResolver,
        RessursLedereResolver ressursLedereResolver,
        KafkaTopicReader kafkaTopicReader,
        NomMastered nomMastered
    ) {
        this.ressursStateRepository = ressursStateRepository;
        this.ressursCommadService = ressursCommadService;
        this.orgtilknytningCommandService = orgtilknytningCommandService;
        this.orgtilknytningRestService = orgtilknytningRestService;
        this.nikClient = nikClient;
        this.pdlClient = pdlClient;
        this.nomRemedyIntegrasjonClient = nomRemedyIntegrasjonClient;
        this.orgEnhetStateRepository = orgEnhetStateRepository;
        this.ressursOpprettetEmailService = ressursOpprettetEmailService;
        this.kafkaTopicReader = kafkaTopicReader;
        this.ressursContactDetailsResolver = ressursContactDetailsResolver;
        this.ressursLedereResolver = ressursLedereResolver;
        this.nomMastered = nomMastered;
    }

    public List<RessursState> findRessurser(RessursFilter ressursFilter) {
        return ressursStateRepository.getAll().stream()
            .filter(ressursState -> (ressursFilter.sektor().isEmpty() || ressursState.sektor().equals(ressursFilter.sektor().get()))
                && (ressursFilter.sluttdato().isEmpty() || ressursState.sluttdato().equals(ressursFilter.sluttdato())))
            .map(ressursState -> ressursState.toBuilder()
                .personident(ressursState.personident().replaceFirst(".{5}$", "*****"))
                .build()
            )
            .toList();
    }

    public Ressurs findRessursByNavident(String navident, Boolean obfuscatePersonident) {
        var ressursState = findRessursStateByNavident(navident);
        return obfuscatePersonidentAndEnrichRessursStateWithDvhData(ressursState, obfuscatePersonident);
    }

    private RessursState findRessursStateByNavident(String navident) {
        return ressursStateRepository.findByNavident(navident).orElseThrow(() -> RessursNotFoundException.navident(navident));
    }

    private RessursState findRessursStateByFid(String fid) {
        return ressursStateRepository.findByFid(fid).orElseThrow(() -> RessursNotFoundException.fid(fid));
    }

    public Ressurs findRessursByFid(String fid, Boolean obfuscatePersonident) {
        var ressursState = findRessursStateByFid(fid);
        return obfuscatePersonidentAndEnrichRessursStateWithDvhData(ressursState, obfuscatePersonident);
    }

    private Ressurs obfuscatePersonidentAndEnrichRessursStateWithDvhData(RessursState ressursState, Boolean obfuscatePersonident) {
        var telefoner = ressursContactDetailsResolver.fetchTelefoner(ressursState.navident());
        var epost = ressursContactDetailsResolver.fetchEmail(ressursState.navident());
        var personident = obfuscatePersonident
            ? ressursState.personident().replaceFirst(".{5}$", "*****")
            : ressursState.personident();

        return new Ressurs(
            ressursState.toBuilder()
                .personident(personident).build(),
            telefoner,
            epost.orElse(null),
            nomMastered.isMastered(ressursState.navident())
        );
    }

    public void updateRessursNavn(String navident, UpdateVisningsnavnRequest updateVisningsnavnRequest) {
        var ressursState = findRessursStateByNavident(navident);

        if (ressursState.sektor() != Sektor.EKSTERN) {
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

    public void updateFysiskeBrukersteder(String fid, UpdateFysiskeBrukerstederRequest updateFysiskeBrukerstederRequest) {
        if (updateFysiskeBrukerstederRequest.primaryFysiskBrukersted() == null || updateFysiskeBrukerstederRequest.primaryFysiskBrukersted().isBlank()) {
            throw new IllegalArgumentException("primaryFysiskBrukersted cannot be blank or null");
        }
        if (!updateFysiskeBrukerstederRequest.fysiskeBrukersteder().contains(updateFysiskeBrukerstederRequest.primaryFysiskBrukersted())) {
            throw new IllegalArgumentException("Value for primaryFysiskBrukersted must be included in fysiske brukersteder");
        }

        validateFysiskeBrukerstederExists(updateFysiskeBrukerstederRequest.fysiskeBrukersteder());

        var ressursState = ressursStateRepository.findByFid(fid).orElseThrow(() -> RessursNotFoundException.fid(fid));

        var existingFysiskeBrukerstederForRessurs = Optional.ofNullable(ressursState.fysiskeBrukersteder()).orElse(Collections.emptyList());

        var requestContainsNewValues = !Set.copyOf(existingFysiskeBrukerstederForRessurs).equals(updateFysiskeBrukerstederRequest.fysiskeBrukersteder())
            || !ressursState.primaryFysiskBrukersted().equals(Optional.of(updateFysiskeBrukerstederRequest.primaryFysiskBrukersted()));

        if (requestContainsNewValues) {

            ressursCommadService.oppdaterFysiskeBrukerstederCommand(
                OppdaterFysiskeBrukerstederCommand.builder()
                    .fid(ressursState.fid())
                    .primaryFysiskBrukersted(updateFysiskeBrukerstederRequest.primaryFysiskBrukersted())
                    .fysiskeBrukersteder(updateFysiskeBrukerstederRequest.fysiskeBrukersteder())
                    .build(),
                new InputMetadata(
                    "REST-API",
                    "updateFysiskeBrukersteder",
                    Map.of("navident", ressursState.navident()),
                    new Origination(
                        getAuthenticatedNavIdent(),
                        SecurityUtil.getAuthenticatedAzpName()
                    )
                )
            );
        } else {
            logger.info("UpdateFysiskeBrukerstederRequest contained no new values, skipping update on ressurs with fid {}", fid);
        }
    }

    private void validateFysiskeBrukerstederExists(Set<String> brukerstedNrs) {
        var existingBrukerstedNrs = nomRemedyIntegrasjonClient.getFysiskeBrukerstedNrs(brukerstedNrs.stream().toList());

        if (!existingBrukerstedNrs.containsAll(brukerstedNrs)) {
            var nonExistingBrukerstedNrs = brukerstedNrs.stream().filter(existingBrukerstedNrs::contains)
                .collect(Collectors.toSet());

            throw new IllegalArgumentException(String.format(
                "Fysiske brukersteder with brukerstedNr %s does not exist",
                nonExistingBrukerstedNrs
            ));
        }
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
        validateOpprettRessursRequest(request);

        return ressursStateRepository.findByPersonident(request.personident()).orElseGet(() -> {
            final var createdRessurs = createRessurs(request);

            try {
                ressursOpprettetEmailService.notifyByEmail(createdRessurs.navident(), request.orgEnhetId());
            } catch (RessursOpprettetEmailException exception) {
                //TODO: consider handling exception if email sending is important
                logger.warn("Epost varsling om opprettelse av ressurs til orgenhet leder feilet. {}", exception.getMessage());
            }

            return createdRessurs;
        });

    }

    private RessursState createRessurs(OpprettRessursRequest request) {

        final var opprettNavidentResponse = nikClient.opprettNavidentOnBehalfOfUser(
            OpprettNavidentRequest.builder()
                .personid(request.personident())
                .identPrefix(request.visningsetternavn().substring(0, 1))
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

        final var pdlPerson = pdlClient.hentPerson(request.personident());

        if (request.visningsmellomnavn() != null && request.visningsmellomnavn().isBlank()) {
            request = request.toBuilder().visningsmellomnavn(null).build();
        }

        logger.debug("createRessurs for {} - {}", opprettNavidentResponse.getBody().getNavident(), request.visningsetternavn());

        var metaData = new InputMetadata(
            "REST-API",
            "opprettRessurs",
            request,
            new Origination(
                getAuthenticatedNavIdent(),
                SecurityUtil.getAuthenticatedAzpName()
            )
        );

        final var opprettRessursCommand = new OpprettRessursCommand(
            request.personident(),
            opprettNavidentResponse.getBody().getNavident(),
            pdlPerson,
            request.sektor(),
            new Visningsnavn(
                request.visningsfornavn(),
                Optional.ofNullable(request.visningsmellomnavn()),
                request.visningsetternavn()
            ),
            metaData,
            request.startDato(),
            Optional.ofNullable(request.orgEnhetId())
                .flatMap(orgEnhetStateRepository::findById)
                .flatMap(OrgEnhetState::agressoId)
                .orElse(null), // TODO: Validate not null in RessursCommandService
            request.ansettelsesType(),
            request.fysiskBrukerstedNr()
        );


        try {
            RessursState opprettetRessurs = ressursCommadService.createRessursSync(opprettRessursCommand);
            nomMastered.markRessursAsMasteredInNom(opprettetRessurs.navident());

            // If ressurs is EKSTERN we fire a separate "avsluttRessursEvent" so that other systems, such as skjerming, know the ressurs already has a sluttDato.
            if (request.sektor() == Sektor.EKSTERN && request.sluttDato().isPresent()) {
                ressursCommadService.avsluttRessursEvent(
                    new OpphorRessursCommand(opprettetRessurs.fid(), request.sluttDato().get()),
                    metaData
                );

                var orgTilknytningFid = orgtilknytningCommandService.opprettOrgtilknytning(
                    new OpprettOrgtilknytning(
                        opprettetRessurs.navident(),
                        request.orgEnhetId(),
                        request.startDato(),
                        metaData,
                        null
                    )
                );

                orgtilknytningCommandService.avvikleOrgtilknytning(
                    orgTilknytningFid,
                    new AvvikleOrgtilknytning(
                        opprettetRessurs.navident(),
                        request.orgEnhetId(),
                        request.sluttDato().get(),
                        metaData,
                        null
                    )
                );

                orgtilknytningCommandService.endreDagligOppfolgingOrgtilknytning(
                    orgTilknytningFid,
                    new EndreDagligOppfolgingOrgtilknytning(
                        opprettetRessurs.navident(),
                        request.orgEnhetId(),
                        request.startDato(),
                        request.sluttDato(),
                        true,
                        metaData,
                        null
                    )
                );

                orgtilknytningCommandService.endreTilknytningtype(
                    orgTilknytningFid,
                    new EndreTilknytningtype(
                        Optional.of(request.tilknytningtype()),
                        metaData
                    )
                );
            }

            return opprettetRessurs;
        } catch (ExecutionException | TimeoutException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void avvikleRessurs(String fid, AvvikleRessursRequest request) {
        var ressursState = findRessursStateByFid(fid);
        var metaData = new InputMetadata(
            "REST",
            "avvikleRessurs",
            request,
            new Origination(
                getAuthenticatedNavIdent(),
                SecurityUtil.getAuthenticatedAzpName()
            )
        );

        ressursCommadService.avsluttRessursEvent(
            new OpphorRessursCommand(fid, request.sluttDato()),
            metaData
        );

        var orgtilknytninger = orgtilknytningRestService.getOrgtilknytninger(Optional.of(ressursState.navident()));

        orgtilknytninger.forEach(orgtilknytningState -> {
            var gyldigTomIsAfterAvvikletDato = orgtilknytningState.gyldigTom().orElse(LocalDate.MAX).isAfter(request.sluttDato());
            if (gyldigTomIsAfterAvvikletDato) {
                orgtilknytningCommandService.avvikleOrgtilknytning(
                    orgtilknytningState.fid(),
                    new AvvikleOrgtilknytning(
                        ressursState.navident(),
                        orgtilknytningState.orgEnhetId(),
                        request.sluttDato(),
                        metaData,
                        null
                    )
                );
            }
        });

        nomMastered.markRessursAsMasteredInNom(ressursState.navident());
    }

    private void validateOpprettRessursRequest(OpprettRessursRequest request) {
        if (request.sektor() != Sektor.EKSTERN) {
            throw InvalidSektorException.actionNotAllowedForSektor(request.sektor());
        }
        validateChronologicalDates(request.startDato(), request.sluttDato().orElse(null));
        if (request.sluttDato().isEmpty()) {
            throw new IllegalArgumentException("Sluttdato må oppgis for opprettelse av ekstern ressurs");
        }
        validateFysiskeBrukerstederExists(Set.of(request.fysiskBrukerstedNr()));
        validateOrgtilknytningTypeSektorCombination(request.tilknytningtype(), request.sektor());

    }
}
