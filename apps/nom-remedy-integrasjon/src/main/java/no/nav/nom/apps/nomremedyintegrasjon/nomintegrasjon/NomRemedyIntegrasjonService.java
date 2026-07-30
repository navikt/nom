package no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon;

import no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.client.NomApiClient;
import no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.exceptions.OrgtilknytningNotFoundException;
import no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.exceptions.RequiredValueMissingException;
import no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.exceptions.RessursNotFoundException;
import no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.repository.aggregatestate.AggregateState;
import no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.repository.aggregatestate.AggregateStateRepository;
import no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.repository.delayedevent.DelayedEvent;
import no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.repository.delayedevent.DelayedEventRepository;
import no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.repository.errorstate.ErrorState;
import no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.repository.errorstate.ErrorStateRepository;
import no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.repository.remedy.NomIntegrasjonEntity;
import no.nav.nom.contract.nom.Tilknytningtype;
import no.nav.nom.contract.nom.eventsource.Event;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateRepository;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import no.nav.nom.libs.utils.common.platform.LeaderElection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNullElse;
import static no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.Utils.epochSecondsNorwegianTimeNoon;
import static no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.Utils.formatFornavnWithMellomnavn;
import static no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.Utils.isActive;
import static no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.Utils.isFuture;
import static no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.Utils.nomSektorToRemedyCode;
import static org.apache.commons.lang3.StringUtils.deleteWhitespace;

@Service
public class NomRemedyIntegrasjonService {
    private final static Logger logger = LoggerFactory.getLogger(NomRemedyIntegrasjonService.class);
    private final LeaderElection leaderElection;
    private final RemedyIntegrasjonRepositoryService remedyIntegrasjonRepositoryService;
    private final NomApiClient nomApiClient;
    private final AggregateStateRepository aggregateStateRepository;
    private final ErrorStateRepository errorStateRepository;
    private final RessursStateRepository ressursStateRepository;
    private final OrgtilknytningStateRepository orgtilknytningStateRepository;
    private final DelayedEventRepository delayedEventRepository;
    private final ThreadLocal<Boolean> isAdmin = ThreadLocal.withInitial(() -> false);
    private final JsonMapper jsonMapper;

    public NomRemedyIntegrasjonService(
        LeaderElection leaderElection,
        RemedyIntegrasjonRepositoryService remedyIntegrasjonRepositoryService,
        NomApiClient nomApiClient,
        AggregateStateRepository aggregateStateRepository,
        ErrorStateRepository errorStateRepository,
        RessursStateRepository ressursStateRepository,
        OrgtilknytningStateRepository orgtilknytningStateRepository,
        DelayedEventRepository delayedEventRepository,
        JsonMapper jsonMapper
    ) {
        this.leaderElection = leaderElection;
        this.remedyIntegrasjonRepositoryService = remedyIntegrasjonRepositoryService;
        this.nomApiClient = nomApiClient;
        this.aggregateStateRepository = aggregateStateRepository;
        this.errorStateRepository = errorStateRepository;
        this.ressursStateRepository = ressursStateRepository;
        this.orgtilknytningStateRepository = orgtilknytningStateRepository;
        this.delayedEventRepository = delayedEventRepository;
        this.jsonMapper = jsonMapper;
    }

    @Transactional
    @Scheduled(cron = "*/5 * * * * *")
    public void executeImmediateStateUpdate() {
        if (leaderElection.isLeader()) {
            var statesToUpdate = aggregateStateRepository.findAllOlderThanFiveSecondsWhereDelayedUntilIsNull();
            List<UUID> rowsToDelete = executeStateUpdates(statesToUpdate);
            aggregateStateRepository.deleteAllById(rowsToDelete);
        }
    }

    @Transactional
    @Scheduled(cron = "0 15 0 * * *")
    public void executeDelayedStateUpdate() {
        if (leaderElection.isLeader()) {
            logger.info("Executing delayed state update");
            var delayedStates = aggregateStateRepository.findAggregateStatesByMasseflyttingIsFalseOrMasseflyttingIsNullAndDelayedUntilIsLessThanEqual(LocalDate.now());
            logger.info("Delayed state update: {}", delayedStates);
            List<UUID> rowsToDelete = executeStateUpdates(delayedStates);
            aggregateStateRepository.deleteAllById(rowsToDelete);
        }
    }

