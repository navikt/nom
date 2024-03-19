package no.nav.nom.apps.api.orgtilknytning.api.app;

import no.nav.nom.apps.api.common.DateUtils;
import no.nav.nom.apps.api.graphql.common.SecurityUtil;
import no.nav.nom.apps.api.orgenhet.eventsourcing.state.OrgEnhetStateRepository;
import no.nav.nom.apps.api.orgtilknytning.api.exceptions.InvalidOppdaterOrgtilknytningerForRessursRequestException;
import no.nav.nom.apps.api.orgtilknytning.api.exceptions.InvalidOrgtilknytningStartDateWhenNewRemedyEnhetException;
import no.nav.nom.apps.api.orgtilknytning.api.exceptions.InvalidOrgtilknytningTypeAndSektorCombinationException;
import no.nav.nom.apps.api.orgtilknytning.api.exceptions.OrgtilknytningPeriodeOverlapException;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.OrgtilknytningCommandService;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.AvvikleOrgtilknytning;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.EndreDagligOppfolgingOrgtilknytning;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.EndreDagligOppfolgingOrgtilknytningCommand;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.EndreOrgtilknytningDato;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.EndreOrgtilknytningDatoCommand;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.EndreTilknytningtypeCommand;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.OpprettOrgtilknytning;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.state.OrgtilknytningStateRepository;
import no.nav.nom.apps.api.orgtilknytning.api.exceptions.OrgtilknytningNotFoundException;
import no.nav.nom.apps.api.remedy.RemedyEnhet;
import no.nav.nom.apps.api.remedy.RemedyEnhetMapperService;
import no.nav.nom.apps.api.ressurs.api.exceptions.InvalidSektorException;
import no.nav.nom.apps.api.ressurs.api.exceptions.RessursNotFoundException;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.RessursCommadService;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OppdaterSluttdatoCommand;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OppdaterStartdatoCommand;
import no.nav.nom.apps.api.ressurs.eventsourcing.state.RessursStateRepository;
import no.nav.nom.apps.api.ressurs.masterdata.NomMastered;
import no.nav.nom.contract.nom.Tilknytningtype;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.Origination;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.contract.nom.state.RessursState;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static no.nav.nom.apps.api.common.DateUtils.isPeriodActive;
import static no.nav.nom.apps.api.common.DateUtils.isPerioderOverlapping;
import static no.nav.nom.apps.api.common.DateUtils.isPeriodsOverlapping;
import static no.nav.nom.apps.api.common.DateUtils.nextWorkingDay;
import static no.nav.nom.apps.api.graphql.common.SecurityUtil.getAuthenticatedNavIdent;

@Service
public class OrgtilknytningRestService {

    private final OrgtilknytningStateRepository orgtilknytningStateRepository;
    private final OrgtilknytningCommandService orgtilknytningCommandService;
    private final RessursCommadService ressursCommadService;
    private final OrgEnhetStateRepository orgEnhetStateRepository;
    private final RessursStateRepository ressursStateRepository;
    private final RemedyEnhetMapperService remedyEnhetMapperService;
    private final NomMastered nomMastered;

    public OrgtilknytningRestService(
        OrgtilknytningStateRepository orgtilknytningStateRepository,
        OrgtilknytningCommandService orgtilknytningCommandService,
        RessursCommadService ressursCommadService, OrgEnhetStateRepository orgEnhetStateRepository,
        RessursStateRepository ressursStateRepository,
        RemedyEnhetMapperService remedyEnhetMapperService,
        NomMastered nomMastered
    ) {
        this.orgtilknytningStateRepository = orgtilknytningStateRepository;
        this.orgtilknytningCommandService = orgtilknytningCommandService;
        this.ressursCommadService = ressursCommadService;
        this.orgEnhetStateRepository = orgEnhetStateRepository;
        this.ressursStateRepository = ressursStateRepository;
        this.remedyEnhetMapperService = remedyEnhetMapperService;
        this.nomMastered = nomMastered;
    }

    public List<OrgtilknytningState> getOrgtilknytninger(Optional<String> navident) {
        return navident.map(
                presentNavident -> orgtilknytningStateRepository.findByNavident(presentNavident).stream().toList()
            )
            .orElseGet(
                () -> orgtilknytningStateRepository.getAll().stream().toList()
            );
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

        validateOppdaterOrgtilknytningerForRessursRequest(oppdaterOrgtilknytningerForRessursRequest);

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

        if( oppdaterOrgtilknytningerForRessursRequest.setRessursStartdato()) {
            oppdaterRessursStartdato(
                tilknyttetRessurs,
                oppdaterOrgtilknytningerForRessursRequest
            );
        }
    }

