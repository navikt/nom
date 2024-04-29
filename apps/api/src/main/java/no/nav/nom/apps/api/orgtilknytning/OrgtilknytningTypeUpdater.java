package no.nav.nom.apps.api.orgtilknytning;

import no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.OrgtilknytningCommandService;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.EndreTilknytningtypeCommand;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.Tilknytningtype;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.Origination;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import no.nav.nom.libs.utils.common.platform.LeaderElection;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class OrgtilknytningTypeUpdater {

    private final RessursStateRepository ressursStateRepository;
    private final OrgtilknytningService orgtilknytningService;
    private final OrgtilknytningCommandService orgtilknytningCommandService;
    private final LeaderElection election;

    public OrgtilknytningTypeUpdater(
            RessursStateRepository ressursStateRepository,
            OrgtilknytningService orgtilknytningService,
            OrgtilknytningCommandService orgtilknytningCommandService, LeaderElection election)
    {
        this.ressursStateRepository = ressursStateRepository;
        this.orgtilknytningService = orgtilknytningService;
        this.orgtilknytningCommandService = orgtilknytningCommandService;
        this.election = election;
    }


    @Scheduled(initialDelay = 60*1000, fixedDelay = 5*60*1000) // Run on startup and every 30 min
    public void endreTilknytningType() {
        var metadata = new InputMetadata("nom-api BatchJob", "endreTilknytningType", null, new Origination("nom-api", "nom-api"));


        if (election.isLeader()) {
            var aktiveKommunale = getAktiveKommunaleNavidenter();

            orgtilknytningService.getByNavidenter(aktiveKommunale, Function.identity()).values().stream()
                .flatMap(Collection::stream)
                .filter(orgtilknytningState ->
                    PeriodeUtil.isCurrentOrFuturePeriod(orgtilknytningState) && !orgtilknytningState.orgtilknytningType().isPresent()
                ).forEach(orgtilknytningState ->
                    orgtilknytningCommandService.endreTilknytningtype(
                        EndreTilknytningtypeCommand.builder()
                            .fid(orgtilknytningState.fid())
                            .tilknytningtype(Optional.of(Tilknytningtype.INGEN))
                            .inputMetadata(metadata)
                            .build()
                    )
                );
        }
    }

    private Set<String> getAktiveKommunaleNavidenter() {
        return ressursStateRepository.getAll().stream()
            .filter(r -> (r.startdato().isEmpty() || r.startdato().get().isBefore(LocalDate.now()))
                && (r.sluttdato().isEmpty() || r.sluttdato().get().isAfter(LocalDate.now()))
            )
            .filter(r -> r.sektor().equals(Sektor.NAV_KOMMUNAL))
            .map(RessursState::navident)
            .collect(Collectors.toSet());
    }
}
