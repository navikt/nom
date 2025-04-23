package no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon;

import io.getunleash.Unleash;
import no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.client.NomApiClient;
import no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.exceptions.OrgtilknytningNotFoundException;
import no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.exceptions.RequiredValueMissingException;
import no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.exceptions.RessursNotFoundException;
import no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.repository.aggregatestate.AggregateState;
import no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.repository.aggregatestate.AggregateStateRepository;
import no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.repository.remedy.NomIntegrasjonEntity;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateRepository;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import no.nav.nom.libs.utils.common.platform.LeaderElection;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Objects.isNull;
import static no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.Utils.capitalizeNavn;
import static no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.Utils.epochSecondsNorwegianTimeNoon;
import static no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.Utils.formatFornavnWithMellomnavn;
import static no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.Utils.isActive;
import static no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.Utils.isFuture;
import static no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.Utils.nomSektorToRemedyCode;

@Service
public class NomRemedyIntegrasjonServiceNew {
    private final static String UNLEASH_TOGGLE = "nom-remedy.ny-remedy-integrasjon";
    private final static Logger logger = LoggerFactory.getLogger(NomRemedyIntegrasjonServiceNew.class);
    private final LeaderElection leaderElection;
    private final RemedyIntegrasjonRepositoryService remedyIntegrasjonRepositoryService;
    private final NomApiClient nomApiClient;
    private final AggregateStateRepository aggregateStateRepository;
    private final RessursStateRepository ressursStateRepository;
    private final OrgtilknytningStateRepository orgtilknytningStateRepository;
    private final Unleash unleash;

    public NomRemedyIntegrasjonServiceNew(
        LeaderElection leaderElection,
        RemedyIntegrasjonRepositoryService remedyIntegrasjonRepositoryService,
        NomApiClient nomApiClient,
        AggregateStateRepository aggregateStateRepository,
        RessursStateRepository ressursStateRepository,
        OrgtilknytningStateRepository orgtilknytningStateRepository,
        Unleash unleash
    ) {
        this.leaderElection = leaderElection;
        this.remedyIntegrasjonRepositoryService = remedyIntegrasjonRepositoryService;
        this.nomApiClient = nomApiClient;
        this.aggregateStateRepository = aggregateStateRepository;
        this.ressursStateRepository = ressursStateRepository;
        this.orgtilknytningStateRepository = orgtilknytningStateRepository;
        this.unleash = unleash;
    }

    @Scheduled(cron = "*/5 * * * * *")
    public void executeImmediateStateUpdate() {
        if (leaderElection.isLeader()) {
            var statesToUpdate = aggregateStateRepository.findAllOlderThanFiveSecondsWhereDelayedUntilIsNull();
            List<UUID> rowsToDelete = executeStateUpdates(statesToUpdate);
            aggregateStateRepository.deleteAllById(rowsToDelete);
        }
    }

    @Scheduled(cron = "0 15 0 * * *")
    public void executeDelayedStateUpdate() {
        if (leaderElection.isLeader()) {
            var delayedStates = aggregateStateRepository.findByDelayedUntilAndMasseflyttingIsFalse(LocalDate.now());
            List<UUID> rowsToDelete = executeStateUpdates(delayedStates);
            aggregateStateRepository.deleteAllById(rowsToDelete);
        }
    }

    private List<UUID> executeStateUpdates(List<AggregateState> statesToUpdate) {
        return statesToUpdate.stream().map(aggregateState -> {
            AtomicReference<UUID> fid = new AtomicReference<>(null);
            var optionalRessursState = aggregateState.getStateType().equals(RessursState.class.getSimpleName()) ? ressursStateRepository.findByFid(
                aggregateState.getStateFid()) : Optional.<RessursState>empty();
            var optionalOrgtilknytningState = aggregateState.getStateType()
                .equals(OrgtilknytningState.class.getSimpleName()) ? orgtilknytningStateRepository.findByFid(aggregateState.getStateFid()) : Optional.<OrgtilknytningState>empty();
            try {
                optionalRessursState.ifPresent(ressursState -> {
                    if (ressursStateUpdate(ressursState, aggregateState)) {
                        fid.set(aggregateState.getAggregateStateId());
                    }
                });
                optionalOrgtilknytningState.ifPresent(orgtilknytningState -> {
                    if (orgtilknytningStateUpdate(orgtilknytningState, aggregateState)) {
                        fid.set(aggregateState.getAggregateStateId());
                    }
                });
            } catch (Exception e) {
                logger.error("ALERT: Failed to handle aggregate state with fid {} and type {}", aggregateState.getStateFid(), aggregateState.getStateType(), e);
            }
            return isNull(fid.get()) ? null : fid.get();
        }).filter(Objects::nonNull).toList();
    }

