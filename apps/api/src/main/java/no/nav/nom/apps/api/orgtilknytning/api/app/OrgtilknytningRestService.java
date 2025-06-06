package no.nav.nom.apps.api.orgtilknytning.api.app;

import io.getunleash.Unleash;
import no.nav.nom.apps.api.authorize.AuthResolver;
import no.nav.nom.apps.api.authorize.redigereressurs.AuthorizedNavident;
import no.nav.nom.apps.api.common.DateUtils;
import no.nav.nom.apps.api.common.security.NomApiAccessScope;
import no.nav.nom.apps.api.graphql.common.SecurityUtil;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.apps.api.orgtilknytning.MasseflyttOrgtilknytningerService;
import no.nav.nom.apps.api.orgtilknytning.api.exceptions.InvalidOppdaterOrgtilknytningerForRessursRequestException;
import no.nav.nom.apps.api.orgtilknytning.api.exceptions.InvalidOrgtilknytningSektorException;
import no.nav.nom.apps.api.orgtilknytning.api.exceptions.InvalidOrgtilknytningTilknytningtypeException;
import no.nav.nom.apps.api.orgtilknytning.api.exceptions.InvalidOrgtilknytningTypeAndSektorCombinationException;
import no.nav.nom.apps.api.orgtilknytning.api.exceptions.OrgtilknytningNotFoundException;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.OrgtilknytningCommandService;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.EndreDagligOppfolgingOrgtilknytningCommand;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.EndreOrgtilknytningDatoCommand;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.EndreOrgtilknytningSektorCommand;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.EndreTilknytningtypeCommand;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.OpprettOrgtilknytning;
import no.nav.nom.apps.api.ressurs.NomMastered;
import no.nav.nom.apps.api.ressurs.api.exceptions.InvalidSektorException;
import no.nav.nom.apps.api.ressurs.api.exceptions.RessursNotFoundException;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.RessursCommandService;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OppdaterSektorCommand;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OppdaterSluttdatoCommand;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OppdaterStartdatoCommand;
import no.nav.nom.contract.nom.IdentType;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.Tilknytningtype;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.Origination;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateRepository;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.isNull;
import static no.nav.nom.apps.api.common.DateUtils.isPeriodActive;
import static no.nav.nom.apps.api.common.DateUtils.isPerioderOverlapping;
import static no.nav.nom.apps.api.common.security.NomApiAccessScope.ADMIN;
import static no.nav.nom.apps.api.common.security.NomApiAccessScope.NOM_WRITE_ROBOT;
import static no.nav.nom.apps.api.common.security.NomApiAccessScope.NOM_WRITE_UTL_KONSULENT;
import static no.nav.nom.apps.api.common.security.NomApiAccessScope.NOM_WRITE_VIKAFOSSEN;
import static no.nav.nom.apps.api.graphql.common.SecurityUtil.getAuthenticatedNavIdent;
import static no.nav.nom.apps.api.graphql.common.SecurityUtil.loggedInUserHasAccessScope;

@Service
public class OrgtilknytningRestService {
    private final static Logger log = LoggerFactory.getLogger(OrgtilknytningRestService.class);

    public static final String BRUK_NY_TILGANGSVALIDERING = "nom.orgtilknytning.bruk_ny_tilgangsvalidering";

    private final OrgtilknytningStateRepository orgtilknytningStateRepository;
    private final RessursStateRepository ressursStateRepository;
    private final NomMastered nomMastered;
    private final OrgtilknytningCommandService orgtilknytningCommandService;
    private final RessursCommandService ressursCommandService;
    private final AuthorizedNavident authorizedNavident;
    private final AuthResolver authResolver;
    private final MasseflyttOrgtilknytningerService masseflyttOrgtilknytningerService;
    private final Unleash unleash;

    @Value("${app.orgenhet.vikafossenId}")
    private String vikafossenOrgEnhetId;

    public OrgtilknytningRestService(
        OrgtilknytningStateRepository orgtilknytningStateRepository,
        OrgtilknytningCommandService orgtilknytningCommandService,
        RessursCommandService ressursCommandService,
        RessursStateRepository ressursStateRepository,
        NomMastered nomMastered,
        AuthorizedNavident authorizedNavident, AuthResolver authResolver,
        MasseflyttOrgtilknytningerService masseflyttOrgtilknytningerService, Unleash unleash
    ) {
        this.orgtilknytningStateRepository = orgtilknytningStateRepository;
        this.orgtilknytningCommandService = orgtilknytningCommandService;
        this.ressursCommandService = ressursCommandService;
        this.ressursStateRepository = ressursStateRepository;
        this.nomMastered = nomMastered;
        this.authorizedNavident = authorizedNavident;
        this.authResolver = authResolver;
        this.masseflyttOrgtilknytningerService = masseflyttOrgtilknytningerService;
        this.unleash = unleash;
    }

    public List<OrgtilknytningState> getOrgtilknytninger(Optional<String> navident) {
        return navident.map(
                presentNavident -> orgtilknytningStateRepository.findByNavident(presentNavident).stream().toList()
            )
            .orElseGet(
                () -> orgtilknytningStateRepository.getAll().stream().toList()
            );
    }

    public Map<String, List<OrgtilknytningState>> getOrgtilknytninger(List<String> navidenter) {
        return orgtilknytningStateRepository.findByNavidentIn(navidenter).stream()
            .collect(Collectors.groupingBy(OrgtilknytningState::navident));
    }

    public OrgtilknytningState getOrgtilknytning(String fid) {
        return orgtilknytningStateRepository.findByFid(fid)
            .orElseThrow(() -> OrgtilknytningNotFoundException.fid(fid));
    }