    @Transactional
    @Scheduled(cron = "0 10 0 * * *")
    public void executeOldDelayedStateUpdate() {
        if (leaderElection.isLeader()) {
            try {
                logger.info("Executing old delayed state update");
                var oldDelayedStates = delayedEventRepository.findByDelayedUntilLessThanEqual(LocalDate.now());
                if (oldDelayedStates.isEmpty()) {
                    logger.info("No old delayed states to update");
                    return;
                }
                List<AggregateState> oldDelayedStatesToUpdate = new ArrayList<>();
                List<UUID> oldDelayedEventsNotFoundToDelete = new ArrayList<>();
                List<UUID> oldDelayedEventsConvertedToDelete = new ArrayList<>();
                oldDelayedStates.forEach(state -> {
                    Event eventState = jsonMapper.treeToValue(state.getEvent(), Event.class);
                    var fid = eventState.getFid();
                    var fidType = fid.contains("ressurs") ? RessursState.class.getSimpleName() : OrgtilknytningState.class.getSimpleName();
                    var navident = fidType.equals(OrgtilknytningState.class.getSimpleName()) ? orgtilknytningStateRepository.findByFid(fid)
                        .map(OrgtilknytningState::navident)
                        .orElse(null) :
                        ressursStateRepository.findByFid(fid).map(RessursState::navident)
                            .orElse(null);

                    if (navident == null) {
                        logger.error("Fant ikke state for {}", fid);
                        oldDelayedEventsNotFoundToDelete.add(state.getDelayedEventId());
                    } else {
                        oldDelayedStatesToUpdate.add(
                            AggregateState.builder()
                                .createdAt(LocalDateTime.now())
                                .navident(navident)
                                .stateFid(fid)
                                .stateType(fidType)
                                .delayedUntil(state.getDelayedUntil())
                                .build()
                        );
                        oldDelayedEventsConvertedToDelete.add(state.getDelayedEventId());
                    }
                });

                if (!oldDelayedEventsNotFoundToDelete.isEmpty()) {
                    delayedEventRepository.deleteAllById(oldDelayedEventsNotFoundToDelete);
                    logger.info("Sletter alle gamle events som ikke kunne bli prossesert {}",  oldDelayedEventsNotFoundToDelete);
                }
                logger.info("Old delayed state update: {}", oldDelayedStates);
                List<UUID> rowsToDelete = executeStateUpdates(oldDelayedStatesToUpdate);
                aggregateStateRepository.deleteAllById(rowsToDelete);
                delayedEventRepository.deleteAllById(oldDelayedEventsConvertedToDelete);
            } catch (Exception e) {
                logger.error("Uventet error skjedde {}", e.getMessage());
                throw e;
            }
        }
    }

    @Transactional
    @Scheduled(cron = "0 30 8 * * MON-FRI")
    public void logErrorState() {
        if (leaderElection.isLeader()) {
            var errorStates = errorStateRepository.findAll();
            if (!errorStates.isEmpty()) {
                logger.info(
                    "ALERT: En sjekk har blitt gjort på nom-remedy-integrasjon error states, og det er {} feil i systemet, sjekk i nom-admin",
                    errorStates.size()
                );
            }
        }
    }

    @Transactional
    protected List<UUID> executeStateUpdates(List<AggregateState> statesToUpdate) {
        return statesToUpdate.stream().map(aggregateState -> {
            AtomicReference<UUID> fid = new AtomicReference<>(null);

            var optionalRessursState = aggregateState.getStateType().equals(RessursState.class.getSimpleName()) ?
                ressursStateRepository.findByFid(aggregateState.getStateFid()) : Optional.<RessursState>empty();

            var optionalOrgtilknytningState = aggregateState.getStateType().equals(OrgtilknytningState.class.getSimpleName()) ?
                orgtilknytningStateRepository.findByFid(aggregateState.getStateFid()) : Optional.<OrgtilknytningState>empty();

            try {
                optionalRessursState.ifPresent(ressursState -> {
                    if (ressursStateUpdate(ressursState, aggregateState)) {
                        fid.set(aggregateState.getAggregateStateId());
                        errorStateRepository.deleteErrorStateByErrorStateFid(ressursState.fid());
                    }
                });
                optionalOrgtilknytningState.ifPresent(orgtilknytningState -> {
                    if (orgtilknytningStateUpdate(orgtilknytningState, aggregateState)) {
                        fid.set(aggregateState.getAggregateStateId());
                        errorStateRepository.deleteErrorStateByErrorStateFid(orgtilknytningState.fid());
                    }
                });
            } catch (Exception e) {
                logger.error(
                    "ALERT: Kunne ikke prosessere state av typen {} til navident {}, med følgende feilmelding {}",
                    aggregateState.getStateType(),
                    aggregateState.getNavident(),
                    e.getMessage(),
                    e
                );
                fid.set(aggregateState.getAggregateStateId());
                errorStateRepository.save(ErrorState.builder()
                    .errorStateFid(aggregateState.getStateFid())
                    .errorStateType(aggregateState.getStateType())
                    .errorMessage(e.getMessage())
                    .createdAt(LocalDateTime.now())
                    .navident(aggregateState.getNavident())
                    .build());
            }
            return isNull(fid.get()) ? null : fid.get();
        }).filter(Objects::nonNull).toList();
    }