    private void oppdaterRessursStartdato(RessursState tilknyttetRessurs, OppdaterOrgtilknytningerForRessursRequest request) {

        var endredeFomDatoer = request.eksisterendeOrgtilknytninger().stream()
            .map(EndreOrgtilknytningRequest::gyldigFom);
        var nyeFomDatoer = request.nyeOrgtilknytninger().stream()
            .map(LeggTilOrgtilknytningRequest::gyldigFom);

        var nyMinimumStartdato = Stream.concat(endredeFomDatoer, nyeFomDatoer).reduce(LocalDate.MAX, (it, acc) -> it.isBefore(acc) ? it : acc);
        if(nyMinimumStartdato != LocalDate.MAX && !nyMinimumStartdato.equals(tilknyttetRessurs.startdato().orElse(null))){
            ressursCommadService.oppdaterRessursStartdatoEvent(
                new OppdaterStartdatoCommand(tilknyttetRessurs.fid(), nyMinimumStartdato),
                getInputMetadataOppdaterOrgtilknytningerForRessurs(request)
            );
        }
    }

    private void validateOppdaterOrgtilknytningerForRessursRequest(OppdaterOrgtilknytningerForRessursRequest oppdaterOrgtilknytningerForRessursRequest) {
        validateChronologicalDates(oppdaterOrgtilknytningerForRessursRequest);
        validateNoOverlappingDagligeOppfolginger(oppdaterOrgtilknytningerForRessursRequest);
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

        // TODO: Burde innføre et nytt event OrgtilknytningDagligOppfolgingEndretEventV2 som kun setter feltene erDagligOppfolging, gyldigFom og gyldigTom
        //  slik at man ikke ved et uhell endrer navident eller orgenhet for orgtilknytningen.
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

        if (!Optional.ofNullable(endreOrgtilknytningRequest.tilknytningtype()).equals(orgtilknytningStateBeforeChanges.orgtilknytningType())) {
            orgtilknytningCommandService.endreTilknytningtype(
                EndreTilknytningtypeCommand.builder()
                    .fid(endreOrgtilknytningRequest.fid())
                    .tilknytningtype(Optional.ofNullable(endreOrgtilknytningRequest.tilknytningtype()))
                    .inputMetadata(inputMetadata)
                    .build()
            );
        }

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

        var opprettetOrgtilknytningFid = orgtilknytningCommandService.opprettOrgtilknytning(
            OpprettOrgtilknytning.builder()
                .navident(navident)
                .orgenhetId(leggTilOrgtilknytningRequest.orgEnhetId())
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
        } else {
            orgtilknytningCommandService.endreOrgtilknytningDato(EndreOrgtilknytningDatoCommand.builder()
                .fid(opprettetOrgtilknytningFid)
                .gyldigFom(leggTilOrgtilknytningRequest.gyldigFom())
                .gyldigTom(Optional.ofNullable(leggTilOrgtilknytningRequest.gyldigTom()))
                .inputMetadata(inputMetadata)
                .build()
            );
        }

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
            .max(Comparator.comparing(
                gyldigTom -> Optional.ofNullable(gyldigTom).orElse(LocalDate.MAX))
            );

        if (!newSluttdato.equals(tilknyttetRessurs.sluttdato())) {
            ressursCommadService.oppdaterRessursSluttdatoEvent(
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
        return !(
            endreOrgtilknytningRequest.gyldigFom().equals(currentOrgtilknytningState.gyldigFom())
                && Optional.ofNullable(endreOrgtilknytningRequest.gyldigTom()).equals(currentOrgtilknytningState.gyldigTom())
                && endreOrgtilknytningRequest.erDagligOppfolging() == currentOrgtilknytningState.erDagligOppfolging()
                && Optional.ofNullable(endreOrgtilknytningRequest.tilknytningtype()).equals(currentOrgtilknytningState.orgtilknytningType())
        );

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


    public void updateOrgtilknytingDato(String fid, EndreOrgtilknytningDatoRequest endreOrgtilknytningDatoRequest) {

        var orgtilknytning = orgtilknytningStateRepository.findByFid(fid)
            .orElseThrow(() -> OrgtilknytningNotFoundException.fid(fid));

        validateEndreOrgtilknytningDato(orgtilknytning, endreOrgtilknytningDatoRequest);

        var tilknyttetRessurs = ressursStateRepository.findByNavident(orgtilknytning.navident())
            .orElseThrow(() -> RessursNotFoundException.navident(orgtilknytning.navident()));


        if (!tilknyttetRessurs.sektor().equals(Sektor.EKSTERN)) {
            throw InvalidSektorException.actionNotAllowedForSektor(tilknyttetRessurs.sektor());
        }

        validateEksternOrgtilknytningPeriode(endreOrgtilknytningDatoRequest.gyldigTom());

        // If gyldigTom is set to before today, verify there are other active tilknytninger
        var isGyldigTomInThePast = LocalDate.now().isAfter(endreOrgtilknytningDatoRequest.gyldigTom().orElse(LocalDate.MIN));
        var setRessursSluttdatoIsEnabled = endreOrgtilknytningDatoRequest.setRessursSluttdato().orElse(false);

        if (isGyldigTomInThePast && !setRessursSluttdatoIsEnabled) {
            validateThatThereAreOtherActiveOrgtilknytninger(orgtilknytning);
        }

        var origination = new Origination(SecurityUtil.getAuthenticatedNavIdent(), SecurityUtil.getAuthenticatedAzpName());

        orgtilknytningCommandService.endreOrgtilknytningDato(
            fid,
            new EndreOrgtilknytningDato(
                orgtilknytning.navident(),
                orgtilknytning.orgEnhetId(),
                endreOrgtilknytningDatoRequest.gyldigFom(),
                endreOrgtilknytningDatoRequest.gyldigTom(),
                new InputMetadata(
                    "REST",
                    "updateOrgtilknytingDato",
                    Map.of(
                        "navident", orgtilknytning.navident(),
                        "orgEnhetId", orgtilknytning.orgEnhetId()
                    ),
                    origination
                ),
                null
            )
        );

        if (setRessursSluttdatoIsEnabled) {
            var maxGyldigTomOnExistingOrgtilknytningerIsBeforeNewGyldigTom = orgtilknytningStateRepository.findByNavident(orgtilknytning.navident()).stream()
                .filter(orgtilknytningState -> !orgtilknytningState.fid().equals(orgtilknytning.fid()))
                .map(orgtilknytningState -> orgtilknytningState.gyldigTom().orElse(LocalDate.MAX))
                .max(LocalDate::compareTo)
                .map(maxGyldigTom -> maxGyldigTom.isBefore(endreOrgtilknytningDatoRequest.gyldigTom().orElse(LocalDate.MAX)))
                .orElse(true);

            var tilknyttetRessursSluttdatoNotEqualToNewGyldigTom = !tilknyttetRessurs.sluttdato().equals(endreOrgtilknytningDatoRequest.gyldigTom());

            if (maxGyldigTomOnExistingOrgtilknytningerIsBeforeNewGyldigTom && tilknyttetRessursSluttdatoNotEqualToNewGyldigTom) {

                ressursCommadService.oppdaterRessursSluttdatoEvent(
                    new OppdaterSluttdatoCommand(
                        tilknyttetRessurs.fid(),
                        endreOrgtilknytningDatoRequest.gyldigTom(),
                        new InputMetadata(
                            "REST",
                            "updateOrgtilknytingDato",
                            endreOrgtilknytningDatoRequest,
                            origination
                        )
                    )
                );
            }
        }

        nomMastered.markRessursAsMasteredInNom(orgtilknytning.navident());
    }

    public void updateOrgtilknytningType(String fid, EndreTilknytningtypeRequest endreTilknytningtypeRequest) {

        var orgtilknytning = orgtilknytningStateRepository.findByFid(fid)
            .orElseThrow(() -> OrgtilknytningNotFoundException.fid(fid));

        var tilknyttetRessurs = ressursStateRepository.findByNavident(orgtilknytning.navident())
            .orElseThrow(() -> RessursNotFoundException.navident(orgtilknytning.navident()));

        if (tilknyttetRessurs.sektor() != Sektor.EKSTERN) {
            throw InvalidSektorException.actionNotAllowedForSektor(tilknyttetRessurs.sektor());
        }

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

    public static void validateOrgtilknytningTypeSektorCombination(Tilknytningtype tilknytningtype, Sektor sektor) {

        Map<Sektor, List<Tilknytningtype>> allowedOrgtilknytningTypeForSektorMap = Map.of(
            Sektor.EKSTERN, List.of(Tilknytningtype.KONSULENT, Tilknytningtype.EKSTERN_SAMARBEIDSPARTNER),
            Sektor.NAV_KOMMUNAL, emptyList(),
            Sektor.NAV_STATLIG, emptyList()
        );

        if (!allowedOrgtilknytningTypeForSektorMap.get(sektor).contains(tilknytningtype)) {
            throw InvalidOrgtilknytningTypeAndSektorCombinationException.forCombination(tilknytningtype, sektor);
        }

    }

    public void addOrgtilknyting(LeggTilOrgtilknytningRequestOld leggTilOrgtilknytningRequest) {
        validateLeggTilOrgilknytningRequest(leggTilOrgtilknytningRequest);

        var erDagligOppfolging = leggTilOrgtilknytningRequest.erDagligOppfolging().orElse(false);

        if (erDagligOppfolging) {
            validateLeggTilDagligOppfolgingOrgilknytningRequest(leggTilOrgtilknytningRequest);
        }

        var metaData = new InputMetadata(
            "REST",
            "addOrgtilknyting",
            leggTilOrgtilknytningRequest,
            new Origination(
                getAuthenticatedNavIdent(),
                SecurityUtil.getAuthenticatedAzpName()
            )
        );

        var opprettetOrgTilknytningFid = orgtilknytningCommandService.opprettOrgtilknytning(
            new OpprettOrgtilknytning(
                leggTilOrgtilknytningRequest.navident(),
                leggTilOrgtilknytningRequest.orgEnhetId(),
                leggTilOrgtilknytningRequest.gyldigFom(),
                metaData,
                null
            )
        );

        leggTilOrgtilknytningRequest.gyldigTom().ifPresent(gyldigTom -> orgtilknytningCommandService.avvikleOrgtilknytning(
            opprettetOrgTilknytningFid,
            new AvvikleOrgtilknytning(
                leggTilOrgtilknytningRequest.navident(),
                leggTilOrgtilknytningRequest.orgEnhetId(),
                gyldigTom,
                metaData,
                null
            )
        ));

        if (erDagligOppfolging) {
            orgtilknytningCommandService.endreDagligOppfolgingOrgtilknytning(
                opprettetOrgTilknytningFid,
                new EndreDagligOppfolgingOrgtilknytning(
                    leggTilOrgtilknytningRequest.navident(),
                    leggTilOrgtilknytningRequest.orgEnhetId(),
                    leggTilOrgtilknytningRequest.gyldigFom(),
                    leggTilOrgtilknytningRequest.gyldigTom(),
                    erDagligOppfolging,
                    metaData,
                    null
                )
            );
        }

        if (leggTilOrgtilknytningRequest.setRessursSluttdato().orElse(false)) {
            var maxGyldigTomOnExistingOrgtilknytningerIsBeforeNewGyldigTom = orgtilknytningStateRepository
                .findByNavident(leggTilOrgtilknytningRequest.navident()).stream()
                .filter(orgtilknytningState -> !orgtilknytningState.fid().equals(opprettetOrgTilknytningFid))
                .map(orgtilknytningState -> orgtilknytningState.gyldigTom().orElse(LocalDate.MAX))
                .max(LocalDate::compareTo)
                .map(maxGyldigTom -> maxGyldigTom.isBefore(leggTilOrgtilknytningRequest.gyldigTom().orElse(LocalDate.MAX)))
                .orElse(true);

            var tilknyttetRessurs = ressursStateRepository.findByNavident(leggTilOrgtilknytningRequest.navident()).orElseThrow(
                () -> RessursNotFoundException.navident(leggTilOrgtilknytningRequest.navident())
            );

            var tilknyttetRessursSluttdatoNotEqualToNewGyldigTom = !tilknyttetRessurs.sluttdato().equals(leggTilOrgtilknytningRequest.gyldigTom());

            if (maxGyldigTomOnExistingOrgtilknytningerIsBeforeNewGyldigTom && tilknyttetRessursSluttdatoNotEqualToNewGyldigTom) {

                ressursCommadService.oppdaterRessursSluttdatoEvent(
                    new OppdaterSluttdatoCommand(
                        tilknyttetRessurs.fid(),
                        leggTilOrgtilknytningRequest.gyldigTom(),
                        metaData
                    )
                );
            }
        }

        nomMastered.markRessursAsMasteredInNom(leggTilOrgtilknytningRequest.navident());
    }

    private void validateLeggTilOrgilknytningRequest(LeggTilOrgtilknytningRequestOld leggTilOrgtilknytningRequest) {
        var ressursState = ressursStateRepository.findByNavident(leggTilOrgtilknytningRequest.navident()).orElseThrow(
            () -> new IllegalArgumentException("Finner ikke ressurs med navident %s".formatted(leggTilOrgtilknytningRequest.navident()))
        );

        if (ressursState.sektor() == Sektor.EKSTERN) {
            validateEksternOrgtilknytningPeriode(leggTilOrgtilknytningRequest.gyldigTom());
        }

        if (orgEnhetStateRepository.findById(leggTilOrgtilknytningRequest.orgEnhetId()).isEmpty()) {
            throw new IllegalArgumentException("Finner ikke orgenhet med id %s".formatted(leggTilOrgtilknytningRequest.orgEnhetId()));
        }

        DateUtils.validateChronologicalDates(leggTilOrgtilknytningRequest.gyldigFom(), leggTilOrgtilknytningRequest.gyldigTom().orElse(null));

        var existingOegtilknytningerForOrgEnhet = orgtilknytningStateRepository.findByNavident(leggTilOrgtilknytningRequest.navident())
            .stream()
            .filter(orgtilknytningState -> orgtilknytningState.orgEnhetId().equals(leggTilOrgtilknytningRequest.orgEnhetId()))
            .toList();

        existingOegtilknytningerForOrgEnhet.forEach(orgtilknytningState -> {
            if (isPeriodsOverlapping(
                orgtilknytningState.gyldigFom(),
                orgtilknytningState.gyldigTom(),
                leggTilOrgtilknytningRequest.gyldigFom(),
                leggTilOrgtilknytningRequest.gyldigTom()
            )) {
                throw new OrgtilknytningPeriodeOverlapException("Det finnes allerede en orgtilknytning for samme orgenhet innenfor forespurt periode");

            }
        });
    }

    private void validateLeggTilDagligOppfolgingOrgilknytningRequest(LeggTilOrgtilknytningRequestOld leggTilOrgtilknytningRequest) {
        var existingDagligOppfolgningTilknytninger = orgtilknytningStateRepository.findByNavident(leggTilOrgtilknytningRequest.navident())
            .stream()
            .filter(OrgtilknytningState::erDagligOppfolging)
            .toList();

        // https://jira.adeo.no/browse/ORG-1759
        existingDagligOppfolgningTilknytninger.stream()
            .filter(orgtilknytningState -> isPeriodActive(orgtilknytningState.gyldigFom(), orgtilknytningState.gyldigTom()))
            .findFirst()
            .ifPresent(currentlyActiveDagligOppfolging -> {
                var currentRemedyEnhetId = remedyEnhetMapperService.findByNomId(currentlyActiveDagligOppfolging.orgEnhetId())
                    .map(RemedyEnhet::remedyEnhetId);
                var newRemedyEnhetId = remedyEnhetMapperService.findByNomId(leggTilOrgtilknytningRequest.orgEnhetId())
                    .map(RemedyEnhet::remedyEnhetId);

                var newDagligOppfolgningIsActivatedAfterTomorrow = leggTilOrgtilknytningRequest.gyldigFom()
                    .isAfter(nextWorkingDay());

                if (!currentRemedyEnhetId.equals(newRemedyEnhetId) && newDagligOppfolgningIsActivatedAfterTomorrow) {
                    throw new InvalidOrgtilknytningStartDateWhenNewRemedyEnhetException(
                        "Siden den nye daglig oppfolging orgtilknytningen vil endre den ansattes remedy enhet kan 'gyldigFom' være maks én virkedag frem i tid"
                    );
                }
            });


        existingDagligOppfolgningTilknytninger.forEach(orgtilknytningState -> {
            if (isPeriodsOverlapping(
                orgtilknytningState.gyldigFom(),
                orgtilknytningState.gyldigTom(),
                leggTilOrgtilknytningRequest.gyldigFom(),
                leggTilOrgtilknytningRequest.gyldigTom()
            )) {
                throw OrgtilknytningPeriodeOverlapException.forDagligOppfolgingInPeriod(
                    orgEnhetStateRepository.findById(orgtilknytningState.orgEnhetId()).map(OrgEnhetState::navn).orElse(orgtilknytningState.orgEnhetId()),
                    orgtilknytningState.gyldigFom(),
                    orgtilknytningState.gyldigTom()
                );
            }
        });

    }

    private void validateEndreOrgtilknytningDato(
        OrgtilknytningState existingOrgtilknytningToUpdate,
        EndreOrgtilknytningDatoRequest endreOrgtilknytningDatoRequest
    ) {
        DateUtils.validateChronologicalDates(endreOrgtilknytningDatoRequest.gyldigFom(), endreOrgtilknytningDatoRequest.gyldigTom().orElse(null));

        var ressursState = ressursStateRepository.findByNavident(existingOrgtilknytningToUpdate.navident()).orElseThrow(
            () -> new IllegalArgumentException("Finner ikke ressurs med navident %s".formatted(existingOrgtilknytningToUpdate.navident()))
        );

        if (ressursState.sektor() == Sektor.EKSTERN) {
            validateEksternOrgtilknytningPeriode(endreOrgtilknytningDatoRequest.gyldigTom());
        }

        var otherOrgtilknytningerForRessurs = orgtilknytningStateRepository.findByNavident(existingOrgtilknytningToUpdate.navident()).stream()
            .filter(orgtilknytningState -> !orgtilknytningState.fid().equals(existingOrgtilknytningToUpdate.fid()))
            .toList();

        otherOrgtilknytningerForRessurs.stream()
            .filter(orgtilknytningState -> orgtilknytningState.orgEnhetId().equals(existingOrgtilknytningToUpdate.orgEnhetId()))
            .forEach(orgtilknytningState -> {
                if (isPeriodsOverlapping(
                    orgtilknytningState.gyldigFom(),
                    orgtilknytningState.gyldigTom(),
                    endreOrgtilknytningDatoRequest.gyldigFom(),
                    endreOrgtilknytningDatoRequest.gyldigTom()
                )) {
                    throw new OrgtilknytningPeriodeOverlapException("Det finnes allerede en orgtilknytning for samme orgenhet innenfor forespurt periode");
                }
            });

        if (existingOrgtilknytningToUpdate.erDagligOppfolging()) {
            otherOrgtilknytningerForRessurs
                .stream().filter(OrgtilknytningState::erDagligOppfolging)
                .forEach(orgtilknytningState -> {
                    if (isPeriodsOverlapping(
                        orgtilknytningState.gyldigFom(),
                        orgtilknytningState.gyldigTom(),
                        endreOrgtilknytningDatoRequest.gyldigFom(),
                        endreOrgtilknytningDatoRequest.gyldigTom()
                    )) {
                        throw OrgtilknytningPeriodeOverlapException.forDagligOppfolgingInPeriod(
                            orgEnhetStateRepository.findById(orgtilknytningState.orgEnhetId())
                                .map(OrgEnhetState::navn)
                                .orElse(orgtilknytningState.orgEnhetId()),
                            orgtilknytningState.gyldigFom(),
                            orgtilknytningState.gyldigTom()
                        );
                    }
                });
        }
    }

    private void validateThatThereAreOtherActiveOrgtilknytninger(OrgtilknytningState orgtilknytningState) {
        var activeOrgtilknytningExists = getOrgtilknytninger(Optional.ofNullable(orgtilknytningState.navident())).stream()
            .filter(otherOrgtilknytningState -> !otherOrgtilknytningState.fid().equals(orgtilknytningState.fid()))
            .anyMatch(otherOrgtilknytningState -> otherOrgtilknytningState.gyldigTom().orElse(LocalDate.MAX).isAfter(LocalDate.now()));

        if (!activeOrgtilknytningExists) {
            throw new IllegalArgumentException(
                "Du kan ikke avslutte denne tilknytningen før du har lagt til en ny tilknytning  som gjelder fra denne tilknytningens sluttdato. Medarbeideren må til enhver tid være tilknyttet en enhet.");
        }
    }

    private void validateEksternOrgtilknytningPeriode(Optional<LocalDate> gyldigTom) {
        var requestedGyldigTomIsMoreThan1YearFromNow = gyldigTom.orElse(LocalDate.MAX).isAfter(LocalDate.now().plusYears(1));
        if (requestedGyldigTomIsMoreThan1YearFromNow) {
            throw new IllegalArgumentException("Sluttdato kan være maks 1 år frem i tid");
        }
    }
}