    // Oppdaterer eksisterende og legger til nye orgtilknytninger for en ressurs
    // Validerer at det oppgis gyldige tidsintervall (gyldigFom < gyldigTom)
    // Validerer at det ikke settes DOL-orgtilknytninger (erDagligOppfolging=true) med overlappende gyldighetsperiode
    // Validerer at oppdaterOrgtilknytningerForRessursRequest.eksisterendeOrgtilknytninger inneholder alle, og inneholder kun, eksisterende orgtilknytninger for tilknyttet ressurs.
    // Påser at det ikke sendes unødvendige endring-events (endring-events som ikke gir ny verdi til state)
    // TODO: Valider at det til en hver tid finnes en aktiv daglig oppfolging
    public void oppdaterOrgtilknytningerForRessurs(String navident, OppdaterOrgtilknytningerForRessursRequest oppdaterOrgtilknytningerForRessursRequest) {
        validateOppdaterOrgtilknytningerForRessursRequest(oppdaterOrgtilknytningerForRessursRequest, authResolver.kanRedigereRessurs(navident), navident);
        var tilknyttetRessurs = ressursStateRepository.findByNavident(navident)
            .orElseThrow(() -> RessursNotFoundException.navident(navident));

        var existingOrgtilknytningerForRessurs = orgtilknytningStateRepository.findByNavident(navident).stream()
            .collect(Collectors.toMap(OrgtilknytningState::fid, Function.identity()));

        validateOrgTilknytningerToUpdateHasNoDuplicateFidsAndIncludesAllExistingOrgtilknytningerForRessurs(
            existingOrgtilknytningerForRessurs.keySet(), oppdaterOrgtilknytningerForRessursRequest.eksisterendeOrgtilknytninger()
        );
        oppdaterOrgtilknytningerForRessursRequest.eksisterendeOrgtilknytninger().stream()
            .filter(endreOrgtilknytningRequest -> endreOrgtilknytningRequestProvidesChangesToState(
                endreOrgtilknytningRequest,
                existingOrgtilknytningerForRessurs.get(endreOrgtilknytningRequest.fid())
            ))
            .forEach(this::endreOrgtilknytning);

        oppdaterOrgtilknytningerForRessursRequest.nyeOrgtilknytninger().forEach(
            leggTilOrgtilknytningRequest -> leggTilOrgtilknytningForRessurs(
                tilknyttetRessurs.navident(), leggTilOrgtilknytningRequest
            )
        );

        if (oppdaterOrgtilknytningerForRessursRequest.setRessursSluttdato()) {
            oppdaterRessursSluttdato(
                tilknyttetRessurs,
                oppdaterOrgtilknytningerForRessursRequest
            );
        }

        if (oppdaterOrgtilknytningerForRessursRequest.setRessursStartdato()) {
            oppdaterRessursStartdato(
                tilknyttetRessurs,
                oppdaterOrgtilknytningerForRessursRequest
            );
        }
        nomMastered.markRessursAsMasteredInNom(navident);
    }

    private void validateOrgtilknytningerHasNoActiveAndSektorAndTilknytningDuplicates(OppdaterOrgtilknytningerForRessursRequest oppdaterOrgtilknytningerForRessursRequest) {
        var nyeOrgtilknytninger = oppdaterOrgtilknytningerForRessursRequest.nyeOrgtilknytninger().stream()
            .map(leggTilOrgtilknytningRequest ->
                OrgtilknytningState.builder()
                    .orgEnhetId(leggTilOrgtilknytningRequest.orgEnhetId())
                    .sektor(leggTilOrgtilknytningRequest.sektor())
                    .orgtilknytningType(Optional.ofNullable(leggTilOrgtilknytningRequest.tilknytningtype()))
                    .gyldigFom(leggTilOrgtilknytningRequest.gyldigFom())
                    .gyldigTom(Optional.ofNullable(leggTilOrgtilknytningRequest.gyldigTom()))
                    .build())
            .toList();

        var eksisterendeOrgtilknytninger = oppdaterOrgtilknytningerForRessursRequest.eksisterendeOrgtilknytninger().stream()
            .map(endreOrgtilknytningRequest ->
                OrgtilknytningState.builder()
                    .orgEnhetId(orgtilknytningStateRepository.findByFid(endreOrgtilknytningRequest.fid()).map(OrgtilknytningState::orgEnhetId).orElse(""))
                    .sektor(endreOrgtilknytningRequest.sektor())
                    .orgtilknytningType(Optional.ofNullable(endreOrgtilknytningRequest.tilknytningtype()))
                    .gyldigFom(endreOrgtilknytningRequest.gyldigFom())
                    .gyldigTom(Optional.ofNullable(endreOrgtilknytningRequest.gyldigTom()))
                    .build())
            .toList();

        Map<String, List<OrgtilknytningState>> possibleDuplicates = Stream.concat(nyeOrgtilknytninger.stream(), eksisterendeOrgtilknytninger.stream())
            .filter(orgtilknytningState -> isPeriodActive(orgtilknytningState.gyldigFom(), orgtilknytningState.gyldigTom()) || orgtilknytningState.gyldigFom().isAfter(LocalDate.now()))
            .collect(Collectors.groupingBy(OrgtilknytningState::orgEnhetId))
            .entrySet().stream()
            .filter(entry -> entry.getValue().size() > 1)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        for (Map.Entry<String, List<OrgtilknytningState>> entry : possibleDuplicates.entrySet()) {
            entry.getValue().sort(Comparator.comparing(OrgtilknytningState::gyldigFom));
            var firstDuplicate = entry.getValue().getFirst();
            for (int i = 1; i < entry.getValue().size(); i++) {
                var duplicate = entry.getValue().get(i);
                if (isPeriodActive(firstDuplicate.gyldigFom(), firstDuplicate.gyldigTom())
                    && isPeriodActive(duplicate.gyldigFom(), duplicate.gyldigTom())
                    && firstDuplicate.sektor().equals(duplicate.sektor())
                    && firstDuplicate.orgtilknytningType().equals(duplicate.orgtilknytningType())) {
                    throw InvalidOppdaterOrgtilknytningerForRessursRequestException.duplikatOrgtilknytning(
                        firstDuplicate.orgEnhetId(),
                        entry.getKey()
                    );
                }

                if (firstDuplicate.gyldigTom().isPresent() && duplicate.gyldigFom().isBefore(firstDuplicate.gyldigTom().get())
                    && firstDuplicate.sektor().equals(duplicate.sektor())
                    && firstDuplicate.orgtilknytningType().equals(duplicate.orgtilknytningType())) {
                    throw InvalidOppdaterOrgtilknytningerForRessursRequestException.duplikatOrgtilknytning(
                        firstDuplicate.orgEnhetId(),
                        entry.getKey()
                    );
                }

                if (firstDuplicate.gyldigTom().isEmpty() && duplicate.gyldigFom().isAfter(firstDuplicate.gyldigFom())
                    && firstDuplicate.sektor().equals(duplicate.sektor())
                    && firstDuplicate.orgtilknytningType().equals(duplicate.orgtilknytningType())) {
                    throw InvalidOppdaterOrgtilknytningerForRessursRequestException.duplikatOrgtilknytning(
                        firstDuplicate.orgEnhetId(),
                        entry.getKey()
                    );
                }
                firstDuplicate = duplicate;
            }
        }
    }