    private boolean orgtilknytningStateUpdate(OrgtilknytningState orgtilknytningState, AggregateState aggregateState) {
        if (orgtilknytningState.gyldigTom() != null && orgtilknytningState.gyldigTom().isBefore(LocalDate.now())) {
            return true;
        }

        var ressursState = ressursStateRepository.findByNavident(orgtilknytningState.navident())
            .orElseThrow(() -> RessursNotFoundException.notFound(orgtilknytningState.navident(), orgtilknytningState.fid()));

        var entryBestiller = orgtilknytningState.sistEndretAv() != null ? orgtilknytningState.sistEndretAv() : "nom-api";
        var erDagligOppfolging = orgtilknytningState.erDagligOppfolging();
        var erMasseflytting = !isNull(aggregateState.getMasseflytting()) && aggregateState.getMasseflytting();

        if (erDagligOppfolging && (isActive(orgtilknytningState) || (isFuture(orgtilknytningState) && erMasseflytting))) {
            logger.info("OrgtilknytningState er aktiv eller er i fremtiden og er masseflytting for navident {}, lagrer i IR", orgtilknytningState.navident());
            logger.info("navident: {} orgtilknytningState: {}", orgtilknytningState.navident(), orgtilknytningState);
            saveStateToRemedy(nomIntegrasjonEntityFromNomData(ressursState, orgtilknytningState, erMasseflytting), entryBestiller);
            return true;
        }

        if (isFuture(orgtilknytningState) && erDagligOppfolging) {
            logger.info("OrgtilknytningState er i fremtiden for navident {}, utsetter i aggregate_state tabellen", orgtilknytningState.navident());
            var currentDolOrgtilknytning = getCurrentDolByNavident(orgtilknytningState.navident());
            if (currentDolOrgtilknytning.isPresent()) {
                logger.info("Fant eksisterende orgtilknytning for navident {}, oppdaterer aggregate_state tabellen", orgtilknytningState.navident());
                try {
                    aggregateStateRepository.save(
                        aggregateState.toBuilder()
                            .delayedUntil(orgtilknytningState.gyldigFom())
                            .build());
                } catch (Exception e) {
                    logger.error("Klarte ikke lagre? Feilmelding {}", e.getMessage(), e);
                }
                logger.info(
                    "OrgtilknytningState med fid {} ble utsatt i aggregateStateTabellen helt til {}",
                    aggregateState.getStateFid(),
                    orgtilknytningState.gyldigFom()
                );
                return false;
            } else {
                saveStateToRemedyIfNewOrReactivatedRessurs(ressursState, orgtilknytningState);
                return true;
            }
        }

        return !erDagligOppfolging;
    }

