package no.nav.nom.apps.admin.sanitycheck.service;

import no.nav.nom.apps.admin.PeriodeUtil;
import no.nav.nom.apps.admin.sanitycheck.dto.orgtilknytning.DuplikatOrgtilknytning;
import no.nav.nom.apps.admin.sanitycheck.dto.orgtilknytning.OrgtilknytningMedOrgEnhet;
import no.nav.nom.apps.admin.sanitycheck.dto.orgtilknytning.OverlappendeOrgtilknytning;
import no.nav.nom.apps.admin.webclient.nomremedyapiclient.NomRemedyApiClient;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.Tilknytningtype;
import no.nav.nom.contract.nom.eventsource.Event;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningEvent;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepository;
import no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;

@Service
public class OrgtilknytningSanityCheckService {
    private final static Logger logger = LoggerFactory.getLogger(OrgtilknytningSanityCheckService.class);
    private final OrgtilknytningStateRepository orgtilknytningStateRepository;
    private final OrgEnhetStateRepository orgEnhetStateRepository;
    private final NomRemedyApiClient nomRemedyApiClient;

    public OrgtilknytningSanityCheckService(OrgtilknytningStateRepository orgtilknytningStateRepository,
                                            OrgEnhetStateRepository orgEnhetStateRepository,
                                            NomRemedyApiClient nomRemedyApiClient
    ) {
        this.orgtilknytningStateRepository = orgtilknytningStateRepository;
        this.orgEnhetStateRepository = orgEnhetStateRepository;
        this.nomRemedyApiClient = nomRemedyApiClient;
    }

    public List<OrgtilknytningMedOrgEnhet> orgtilknytningerUtoverEnhetGyldighetsPeriode() {
        var alleOrgtilknytningState = orgtilknytningStateRepository.getAll();
        var aktiveOrgtilknytningState = aktiveOrgtilknytninger(alleOrgtilknytningState);
        var aktiveTilknytningerUtoverEnhetPeriode = orgtilknytningerUtoverEnhetPeriode(aktiveOrgtilknytningState);

        List<OrgtilknytningMedOrgEnhet> orgtilknytningMedOrgEnhetList = new ArrayList<>();
        aktiveTilknytningerUtoverEnhetPeriode.forEach(t -> {
            var oe = orgEnhetStateRepository.findById(t.orgEnhetId()).get();
            orgtilknytningMedOrgEnhetList.add(new OrgtilknytningMedOrgEnhet(
                    t.navident(),
                    oe.uniqueAgressoId().isPresent() ? oe.uniqueAgressoId().get() : "",
                    t.orgEnhetId(),
                    t.gyldigFom().toString(),
                    t.gyldigTom().isPresent() ? t.gyldigTom().get().toString() : "",
                    oe.gyldigFom().toString(),
                    oe.gyldigTom().isPresent() ? oe.gyldigTom().get().toString() : ""
            ));
        });

        return orgtilknytningMedOrgEnhetList;
    }

    public List<OrgtilknytningState> orgtilknytningNegativPeriode() {
        return orgtilknytningStateRepository.getAll().stream()
                .filter(ot -> ot.gyldigTom().isPresent() && ot.gyldigTom().get().isBefore(ot.gyldigFom()))
                .toList();
    }

    public List<OrgtilknytningState> orgtilknytningerMedFlereDOLSammePeriode() {
        Map<String, List<OrgtilknytningState>> groupedByNavidentForCurrentPeriodWithDagligDagligOppfolging = orgtilknytningStateRepository.getAll().stream()
                .filter(ot -> ot.gyldigFom().isBefore(LocalDate.now().plusDays(1)) &&
                        (ot.gyldigTom().isEmpty() || ot.gyldigTom().get().isAfter(LocalDate.now()))) // er current date
                .filter(OrgtilknytningState::erDagligOppfolging) // er dagligoppfolging
                .collect(Collectors.groupingBy(OrgtilknytningState::navident));


        // Filter out groups where there is more than one dagligOppfolging in current period
        return groupedByNavidentForCurrentPeriodWithDagligDagligOppfolging.values().stream()
                .filter(group -> group.size() > 1)
                .flatMap(List::stream)
                .toList();
    }