    private void oppdaterRessursStartdato(RessursState tilknyttetRessurs, OppdaterOrgtilknytningerForRessursRequest request) {

        var endredeFomDatoer = request.eksisterendeOrgtilknytninger().stream()
            .map(EndreOrgtilknytningRequest::gyldigFom);
        var nyeFomDatoer = request.nyeOrgtilknytninger().stream()
            .map(LeggTilOrgtilknytningRequest::gyldigFom);

        var nyMinimumStartdato = Stream.concat(endredeFomDatoer, nyeFomDatoer).reduce(LocalDate.MAX, (it, acc) -> it.isBefore(acc) ? it : acc);
        if (nyMinimumStartdato != LocalDate.MAX && !nyMinimumStartdato.equals(tilknyttetRessurs.startdato().orElse(null))) {
            ressursCommandService.oppdaterRessursStartdatoEvent(
                new OppdaterStartdatoCommand(tilknyttetRessurs.fid(), nyMinimumStartdato),
                getInputMetadataOppdaterOrgtilknytningerForRessurs(request)
            );
        }
    }

    private void validateOppdaterOrgtilknytningerForRessursRequest(OppdaterOrgtilknytningerForRessursRequest oppdaterOrgtilknytningerForRessursRequest,
                                                                   boolean isLoggedInUserLederForNavident, String navident) {
        if (!unleash.isEnabled(BRUK_NY_TILGANGSVALIDERING)) {
            validateLederKanOppdatereOrgtilknytninger(oppdaterOrgtilknytningerForRessursRequest, isLoggedInUserLederForNavident);
        }
        validateEndreOrgtilknytningUnderToMaaneder(oppdaterOrgtilknytningerForRessursRequest);
        validateChronologicalDates(oppdaterOrgtilknytningerForRessursRequest);
        validateNoOverlappingDagligeOppfolginger(oppdaterOrgtilknytningerForRessursRequest);

        validateOrgtilknytningHasSektor(oppdaterOrgtilknytningerForRessursRequest);
        validateOrgtilknytningHasTilknytningtype(oppdaterOrgtilknytningerForRessursRequest);
        validateOrgtilknytningerHasNoActiveAndSektorAndTilknytningDuplicates(oppdaterOrgtilknytningerForRessursRequest);

        validateOrgtilknytningTypeSektorCombination(oppdaterOrgtilknytningerForRessursRequest);

        validateOrgtilknytningForVikafossen(oppdaterOrgtilknytningerForRessursRequest, navident);
    }

    private void validateOrgtilknytningTypeSektorCombination(OppdaterOrgtilknytningerForRessursRequest oppdaterOrgtilknytningerForRessursRequest) {
        oppdaterOrgtilknytningerForRessursRequest.eksisterendeOrgtilknytninger().forEach(endreOrgtilknytningRequest -> {
            var origOT = orgtilknytningStateRepository.findByFid(endreOrgtilknytningRequest.fid()).orElseThrow(() -> OrgtilknytningNotFoundException.fid(endreOrgtilknytningRequest.fid()));
            if (!Objects.equals(origOT.orgtilknytningType().orElse(null), endreOrgtilknytningRequest.tilknytningtype())) {
                validateOrgtilknytningTypeSektorCombination(endreOrgtilknytningRequest.tilknytningtype(), endreOrgtilknytningRequest.sektor());
            }
        });

        oppdaterOrgtilknytningerForRessursRequest.nyeOrgtilknytninger().forEach(nyeOrgtilknytningRequest ->
            validateOrgtilknytningTypeSektorCombination(nyeOrgtilknytningRequest.tilknytningtype(), nyeOrgtilknytningRequest.sektor())
        );
    }

    private void validateOrgtilknytningHasSektor(OppdaterOrgtilknytningerForRessursRequest oppdaterOrgtilknytningerForRessursRequest) {
        oppdaterOrgtilknytningerForRessursRequest.eksisterendeOrgtilknytninger().forEach(endreOrgtilknytningRequest -> {
            if (!isActiveOrFuture(endreOrgtilknytningRequest.gyldigFom(), endreOrgtilknytningRequest.gyldigTom())) return;
            if (isNull(endreOrgtilknytningRequest.sektor())) {
                orgtilknytningStateRepository.findByFid(endreOrgtilknytningRequest.fid()).ifPresent(orgtilknytningState -> {
                    throw InvalidOrgtilknytningSektorException.sektorIsNull(orgtilknytningState.orgEnhetId());
                });
                throw new InvalidOrgtilknytningSektorException("Aktive Orgtilknytninger må ha en sektor");
            }
        });

        oppdaterOrgtilknytningerForRessursRequest.nyeOrgtilknytninger().forEach(endreOrgtilknytningRequest -> {
            if (!isActiveOrFuture(endreOrgtilknytningRequest.gyldigFom(), endreOrgtilknytningRequest.gyldigTom())) return;
            if (isNull(endreOrgtilknytningRequest.sektor())) {
                throw InvalidOrgtilknytningSektorException.sektorIsNull(endreOrgtilknytningRequest.orgEnhetId());
            }
        });
    }

    private void validateOrgtilknytningHasTilknytningtype(OppdaterOrgtilknytningerForRessursRequest oppdaterOrgtilknytningerForRessursRequest) {
        oppdaterOrgtilknytningerForRessursRequest.eksisterendeOrgtilknytninger().forEach(endreOrgtilknytningRequest -> {
            if (!isActiveOrFuture(endreOrgtilknytningRequest.gyldigFom(), endreOrgtilknytningRequest.gyldigTom())) return;
            if (endreOrgtilknytningRequest.sektor().erKommunal()) return;
            if (isNull(endreOrgtilknytningRequest.tilknytningtype())) {
                orgtilknytningStateRepository.findByFid(endreOrgtilknytningRequest.fid()).ifPresent(orgtilknytningState -> {
                    throw InvalidOrgtilknytningTilknytningtypeException.tilknytningtypeIsNull(orgtilknytningState.orgEnhetId());
                });
                throw new InvalidOrgtilknytningTilknytningtypeException("Tilknytningstype er ikke satt på Orgtilknytning");
            }
        });

        oppdaterOrgtilknytningerForRessursRequest.nyeOrgtilknytninger().forEach(endreOrgtilknytningRequest -> {
            if (!isActiveOrFuture(endreOrgtilknytningRequest.gyldigFom(), endreOrgtilknytningRequest.gyldigTom())) return;
            if (endreOrgtilknytningRequest.sektor().erKommunal()) return;
            if (isNull(endreOrgtilknytningRequest.tilknytningtype())) {
                throw InvalidOrgtilknytningTilknytningtypeException.tilknytningtypeIsNull(endreOrgtilknytningRequest.orgEnhetId());
            }
        });
    }