    private boolean ressursStateUpdate(RessursState ressursState, AggregateState aggregateState) {
        var navident = ressursState.navident();
        var currentDolOrgtilknytning = getCurrentDolByNavident(navident);
        var futureDolOrgtilknytning = getFutureDolByNavident(navident);
        var erMasseflytting = !isNull(aggregateState.getMasseflytting()) && aggregateState.getMasseflytting();
        logger.info("Masseflytting: {} for navident: {}", erMasseflytting, navident);

        if (ressursState.sluttdato() != null) {
            logger.info("RessursState har en sluttdato for navident {}, sjekker om den er før dagens dato", navident);
            var sluttdato = ressursState.sluttdato();
            if (sluttdato.isBefore(LocalDate.now())) {
                logger.info("RessursState med navident {} har en sluttdato før dagens dato, sletter ressurs i IR", navident);
                remedyIntegrasjonRepositoryService.findTopByNavIdEqualsOrderByEntryIdDesc(navident).ifPresentOrElse(
                    nomIntegrasjonEntity -> {
                        if (nomIntegrasjonEntity.getEntryOperasjon().equals("Slette") && !isAdmin.get()) {
                            logger.info("Ressurs {} er allerede slettet i IR", navident);
                        } else {
                            remedyIntegrasjonRepositoryService.markRessursForDeletion(navident, ressursState.sistEndretAv());
                            logger.info("Ressurs {} er merket for sletting i IR", navident);
                        }
                    }, () -> remedyIntegrasjonRepositoryService.markRessursForDeletion(navident, ressursState.sistEndretAv())
                );

            } else if (!sluttdato.isBefore(LocalDate.now())) {
                logger.info("Ressurs {} har en sluttdato i fremtiden, oppdaterer ressurs i aggregate_state tabellen", navident);
                aggregateStateRepository.save(
                    aggregateState.toBuilder()
                        .delayedUntil(ressursState.sluttdato().plusDays(1))
                        .build());
                logger.info(
                    "RessursState med fid {} ble utsatt i aggregateStateTabellen helt til {}",
                    aggregateState.getStateFid(),
                    ressursState.sluttdato().plusDays(1)
                );
                var orgtilknytningState = currentDolOrgtilknytning.orElseGet(() -> futureDolOrgtilknytning.orElse(null));
                var nomIntegrasjonEntity = nomIntegrasjonEntityFromNomData(ressursState, orgtilknytningState, erMasseflytting);
                var entryBestiller = (ressursState.sistEndretAv() != null && !ressursState.sistEndretAv()
                    .isEmpty()) ? ressursState.sistEndretAv() : (orgtilknytningState != null && orgtilknytningState.sistEndretAv() != null && !orgtilknytningState.sistEndretAv()
                    .isEmpty()) ? orgtilknytningState.sistEndretAv() : "nom-api";
                saveStateToRemedy(nomIntegrasjonEntity, entryBestiller);
                return false;
            }
        } else {
            if (currentDolOrgtilknytning.isPresent()) {
                saveStateToRemedy(
                    nomIntegrasjonEntityFromNomData(ressursState, currentDolOrgtilknytning.get(), erMasseflytting),
                    ressursState.sistEndretAv()
                );
            } else if (futureDolOrgtilknytning.isPresent()) {
                var dolOrgtilknytning = futureDolOrgtilknytning.get();
                saveStateToRemedyIfNewOrReactivatedRessurs(ressursState, dolOrgtilknytning);
            } else {
                throw OrgtilknytningNotFoundException.orgtilknytningNotFoundException(navident);
            }
        }
        return true;
    }

    private void saveStateToRemedyIfNewOrReactivatedRessurs(RessursState ressursState, OrgtilknytningState dolOrgtilknytning) {
        if ((ressursState.startdato() != null && dolOrgtilknytning.gyldigFom() != null)
            && (ressursState.startdato().isAfter(LocalDate.now()) || dolOrgtilknytning.gyldigFom().isAfter(LocalDate.now()))) {

            remedyIntegrasjonRepositoryService.saveNewNomIntegrasjonEntityNew(
                nomIntegrasjonEntityFromNomData(ressursState, dolOrgtilknytning, false), ressursState.sistEndretAv(), isAdmin.get());
            logger.info(
                "Opprettet ressurs i IR med navident {}, startdato {}",
                ressursState.navident(),
                ressursState.startdato()
            );

        } else {
            throw new RequiredValueMissingException(String.format("Ressursen %s manglet tiltredato", ressursState.navident()));
        }
    }

    private void saveStateToRemedy(NomIntegrasjonEntity nomIntegrasjonEntity, String initatedBy) {
        remedyIntegrasjonRepositoryService.saveNewNomIntegrasjonEntityNew(nomIntegrasjonEntity, initatedBy, isAdmin.get());
    }

    private NomIntegrasjonEntity nomIntegrasjonEntityFromNomData(RessursState ressursState, OrgtilknytningState dolOrgtilknytning, boolean isMasseflytting) {
        var jobbtelefon = ressursState.jobbtelefon() != null ? ressursState.jobbtelefon() : null;
        var mobil = getTelefonerFromNom(ressursState.navident());
        var sektor = getSektor(ressursState, dolOrgtilknytning);
        var utlopsdato = getUtlopsdato(sektor, dolOrgtilknytning);

        return NomIntegrasjonEntity.builder()
            .fornavn(formatFornavnWithMellomnavn(ressursState.visningsnavn()).trim())
            .etternavn(ressursState.visningsnavn().etternavn().trim())
            .navId(ressursState.navident())
            .fodselsnummer(ressursState.personident())
            .sektor(sektor)
            .enhetsId(getRemedyEnhetId(dolOrgtilknytning.orgEnhetId()))
            .fysiskBrukerstedId(nonNull(dolOrgtilknytning.primaryFysiskBrukerstedNummer()) ? dolOrgtilknytning.primaryFysiskBrukerstedNummer() : ressursState.primaryFysiskBrukersted())
            .tiltredelsesdato(epochSecondsNorwegianTimeNoon(dolOrgtilknytning.gyldigFom()))
            .utlopsdato(utlopsdato.map(Utils::epochSecondsNorwegianTimeEndOfDay).orElse(null))
            .mobil(jobbtelefon != null ? mapAndTrimNumber(jobbtelefon.nummer()) : mapAndTrimNumber(mobil))
            .masseflytting(isMasseflytting ? "X" : null)
            .build();
    }

