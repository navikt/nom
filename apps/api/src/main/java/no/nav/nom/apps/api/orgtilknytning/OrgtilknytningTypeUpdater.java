package no.nav.nom.apps.api.orgtilknytning;

import no.nav.nom.apps.api.orgenhet.OrgenhetService;
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
import no.nav.nom.libs.utils.common.util.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class OrgtilknytningTypeUpdater {

    private static final Logger log = LoggerFactory.getLogger(OrgtilknytningTypeUpdater.class);
    private final RessursStateRepository ressursStateRepository;
    private final OrgtilknytningService orgtilknytningService;
    private final OrgtilknytningCommandService orgtilknytningCommandService;
    private final LeaderElection election;
    private final OrgenhetService orgenhetService;

    public OrgtilknytningTypeUpdater(
        RessursStateRepository ressursStateRepository,
        OrgtilknytningService orgtilknytningService,
        OrgtilknytningCommandService orgtilknytningCommandService, LeaderElection election,
        OrgenhetService orgenhetService
        )
    {
        this.ressursStateRepository = ressursStateRepository;
        this.orgtilknytningService = orgtilknytningService;
        this.orgtilknytningCommandService = orgtilknytningCommandService;
        this.election = election;
        this.orgenhetService = orgenhetService;
    }


    @Scheduled(initialDelay = 60*1000, fixedDelay = 5*60*1000) // Run on startup and every 30 min
    public void endreTilknytningType() {
        if (election.isLeader()) {
            var metadata = new InputMetadata("nom-api BatchJob", "endreTilknytningType", null, new Origination("nom-api", "nom-api"));
            endreTilknytningtypeForKommunale(metadata);
            endreTilknytningtypeForVikafossen(metadata);
        }
    }

    private void endreTilknytningtypeForVikafossen(InputMetadata metadata) {
        var aktiveEksterneNavidenter = ressursStateRepository.getAll().stream()
            .filter(ressursState -> ressursState.sektor().equals(Sektor.EKSTERN))
            .filter(OrgtilknytningTypeUpdater::ressursErAktiv)
            .map(RessursState::navident)
            .collect(Collectors.toSet());

        var vikafossenOrgenhet = orgenhetService.getEnhetByAgressoIdAndOrgniv("VIKAFOSSEN","ORGENHET").orElseThrow(() -> new RuntimeException("Kunne ikke finne vikafossen enhet via agresso-id"));


        var orgtilknytningerTilVikafossen = orgtilknytningService.getByNavidenter(aktiveEksterneNavidenter, Function.identity())
            .values().stream()
            .flatMap(Collection::stream)
            .filter(it -> it.orgEnhetId().equals(vikafossenOrgenhet.getId()));

        var ikkeRiktigTilknytningOter = orgtilknytningerTilVikafossen.filter(it -> !Tilknytningtype.INGEN.equals(it.orgtilknytningType().orElse(null)))
            .collect(Collectors.toSet());

        log.info("Oppdaterer orgtilknytning tilknytningstype for %s vikafossen enheter".formatted(ikkeRiktigTilknytningOter.size()));

        for (var orgtilknytningState : ikkeRiktigTilknytningOter) {
            var cmd = new EndreTilknytningtypeCommand(orgtilknytningState.fid(), Optional.of(Tilknytningtype.INGEN), metadata);
            orgtilknytningCommandService.endreTilknytningtype(cmd);
        }

    }

    private void endreTilknytningtypeForKommunale(InputMetadata metadata) {
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

    private Set<String> getAktiveKommunaleNavidenter() {
        return ressursStateRepository.getAll().stream()
            .filter(OrgtilknytningTypeUpdater::ressursErAktiv)
            .filter(r -> r.sektor().equals(Sektor.NAV_KOMMUNAL))
            .map(RessursState::navident)
            .collect(Collectors.toSet());
    }

    private static boolean ressursErAktiv(RessursState r) {
        return DateUtil.isNow(r.startdato().orElse(null),r.sluttdato().orElse(null));
    }
}
