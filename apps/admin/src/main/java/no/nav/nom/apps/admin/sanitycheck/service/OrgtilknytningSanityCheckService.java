package no.nav.nom.apps.admin.sanitycheck.service;

import no.nav.nom.apps.admin.sanitycheck.dto.orgtilknytning.DuplikatOrgtilknytning;
import no.nav.nom.apps.admin.sanitycheck.dto.orgtilknytning.OrgtilknytningMedOrgEnhet;
import no.nav.nom.apps.admin.sanitycheck.dto.orgtilknytning.OverlappendeOrgtilknytninger;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.Tilknytningtype;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepository;
import no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static no.nav.nom.apps.admin.sanitycheck.service.RessursSanityCheckService.today;

@Service
public class OrgtilknytningSanityCheckService {
    private static final Logger LOG = LoggerFactory.getLogger(OrgtilknytningSanityCheckService.class);
    private final OrgtilknytningStateRepository orgtilknytningStateRepository;
    private final OrgEnhetStateRepository orgEnhetStateRepository;

    public OrgtilknytningSanityCheckService(OrgtilknytningStateRepository orgtilknytningStateRepository,
                                            OrgEnhetStateRepository orgEnhetStateRepository) {
        this.orgtilknytningStateRepository = orgtilknytningStateRepository;
        this.orgEnhetStateRepository = orgEnhetStateRepository;
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

    public List<OverlappendeOrgtilknytninger> orgtilknytningMedFlereOverlappendeDagligOppfolgingCurrentOgFuture() {
        // Group by navident for current period and dagligOppfolging
        Map<String, List<OrgtilknytningState>> groupedByNavidentForCurrentAndFuturePeriodWithDagligDagligOppfolging =
                orgtilknytningStateRepository.getAll().stream()
                        .filter(OrgtilknytningSanityCheckService::isCurrentOrFuturePeriod) // er current eller fremtidig
                        .filter(OrgtilknytningState::erDagligOppfolging) // er dagligoppfolging
                        .sorted(Comparator.comparing(OrgtilknytningState::gyldigFom))
                        .collect(Collectors.groupingBy(OrgtilknytningState::navident));

        List<OverlappendeOrgtilknytninger> overlappendeOrgtilknytninger = new ArrayList<>();
        // Filter out groups where there is more than one dagligOppfolging in current period
        groupedByNavidentForCurrentAndFuturePeriodWithDagligDagligOppfolging.values().stream()
                .filter(group -> group.size() > 1)
                .forEach(otEr -> {
                    OrgtilknytningState previous = null;
                    for (OrgtilknytningState ot : otEr) {
                        if (previous != null) {
                            if (overlapsInterval(ot, previous)) {
                                var overlappendeOTer = new OverlappendeOrgtilknytninger(
                                        previous,
                                        ot
                                );
                                overlappendeOrgtilknytninger.add(overlappendeOTer);
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
                            if (original.orgEnhetId().equals(ot.orgEnhetId()) &&
                                    original.gyldigFom().equals(ot.gyldigFom()) &&
                                    original.sektor().equals(ot.sektor()) &&
                                    original.orgtilknytningType().equals(ot.orgtilknytningType())
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

    public boolean validateOrgtilknytningTypeSektorCombination(OrgtilknytningState orgtilknytningState) {

        Map<Sektor, List<Tilknytningtype>> allowedOrgtilknytningTypeForSektorMap = Map.of(
                Sektor.EKSTERN, List.of(Tilknytningtype.KONSULENT, Tilknytningtype.EKSTERN_SAMARBEIDSPARTNER, Tilknytningtype.ARBEIDSTRENING, Tilknytningtype.ANNET, Tilknytningtype.STUDENT, Tilknytningtype.JOBBSPESIALIST),
                Sektor.NAV_KOMMUNAL, List.of(Tilknytningtype.INGEN),
                Sektor.NAV_STATLIG, List.of(Tilknytningtype.FAST, Tilknytningtype.MIDLERTIDIG, Tilknytningtype.PENSJONIST, Tilknytningtype.LAERLING)
        );
        return !allowedOrgtilknytningTypeForSektorMap.get(orgtilknytningState.sektor()).contains(orgtilknytningState.orgtilknytningType().get());
    }

    public static final LocalDate NO_END_DATE = LocalDate.of(2099, 12, 31);

    public static boolean isCurrentOrFuturePeriod(OrgtilknytningState orgtilknytning) {
        return orgtilknytning.gyldigTom().isEmpty() || orgtilknytning.gyldigTom().get().isAfter(yesterday());
    }

    public static LocalDate yesterday() {
        return today().minusDays(1);
    }

    public static boolean overlapsInterval(OrgtilknytningState orgtilknytning1, OrgtilknytningState orgtilknytning2) {
        return overlapsInterval(orgtilknytning1.gyldigFom(), orgtilknytning1.gyldigTom().orElse(NO_END_DATE), orgtilknytning2);
    }

    public static boolean overlapsInterval(LocalDate start, LocalDate end, OrgtilknytningState orgtilknytning) {
        return orgtilknytning.gyldigFom().isBefore(dayAfter(end)) && orgtilknytning.gyldigTom().orElse(NO_END_DATE).isAfter(dayBefore(start));
    }

    public static LocalDate dayAfter(LocalDate localDate) {
        return localDate.plusDays(1);
    }

    public static LocalDate dayBefore(LocalDate localDate) {
        return localDate.minusDays(1);
    }

}