    public List<OrgtilknytningState> orgtilknytningStatesMedUgyldigeSektorTilknytninger() {
        return aktiveOrgtilknytninger(orgtilknytningStateRepository.getAll()).stream()
                .filter(ot -> !isNull(ot.sektor()) && ot.orgtilknytningType().isPresent())
                .filter(this::validateOrgtilknytningTypeSektorCombination)
                .toList();
    }

    public List<OrgtilknytningState> orgtilknytningStatesMedTilknytningtypeUtenSektor() {
        return aktiveOrgtilknytninger(orgtilknytningStateRepository.getAll()).stream()
                .filter(ot -> isNull(ot.sektor()) && ot.orgtilknytningType().isPresent())
                .toList();
    }

    public List<OverlappendeOrgtilknytning> orgtilknytningMedFlereOverlappendeDagligOppfolgingCurrentOgFuture() {
        // Group by navident for current period and dagligOppfolging
        Map<String, List<OrgtilknytningState>> groupedByNavidentForCurrentAndFuturePeriodWithDagligDagligOppfolging =
                orgtilknytningStateRepository.getAll().stream()
                        .filter(PeriodeUtil::isCurrentOrFuturePeriod) // er current eller fremtidig
                        .filter(OrgtilknytningState::erDagligOppfolging) // er dagligoppfolging
                        .sorted(Comparator.comparing(OrgtilknytningState::gyldigFom))
                        .collect(Collectors.groupingBy(OrgtilknytningState::navident));

        List<OverlappendeOrgtilknytning> overlappendeOrgtilknytninger = new ArrayList<>();
        // Filter out groups where there is more than one dagligOppfolging in current period
        groupedByNavidentForCurrentAndFuturePeriodWithDagligDagligOppfolging.values().stream()
                .filter(group -> group.size() > 1)
                .forEach(otEr -> {
                    OrgtilknytningState previous = null;
                    for (OrgtilknytningState ot : otEr) {
                        if (previous != null) {
                            if (PeriodeUtil.overlapsInterval(ot, previous)) {
                                overlappendeOrgtilknytninger.add(overlappendeOrgtilknytning(ot, previous));
                            }
                        }
                        previous = ot;
                    }
                });

        return overlappendeOrgtilknytninger;
    }

    public List<DuplikatOrgtilknytning> duplikateOrgtilknytninger() {
        // Group by navident
        Map<String, List<OrgtilknytningState>> groupedByNavident = orgtilknytningStateRepository.getAll().stream()
                .filter(orgtilknytningState -> {
                    if (orgtilknytningState.gyldigTom() != null && orgtilknytningState.gyldigTom().isPresent()) {
                        return PeriodeUtil.innenforSisteSeksMaaneder(orgtilknytningState.gyldigTom().get());
                    } else {
                        return true;
                    }
                })
                .sorted(Comparator
                        .comparing(OrgtilknytningState::orgEnhetId)
                        .thenComparing(OrgtilknytningState::gyldigFom)
                )
                .collect(Collectors.groupingBy(OrgtilknytningState::navident));

        List<DuplikatOrgtilknytning> duplikateOTer = new ArrayList<>();
        // Filter out groups where there is more than one dagligOppfolging in current period
        groupedByNavident.values().stream()
                .filter(group -> group.size() > 1)
                .forEach(otEr -> {
                    OrgtilknytningState original = null;
                    for (OrgtilknytningState ot : otEr) {
                        if (original != null) {
                            if (Objects.equals(original.orgEnhetId(), ot.orgEnhetId()) &&
                                    Objects.equals(original.gyldigFom(), ot.gyldigFom()) &&
                                    Objects.equals(original.sektor(), ot.sektor()) &&
                                    Objects.equals(original.orgtilknytningType(), ot.orgtilknytningType())
                            ) {
                                var duplikatOT = DuplikatOrgtilknytning.builder()
                                        .fid1(original.fid())
                                        .fid2(ot.fid())
                                        .navident(ot.navident())
                                        .orgEnhetId(ot.orgEnhetId())
                                        .sektor(ot.sektor())
                                        .erDagligOppfolging(ot.erDagligOppfolging())
                                        .gyldigFom(ot.gyldigFom())
                                        .orgtilknytningType(ot.orgtilknytningType())
                                        .build();
                                duplikateOTer.add(duplikatOT);
                            } else {
                                original = ot;
                            }
                        } else {
                            original = ot;
                        }
                    }
                });
        return duplikateOTer;
    }

