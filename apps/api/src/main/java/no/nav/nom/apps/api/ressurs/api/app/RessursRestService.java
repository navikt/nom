package no.nav.nom.apps.api.ressurs.api.app;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.authorize.AuthResolver;
import no.nav.nom.apps.api.clients.msgraph.EmailMessage;
import no.nav.nom.apps.api.clients.msgraph.MicrosoftGraphClient;
import no.nav.nom.apps.api.common.security.NomApiAccessScope;
import no.nav.nom.apps.api.graphql.common.SecurityUtil;
import no.nav.nom.apps.api.nik.NIKClient;
import no.nav.nom.apps.api.nik.OpprettNavidentRequest;
import no.nav.nom.apps.api.nik.OpprettNavidentResponse;
import no.nav.nom.apps.api.orgtilknytning.OrgtilknytningService;
import no.nav.nom.apps.api.orgtilknytning.api.app.OrgtilknytningRestService;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.OrgtilknytningCommandService;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.AvvikleOrgtilknytning;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.EndreDagligOppfolgingOrgtilknytning;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.EndreTilknytningtypeCommand;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.FjernOrgtilknytning;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.OpprettOrgtilknytning;
import no.nav.nom.apps.api.pdl.PdlClient;
import no.nav.nom.apps.api.remedy.RemedyEnhet;
import no.nav.nom.apps.api.remedy.RemedyEnhetMapperService;
import no.nav.nom.apps.api.ressurs.FysiskBrukerstedService;
import no.nav.nom.apps.api.ressurs.NomMastered;
import no.nav.nom.apps.api.ressurs.api.dto.Ressurs;
import no.nav.nom.apps.api.ressurs.api.dto.RessursLeder;
import no.nav.nom.apps.api.ressurs.api.exceptions.DuplicateRessursException;
import no.nav.nom.apps.api.ressurs.api.exceptions.RessursNotFoundException;
import no.nav.nom.apps.api.ressurs.email.RessursOpprettetEmailException;
import no.nav.nom.apps.api.ressurs.email.RessursOpprettetEmailService;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.RessursCommandService;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OppdaterFysiskeBrukerstederCommand;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OppdaterPersonaliaCommand;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OppdaterPersonidentCommand;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OppdaterPrimaryTelefonnummerCommand;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OppdaterSektorCommand;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OppdaterSluttdatoCommand;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OpphorRessursCommand;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OpprettRessursCommand;
import no.nav.nom.apps.api.topicreader.KafkaTopicReader;
import no.nav.nom.apps.api.topicreader.ReadTopicRequest;
import no.nav.nom.contract.nom.IdentType;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.TelefonType;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.Origination;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepository;
import no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateRepository;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static no.nav.nom.apps.api.common.DateUtils.validateChronologicalDates;
import static no.nav.nom.apps.api.common.security.NomApiAccessScope.ADMIN;
import static no.nav.nom.apps.api.common.security.NomApiAccessScope.NOM_WRITE_ROBOT;
import static no.nav.nom.apps.api.common.security.NomApiAccessScope.NOM_WRITE_UTL_KONSULENT;
import static no.nav.nom.apps.api.common.security.NomApiAccessScope.NOM_WRITE_VIKAFOSSEN;
import static no.nav.nom.apps.api.graphql.common.SecurityUtil.getAuthenticatedNavIdent;
import static no.nav.nom.apps.api.graphql.common.SecurityUtil.loggedInUserHasAccessScope;
import static no.nav.nom.apps.api.orgtilknytning.api.app.OrgtilknytningRestService.validateOrgtilknytningTypeSektorCombination;
import static org.apache.commons.lang3.StringUtils.trimToNull;

@Slf4j
@Service
public class RessursRestService {

    private final static Logger logger = LoggerFactory.getLogger(RessursRestService.class);

    private final RessursStateRepository ressursStateRepository;
    private final OrgtilknytningStateRepository orgtilknytningStateRepository;
    private final RessursCommandService ressursCommandService;
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
    private final AuthResolver authResolver;
    private final FysiskBrukerstedService fysiskBrukerstedService;
    private final RemedyEnhetMapperService remedyEnhetMapperService;
    private final OrgtilknytningService orgtilknytningService;
    private final MicrosoftGraphClient microsoftGraphClient;

    @Value("${app.orgenhet.vikafossenId}")
    private String vikafossenOrgEnhetId;