    private void validateEndreOrgtilknytningUnderToMaaneder(OppdaterOrgtilknytningerForRessursRequest oppdaterOrgtilknytningerForRessursRequest) {
        if (loggedInUserHasAccessScope(NomApiAccessScope.ADMIN)) return;
        log.info("Validerer at nyeOrgtilknytninger ikke er eldre enn to måneder");
        oppdaterOrgtilknytningerForRessursRequest.nyeOrgtilknytninger().forEach(leggTilOrgtilknytningRequest -> {
            if (leggTilOrgtilknytningRequest.gyldigTom() != null && leggTilOrgtilknytningRequest.gyldigTom().isBefore(LocalDate.now().minusMonths(2))) {
                throw new AccessDeniedException("Det er ikke lov å legge til en orgtilknytning som er eldre enn to måneder gammelt, gjelder endring for orgenhet " + leggTilOrgtilknytningRequest.orgEnhetId());
            }
        });

        log.info("Validerer at eksisterendeOrgtilknytninger ikke er eldre enn to måneder");
        oppdaterOrgtilknytningerForRessursRequest.eksisterendeOrgtilknytninger().forEach(orgtilknytningRequest -> orgtilknytningStateRepository.findByFid(orgtilknytningRequest.fid()).ifPresent(orgtilknytningState -> {
            if (compareDateForExistingOrgtilknytningWithStateOrgtilknytning(orgtilknytningRequest, orgtilknytningState)) return;
            if (orgtilknytningState.gyldigTom().isPresent() && orgtilknytningState.gyldigTom().get().isBefore(LocalDate.now().minusMonths(2))) {
                throw new AccessDeniedException("Det er ikke lov å endre en orgtilknytning som er eldre enn to måneder gammelt, gjelder endring for orgenhet " + orgtilknytningState.orgEnhetId());
            }
        }));
    }

    private boolean compareDateForExistingOrgtilknytningWithStateOrgtilknytning(
        EndreOrgtilknytningRequest orgtilknytningRequest,
        OrgtilknytningState orgtilknytningState
    ) {
        var stateDate = orgtilknytningState.gyldigTom().isPresent() ? orgtilknytningState.gyldigTom().get() : LocalDate.MAX;
        return new EqualsBuilder()
            .append(orgtilknytningRequest.gyldigFom(), orgtilknytningState.gyldigFom())
            .append(orgtilknytningRequest.gyldigTom(), stateDate)
            .isEquals();
    }

    private void validateLederKanOppdatereOrgtilknytninger(OppdaterOrgtilknytningerForRessursRequest oppdaterOrgtilknytningerForRessursRequest,
                                                           boolean isLeder) {
        var alleUnderliggendeEnheterTilLeder = authorizedNavident.orgenheterLederKanRedigere(getAuthenticatedNavIdent());
        Set<String> orgenhetIds = alleUnderliggendeEnheterTilLeder.stream().map(IOrgenhet::getId).collect(Collectors.toSet());
        var orgtilknytningFids = oppdaterOrgtilknytningerForRessursRequest.eksisterendeOrgtilknytninger().stream()
            .map(EndreOrgtilknytningRequest::fid)
            .distinct()
            .toList();

        var eksisterendeOrgtilknytninger = orgtilknytningStateRepository.findByFidIn(orgtilknytningFids);
        log.info("Leder har tilgang til orgenhetene: {}", orgenhetIds);

        if (!isLeder) {
            if (!oppdaterOrgtilknytningerForRessursRequest.nyeOrgtilknytninger().isEmpty()) {
                boolean hasAccessToAddOrgtilknytning = oppdaterOrgtilknytningerForRessursRequest.nyeOrgtilknytninger()
                    .stream()
                    .anyMatch(leggTilOrgtilknytningRequest -> orgenhetIds.contains(leggTilOrgtilknytningRequest.orgEnhetId()));

                if (!hasAccessToAddOrgtilknytning) {
                    eksisterendeOrgtilknytninger.stream()
                        .filter(endreOrgtilknytningRequest -> orgenhetIds.contains(endreOrgtilknytningRequest.orgEnhetId()))
                        .findAny()
                        .orElseThrow(() -> new AccessDeniedException("Du har lagt til og/eller prøvd å endre en organisasjonstilknytning for en orgenhet som du ikke har tilgang til."));
                }
            } else {
                eksisterendeOrgtilknytninger.stream()
                    .filter(eksisterendeOrgtilknytning -> orgenhetIds.contains(eksisterendeOrgtilknytning.orgEnhetId()))
                    .findAny()
                    .orElseThrow(() -> new AccessDeniedException("Du er ikke leder for en eksisterende Orgtilknytning til denne medarbeideren og kan derfor ikke endre personens orgtilknytning."));
            }
        }
    }


    private void validateChronologicalDates(OppdaterOrgtilknytningerForRessursRequest oppdaterOrgtilknytningerForRessursRequest) {
        oppdaterOrgtilknytningerForRessursRequest.nyeOrgtilknytninger()
            .forEach(leggTilOrgtilknytningRequest -> validateGyldigFomNotAfterGyldigTom(
                leggTilOrgtilknytningRequest.gyldigFom(),
                Optional.ofNullable(leggTilOrgtilknytningRequest.gyldigTom()).orElse(LocalDate.MAX)
            ));

        oppdaterOrgtilknytningerForRessursRequest.eksisterendeOrgtilknytninger()
            .forEach(endreOrgtilknytningRequest -> validateGyldigFomNotAfterGyldigTom(
                endreOrgtilknytningRequest.gyldigFom(),
                Optional.ofNullable(endreOrgtilknytningRequest.gyldigTom()).orElse(LocalDate.MAX)
            ));
    }

