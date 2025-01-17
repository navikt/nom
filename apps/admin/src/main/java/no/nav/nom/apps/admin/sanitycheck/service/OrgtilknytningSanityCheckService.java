package no.nav.nom.apps.admin.sanitycheck.service;

import no.nav.nom.apps.admin.sanitycheck.dto.orgtilknytning.OrgtilknytningMedOrgEnhet;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.Tilknytningtype;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepository;
import no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;

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
}