    private boolean orgtilknytningStateUpdate(OrgtilknytningState orgtilknytningState, AggregateState aggregateState) {
        if (orgtilknytningState.gyldigTom().isPresent() && orgtilknytningState.gyldigTom().get().isBefore(LocalDate.now())) return true;

        var ressursState = ressursStateRepository.findByNavident(orgtilknytningState.navident())
            .orElseThrow(() -> RessursNotFoundException.notFound(String.format("Fant ikke ressurs for orgtilknytning %s", orgtilknytningState.fid())));

        var entryBestiller = orgtilknytningState.sistEndretAv() != null ? orgtilknytningState.sistEndretAv() : "nom-api";
        var erDagligOppfolging = orgtilknytningState.erDagligOppfolging();
        var erMasseflytting = !isNull(aggregateState.getMasseflytting()) && aggregateState.getMasseflytting();

        if (erDagligOppfolging && (isActive(orgtilknytningState) || (isFuture(orgtilknytningState) && erMasseflytting))) {
            saveStateToRemedy(nomIntegrasjonEntityFromNomData(ressursState, orgtilknytningState, erMasseflytting), entryBestiller);
            return true;
        }

        if (isFuture(orgtilknytningState) && erDagligOppfolging) {
            var currentDolOrgtilknytning = getCurrentDolByNavident(orgtilknytningState.navident());
            if (currentDolOrgtilknytning.isPresent()) {
                aggregateStateRepository.save(
                    aggregateState.toBuilder()
                        .delayedUntil(orgtilknytningState.gyldigFom())
                        .build());
                logger.info(
                    "OrgtilknytningState med fid {} ble utsatt i aggregateStateTabellen helt til {}",
                    aggregateState.getStateFid(),
                    orgtilknytningState.gyldigFom()
                );
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
        var erMasseflytting = !isNull(aggregateState.getMasseflytting()) && aggregateState.getMasseflytting();
        logger.info("Masseflytting: {} for navident: {}", erMasseflytting, navident);

        var futureDolOrgtilknytning = getFutureDolByNavident(navident);

        if (currentDolOrgtilknytning.isPresent()) {
            saveStateToRemedy(
                nomIntegrasjonEntityFromNomData(ressursState, currentDolOrgtilknytning.get(), erMasseflytting),
                ressursState.sistEndretAv()
            );
        } else if (futureDolOrgtilknytning.isPresent()) {
            var dolOrgtilknytning = futureDolOrgtilknytning.get();
            saveStateToRemedyIfNewOrReactivatedRessurs(ressursState, dolOrgtilknytning);
        } else if (ressursState.sluttdato().isPresent() && ressursState.sluttdato().get().isBefore(LocalDate.now())) {
            if (unleash.isEnabled(UNLEASH_TOGGLE)) {
                remedyIntegrasjonRepositoryService.findTopByNavIdEqualsOrderByEntryIdDesc(navident).ifPresentOrElse(nomIntegrasjonEntity -> {
                    if (nomIntegrasjonEntity.getEntryOperasjon().equals("Slette")) {
                        logger.info("Ressurs {} er allerede slettet i IR", navident);
                    } else {
                        remedyIntegrasjonRepositoryService.markRessursForDeletion(navident, ressursState.sistEndretAv());
                        logger.info("Ressurs {} er merket for sletting i IR", navident);
                    }
                }, () -> remedyIntegrasjonRepositoryService.markRessursForDeletion(navident, ressursState.sistEndretAv()));
            } else {
                logger.info("Ressurs {} er merket for sletting i IR", navident);
            }
        } else {
            throw OrgtilknytningNotFoundException.orgtilknytningNotFoundException(navident);
        }
        return true;
    }

    private void saveStateToRemedyIfNewOrReactivatedRessurs(RessursState ressursState, OrgtilknytningState dolOrgtilknytning) {
        if ((ressursState.startdato().isPresent() && dolOrgtilknytning.gyldigFom() != null)
            && (ressursState.startdato().get().isAfter(LocalDate.now()) || dolOrgtilknytning.gyldigFom().isAfter(LocalDate.now()))) {
            if (unleash.isEnabled(UNLEASH_TOGGLE)) {
                var entryId = remedyIntegrasjonRepositoryService.saveNewNomIntegrasjonEntityNew(
                        nomIntegrasjonEntityFromNomData(ressursState, dolOrgtilknytning, false), ressursState.sistEndretAv())
                    .getEntryId();
                logger.info(
                    "Opprettet ressurs i IR med navident {}, startdato {} og entryId {}",
                    ressursState.navident(),
                    ressursState.startdato().get(),
                    entryId
                );
            } else {
                logger.info("Opprettet ressurs i IR med navident {}, startdato {}", ressursState.navident(), ressursState.startdato().get());
            }
        } else {
            throw new RequiredValueMissingException(String.format("Ressursen %s manglet tiltredato", ressursState.navident()));
        }
    }

    private void saveStateToRemedy(NomIntegrasjonEntity nomIntegrasjonEntity, String initatedBy) {
        if (unleash.isEnabled(UNLEASH_TOGGLE)) {
            remedyIntegrasjonRepositoryService.saveNewNomIntegrasjonEntityNew(nomIntegrasjonEntity, initatedBy);
        } else {
            logger.info("Lagret ny rad for ressurs {} med enhetsId {}", nomIntegrasjonEntity.getNavId(), nomIntegrasjonEntity.getEnhetsId());
        }
    }

    private NomIntegrasjonEntity nomIntegrasjonEntityFromNomData(RessursState ressursState, OrgtilknytningState dolOrgtilknytning, boolean isMasseflytting) {
        var mobil = getTelefonerFromNom(ressursState.navident());
        var sektor = getSektor(ressursState, dolOrgtilknytning);
        var utlopsdato = getUtlopsdato(sektor, dolOrgtilknytning);

        return NomIntegrasjonEntity.builder()
            .fornavn(capitalizeNavn(formatFornavnWithMellomnavn(ressursState.visningsnavn()).trim()))
            .etternavn(ressursState.visningsnavn().etternavn().trim())
            .navId(ressursState.navident())
            .fodselsnummer(ressursState.personident())
            .sektor(sektor)
            .enhetsId(getRemedyEnhetId(dolOrgtilknytning.orgEnhetId()))
            .fysiskBrukerstedId(ressursState.primaryFysiskBrukersted().orElse(null))
            .tiltredelsesdato(epochSecondsNorwegianTimeNoon(dolOrgtilknytning.gyldigFom()))
            .utlopsdato(utlopsdato.map(Utils::epochSecondsNorwegianTimeEndOfDay).orElse(null))
            .mobil(mapAndTrimNumber(mobil))
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
        return isNull(ressursState.sektor()) ? nomSektorToRemedyCode(dolOrgtilknytning.sektor()) : nomSektorToRemedyCode(ressursState.sektor());
    }

    private String getTelefonerFromNom(String navident) {
        return nomApiClient.getTelefonnummer(navident);
    }

    private String mapAndTrimNumber(String nummer) {
        if (isNull(nummer)) return null;
        if (nummer.length() >= 10) {
            if (nummer.startsWith("47")) {
                return nummer.substring(2);
            }
            if (nummer.startsWith("+47")) {
                return nummer.substring(3);
            }
        }
        return StringUtils.deleteWhitespace(nummer);
    }

    private String getRemedyEnhetId(String orgEnhetId) {
        return Optional.ofNullable(nomApiClient.getRemedyEnhetId(orgEnhetId))
            .orElseThrow(() -> new RequiredValueMissingException(String.format("No remedyEnhetId set for orgEnhet with id %s", orgEnhetId)));
    }

    private Optional<LocalDate> getUtlopsdato(Integer sektor, OrgtilknytningState dolOrgtilknytning) {
        if (sektor == 2 && dolOrgtilknytning.gyldigTom().isEmpty()) {
            throw new RequiredValueMissingException("Utløpsdato er påkrevd for ekstern sektor");
        }
        return sektor == 2 ? dolOrgtilknytning.gyldigTom() : Optional.empty();
    }
}