    private void validateNoOverlappingDagligeOppfolginger(OppdaterOrgtilknytningerForRessursRequest oppdaterOrgtilknytningerForRessursRequest) {

        var dagligOppfolgingPerioder = Stream.of(
            oppdaterOrgtilknytningerForRessursRequest.eksisterendeOrgtilknytninger().stream()
                .filter(EndreOrgtilknytningRequest::erDagligOppfolging)
                .map(leggTilOrgtilknytningRequest ->
                    new DateUtils.Periode(
                        leggTilOrgtilknytningRequest.gyldigFom(),
                        Optional.ofNullable(leggTilOrgtilknytningRequest.gyldigTom()).orElse(LocalDate.MAX)
                    )
                ).toList(),
            oppdaterOrgtilknytningerForRessursRequest.nyeOrgtilknytninger().stream()
                .filter(LeggTilOrgtilknytningRequest::erDagligOppfolging)
                .map(leggTilOrgtilknytningRequest ->
                    new DateUtils.Periode(
                        leggTilOrgtilknytningRequest.gyldigFom(),
                        Optional.ofNullable(leggTilOrgtilknytningRequest.gyldigTom()).orElse(LocalDate.MAX)
                    )
                ).toList()
        ).flatMap(Collection::stream).toList();


        var overlappendeDagligeOppfolginger = dagligOppfolgingPerioder.stream()
            .filter(periode -> dagligOppfolgingPerioder.stream()
                .filter(
                    otherPeriode -> isPerioderOverlapping(periode, otherPeriode)
                ).toList().size() > 1
            ).toList();

        if (overlappendeDagligeOppfolginger.size() > 1) {
            throw InvalidOppdaterOrgtilknytningerForRessursRequestException.overlappendeDagligeOppfolginger(overlappendeDagligeOppfolginger);
        }
    }

    private void validateGyldigFomNotAfterGyldigTom(LocalDate gyldigFom, LocalDate gyldigTom) {
        if (gyldigFom.isAfter(gyldigTom)) {
            throw InvalidOppdaterOrgtilknytningerForRessursRequestException.gyldigFomAfterGyldigTom(
                gyldigFom,
                gyldigTom
            );
        }
    }

    private void validateOrgtilknytningForVikafossen(OppdaterOrgtilknytningerForRessursRequest oppdaterOrgtilknytningerForRessursRequest, String navident) {
        var ressurs = ressursStateRepository.findByNavident(navident) .orElseThrow(() -> RessursNotFoundException.navident(navident));
        var isVikafossen = ressurs.identType().equals(IdentType.VIKAFOSSEN);

        if (isVikafossen) {
            if (oppdaterOrgtilknytningerForRessursRequest.nyeOrgtilknytninger().stream().anyMatch( nye -> !nye.orgEnhetId().equals(vikafossenOrgEnhetId))) {
                throw InvalidOppdaterOrgtilknytningerForRessursRequestException.vikafossenIdentPaaFeilEnhet();
            }
        } else {
            if (oppdaterOrgtilknytningerForRessursRequest.nyeOrgtilknytninger().stream().anyMatch( nye -> nye.orgEnhetId().equals(vikafossenOrgEnhetId))) {
                throw InvalidOppdaterOrgtilknytningerForRessursRequestException.ikkeVikafossenIdentPaaVikafossenEnhet(ressurs.identType().toString());
            }
        }
    }

    private void endreOrgtilknytning(EndreOrgtilknytningRequest endreOrgtilknytningRequest) {
        var orgtilknytningStateBeforeChanges = orgtilknytningStateRepository.findByFid(endreOrgtilknytningRequest.fid())
            .orElseThrow(() -> OrgtilknytningNotFoundException.fid(endreOrgtilknytningRequest.fid()));

        var inputMetadata = new InputMetadata(
            "REST",
            "oppdaterOrgtilknytningerForRessurs",
            endreOrgtilknytningRequest,
            new Origination(SecurityUtil.getAuthenticatedNavIdent(), SecurityUtil.getAuthenticatedAzpName())

        );

        var hasChangesInGyldighetsperiode = !endreOrgtilknytningRequest.gyldigFom().equals(orgtilknytningStateBeforeChanges.gyldigFom())
            || !Optional.ofNullable(endreOrgtilknytningRequest.gyldigTom()).equals(orgtilknytningStateBeforeChanges.gyldigTom());

        // TODO: Burder ikke oppdatere OrgtilknytningDagligOppfolgingEndretEventV2 med nye gyldigFom og gyldigTom, bare sende med orgtilknytningStateBeforeChanges verdiene
        // TODO: Bør også fjerne else i else if statementet under, for å sende begge eventer hvis både DOL og dato har endret seg.
        if (endreOrgtilknytningRequest.erDagligOppfolging() != orgtilknytningStateBeforeChanges.erDagligOppfolging()) {
            orgtilknytningCommandService.endreDagligOppfolgingOrgtilknytning(
                EndreDagligOppfolgingOrgtilknytningCommand.builder()
                    .fid(endreOrgtilknytningRequest.fid())
                    .navident(orgtilknytningStateBeforeChanges.navident())
                    .orgenhetId(orgtilknytningStateBeforeChanges.orgEnhetId())
                    .gyldigFom(endreOrgtilknytningRequest.gyldigFom())
                    .gyldigTom(Optional.ofNullable(endreOrgtilknytningRequest.gyldigTom()))
                    .erDagligOppfolging(endreOrgtilknytningRequest.erDagligOppfolging())
                    .inputMetadata(inputMetadata)
                    .additionalAgressoData(null)
                    .build()
            );
        } else if (hasChangesInGyldighetsperiode) {
            orgtilknytningCommandService.endreOrgtilknytningDato(EndreOrgtilknytningDatoCommand.builder()
                .fid(endreOrgtilknytningRequest.fid())
                .gyldigFom(endreOrgtilknytningRequest.gyldigFom())
                .gyldigTom(Optional.ofNullable(endreOrgtilknytningRequest.gyldigTom()))
                .inputMetadata(inputMetadata)
                .build()
            );
        }

        if (orgtilknytningStateBeforeChanges.orgtilknytningType().isPresent()
            && !isNull(endreOrgtilknytningRequest.sektor())
            && !endreOrgtilknytningRequest.sektor().equals(orgtilknytningStateBeforeChanges.sektor())
            && !endreOrgtilknytningRequest.sektor().equals(Sektor.EKSTERN)) {
            endreOrgtilknytningRequest.toBuilder().tilknytningtype(Tilknytningtype.INGEN).build();
        }

        if (!Optional.ofNullable(endreOrgtilknytningRequest.tilknytningtype()).equals(orgtilknytningStateBeforeChanges.orgtilknytningType())){
            if (!isNull(endreOrgtilknytningRequest.tilknytningtype())) {
                orgtilknytningCommandService.endreTilknytningtype(
                    EndreTilknytningtypeCommand.builder()
                        .fid(endreOrgtilknytningRequest.fid())
                        .tilknytningtype(Optional.of(endreOrgtilknytningRequest.tilknytningtype()))
                        .inputMetadata(inputMetadata)
                        .build()
                );
            } else {
                log.error("Innsendt tilknytningtype er null, men vi hadde en eksisterende verdi");
            }
        }

        if (!isNull(endreOrgtilknytningRequest.sektor()) && !endreOrgtilknytningRequest.sektor().equals(orgtilknytningStateBeforeChanges.sektor())) {
            orgtilknytningCommandService.endreOrgtilknytningSektor(
                EndreOrgtilknytningSektorCommand.builder()
                    .fid(endreOrgtilknytningRequest.fid())
                    .navident(orgtilknytningStateBeforeChanges.navident())
                    .orgenhetId(orgtilknytningStateBeforeChanges.orgEnhetId())
                    .sektor(endreOrgtilknytningRequest.sektor())
                    .inputMetadata(inputMetadata)
                    .additionalAgressoData(null)
                    .build()
            );
        }

        ressursStateRepository.findByNavident(orgtilknytningStateBeforeChanges.navident()).ifPresent(ressursState -> {
            if (endreOrgtilknytningRequest.erDagligOppfolging()
                && isPeriodActive(endreOrgtilknytningRequest.gyldigFom(), endreOrgtilknytningRequest.gyldigTom())
                && (isNull(ressursState.sektor()) || !ressursState.sektor().equals(endreOrgtilknytningRequest.sektor()))) {
                OppdaterSektorCommand oppdaterSektorCommand = new OppdaterSektorCommand(ressursState.fid(), endreOrgtilknytningRequest.sektor());
                ressursCommandService.oppdaterSektorEvent(oppdaterSektorCommand, inputMetadata);
            }
        });
    }