    public RessursRestService(
        RessursStateRepository ressursStateRepository,
        OrgtilknytningStateRepository orgtilknytningStateRepository,
        RessursCommandService ressursCommandService,
        OrgtilknytningCommandService orgtilknytningCommandService,
        OrgtilknytningRestService orgtilknytningRestService,
        NIKClient nikClient,
        PdlClient pdlClient,
        NomRemedyIntegrasjonClient nomRemedyIntegrasjonClient, OrgEnhetStateRepository orgEnhetStateRepository,
        RessursOpprettetEmailService ressursOpprettetEmailService,
        RessursContactDetailsResolver ressursContactDetailsResolver,
        RessursLedereResolver ressursLedereResolver,
        KafkaTopicReader kafkaTopicReader,
        NomMastered nomMastered,
        AuthResolver authResolver, FysiskBrukerstedService fysiskBrukerstedService, RemedyEnhetMapperService remedyEnhetMapperService,
        OrgtilknytningService orgtilknytningService, MicrosoftGraphClient microsoftGraphClient
    ) {
        this.ressursStateRepository = ressursStateRepository;
        this.orgtilknytningStateRepository = orgtilknytningStateRepository;
        this.ressursCommandService = ressursCommandService;
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
        this.authResolver = authResolver;
        this.fysiskBrukerstedService = fysiskBrukerstedService;
        this.remedyEnhetMapperService = remedyEnhetMapperService;
        this.orgtilknytningService = orgtilknytningService;
        this.microsoftGraphClient = microsoftGraphClient;
    }

    public List<RessursState> findRessurser(RessursFilter ressursFilter) {

        var showFysiskBrukersted = loggedInUserHasAccessScope(ADMIN) ||
            loggedInUserHasAccessScope(NomApiAccessScope.NOM_REMEDY_INTEGRASJON_APP);

        return ressursStateRepository.getAll().stream()
            .filter(ressursState -> (ressursFilter.sektor() == null || ressursState.sektor().equals(ressursFilter.sektor()))
                && (ressursFilter.sluttdato() == null || ressursState.sluttdato().equals(ressursFilter.sluttdato())))
            .map(ressursState -> ressursState.toBuilder()
                .personident(ressursState.personident().replaceFirst(".{5}$", "*****"))
                .primaryFysiskBrukersted(showFysiskBrukersted ? ressursState.primaryFysiskBrukersted() : null)
                .fysiskeBrukersteder(showFysiskBrukersted ? ressursState.fysiskeBrukersteder() : Collections.emptyList())
                .build()
            )
            .toList();
    }

    public Ressurs findRessursByNavident(String navident) {
        var ressursState = findRessursStateByNavident(navident);
        return hideSensitiveDataAndEnrichRessursStateWithDvhData(ressursState);
    }

    public List<Ressurs> findRessurserByNavident(List<String> navidenter) {
        var ressursStates = ressursStateRepository.findByNavidentBulk(navidenter);
        var eposter = ressursContactDetailsResolver.fetchEmails(navidenter);

        return ressursStates.values().stream()
            .map(ressursState -> hideSensitiveDataAndEnrichRessursStateWithDvhData(ressursState, eposter.get(ressursState.navident()).orElse(null)))
            .toList();
    }

    RessursState findRessursStateByNavident(String navident) {
        return ressursStateRepository.findByNavident(navident).orElseThrow(() -> RessursNotFoundException.navident(navident));
    }

    RessursState findRessursStateByFid(String fid) {
        return ressursStateRepository.findByFid(fid).orElseThrow(() -> RessursNotFoundException.fid(fid));
    }

    public Ressurs findRessursByFid(String fid) {
        var ressursState = findRessursStateByFid(fid);
        return hideSensitiveDataAndEnrichRessursStateWithDvhData(ressursState);
    }

    private Ressurs hideSensitiveDataAndEnrichRessursStateWithDvhData(RessursState ressursState) {

        var showPersonident = loggedInUserHasAccessScope(ADMIN) ||
            loggedInUserHasAccessScope(NomApiAccessScope.NOM_REMEDY_INTEGRASJON_APP) ||
            getAuthenticatedNavIdent().equals(ressursState.navident());

        var showFysiskBrukerSted = isShowFysiskBrukerSted(ressursState);

        var telefoner = ressursContactDetailsResolver.fetchTelefoner(ressursState.navident());
        var epost = ressursContactDetailsResolver.fetchEmail(ressursState.navident());

        var personident = showPersonident
            ? ressursState.personident()
            : ressursState.personident().replaceFirst(".{5}$", "*****");


        return new Ressurs(
            ressursState.toBuilder()
                .personident(personident)
                .primaryTelefonnummer(ressursState.primaryTelefonnummer())
                .primaryFysiskBrukersted(showFysiskBrukerSted ? ressursState.primaryFysiskBrukersted() : null)
                .fysiskeBrukersteder(showFysiskBrukerSted ? ressursState.fysiskeBrukersteder() : Collections.emptyList())
                .build(),
            telefoner,
            epost.orElse(null),
            nomMastered.isMastered(ressursState.navident())
        );
    }

