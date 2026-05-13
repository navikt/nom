package no.nav.nom.apps.api.common.speiling;

import no.nav.nom.apps.api.ressurs.NomMastered;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.RessursCommandService;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OppdaterFysiskeBrukerstederCommand;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OppdaterSektorCommand;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OppdaterTilknytningtypeCommand;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.Origination;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateRepository;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import no.nav.nom.libs.utils.common.platform.LeaderElection;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNullElse;
import static no.nav.nom.libs.utils.common.util.DateUtils.isPeriodActive;

@Service
public class SpeilingService {

    private final OrgtilknytningStateRepository orgtilknytningStateRepository;
    private final RessursStateRepository ressursStateRepository;
    private final NomMastered nomMastered;
    private final LeaderElection leaderElection;
    private final RessursCommandService ressursCommandService;

    public SpeilingService(
        OrgtilknytningStateRepository orgtilknytningStateRepository,
        RessursStateRepository ressursStateRepository,
        NomMastered nomMastered,
        LeaderElection leaderElection,
        RessursCommandService ressursCommandService
    ) {
        this.orgtilknytningStateRepository = orgtilknytningStateRepository;
        this.ressursStateRepository = ressursStateRepository;
        this.nomMastered = nomMastered;
        this.leaderElection = leaderElection;
        this.ressursCommandService = ressursCommandService;
    }

    @Scheduled(cron = "0 0 1 * * *")
    public void speilOrgtilknytningSektorTilRessursScheduler() {
        if (leaderElection.isLeader()) {
            speilOrgtilknytningDataTilRessursState();
        }
    }

    private void speilOrgtilknytningDataTilRessursState() {
        var alleAktiveOrgtilknytninger = orgtilknytningStateRepository.getAll()
            .stream()
            .filter(orgtilknytningState -> isPeriodActive(orgtilknytningState.gyldigFom(), orgtilknytningState.gyldigTom()))
            .filter(orgtilknytningState -> nomMastered.isMastered(orgtilknytningState.navident()))
            .collect(Collectors.groupingBy(OrgtilknytningState::navident));

        InputMetadata inputMetadata = InputMetadata.builder()
            .inputInterfaceName("nom-api BatchJob")
            .inputInterfaceMethodName("speilOrgtilknytningDataTilRessursState")
            .origination(new Origination("nom-api", "nom-api"))
            .build();

        alleAktiveOrgtilknytninger.forEach((navident, orgtilknytningStates) ->
            ressursStateRepository.findByNavident(navident).ifPresent(ressursState -> {
                    AtomicReference<String> primaryFysiskBrukersted = new AtomicReference<>();
                    Set<String> alleFysiskeBrukersteder = new HashSet<>();

                    orgtilknytningStates.forEach(orgtilknytningState -> {
                        if (orgtilknytningState.erDagligOppfolging()) {
                            if (nonNull(orgtilknytningState.sektor()) &&
                                (isNull(ressursState.sektor()) || !Objects.equals(ressursState.sektor(), orgtilknytningState.sektor()))) {
                                OppdaterSektorCommand oppdaterSektorCommand = new OppdaterSektorCommand(ressursState.fid(), orgtilknytningState.sektor());
                                ressursCommandService.oppdaterSektorEvent(oppdaterSektorCommand, inputMetadata);
                            }

                            if (nonNull(orgtilknytningState.orgtilknytningType()) &&
                                (isNull(ressursState.tilknytningtype()) || !Objects.equals(
                                    ressursState.tilknytningtype(),
                                    orgtilknytningState.orgtilknytningType()
                                ))) {
                                OppdaterTilknytningtypeCommand oppdaterTilknytningtypeCommand = new OppdaterTilknytningtypeCommand(
                                    ressursState.fid(),
                                    orgtilknytningState.orgtilknytningType()
                                );
                                ressursCommandService.oppdaterTilknytningtypeEvent(oppdaterTilknytningtypeCommand, inputMetadata);
                            }
                            if (nonNull(orgtilknytningState.primaryFysiskBrukerstedNummer())) {
                                primaryFysiskBrukersted.set(orgtilknytningState.primaryFysiskBrukerstedNummer());
                            }
                        }
                        if (nonNull(orgtilknytningState.primaryFysiskBrukerstedNummer())) {
                            alleFysiskeBrukersteder.add(orgtilknytningState.primaryFysiskBrukerstedNummer());
                        }
                        if (nonNull(orgtilknytningState.additionalFysiskeBrukerstederNummer())) {
                            alleFysiskeBrukersteder.addAll(orgtilknytningState.additionalFysiskeBrukerstederNummer());
                        }
                    });

                    var alleFysiskeBrukerstederErLike = optionalFysiskBrukerstederAreEqual(alleFysiskeBrukersteder, ressursState.fysiskeBrukersteder());

                    if ((isNull(ressursState.primaryFysiskBrukersted()) && nonNull(primaryFysiskBrukersted.get()))
                        || (!Objects.equals(ressursState.primaryFysiskBrukersted(), primaryFysiskBrukersted.get()) && nonNull(primaryFysiskBrukersted.get()))
                        || !alleFysiskeBrukerstederErLike) {
                        ressursCommandService.oppdaterFysiskeBrukerstederCommand(
                            new OppdaterFysiskeBrukerstederCommand(
                                ressursState.fid(),
                                alleFysiskeBrukersteder,
                                nonNull(primaryFysiskBrukersted.get()) ? primaryFysiskBrukersted.get() : ressursState.primaryFysiskBrukersted()
                            ),
                            inputMetadata
                        );
                    }
                }
            )
        );
    }