    // TODO: Det burde det muligens lages et nytt event 'OrgtilknytningOpprettetEventV2' som inneholder felter for erDagligOppfolging og orgtilknytningType
    //  slik at man kun trenger å sende ett i event i sammenheng med opprettelse av en ny orgtilknytning
    private void leggTilOrgtilknytningForRessurs(String navident, LeggTilOrgtilknytningRequest leggTilOrgtilknytningRequest) {

        var inputMetadata = new InputMetadata(
            "REST",
            "oppdaterOrgtilknytningerForRessurs",
            leggTilOrgtilknytningRequest,
            new Origination(SecurityUtil.getAuthenticatedNavIdent(), SecurityUtil.getAuthenticatedAzpName())

        );

        if (isNull(leggTilOrgtilknytningRequest.sektor())) throw InvalidSektorException.sektorIsNull();

        var opprettetOrgtilknytningFid = orgtilknytningCommandService.opprettOrgtilknytning(
            OpprettOrgtilknytning.builder()
                .navident(navident)
                .orgenhetId(leggTilOrgtilknytningRequest.orgEnhetId())
                .sektor(leggTilOrgtilknytningRequest.sektor())
                .gyldigFom(leggTilOrgtilknytningRequest.gyldigFom())
                .inputMetadata(inputMetadata)
                .additionalAgressoData(null)
                .build()
        );

        if (leggTilOrgtilknytningRequest.erDagligOppfolging()) {
            orgtilknytningCommandService.endreDagligOppfolgingOrgtilknytning(
                EndreDagligOppfolgingOrgtilknytningCommand.builder()
                    .fid(opprettetOrgtilknytningFid)
                    .navident(navident)
                    .orgenhetId(leggTilOrgtilknytningRequest.orgEnhetId())
                    .gyldigFom(leggTilOrgtilknytningRequest.gyldigFom())
                    .gyldigTom(Optional.ofNullable(leggTilOrgtilknytningRequest.gyldigTom()))
                    .erDagligOppfolging(true)
                    .inputMetadata(inputMetadata)
                    .additionalAgressoData(null)
                    .build()
            );

            ressursStateRepository.findByNavident(navident).ifPresent(ressursState -> {
                if (isPeriodActive(leggTilOrgtilknytningRequest.gyldigFom(), leggTilOrgtilknytningRequest.gyldigTom())
                    && (isNull(ressursState.sektor()) || !ressursState.sektor().equals(leggTilOrgtilknytningRequest.sektor()))) {
                    OppdaterSektorCommand oppdaterSektorCommand = new OppdaterSektorCommand(ressursState.fid(), leggTilOrgtilknytningRequest.sektor());
                    ressursCommandService.oppdaterSektorEvent(oppdaterSektorCommand, inputMetadata);
                }
            });
        } else {
            orgtilknytningCommandService.endreOrgtilknytningDato(EndreOrgtilknytningDatoCommand.builder()
                .fid(opprettetOrgtilknytningFid)
                .gyldigFom(leggTilOrgtilknytningRequest.gyldigFom())
                .gyldigTom(Optional.ofNullable(leggTilOrgtilknytningRequest.gyldigTom()))
                .inputMetadata(inputMetadata)
                .build()
            );
        }

        ressursStateRepository.findByNavident(navident).ifPresent(ressursState -> {
            if (ressursState.sektor().erKommunal()) {
                orgtilknytningCommandService.endreTilknytningtype(
                    EndreTilknytningtypeCommand.builder()
                        .fid(opprettetOrgtilknytningFid)
                        .tilknytningtype(Optional.of(Tilknytningtype.INGEN))
                        .inputMetadata(inputMetadata)
                        .build()
                );
            }
        });

        if (Optional.ofNullable(leggTilOrgtilknytningRequest.tilknytningtype()).isPresent()) {
            orgtilknytningCommandService.endreTilknytningtype(
                EndreTilknytningtypeCommand.builder()
                    .fid(opprettetOrgtilknytningFid)
                    .tilknytningtype(Optional.of(leggTilOrgtilknytningRequest.tilknytningtype()))
                    .inputMetadata(inputMetadata)
                    .build()
            );
        }
    }

    private void oppdaterRessursSluttdato(
        RessursState tilknyttetRessurs, OppdaterOrgtilknytningerForRessursRequest oppdaterOrgtilknytningerForRessursRequest
    ) {
        // Samler alle gyldigTom i endredeOrgtilknytninger g nyeOrgtilknytninger og finner max verdi. null, dvs ingen sluttdato, ansees som høyeste verdi.
        var newSluttdato = Stream.of(
                oppdaterOrgtilknytningerForRessursRequest.eksisterendeOrgtilknytninger().stream()
                    .map(EndreOrgtilknytningRequest::gyldigTom),
                oppdaterOrgtilknytningerForRessursRequest.nyeOrgtilknytninger().stream()
                    .map(LeggTilOrgtilknytningRequest::gyldigTom)
            )
            .flatMap(Function.identity())
            .map(gyldigTom -> Optional.ofNullable(gyldigTom).orElse(LocalDate.MAX))
            .max(Comparator.naturalOrder());

        if (!newSluttdato.equals(tilknyttetRessurs.sluttdato())) {
            if (newSluttdato.isPresent() &&  newSluttdato.get().equals(LocalDate.MAX)) {
                newSluttdato = Optional.empty();
            }
            ressursCommandService.oppdaterRessursSluttdatoEvent(
                OppdaterSluttdatoCommand.builder()
                    .fid(tilknyttetRessurs.fid())
                    .sluttdato(newSluttdato)
                    .inputMetadata(
                        getInputMetadataOppdaterOrgtilknytningerForRessurs(oppdaterOrgtilknytningerForRessursRequest)
                    )
                    .build()
            );
        }
    }