    public List<OrgtilknytningState> compareFutureChangesWithDelayedEvents() {
        List<OrgtilknytningState> futureOrgtilknytningStates = getAllFutureOrgtilknytningStates();
        logger.info("Found {} future orgtilknytning states.", futureOrgtilknytningStates.size());
        List<String> delayedEvents = getAllDelayedEventsBlocking();
        logger.info("Found {} delayed events.", delayedEvents.size());
        List<OrgtilknytningState> futureDelayedEventsMatch = new ArrayList<>();

        if (futureOrgtilknytningStates.isEmpty() && delayedEvents.isEmpty()) {
            logger.info("No future orgtilknytning states or delayed events found.");
            return List.of();
        }

        boolean hasMatch = false;
        for (OrgtilknytningState orgtilknytningState : futureOrgtilknytningStates) {
            String fid = orgtilknytningState.fid();
            for (String delayedEventFid : delayedEvents) {
                if (fid.equals(delayedEventFid)) {
                    hasMatch = true;
                    break;
                }
            }
            if (!hasMatch) {
                futureDelayedEventsMatch.add(orgtilknytningState);
            }
        }
        return futureDelayedEventsMatch;
    }

    private List<OrgtilknytningState> getAllFutureOrgtilknytningStates() {
        return orgtilknytningStateRepository.getAll().stream()
            .filter(OrgtilknytningState::erDagligOppfolging)
            .filter(orgtilknytningState -> orgtilknytningState.gyldigFom().isAfter(LocalDate.now()))
            .collect(Collectors.toList());
    }

    private List<String> getAllDelayedEvents() {
        // or handle other event types as needed
        return nomRemedyApiClient.getAllDelayedEvents().map(
                event -> {
                    if (event instanceof OrgtilknytningEvent orgEvent) {
                        return orgEvent.getFid();
                    } else {
                        return ""; // or handle other event types as needed
                    }
                })
            .collectList().block();
    }

    private List<String> getAllDelayedEventsBlocking() {
        return nomRemedyApiClient.getAllDelayedEventsAsList().stream().map(Event::getFid).toList();
    }

    private List<OrgtilknytningState> aktiveOrgtilknytninger(Collection<OrgtilknytningState> orgtilknytninger) {
        return orgtilknytninger.stream().filter(ot ->
                ot.gyldigFom().isBefore(LocalDate.now().plusDays(1)) &&
                        (ot.gyldigTom().isEmpty() || ot.gyldigTom().get().isAfter(LocalDate.now()))
        ).toList();
    }

    private List<OrgtilknytningState> orgtilknytningerUtoverEnhetPeriode(List<OrgtilknytningState> orgtilknytninger) {
        return orgtilknytninger.stream()
                .filter(t -> {
                    var orgEnhet = orgEnhetStateRepository.findByFid(t.orgEnhetId());
                    if (orgEnhet.isPresent() && orgEnhet.get().gyldigTom().isPresent() && orgEnhet.get().gyldigTom().get().isBefore(t.gyldigFom())) {
                        return true; // orgEnhet.gyldigTom < tilknytning.gyldigFom
                    }
                    if (t.gyldigTom().isPresent() && orgEnhet.isPresent() && orgEnhet.get().gyldigFom().isAfter(t.gyldigTom().get())) {
                        return true; // orgEnhet.gyldigFom > tilknytning.gyldigTom
                    }
                    return t.gyldigTom().isEmpty() && orgEnhet.isPresent() && orgEnhet.get().gyldigTom().isPresent(); // orgEnhet avsluttet > tilknytning ikke avsluttet
                }).toList();
    }