    public void speilSektorOrgtilknytning() {
        var dolOrgtilknytninger = orgtilknytningStateRepository.getAll()
            .stream()
            .filter(OrgtilknytningState::erDagligOppfolging)
            .filter(orgtilknytningState -> isPeriodActive(orgtilknytningState.gyldigFom(), orgtilknytningState.gyldigTom()))
            .filter(orgtilknytningState -> nonNull(orgtilknytningState.sektor()))
            .filter(orgtilknytningState -> nomMastered.isMastered(orgtilknytningState.navident()))
            .toList();

        InputMetadata inputMetadata = InputMetadata.builder()
            .inputInterfaceName("nom-api BatchJob")
            .inputInterfaceMethodName("speilOrgtilknytningSektorTilRessursScheduler")
            .origination(new Origination("nom-api", "nom-api"))
            .build();

        dolOrgtilknytninger.forEach(orgtilknytningState -> ressursStateRepository.findByNavident(orgtilknytningState.navident()).ifPresent(ressursState -> {
            if (isNull(ressursState.sektor()) || !Objects.equals(ressursState.sektor(), orgtilknytningState.sektor())) {
                OppdaterSektorCommand oppdaterSektorCommand = new OppdaterSektorCommand(ressursState.fid(), orgtilknytningState.sektor());
                ressursCommandService.oppdaterSektorEvent(oppdaterSektorCommand, inputMetadata);
            }
        }));
    }

    public void speilTilknytningtypeOrgtilknytning() {
        var dolOrgtilknytninger = orgtilknytningStateRepository.getAll()
            .stream()
            .filter(OrgtilknytningState::erDagligOppfolging)
            .filter(orgtilknytningState -> isPeriodActive(orgtilknytningState.gyldigFom(), orgtilknytningState.gyldigTom()))
            .filter(orgtilknytningState -> nonNull(orgtilknytningState.orgtilknytningType()))
            .filter(orgtilknytningState -> nomMastered.isMastered(orgtilknytningState.navident()))
            .toList();

        InputMetadata inputMetadata = InputMetadata.builder()
            .inputInterfaceName("nom-api BatchJob")
            .inputInterfaceMethodName("speilOrgtilknytningTilknytningtypeTilRessursScheduler")
            .origination(new Origination("nom-api", "nom-api"))
            .build();

        dolOrgtilknytninger.forEach(orgtilknytningState -> ressursStateRepository.findByNavident(orgtilknytningState.navident()).ifPresent(ressursState -> {
            if (isNull(ressursState.tilknytningtype()) || !Objects.equals(ressursState.tilknytningtype(), orgtilknytningState.orgtilknytningType())) {
                OppdaterTilknytningtypeCommand oppdaterTilknytningtypeCommand = new OppdaterTilknytningtypeCommand(
                    ressursState.fid(),
                    orgtilknytningState.orgtilknytningType()
                );
                ressursCommandService.oppdaterTilknytningtypeEvent(oppdaterTilknytningtypeCommand, inputMetadata);
            }
        }));
    }