    private static InputMetadata getInputMetadataOppdaterOrgtilknytningerForRessurs(OppdaterOrgtilknytningerForRessursRequest oppdaterOrgtilknytningerForRessursRequest) {
        return new InputMetadata(
            "REST",
            "oppdaterOrgtilknytningerForRessurs",
            oppdaterOrgtilknytningerForRessursRequest,
            new Origination(SecurityUtil.getAuthenticatedNavIdent(), SecurityUtil.getAuthenticatedAzpName())

        );
    }

    private boolean endreOrgtilknytningRequestProvidesChangesToState(
        EndreOrgtilknytningRequest endreOrgtilknytningRequest, OrgtilknytningState currentOrgtilknytningState
    ) {
        validateSektorCanBeChanged(endreOrgtilknytningRequest, currentOrgtilknytningState);
        validateTilknytningCanBeChanged(endreOrgtilknytningRequest, currentOrgtilknytningState);

        return !(
            endreOrgtilknytningRequest.gyldigFom().equals(currentOrgtilknytningState.gyldigFom())
                && Optional.ofNullable(endreOrgtilknytningRequest.gyldigTom()).equals(currentOrgtilknytningState.gyldigTom())
                && endreOrgtilknytningRequest.erDagligOppfolging() == currentOrgtilknytningState.erDagligOppfolging()
                && Optional.ofNullable(endreOrgtilknytningRequest.tilknytningtype()).equals(currentOrgtilknytningState.orgtilknytningType())
                && (endreOrgtilknytningRequest.sektor() != null && endreOrgtilknytningRequest.sektor().equals(currentOrgtilknytningState.sektor()))
        );
    }

    private static void validateSektorCanBeChanged(EndreOrgtilknytningRequest endreOrgtilknytningRequest, OrgtilknytningState currentOrgtilknytningState) {
        if (!loggedInUserHasAccessScope(NomApiAccessScope.ADMIN)
            && isNotNull(endreOrgtilknytningRequest.sektor())
            && (!endreOrgtilknytningRequest.sektor().equals(currentOrgtilknytningState.sektor()) && isNotNull(currentOrgtilknytningState.sektor()))) {
            throw InvalidOrgtilknytningSektorException.sektorCannotBeChanged();
        }
    }

    private static void validateTilknytningCanBeChanged(EndreOrgtilknytningRequest endreOrgtilknytningRequest, OrgtilknytningState currentOrgtilknytningState) {
        var currentTilknytningType = setCorrectTilknytningtype(currentOrgtilknytningState);
        if (!loggedInUserHasAccessScope(NomApiAccessScope.ADMIN)
            && isNotNull(endreOrgtilknytningRequest.tilknytningtype())
            && (currentTilknytningType.isPresent() && !endreOrgtilknytningRequest.tilknytningtype().equals(currentTilknytningType.get()))) {
            throw InvalidOrgtilknytningTilknytningtypeException.tilknytningtypeCannotBeChanged();
        }
    }

    private static Optional<Tilknytningtype> setCorrectTilknytningtype(OrgtilknytningState currentOrgtilknytningState) {

        if (currentOrgtilknytningState.orgtilknytningType().isPresent()
            && currentOrgtilknytningState.orgtilknytningType().get().equals(Tilknytningtype.INGEN)
            && (isNull(currentOrgtilknytningState.sektor()) || !currentOrgtilknytningState.sektor().erKommunal())) {
            return Optional.empty();
        }
        return currentOrgtilknytningState.orgtilknytningType();
    }

    private void validateOrgTilknytningerToUpdateHasNoDuplicateFidsAndIncludesAllExistingOrgtilknytningerForRessurs(
        Set<String> existingOrgtilknytningFidsForRessurs,
        List<EndreOrgtilknytningRequest> orgTilknytningerToUpdate
    ) {

        var distinctOrgTilknytningFidsToUpdate = new HashSet<String>();

        var duplicateorgTilknytningFidsToUpdate = orgTilknytningerToUpdate.stream()
            .map(EndreOrgtilknytningRequest::fid)
            .filter(fid -> !distinctOrgTilknytningFidsToUpdate.add(fid))
            .collect(Collectors.toSet());

        if (!duplicateorgTilknytningFidsToUpdate.isEmpty()) {
            throw InvalidOppdaterOrgtilknytningerForRessursRequestException.duplicateOrgtilknytningFids(
                distinctOrgTilknytningFidsToUpdate
            );
        }

        var orgtilknytningerNotInRessurs = distinctOrgTilknytningFidsToUpdate.stream().filter(
            fid -> !existingOrgtilknytningFidsForRessurs.contains(fid)
        ).toList();

        if (!orgtilknytningerNotInRessurs.isEmpty()) {
            throw InvalidOppdaterOrgtilknytningerForRessursRequestException
                .orgtilknytningerNotInRessurs(orgtilknytningerNotInRessurs);
        }

        if (!distinctOrgTilknytningFidsToUpdate.equals(existingOrgtilknytningFidsForRessurs)) {
            throw InvalidOppdaterOrgtilknytningerForRessursRequestException.missingOrgtilknytningFids(
                existingOrgtilknytningFidsForRessurs.stream()
                    .filter(fid -> !distinctOrgTilknytningFidsToUpdate.contains(fid)).toList()
            );
        }
    }

