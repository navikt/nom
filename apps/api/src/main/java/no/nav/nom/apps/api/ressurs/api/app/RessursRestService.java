package no.nav.nom.apps.api.ressurs.api.app;

import com.fasterxml.jackson.databind.JsonNode;
import io.getunleash.Unleash;
import no.nav.nom.apps.api.authorize.AuthResolver;
import no.nav.nom.apps.api.common.security.NomApiAccessScope;
import no.nav.nom.apps.api.graphql.common.SecurityUtil;
import no.nav.nom.apps.api.nik.NIKClient;
import no.nav.nom.apps.api.nik.OpprettNavidentRequest;
import no.nav.nom.apps.api.nik.OpprettNavidentResponse;
import no.nav.nom.apps.api.orgtilknytning.api.app.OrgtilknytningRestService;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.OrgtilknytningCommandService;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.AvvikleOrgtilknytning;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.EndreDagligOppfolgingOrgtilknytning;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.EndreTilknytningtypeCommand;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.FjernOrgtilknytning;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.OpprettOrgtilknytning;
import no.nav.nom.apps.api.pdl.PdlClient;
import no.nav.nom.apps.api.ressurs.NomMastered;
import no.nav.nom.apps.api.ressurs.api.dto.Ressurs;
import no.nav.nom.apps.api.ressurs.api.dto.RessursLeder;
import no.nav.nom.apps.api.ressurs.api.exceptions.DuplicateRessursException;
import no.nav.nom.apps.api.ressurs.api.exceptions.InvalidSektorException;
import no.nav.nom.apps.api.ressurs.api.exceptions.NomMasteredException;
import no.nav.nom.apps.api.ressurs.api.exceptions.RessursNotFoundException;
import no.nav.nom.apps.api.ressurs.email.RessursOpprettetEmailException;
import no.nav.nom.apps.api.ressurs.email.RessursOpprettetEmailService;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.RessursCommandService;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OppdaterFysiskeBrukerstederCommand;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OppdaterPersonaliaCommand;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OppdaterPrimaryTelefonnummerCommand;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OppdaterSektorCommand;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OppdaterSluttdatoCommand;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OpphorRessursCommand;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OpprettRessursCommand;
import no.nav.nom.apps.api.topicreader.KafkaTopicReader;
import no.nav.nom.apps.api.topicreader.ReadTopicRequest;
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
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static no.nav.nom.apps.api.common.DateUtils.validateChronologicalDates;
import static no.nav.nom.apps.api.graphql.common.SecurityUtil.getAuthenticatedNavIdent;
import static no.nav.nom.apps.api.graphql.common.SecurityUtil.loggedInUserHasAccessScope;
import static no.nav.nom.apps.api.orgtilknytning.api.app.OrgtilknytningRestService.validateOrgtilknytningTypeSektorCombination;
import static org.apache.commons.lang3.StringUtils.trimToNull;

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
    private final Unleash unleash;

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
        AuthResolver authResolver,
        Unleash unleash
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
        this.unleash = unleash;
    }

    public List<RessursState> findRessurser(RessursFilter ressursFilter) {

        var showFysiskBrukersted = loggedInUserHasAccessScope(NomApiAccessScope.ADMIN) ||
            loggedInUserHasAccessScope(NomApiAccessScope.NOM_REMEDY_INTEGRASJON_APP);

        return ressursStateRepository.getAll().stream()
            .filter(ressursState -> (ressursFilter.sektor().isEmpty() || ressursState.sektor().equals(ressursFilter.sektor().get()))
                && (ressursFilter.sluttdato().isEmpty() || ressursState.sluttdato().equals(ressursFilter.sluttdato())))
            .map(ressursState -> ressursState.toBuilder()
                .personident(ressursState.personident().replaceFirst(".{5}$", "*****"))
                .primaryFysiskBrukersted(showFysiskBrukersted ? ressursState.primaryFysiskBrukersted() : Optional.empty())
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
        return ressursStateRepository.findByNavidentBulk(navidenter).values().stream()
            .map(this::hideSensitiveDataAndEnrichRessursStateWithDvhData)
            .toList();
    }

    private RessursState findRessursStateByNavident(String navident) {
        return ressursStateRepository.findByNavident(navident).orElseThrow(() -> RessursNotFoundException.navident(navident));
    }

    private RessursState findRessursStateByFid(String fid) {
        return ressursStateRepository.findByFid(fid).orElseThrow(() -> RessursNotFoundException.fid(fid));
    }

    public Ressurs findRessursByFid(String fid) {
        var ressursState = findRessursStateByFid(fid);
        return hideSensitiveDataAndEnrichRessursStateWithDvhData(ressursState);
    }

    private Ressurs hideSensitiveDataAndEnrichRessursStateWithDvhData(RessursState ressursState) {

        var showPersonident = loggedInUserHasAccessScope(NomApiAccessScope.ADMIN) ||
            loggedInUserHasAccessScope(NomApiAccessScope.NOM_REMEDY_INTEGRASJON_APP) ||
            getAuthenticatedNavIdent().equals(ressursState.navident());

        var showFysiskBrukerSted = loggedInUserHasAccessScope(NomApiAccessScope.ADMIN) ||
            loggedInUserHasAccessScope(NomApiAccessScope.NOM_REMEDY_INTEGRASJON_APP) ||
            authResolver.kanRedigereRessurs(ressursState.navident()) ||
            getAuthenticatedNavIdent().equals(ressursState.navident());

        var telefoner = ressursContactDetailsResolver.fetchTelefoner(ressursState.navident());
        var epost = ressursContactDetailsResolver.fetchEmail(ressursState.navident());

        var personident = showPersonident
            ? ressursState.personident()
            : ressursState.personident().replaceFirst(".{5}$", "*****");


        return new Ressurs(
            ressursState.toBuilder()
                .personident(personident)
                .primaryTelefonnummer(ressursState.primaryTelefonnummer())
                .primaryFysiskBrukersted(showFysiskBrukerSted ? ressursState.primaryFysiskBrukersted() : Optional.empty())
                .fysiskeBrukersteder(showFysiskBrukerSted ? ressursState.fysiskeBrukersteder() : Collections.emptyList())
                .build(),
            telefoner,
            epost.orElse(null),
            nomMastered.isMastered(ressursState.navident())
        );
    }

    public void updateRessursNavn(String navident, UpdateVisningsnavnRequest updateVisningsnavnRequest) {
        var ressursState = findRessursStateByNavident(navident);

        if (unleash.isEnabled("nom.ressurs.opprett_statlig")
            && (loggedInUserHasAccessScope(NomApiAccessScope.ADMIN) || loggedInUserHasAccessScope(NomApiAccessScope.NOM_PILOT))) {
            logger.info("Feature flag nom.ressurs.opprett_statlig is enabled, allowing update of ressurs with sektor NAV_STATLIG");
        } else if (ressursState.sektor().erStatlig()) {
            throw InvalidSektorException.actionNotAllowedForSektor(Sektor.NAV_STATLIG);
        }

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

        if (unleash.isEnabled("nom.ressurs.opprett_statlig")
            && (loggedInUserHasAccessScope(NomApiAccessScope.ADMIN) || loggedInUserHasAccessScope(NomApiAccessScope.NOM_PILOT))) {
            logger.info("Feature flag nom.ressurs.opprett_statlig is enabled, allowing update of ressurs with sektor NAV_STATLIG");
        } else if (ressursState.sektor().erStatlig()) {
            throw InvalidSektorException.actionNotAllowedForSektor(Sektor.NAV_STATLIG);
        }

        if (!loggedInUserHasAccessScope(NomApiAccessScope.ADMIN)) {
            if (authResolver.kanSelvbetjene(navident) && !nomMastered.isMastered(ressursState.navident())) {
                throw NomMasteredException.actionNotAllowedForNonNomMastered();
            }
        }


        var oppdaterMobilnummerCommand = new OppdaterPrimaryTelefonnummerCommand(
            ressursState.fid(),
            updatePrimarytelefonnummer.nummer()
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

        if (unleash.isEnabled("nom.ressurs.opprett_statlig")
            && (loggedInUserHasAccessScope(NomApiAccessScope.ADMIN) || loggedInUserHasAccessScope(NomApiAccessScope.NOM_PILOT))) {
            logger.info("Feature flag nom.ressurs.opprett_statlig is enabled, allowing update of ressurs with sektor NAV_STATLIG");
        } else if (ressursState.sektor().erStatlig()) {
            throw InvalidSektorException.actionNotAllowedForSektor(Sektor.NAV_STATLIG);
        }

        var existingFysiskeBrukerstederForRessurs = Optional.ofNullable(ressursState.fysiskeBrukersteder()).orElse(Collections.emptyList());

        var requestContainsNewValues = !Set.copyOf(existingFysiskeBrukerstederForRessurs).equals(updateFysiskeBrukerstederRequest.fysiskeBrukersteder())
            || !ressursState.primaryFysiskBrukersted().equals(Optional.of(updateFysiskeBrukerstederRequest.primaryFysiskBrukersted()));

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

        final var existingRessurs = ressursStateRepository.findByPersonident(request.personident());

        if (existingRessurs.isPresent() && isActive(existingRessurs.get())) {
            throw new DuplicateRessursException(
                String.format("Det finnes allerede en aktiv ressurs med personident %s", request.personident())
            );
        }

        final var createdRessurs = existingRessurs.map(ressursState -> reactivateRessurs(request, ressursState))
            .orElseGet(() -> createRessurs(request, existingRessurs));

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


    private RessursState createRessurs(OpprettRessursRequest request, Optional<RessursState> existingRessurs) {
        final var navident = existingRessurs.isPresent() ? existingRessurs.get().navident() : createNavident(request);
        final var startdato = existingRessurs.isPresent() ? existingRessurs.get().startdato().orElse(null) : request.startDato();
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
            pdlPerson,
            request.sektor(),
            new Visningsnavn(
                request.visningsfornavn().trim(),
                Optional.ofNullable(trimToNull(request.visningsmellomnavn())),
                request.visningsetternavn()
            ),
            Optional.ofNullable(trimToNull(request.primaryTelefonnummer())),
            metaData,
            startdato,
            Optional.ofNullable(request.orgEnhetId())
                .flatMap(orgEnhetStateRepository::findById)
                .flatMap(OrgEnhetState::agressoId)
                .orElse(null), // TODO: Validate not null in RessursCommandService
            request.ansettelsesType(),
            request.fysiskBrukerstedNr()
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
                    request.sluttDato(),
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
            if (request.sluttDato().isPresent()) {
                ressursCommandService.avsluttRessursEvent(
                    new OpphorRessursCommand(opprettetRessurs.fid(), request.sluttDato().get()),
                    metaData
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
            }

            logger.debug("createRessurs for {} - {}", navident, request.visningsetternavn());

            return opprettetRessurs;
        } catch (ExecutionException | TimeoutException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private RessursState reactivateRessurs(OpprettRessursRequest request, RessursState existingRessurs) {

        final var reaktivertRessurs = existingRessurs.toBuilder()
            .visningsnavn(
                new Visningsnavn(
                    request.visningsfornavn(),
                    Optional.ofNullable(trimToNull(request.visningsmellomnavn())),
                    request.visningsetternavn()
                ))
            .primaryTelefonnummer(Optional.ofNullable(trimToNull(request.primaryTelefonnummer())))
            .sektor(request.sektor())
            .sluttdato(request.sluttDato())
            .primaryFysiskBrukersted(Optional.ofNullable(request.fysiskBrukerstedNr()))
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
                request.sluttDato(),
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
        if (request.sluttDato().isPresent()) {
            ressursCommandService.avsluttRessursEvent(
                new OpphorRessursCommand(existingRessurs.fid(), request.sluttDato().get()),
                metadata
            );

            orgtilknytningCommandService.avvikleOrgtilknytning(
                orgTilknytningFid,
                new AvvikleOrgtilknytning(
                    existingRessurs.navident(),
                    request.orgEnhetId(),
                    request.sluttDato().get(),
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

        if (reaktivertRessurs.primaryTelefonnummer().isPresent() && !existingRessurs.primaryTelefonnummer().equals(reaktivertRessurs.primaryTelefonnummer()))
            ressursCommandService.oppdatertPrimaryTelefonnummer(
                new OppdaterPrimaryTelefonnummerCommand(existingRessurs.fid(), reaktivertRessurs.primaryTelefonnummer().get()),
                metadata
            );

        if (!reaktivertRessurs.sektor().name().equals(existingRessurs.sektor().name()))
            ressursCommandService.oppdaterSektorEvent(
                new OppdaterSektorCommand(existingRessurs.fid(),request.sektor()),
                metadata
            );

        if (reaktivertRessurs.primaryFysiskBrukersted().isPresent() && existingRessurs.primaryFysiskBrukersted().isPresent()) {
            if (!reaktivertRessurs.primaryFysiskBrukersted().get().equals(existingRessurs.primaryFysiskBrukersted().get()))
                ressursCommandService.oppdaterFysiskeBrukerstederCommand(
                    new OppdaterFysiskeBrukerstederCommand(
                        reaktivertRessurs.fid(),
                        new HashSet<>(existingRessurs.fysiskeBrukersteder()),
                        reaktivertRessurs.primaryFysiskBrukersted().get()
                    ),
                    metadata
                );
        }

        reaktivertRessurs.sluttdato().ifPresentOrElse(sluttdato -> ressursCommandService.oppdaterRessursSluttdatoEvent(
            new OppdaterSluttdatoCommand(reaktivertRessurs.fid(), Optional.of(sluttdato), metadata)
        ), () -> ressursCommandService.oppdaterRessursSluttdatoEvent(
            new OppdaterSluttdatoCommand(reaktivertRessurs.fid(), Optional.empty(), metadata)
        ));

        logger.debug("createRessurs for {} - {}", reaktivertRessurs.navident(), request.visningsetternavn());

        return reaktivertRessurs;
    }

    private String createNavident(OpprettRessursRequest request) {
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

        return opprettNavidentResponse.getBody().getNavident();

    }

    public void avvikleRessurs(String fid, AvvikleRessursRequest request) {
        var ressursState = findRessursStateByFid(fid);

        if (unleash.isEnabled("nom.ressurs.opprett_statlig")
            && (loggedInUserHasAccessScope(NomApiAccessScope.ADMIN) || loggedInUserHasAccessScope(NomApiAccessScope.NOM_PILOT))) {
            logger.info("Feature flag nom.ressurs.opprett_statlig is enabled, allowing update of ressurs with sektor NAV_STATLIG");
        } else if (ressursState.sektor().erStatlig()) {
            throw InvalidSektorException.actionNotAllowedForSektor(Sektor.NAV_STATLIG);
        }

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

        var orgtilknytninger = orgtilknytningRestService.getOrgtilknytninger(Optional.of(ressursState.navident()));

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
        switch (request.sektor()) {
            case EKSTERN -> validateOpprettEksternRessursRequest(request);
            case NAV_STATLIG -> validateOpprettStatligRessursRequest(request);
        }

        validateChronologicalDates(request.startDato(), request.sluttDato().orElse(null));
        validateIngenMellomromEtternavn(request.visningsetternavn());
        validateFysiskeBrukerstederExists(Set.of(request.fysiskBrukerstedNr()));
        validateOrgtilknytningTypeSektorCombination(request.tilknytningtype(), request.sektor());
    }

    private void validateOpprettEksternRessursRequest(OpprettRessursRequest request) {
        if (request.sluttDato().isEmpty()) {
            throw new IllegalArgumentException("Sluttdato må oppgis for opprettelse av ekstern ressurs");
        }
    }

    private void validateOpprettStatligRessursRequest(OpprettRessursRequest request) {
        if (unleash.isEnabled("nom.ressurs.opprett_statlig")
            && (loggedInUserHasAccessScope(NomApiAccessScope.ADMIN) || loggedInUserHasAccessScope(NomApiAccessScope.NOM_PILOT))) {
            logger.info("Feature flag nom.ressurs.opprett_statlig is enabled, allowing update of ressurs with sektor NAV_STATLIG");
        } else if (request.sektor().erStatlig()) {
            throw InvalidSektorException.actionNotAllowedForSektor(request.sektor());
        }
    }

    private void validateIngenMellomromEtternavn(String visningsetternavn) {
        if (visningsetternavn.contains(" ") || !visningsetternavn.matches("^(\\S+)$")) {
            throw new IllegalArgumentException("Etternavn kan ikke inneholde linjeskift, mellomrom o.l");
        }
    }

    public String getTelefonnummer(String navident) {
        logger.info("Fetching telefonnummer for navident: {}", navident);
        var telefoner = ressursContactDetailsResolver.fetchTelefoner(navident);
        if (telefoner.size() == 1) return mapAndTrimNumber(telefoner.getFirst());
        return telefoner.stream().filter(telefon -> {
            if (telefon.type().equals(TelefonType.NAV_KONTOR_TELEFON)) return true;
            return telefon.type().equals(TelefonType.NAV_TJENESTE_TELEFON);
        }).findFirst().map(this::mapAndTrimNumber).orElse("");
    }

    private String mapAndTrimNumber(Ressurs.Telefon telefon) {
        var nummer = telefon.nummer();
        if (nummer.length() > 8) {
            if (nummer.startsWith("47")) {
                return nummer.substring(2);
            }
            if (nummer.startsWith("+47")) {
                return nummer.substring(3);
            }
        }
        return nummer;
    }

}