    public void speilBrukerstederOrgtilknytning() {
        var alleAktiveOrgtilknytninger = orgtilknytningStateRepository.getAll()
            .stream()
            .filter(orgtilknytningState -> isPeriodActive(orgtilknytningState.gyldigFom(), orgtilknytningState.gyldigTom()))
            .filter(orgtilknytningState -> nomMastered.isMastered(orgtilknytningState.navident()))
            .collect(Collectors.groupingBy(OrgtilknytningState::navident));

        InputMetadata inputMetadata = InputMetadata.builder()
            .inputInterfaceName("nom-api BatchJob")
            .inputInterfaceMethodName("speilBrukerstederOrgtilknytning")
            .origination(new Origination("nom-api", "nom-api"))
            .build();

        alleAktiveOrgtilknytninger.forEach((navident, orgtilknytningStates) ->
            ressursStateRepository.findByNavident(navident).ifPresent(ressursState -> {
                    AtomicReference<String> primaryFysiskBrukersted = new AtomicReference<>();
                    Set<String> alleFysiskeBrukersteder = new HashSet<>();

                    orgtilknytningStates.forEach(orgtilknytningState -> {
                        if (orgtilknytningState.erDagligOppfolging()) {
                            if (nonNull(orgtilknytningState.primaryFysiskBrukerstedNummer())) {
                                primaryFysiskBrukersted.set(orgtilknytningState.primaryFysiskBrukerstedNummer());
                            }
                        }
                        if (nonNull(orgtilknytningState.primaryFysiskBrukerstedNummer())) {
                            alleFysiskeBrukersteder.add(orgtilknytningState.primaryFysiskBrukerstedNummer());
                        }
                        if (nonNull(orgtilknytningState.additionalFysiskeBrukerstederNummer())) {
                            alleFysiskeBrukersteder.addAll(orgtilknytningState.additionalFysiskeBrukerstederNummer());
                        }
                    });

                    var alleFysiskeBrukerstederErLike = optionalFysiskBrukerstederAreEqual(alleFysiskeBrukersteder, ressursState.fysiskeBrukersteder());

                    if ((isNull(ressursState.primaryFysiskBrukersted()) && nonNull(primaryFysiskBrukersted.get()))
                        || (!Objects.equals(ressursState.primaryFysiskBrukersted(), primaryFysiskBrukersted.get()) && nonNull(primaryFysiskBrukersted.get()))
                        || !alleFysiskeBrukerstederErLike) {
                        ressursCommandService.oppdaterFysiskeBrukerstederCommand(
                            new OppdaterFysiskeBrukerstederCommand(
                                ressursState.fid(),
                                alleFysiskeBrukersteder,
                                nonNull(primaryFysiskBrukersted.get()) ? primaryFysiskBrukersted.get() : ressursState.primaryFysiskBrukersted()
                            ),
                            inputMetadata
                        );
                    }

                }
            )
        );
    }

    private static boolean optionalFysiskBrukerstederAreEqual(
        Set<String> kommendeEndringer,
        List<String> orgtilknytningStateFysiskeBrukerstederBeforeChanges
    ) {
        return new HashSet<>(requireNonNullElse(
            orgtilknytningStateFysiskeBrukerstederBeforeChanges,
            List.of()
        )).equals(new HashSet<>(
            requireNonNullElse(kommendeEndringer, List.of())));
    }
}