    public void updateOrgtilknytningType(String fid, EndreTilknytningtypeRequest endreTilknytningtypeRequest) {

        var orgtilknytning = orgtilknytningStateRepository.findByFid(fid)
            .orElseThrow(() -> OrgtilknytningNotFoundException.fid(fid));

        var tilknyttetRessurs = ressursStateRepository.findByNavident(orgtilknytning.navident())
            .orElseThrow(() -> RessursNotFoundException.navident(orgtilknytning.navident()));

        endreTilknytningtypeRequest.tilknytningtype().ifPresent(
            orgtilknytningType -> validateOrgtilknytningTypeSektorCombination(orgtilknytningType, tilknyttetRessurs.sektor())
        );

        orgtilknytningCommandService.endreTilknytningtype(
            new EndreTilknytningtypeCommand(
                orgtilknytning.fid(),
                endreTilknytningtypeRequest.tilknytningtype(),
                new InputMetadata(
                    "REST",
                    "updateOrgtilknytningType",
                    Map.of(
                        "navident", orgtilknytning.navident(),
                        "orgEnhetId", orgtilknytning.orgEnhetId()
                    ),
                    new Origination(SecurityUtil.getAuthenticatedNavIdent(), SecurityUtil.getAuthenticatedAzpName())
                )
            )
        );

        nomMastered.markRessursAsMasteredInNom(orgtilknytning.navident());
    }

    public MasseFlyttingResponse masseflyttingAvRessurser(MasseFlyttingRequest request) {
        var origination = new Origination(SecurityUtil.getAuthenticatedNavIdent(), SecurityUtil.getAuthenticatedAzpName());

        var inputMetadata = new InputMetadata(
            "REST",
            "masseflytting",
            request,
            origination
        );

        var res = masseflyttOrgtilknytningerService.flyttOrgtilknytninger(request.orgtilknytningerFid(), request.nyOrgenhetId(), request.beholdTilganger(), request.gyldigFomFlyttedato(), inputMetadata, false);

        return new MasseFlyttingResponse(
            res.entrySet().stream().map(e -> new MasseFlyttingResponse.FeiletFlytting(e.getKey().fid(), e.getValue())).toList(),
            request.orgtilknytningerFid().size() - res.size()
        );
    }

    public static void validateOrgtilknytningTypeSektorCombination(Tilknytningtype tilknytningtype, Sektor sektor) {

        Map<Sektor, List<Tilknytningtype>> allowedOrgtilknytningTypeForSektorMap = Map.of(
            Sektor.EKSTERN, List.of(Tilknytningtype.KONSULENT, Tilknytningtype.EKSTERN_SAMARBEIDSPARTNER, Tilknytningtype.ARBEIDSTRENING, Tilknytningtype.ANNET, Tilknytningtype.STUDENT),
            Sektor.NAV_KOMMUNAL, List.of(Tilknytningtype.INGEN),
            Sektor.NAV_STATLIG, List.of(Tilknytningtype.FAST, Tilknytningtype.MIDLERTIDIG, Tilknytningtype.PENSJONIST, Tilknytningtype.LAERLING)
        );

        if (!allowedOrgtilknytningTypeForSektorMap.get(sektor).contains(tilknytningtype)) {
            throw InvalidOrgtilknytningTypeAndSektorCombinationException.forCombination(tilknytningtype, sektor);
        }

    }

    public boolean kanEndreOrgtilknytning(IdentType identType, String navident) {
        return switch (identType) {
            case ROBOT -> SecurityUtil.loggedInUserHasAnyAccessScope(NOM_WRITE_ROBOT, ADMIN) || authResolver.kanRedigereRessurs(navident);
            case UTENLANDSK_KONSULENT -> SecurityUtil.loggedInUserHasAnyAccessScope(NOM_WRITE_UTL_KONSULENT, ADMIN) || authResolver.kanRedigereRessurs(navident);
            case VIKAFOSSEN -> SecurityUtil.loggedInUserHasAnyAccessScope(NOM_WRITE_VIKAFOSSEN, ADMIN); // NOM admin skal fratas denne rollen etterhvert
            default -> authResolver.kanRedigereRessurs(navident);
        };
    }

    public boolean kanEndreOrgtilknytninger(IdentType identType, String navident, OppdaterOrgtilknytningerForRessursRequest oppdaterOrgtilknytningerForRessursRequest) {
        if (unleash.isEnabled(BRUK_NY_TILGANGSVALIDERING)) {
            var orgenhetIdsUnderEndring = orgEnhetIderForOterUnderEndring(oppdaterOrgtilknytningerForRessursRequest);
            return switch (identType) {
                case ROBOT ->
                    SecurityUtil.loggedInUserHasAnyAccessScope(NOM_WRITE_ROBOT,ADMIN)
                    || authResolver.kanRedigereRessurs(navident)
                    || authResolver.kanRedigereNoenAvOrgenhetene(orgenhetIdsUnderEndring);
                case UTENLANDSK_KONSULENT ->
                    SecurityUtil.loggedInUserHasAnyAccessScope(NOM_WRITE_UTL_KONSULENT,ADMIN)
                    || authResolver.kanRedigereRessurs(navident)
                    || authResolver.kanRedigereNoenAvOrgenhetene(orgenhetIdsUnderEndring);
                case VIKAFOSSEN ->
                    SecurityUtil.loggedInUserHasAnyAccessScope(NOM_WRITE_VIKAFOSSEN, ADMIN); // NOM admin skal fratas denne rollen etterhvert
                default ->
                    authResolver.kanRedigereRessurs(navident)
                    || authResolver.kanRedigereNoenAvOrgenhetene(orgenhetIdsUnderEndring);
            };
        }
        return true;
    }

    private Set<String> orgEnhetIderForOterUnderEndring(OppdaterOrgtilknytningerForRessursRequest oppdaterOrgtilknytningerForRessursRequest) {
        var enheterTilOTerUnderEndring = new HashSet<String>();
        oppdaterOrgtilknytningerForRessursRequest.eksisterendeOrgtilknytninger().forEach(endreOrgtilknytningRequest -> {
            var otFid = endreOrgtilknytningRequest.fid();
            var ot = orgtilknytningStateRepository.findByFid(otFid).orElseThrow(() -> new OrgtilknytningNotFoundException(otFid));
            enheterTilOTerUnderEndring.add(ot.orgEnhetId());
        });
        oppdaterOrgtilknytningerForRessursRequest.nyeOrgtilknytninger().forEach(leggTilOrgtilknytningRequest ->
            enheterTilOTerUnderEndring.add(leggTilOrgtilknytningRequest.orgEnhetId())
        );

        return enheterTilOTerUnderEndring;
    }

    private boolean isActiveOrFuture(LocalDate gyldigFom, LocalDate gyldigTom) {
        return isActive(gyldigFom, gyldigTom) || isFuture(gyldigFom);
    }

    private boolean isFuture(LocalDate gyldigFom) {
        return gyldigFom.isAfter(LocalDate.now());
    }

    private boolean isActive(LocalDate gyldigFom, LocalDate gyldigTom) {
        return isPeriodActive(gyldigFom, gyldigTom);
    }

    private static boolean isNotNull(Object object) {
        return !isNull(object);
    }
}