    private boolean isShowFysiskBrukerSted(RessursState ressursState) {
        return loggedInUserHasAccessScope(ADMIN) ||
            loggedInUserHasAccessScope(NomApiAccessScope.READONLY) ||
            loggedInUserHasAccessScope(NomApiAccessScope.NOM_REMEDY_INTEGRASJON_APP) ||
            authResolver.kanRedigereRessurs(ressursState.navident()) ||
            getAuthenticatedNavIdent().equals(ressursState.navident());
    }

    private Ressurs hideSensitiveDataAndEnrichRessursStateWithDvhData(RessursState ressursState, String epost) {

        var showPersonident = loggedInUserHasAccessScope(ADMIN) ||
            loggedInUserHasAccessScope(NomApiAccessScope.NOM_REMEDY_INTEGRASJON_APP) ||
            getAuthenticatedNavIdent().equals(ressursState.navident());

        var showFysiskBrukerSted = loggedInUserHasAccessScope(ADMIN) ||
            loggedInUserHasAccessScope(NomApiAccessScope.NOM_REMEDY_INTEGRASJON_APP) ||
            authResolver.kanRedigereRessurs(ressursState.navident()) ||
            getAuthenticatedNavIdent().equals(ressursState.navident());

        var telefoner = ressursContactDetailsResolver.fetchTelefoner(ressursState.navident());

        var personident = showPersonident
            ? ressursState.personident()
            : ressursState.personident().replaceFirst(".{5}$", "*****");

        return new Ressurs(
            ressursState.toBuilder()
                .personident(personident)
                .primaryTelefonnummer(ressursState.primaryTelefonnummer())
                .primaryFysiskBrukersted(showFysiskBrukerSted ? ressursState.primaryFysiskBrukersted() : null)
                .fysiskeBrukersteder(showFysiskBrukerSted ? ressursState.fysiskeBrukersteder() : Collections.emptyList())
                .build(),
            telefoner,
            epost,
            nomMastered.isMastered(ressursState.navident())
        );
    }

    public void updateRessursNavn(String navident, UpdateVisningsnavnRequest updateVisningsnavnRequest) {
        var ressursState = findRessursStateByNavident(navident);
        validateIngenMellomromEtternavn(updateVisningsnavnRequest.etternavn());

        var oppdaterPersonaliaCommand = new OppdaterPersonaliaCommand(
            ressursState.fid(),
            new Visningsnavn(
                updateVisningsnavnRequest.fornavn().trim(),
                updateVisningsnavnRequest.mellomnavn().map(StringUtils::trimToNull),
                updateVisningsnavnRequest.etternavn()
            )
        );

        var inputMetadata = new InputMetadata("REST", "updateRessursNavn", updateVisningsnavnRequest, new Origination(
            getAuthenticatedNavIdent(),
            SecurityUtil.getAuthenticatedAzpName()
        ));

        ressursCommandService.oppdaterPersonaliaEvent(oppdaterPersonaliaCommand, inputMetadata);
        nomMastered.markRessursAsMasteredInNom(navident);
    }