    private boolean validateOrgtilknytningTypeSektorCombination(OrgtilknytningState orgtilknytningState) {

        Map<Sektor, List<Tilknytningtype>> allowedOrgtilknytningTypeForSektorMap = Map.of(
                Sektor.EKSTERN, List.of(Tilknytningtype.KONSULENT, Tilknytningtype.EKSTERN_SAMARBEIDSPARTNER, Tilknytningtype.ARBEIDSTRENING, Tilknytningtype.ANNET, Tilknytningtype.STUDENT),
                Sektor.NAV_KOMMUNAL, List.of(Tilknytningtype.INGEN),
                Sektor.NAV_STATLIG, List.of(Tilknytningtype.FAST, Tilknytningtype.MIDLERTIDIG, Tilknytningtype.PENSJONIST, Tilknytningtype.LAERLING)
        );
        return !allowedOrgtilknytningTypeForSektorMap.get(orgtilknytningState.sektor()).contains(orgtilknytningState.orgtilknytningType().get());
    }

    private OverlappendeOrgtilknytning overlappendeOrgtilknytning(OrgtilknytningState ot1, OrgtilknytningState ot2) {
        return OverlappendeOrgtilknytning.builder()
                .navident1(ot1.navident())
                .orgEnhetId1(ot1.orgEnhetId())
                .fid1(ot1.fid())
                .gyldigFom1(ot1.gyldigFom())
                .gyldigTom1(ot1.gyldigTom())
                .erDagligOppfolging1(ot1.erDagligOppfolging())
                .navident2(ot2.navident())
                .orgEnhetId2(ot2.orgEnhetId())
                .fid2(ot2.fid())
                .gyldigFom2(ot2.gyldigFom())
                .gyldigTom2(ot2.gyldigTom())
                .erDagligOppfolging2(ot2.erDagligOppfolging())
                .build();
    }

    public List<OrgtilknytningState> listTilkoblingerSomharTomDatoFoorstDagImaneden() {
        var alleOrgtilknytningState = orgtilknytningStateRepository.getAll();
        var aktiveOrgtilknytningState = aktiveOrgtilknytninger(alleOrgtilknytningState);
        return aktiveOrgtilknytningState.stream()
            .filter(ot -> ot.gyldigTom().isPresent() && ot.gyldigTom().get().getDayOfMonth() == 1)
            .toList();
    }

    public List<OverlappendeOrgtilknytning> orgtilknytningerMedOverlappendeHistoriskeDol() {
        var dolOrgtilknytningerSomErAvsluttet = orgtilknytningStateRepository.getAll().stream()
            .filter(ot -> ot.erDagligOppfolging()
                && ot.gyldigFom() != null
                && ot.gyldigTom().isPresent()
                && ot.gyldigTom().get().isBefore(LocalDate.now()))
            .sorted(Comparator.comparing(OrgtilknytningState::gyldigFom))
            .collect(Collectors.groupingBy(OrgtilknytningState::navident));

        List<OverlappendeOrgtilknytning> overlappendeOrgtilknytninger = new ArrayList<>();

        dolOrgtilknytningerSomErAvsluttet.forEach((navident, orgtilknytningStates) -> {
            OrgtilknytningState previous = null;
            for (OrgtilknytningState ot : orgtilknytningStates) {
                if (previous != null) {
                    if (PeriodeUtil.overlapsInterval(ot, previous)) {
                        overlappendeOrgtilknytninger.add(overlappendeOrgtilknytning(ot, previous));
                    }
                }
                previous = ot;
            }
        });
        return overlappendeOrgtilknytninger;
    }
}