    private Optional<OrgtilknytningState> getFutureDolByNavident(String navident) {
        return orgtilknytningStateRepository.findByNavident(navident)
            .stream()
            .filter(OrgtilknytningState::erDagligOppfolging)
            .filter(Utils::isFuture)
            .findFirst();
    }

    private Optional<OrgtilknytningState> getCurrentDolByNavident(String navident) {
        return orgtilknytningStateRepository.findByNavident(navident)
            .stream()
            .filter(OrgtilknytningState::erDagligOppfolging)
            .filter(Utils::isActive)
            .findFirst();
    }

    private Integer getSektor(RessursState ressursState, OrgtilknytningState dolOrgtilknytning) {
        return isNull(dolOrgtilknytning.sektor()) ? nomSektorToRemedyCode(ressursState.sektor()) : nomSektorToRemedyCode(dolOrgtilknytning.sektor());
    }

    private String getTelefonerFromNom(String navident) {
        return nomApiClient.getTelefonnummer(navident);
    }

    private String mapAndTrimNumber(String nummer) {
        if (isNull(nummer)) {
            return null;
        }
        if (nummer.length() >= 10) {
            if (nummer.startsWith("47")) {
                return deleteWhitespace(nummer.substring(2));
            }
            if (nummer.startsWith("+47")) {
                return deleteWhitespace(nummer.substring(3));
            }
        }
        return deleteWhitespace(nummer);
    }

    private String getRemedyEnhetId(String orgEnhetId) {
        return Optional.ofNullable(nomApiClient.getRemedyEnhetId(orgEnhetId))
            .orElseThrow(() -> new RequiredValueMissingException(String.format("No remedyEnhetId set for orgEnhet with id %s", orgEnhetId)));
    }

    private Optional<LocalDate> getUtlopsdato(Integer sektor, OrgtilknytningState dolOrgtilknytning) {
        var maaHaSluttDato = erKonsulentSomMaaHaSluttDato(sektor, dolOrgtilknytning);
        if (maaHaSluttDato && isNull(dolOrgtilknytning.gyldigTom())) {
            throw new RequiredValueMissingException("Utløpsdato er påkrevd for ekstern sektor");
        }
        return maaHaSluttDato ? Optional.of(dolOrgtilknytning.gyldigTom()) : Optional.empty();
    }

    private boolean erKonsulentSomMaaHaSluttDato(Integer sektor, OrgtilknytningState ot) {
        var tilknytningstype = requireNonNullElse(ot.orgtilknytningType(), Tilknytningtype.ANNET);
        //Hvis 'Ekstern' sektor, krev sluttdato med unntak av HELFO og Skatteetaten
        return sektor == 2 && tilknytningstype != Tilknytningtype.HELFO && tilknytningstype != Tilknytningtype.SKATTEETATEN;
    }

    public List<ErrorState> getAllErrorStates() {
        return errorStateRepository.findAll();
    }

    @Transactional
    public void deleteAllErrorStates() {
        errorStateRepository.deleteAll();
        logger.info("Slettet alle error states");
    }

    @Transactional
    public void deleteErrorState(String fid) {
        errorStateRepository.deleteErrorStateByErrorStateFid(fid);
        logger.info("Slettet error state med id: {}", fid);
    }

    @Transactional
    public void lesStatePaaNytt(String navident, boolean isAdmin) {
        try {
            this.isAdmin.set(isAdmin);
            var ressursState = ressursStateRepository.findByNavident(navident)
                .orElseThrow(() -> RessursNotFoundException.notFound(navident));
            executeStateUpdates(
                List.of(
                    AggregateState.builder()
                        .navident(navident)
                        .createdAt(LocalDateTime.now())
                        .stateFid(ressursState.fid())
                        .stateType(ressursState.getClass().getSimpleName())
                        .build()
                ));
        } finally {
            this.isAdmin.remove();
        }

    }

    public List<Event> getAllDelayedEventsBlocking() {
        return delayedEventRepository.findAll().stream()
            .map(DelayedEvent::getEvent)
            .map(jsonNode -> jsonMapper.treeToValue(jsonNode, Event.class))
            .toList();
    }

    public List<NomIntegrasjonEntity> getNomIntegrasjonEntities() {
        return remedyIntegrasjonRepositoryService.findAll();
    }
}