    public void updateRessursPrimaryTelefonnummer(String navident, UpdatePrimarytelefonnummer updatePrimarytelefonnummer) {
        var ressursState = findRessursStateByNavident(navident);

        var oppdaterMobilnummerCommand = new OppdaterPrimaryTelefonnummerCommand(
            ressursState.fid(),
            trimToNull(updatePrimarytelefonnummer.nummer())
        );

        var inputMetadata = new InputMetadata("REST", "updateRessursMobilnummer", updatePrimarytelefonnummer, new Origination(
            getAuthenticatedNavIdent(),
            SecurityUtil.getAuthenticatedAzpName()
        ));

        ressursCommandService.oppdatertPrimaryTelefonnummer(oppdaterMobilnummerCommand, inputMetadata);
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
            || !ressursState.primaryFysiskBrukersted().equals(updateFysiskeBrukerstederRequest.primaryFysiskBrukersted());

        if (requestContainsNewValues) {

            ressursCommandService.oppdaterFysiskeBrukerstederCommand(
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
            nomMastered.markRessursAsMasteredInNom(ressursState.navident());
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

    public RessursState createRessursAndNotify(OpprettRessursRequest request) {
        validateOpprettRessursRequest(request);
        final var navident = createNavident(request);
        final var existingRessursByNavident = ressursStateRepository.findByNavident(navident);

        if (existingRessursByNavident.isPresent() && isActive(existingRessursByNavident.get())) {
            throw new DuplicateRessursException(
                String.format("Det finnes allerede en aktiv ressurs med personident %s", request.personident())
            );
        }

        final var createdRessurs = existingRessursByNavident.map(ressursState -> reactivateRessurs(request, ressursState))
            .orElseGet(() -> createRessurs(request, navident));

        try {
            final var orgEnhet = orgEnhetStateRepository.findById(request.orgEnhetId())
                .orElseThrow(() -> new RessursOpprettetEmailException("Fant ikke orgenhet med id " + request.orgEnhetId()));
            ressursOpprettetEmailService.notifyByEmail(createdRessurs, orgEnhet);
        } catch (RessursOpprettetEmailException exception) {
            //TODO: consider handling exception if email sending is important
            logger.warn("Epost varsling om opprettelse av ressurs til orgenhet leder feilet. {}", exception.getMessage());
        }

        return createdRessurs;
    }

    private boolean isActive(RessursState ressursState) {
        return orgtilknytningStateRepository.findByNavident(ressursState.navident()).stream()
            .anyMatch(state -> !(state.gyldigTom().isPresent() && state.gyldigTom().get().isBefore(LocalDate.now())));
    }


    private RessursState createRessurs(OpprettRessursRequest request, String navident) {
        final var pdlPerson = pdlClient.hentPerson(request.personident());

        if (!isNull(request.visningsmellomnavn()) && request.visningsmellomnavn().isBlank()) {
            request = request.toBuilder().visningsmellomnavn(null).build();
        }

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
            navident,
            pdlPerson.orElse(null),
            request.sektor(),
            new Visningsnavn(
                request.visningsfornavn().trim(),
                Optional.ofNullable(trimToNull(request.visningsmellomnavn())),
                request.visningsetternavn()
            ),
            trimToNull(request.primaryTelefonnummer()),
            metaData,
            request.startDato(),
            Optional.ofNullable(request.orgEnhetId())
                .flatMap(orgEnhetStateRepository::findById)
                .flatMap(OrgEnhetState::agressoId)
                .orElse(null), // TODO: Validate not null in RessursCommandService
            request.ansettelsesType(),
            request.fysiskBrukerstedNr(),
            request.identType()
        );


        try {
            RessursState opprettetRessurs = ressursCommandService.createRessursSync(opprettRessursCommand);
            nomMastered.markRessursAsMasteredInNom(opprettetRessurs.navident());

            var orgTilknytningFid = orgtilknytningCommandService.opprettOrgtilknytning(
                new OpprettOrgtilknytning(
                    opprettetRessurs.navident(),
                    request.orgEnhetId(),
                    request.sektor(),
                    request.startDato(),
                    null,
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
                    Optional.ofNullable(request.sluttDato()),
                    true,
                    metaData,
                    null
                )
            );

            orgtilknytningCommandService.endreTilknytningtype(
                new EndreTilknytningtypeCommand(
                    orgTilknytningFid,
                    Optional.of(request.tilknytningtype()),
                    metaData
                )
            );

            // If sluttdato is present we fire a separate "avsluttRessursEvent" so that other systems, such as skjerming, know the ressurs already has a sluttDato.
            if (request.sluttDato() != null) {
                ressursCommandService.avsluttRessursEvent(
                    new OpphorRessursCommand(opprettetRessurs.fid(), request.sluttDato()),
                    metaData
                );


                orgtilknytningCommandService.avvikleOrgtilknytning(
                    orgTilknytningFid,
                    new AvvikleOrgtilknytning(
                        opprettetRessurs.navident(),
                        request.orgEnhetId(),
                        request.sluttDato(),
                        metaData,
                        null
                    )
                );
            }

            logger.debug("createRessurs for {} - {}", navident, request.visningsetternavn());

            return opprettetRessurs;
        } catch (ExecutionException | TimeoutException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private RessursState reactivateRessurs(OpprettRessursRequest request, RessursState existingRessurs) {
        validateOpprettRessursRequest(request);

        final var reaktivertRessurs = existingRessurs.toBuilder()
            .personident(request.personident())
            .visningsnavn(
                new Visningsnavn(
                    request.visningsfornavn(),
                    Optional.ofNullable(trimToNull(request.visningsmellomnavn())),
                    request.visningsetternavn()
                ))
            .primaryTelefonnummer(trimToNull(request.primaryTelefonnummer()))
            .sektor(request.sektor())
            .sluttdato(request.sluttDato())
            .primaryFysiskBrukersted(request.fysiskBrukerstedNr())
            .build();

        var metadata = new InputMetadata(
            "REST-API",
            "opprettRessurs",
            request,
            new Origination(
                getAuthenticatedNavIdent(),
                SecurityUtil.getAuthenticatedAzpName()
            )
        );

        nomMastered.markRessursAsMasteredInNom(existingRessurs.navident());

        var orgTilknytningFid = orgtilknytningCommandService.opprettOrgtilknytning(
            new OpprettOrgtilknytning(
                existingRessurs.navident(),
                request.orgEnhetId(),
                request.sektor(),
                request.startDato(),
                null,
                metadata,
                null
            )
        );

        orgtilknytningCommandService.endreDagligOppfolgingOrgtilknytning(
            orgTilknytningFid,
            new EndreDagligOppfolgingOrgtilknytning(
                existingRessurs.navident(),
                request.orgEnhetId(),
                request.startDato(),
                Optional.ofNullable(request.sluttDato()),
                true,
                metadata,
                null
            )
        );

        orgtilknytningCommandService.endreTilknytningtype(
            new EndreTilknytningtypeCommand(
                orgTilknytningFid,
                Optional.of(request.tilknytningtype()),
                metadata
            )
        );

        // If sluttdato is present we fire a separate "avsluttRessursEvent" so that other systems, such as skjerming, know the ressurs already has a sluttDato.
        if (request.sluttDato() != null) {
            ressursCommandService.avsluttRessursEvent(
                new OpphorRessursCommand(existingRessurs.fid(), request.sluttDato()),
                metadata
            );

            orgtilknytningCommandService.avvikleOrgtilknytning(
                orgTilknytningFid,
                new AvvikleOrgtilknytning(
                    existingRessurs.navident(),
                    request.orgEnhetId(),
                    request.sluttDato(),
                    metadata,
                    null
                )
            );
        }

        if (!existingRessurs.visningsnavn().getFormattedVisningsnavn().equals(reaktivertRessurs.visningsnavn().getFormattedVisningsnavn()))
            ressursCommandService.oppdaterPersonaliaEvent(
                new OppdaterPersonaliaCommand(existingRessurs.fid(), reaktivertRessurs.visningsnavn()),
                metadata
            );



        if (!reaktivertRessurs.sektor().name().equals(existingRessurs.sektor().name()))
            ressursCommandService.oppdaterSektorEvent(
                new OppdaterSektorCommand(existingRessurs.fid(),request.sektor()),
                metadata
            );

        var reaktivertPrimaryFysiskBrukersted = reaktivertRessurs.primaryFysiskBrukersted();
        if ((reaktivertPrimaryFysiskBrukersted != null && !reaktivertPrimaryFysiskBrukersted.isEmpty())
            && (existingRessurs.primaryFysiskBrukersted() != null && !existingRessurs.primaryFysiskBrukersted().isEmpty())) {
            if (!reaktivertRessurs.primaryFysiskBrukersted().equals(existingRessurs.primaryFysiskBrukersted()))
                ressursCommandService.oppdaterFysiskeBrukerstederCommand(
                    new OppdaterFysiskeBrukerstederCommand(
                        reaktivertRessurs.fid(),
                        new HashSet<>(existingRessurs.fysiskeBrukersteder()),
                        reaktivertRessurs.primaryFysiskBrukersted()
                    ),
                    metadata
                );
        }

        if (reaktivertRessurs.sluttdato() != null) {
            ressursCommandService.oppdaterRessursSluttdatoEvent(new OppdaterSluttdatoCommand(reaktivertRessurs.fid(), reaktivertRessurs.sluttdato(), metadata));
        } else {
            ressursCommandService.oppdaterRessursSluttdatoEvent(new OppdaterSluttdatoCommand(reaktivertRessurs.fid(), null, metadata));
        }

        if (!reaktivertRessurs.personident().equals(existingRessurs.personident())) {
            ressursCommandService.oppdaterPersonidentCommand(
                new OppdaterPersonidentCommand(reaktivertRessurs.fid(), reaktivertRessurs.personident()),
                metadata
            );
        }

        if (reaktivertRessurs.primaryTelefonnummer() != null && !Objects.equals(existingRessurs.primaryTelefonnummer(), reaktivertRessurs.primaryTelefonnummer()))
            ressursCommandService.oppdatertPrimaryTelefonnummer(
                new OppdaterPrimaryTelefonnummerCommand(existingRessurs.fid(), reaktivertRessurs.primaryTelefonnummer()),
                metadata
            );

        logger.debug("createRessurs for {} - {}", reaktivertRessurs.navident(), request.visningsetternavn());

        try {
            emailNotificationForReaktivertRessurs(existingRessurs, reaktivertRessurs);
        } catch (Exception ignored) {}

        return reaktivertRessurs;
    }

    private void emailNotificationForReaktivertRessurs(RessursState existingRessurs, RessursState reaktivertRessurs) {
        try {
            List<String> recipients = new ArrayList<>();
            recipients.add("Linn.Renate.Fredriksen@nav.no");
            recipients.add("Christian.Prestvik@nav.no");
            recipients.add("Roy.Grini@nav.no");

            var emailMessage = new EmailMessage("Reaktivering av ressurs - " + existingRessurs.navident(),
            """
            Navident: %s
            
            Tidligere sektor: %s
            Ny sektor: %s
            """.formatted(existingRessurs.navident(), existingRessurs.sektor(), reaktivertRessurs.sektor()), recipients);
            microsoftGraphClient.sendEmail(emailMessage).subscribe();
        } catch (Exception ignored) {}
    }

    private String createNavident(OpprettRessursRequest request) {
        final var opprettNavidentResponse = nikClient.opprettNavidentOnBehalfOfUser(
            OpprettNavidentRequest.builder()
                .personid(request.personident())
                .identPrefix(request.visningsetternavn().substring(0, 1))
                .identType(isNull(request.identType()) ? IdentType.STANDARD.name() : request.identType().name())
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

        return opprettNavidentResponse.getBody().getNavident();

    }

    public void avvikleRessurs(String fid, AvvikleRessursRequest request) {
        var ressursState = findRessursStateByFid(fid);

        var metadata = new InputMetadata(
            "REST",
            "avvikleRessurs",
            request,
            new Origination(
                getAuthenticatedNavIdent(),
                SecurityUtil.getAuthenticatedAzpName()
            )
        );

        ressursCommandService.avsluttRessursEvent(
            new OpphorRessursCommand(fid, request.sluttDato()),
            metadata
        );

        var orgtilknytninger = orgtilknytningRestService.getOrgtilknytninger(ressursState.navident());

        orgtilknytninger.forEach(orgtilknytningState -> {
            var gyldigFomIsAfterAvvikletDato = orgtilknytningState.gyldigFom().isAfter(request.sluttDato());
            var gyldigTomIsAfterAvvikletDato = orgtilknytningState.gyldigTom().orElse(LocalDate.MAX).isAfter(request.sluttDato());
            if (gyldigFomIsAfterAvvikletDato) {
                orgtilknytningCommandService.fjernOrgtilknytning(
                    orgtilknytningState.fid(),
                    new FjernOrgtilknytning(
                        ressursState.navident(),
                        orgtilknytningState.orgEnhetId(),
                        orgtilknytningState.gyldigFom(),
                        orgtilknytningState.gyldigTom(),
                        metadata,
                        null
                    ));
            } else if (gyldigTomIsAfterAvvikletDato) {
                orgtilknytningCommandService.avvikleOrgtilknytning(
                    orgtilknytningState.fid(),
                    new AvvikleOrgtilknytning(
                        ressursState.navident(),
                        orgtilknytningState.orgEnhetId(),
                        request.sluttDato(),
                        metadata,
                        null
                    )
                );
            }
        });

        nomMastered.markRessursAsMasteredInNom(ressursState.navident());
    }

    private void validateOpprettRessursRequest(OpprettRessursRequest request) {
        if (Objects.requireNonNull(request.sektor()) == Sektor.EKSTERN) {
            validateOpprettEksternRessursRequest(request);
        } else if (request.sluttDato() != null) {
            validateSluttdatoErInnenSeksAar(request.sluttDato());
        }

        validateChronologicalDates(request.startDato(), request.sluttDato());
        validateIngenMellomromEtternavn(request.visningsetternavn());
        validateFysiskeBrukerstederExists(Set.of(request.fysiskBrukerstedNr()));
        validateOrgtilknytningTypeSektorCombination(request.tilknytningtype(), request.sektor());
        validateOrgenhetForIdenttype(request.identType(), request.orgEnhetId());
    }

    private void validateOrgenhetForIdenttype(IdentType identType, String orgenhetId) {
        if (identType == IdentType.VIKAFOSSEN && !Objects.equals(orgenhetId, vikafossenOrgEnhetId)) {
            throw new IllegalArgumentException("Vikafossen identtype kan kun opprettes på Vikafossen orgenhet (" + vikafossenOrgEnhetId + ")");
        }
        if (Objects.equals(orgenhetId, vikafossenOrgEnhetId) && identType != IdentType.VIKAFOSSEN) {
            throw new IllegalArgumentException("Vikafossen orgenhet kan kun ha identer av Vikafossen identtype");
        }
    }

    private void validateSluttdatoErInnenSeksAar(LocalDate localDate) {
        var sixYearsFromNow = LocalDate.now().plusYears(6);
        if (localDate.equals(sixYearsFromNow) || localDate.isAfter(sixYearsFromNow)) {
            throw new IllegalArgumentException("Sluttdato kan ikke være mer enn 6 år frem i tid");
        }
    }

    private void validateOpprettEksternRessursRequest(OpprettRessursRequest request) {
        if (request.sluttDato() == null) {
            throw new IllegalArgumentException("Sluttdato må oppgis for opprettelse av ekstern ressurs");
        }
    }

    private void validateIngenMellomromEtternavn(String visningsetternavn) {
        if (visningsetternavn.contains(" ") || !visningsetternavn.matches("^(\\S+)$")) {
            throw new IllegalArgumentException("Etternavn kan ikke inneholde linjeskift, mellomrom o.l");
        }
    }

    public List<Ressurs.Telefon> getTelefonnummere(String navident) {
        logger.info("Fetching telefonnumre for navident: {}", navident);
        var telefoner = new ArrayList<>(ressursContactDetailsResolver.fetchTelefoner(navident));
        var ressursState = findRessursStateByNavident(navident);
        if (ressursState.primaryTelefonnummer() != null) telefoner.add(new Ressurs.Telefon(ressursState.primaryTelefonnummer(), TelefonType.PRIVAT_TELEFON));
        return telefoner.isEmpty() ? Collections.emptyList() : telefoner;
    }

    public String getTelefonnummer(String navident) {
        logger.info("Fetching telefonnummer for navident: {}", navident);
        var telefoner = new ArrayList<>(ressursContactDetailsResolver.fetchTelefoner(navident));
        var ressursState = findRessursStateByNavident(navident);
        if (ressursState.primaryTelefonnummer() != null) telefoner.add(new Ressurs.Telefon(ressursState.primaryTelefonnummer(), TelefonType.PRIVAT_TELEFON));
        telefoner.sort(
            (o1, o2) -> {
                if (o1.type().equals(o2.type())) return 0;
                if (o1.type().equals(TelefonType.NAV_TJENESTE_TELEFON)) return -1;
                if (o2.type().equals(TelefonType.NAV_TJENESTE_TELEFON)) return 1;
                if (o1.type().equals(TelefonType.NAV_KONTOR_TELEFON)) return -1;
                if (o2.type().equals(TelefonType.NAV_KONTOR_TELEFON)) return 1;
                if (o1.type().equals(TelefonType.PRIVAT_TELEFON)) return -1;
                if (o2.type().equals(TelefonType.PRIVAT_TELEFON)) return 1;
                return 0;});
        return telefoner.isEmpty() ? null : telefoner.getFirst().nummer();
    }

    public List<RessursState> getRessurserMedFysiskBrukersted(String fysiskBrukerstedNr) {
        var ressursStates = fysiskBrukerstedService.getAlleRessurserMedFysiskBrukersted(fysiskBrukerstedNr);

        return ressursStates.stream()
            .map(ressursState -> ressursState.toBuilder()
                .personident(ressursState.personident().replaceFirst(".{5}$", "*****"))
                .build()
            )
            .toList();
    }

    public List<RessursState> getRessurserPaaEnhetMedFysiskBrukersted(String orgenhetId, String fysiskBrukerstedNr) {
        var ressursStates = fysiskBrukerstedService.getAlleRessurserPaaEnhetMedFysiskBrukersted(orgenhetId, fysiskBrukerstedNr);

        return ressursStates.stream()
            .map(ressursState -> ressursState.toBuilder()
                .personident(ressursState.personident().replaceFirst(".{5}$", "*****"))
                .build()
            )
            .toList();
    }

    public void updateFysiskBrukerstedForRessurserByFysiskbrukersted(String fraFysiskBrukerstedNr, String tilFysiskBrukerstedNr) {
        var metadata = new InputMetadata(
            "REST",
            "updateFysiskBrukerstedForRessurserByFysiskbrukersted",
            Map.of("fraFysiskBrukersted", fraFysiskBrukerstedNr, "tilFysiskBrukersted", tilFysiskBrukerstedNr),
            new Origination(
                getAuthenticatedNavIdent(),
                SecurityUtil.getAuthenticatedAzpName()
            )
        );

        fysiskBrukerstedService.flyttAlleRessurserFraFysiskBrukerstedTilFysiskBrukersted(
            fraFysiskBrukerstedNr,
            tilFysiskBrukerstedNr,
            metadata
        );
    }

    public void updateFysiskBrukerstedForRessurserByEnhetAndFysiskbrukersted(String orgenhetId, String fraFysiskBrukerstedNr, String tilFysiskBrukerstedNr) {
        var metadata = new InputMetadata(
            "REST",
            "updateFysiskBrukerstedForRessurserByEnhetAndFysiskbrukersted",
            Map.of("orgenhet", orgenhetId, "fraFysiskBrukersted", fraFysiskBrukerstedNr, "tilFysiskBrukersted", tilFysiskBrukerstedNr),
            new Origination(
                getAuthenticatedNavIdent(),
                SecurityUtil.getAuthenticatedAzpName()
            )
        );

        fysiskBrukerstedService.flyttAlleRessurserPaaEnhetFraFysiskBrukerstedTilFysiskBrukersted(
            orgenhetId,
            fraFysiskBrukerstedNr,
            tilFysiskBrukerstedNr,
            metadata
        );
    }

    public void updateFysiskBrukerstedForRessurserByNavidentList(List<String> navidenter, String tilFysiskBrukerstedNr) {
        var metadata = new InputMetadata(
            "REST",
            "updateFysiskBrukerstedForRessurserByNavidentList",
            Map.of("navidenter", navidenter, "tilFysiskBrukersted", tilFysiskBrukerstedNr),
            new Origination(
                getAuthenticatedNavIdent(),
                SecurityUtil.getAuthenticatedAzpName()
            )
        );

        fysiskBrukerstedService.flyttRessurserTilFysiskBrukersted(navidenter, tilFysiskBrukerstedNr, metadata);
    }

    public String getRemedyEnhet(String navident) {
        log.info("Fetching RemedyEnhet for navident: {}", navident);
        var aktivDolOT = orgtilknytningService.getAktivDolOTForRessurs(navident);
        Optional<RemedyEnhet> remedyEnhet = aktivDolOT == null ? Optional.empty() : remedyEnhetMapperService.findByNomId(aktivDolOT.orgEnhetId());
        return remedyEnhet.map(RemedyEnhet::remedyEnhetId).orElse(null);
    }

    public boolean ressursExists(String navident) {
        return ressursStateRepository.findByNavident(navident).isPresent();
    }

    public String getEpost(String navident) {
        log.info("Fetching Epost for navident: {}", navident);
        return ressursContactDetailsResolver.fetchEmail(navident).orElse(null);
    }

    public boolean kanOppretteAngittRessurs(IdentType identType, String orgenhet) {
        return switch (identType) {
            case ROBOT -> SecurityUtil.loggedInUserHasAnyAccessScope(NOM_WRITE_ROBOT, ADMIN);
            case UTENLANDSK_KONSULENT -> SecurityUtil.loggedInUserHasAnyAccessScope(NOM_WRITE_UTL_KONSULENT, ADMIN);
            case VIKAFOSSEN -> SecurityUtil.loggedInUserHasAnyAccessScope(NOM_WRITE_VIKAFOSSEN, ADMIN); // NOM admin skal fratas denne rollen etterhvert
            default -> authResolver.kanOppretteRessurs(orgenhet);
        };
    }

    public boolean kanEndreEllerAvvikleAngittRessurs(IdentType identType, String navident) {
        return switch (identType) {
            case ROBOT -> SecurityUtil.loggedInUserHasAnyAccessScope(NOM_WRITE_ROBOT, ADMIN) || authResolver.kanRedigereRessurs(navident);
            case UTENLANDSK_KONSULENT -> SecurityUtil.loggedInUserHasAnyAccessScope(NOM_WRITE_UTL_KONSULENT, ADMIN) || authResolver.kanRedigereRessurs(navident);
            case VIKAFOSSEN -> SecurityUtil.loggedInUserHasAnyAccessScope(NOM_WRITE_VIKAFOSSEN, ADMIN); // NOM admin skal fratas denne rollen etterhvert
            default -> authResolver.kanRedigereRessurs(navident);
        };
    }
}
