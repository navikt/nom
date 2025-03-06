package no.nav.nom.apps.admin.sanitycheck.service;

import no.nav.nom.apps.admin.PeriodeUtil;
import no.nav.nom.apps.admin.sanitycheck.dto.ressurs.RessursMedEnhet;
import no.nav.nom.apps.admin.sanitycheck.dto.ressurs.RessursMedFlereDOL;
import no.nav.nom.apps.admin.sanitycheck.dto.ressurs.RessursOrgtilknytningKoblingInfo;
import no.nav.nom.apps.admin.sanitycheck.dto.ressurs.RessursOrgtilknytningUlikSluttdato;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.contract.nom.state.OrganiseringState;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.organisering.OrganiseringStateRepository;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepository;
import no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateRepository;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static no.nav.nom.apps.admin.PeriodeUtil.tomorrow;
import static no.nav.nom.apps.admin.PeriodeUtil.yesterday;

@Service
public class RessursSanityCheckService {
    private static final Logger log = LoggerFactory.getLogger(RessursSanityCheckService.class);
    private final RessursStateRepository ressursStateRepository;
    private final OrgEnhetStateRepository orgEnhetStateRepository;
    private final OrganiseringStateRepository organiseringStateRepository;
    private final OrgtilknytningStateRepository orgtilknytningStateRepository;

    public RessursSanityCheckService(RessursStateRepository ressursStateRepository,
                                     OrgEnhetStateRepository orgEnhetStateRepository,
                                     OrganiseringStateRepository organiseringStateRepository,
                                     OrgtilknytningStateRepository orgtilknytningStateRepository
    ) {
        this.ressursStateRepository = ressursStateRepository;
        this.orgEnhetStateRepository = orgEnhetStateRepository;
        this.organiseringStateRepository = organiseringStateRepository;
        this.orgtilknytningStateRepository = orgtilknytningStateRepository;
    }

    public RessursOrgtilknytningKoblingInfo aktiveRessurser() {
        List<RessursState> alleRessurser = ressursStateRepository.getAll().stream().toList();

        List<OrgEnhetState> alleEnheter = orgEnhetStateRepository.getAll().stream().toList();

        List<OrgEnhetState> enheterIkkeAktiveEllerUtenOrganisering;
        log.info("Beregner aktive ressurser med ny implementasjon");

        List<OrganiseringState> aktiveOrganiseringer = organiseringStateRepository.getOrganiseringer();

        List<String> aktiveOverEnheterUniqueIder = uniqueIdForOverEnhet(aktiveOrganiseringer);
        List<String> aktiveUnderEnheterUniqueIder = uniqueIdForUnderEnhet(aktiveOrganiseringer);

        List<String> aktiveOrganiseringerUniqueIder = new ArrayList<>(aktiveOverEnheterUniqueIder);
        aktiveOrganiseringerUniqueIder.addAll(aktiveUnderEnheterUniqueIder);

        enheterIkkeAktiveEllerUtenOrganisering = enheterIkkeBlantEnhetIder(alleEnheter, aktiveOrganiseringerUniqueIder);

        var alleOrgtilknytningState = orgtilknytningStateRepository.getAll();
        var aktiveOrgtilknytningState = aktiveOrgtilknytninger(alleOrgtilknytningState);
        var aktiveOrgtilknytningerStateUtoverEnhetPeriode = orgtilknytningerUtoverEnhetPeriode(aktiveOrgtilknytningState);

        List<String> navIdenterMedAktiveTilknytninger =
                aktiveOrgtilknytningState.stream().map(OrgtilknytningState::navident).toList();

        List<RessursState> ressurserMedAktiveTilknytninger = ressurserForNavIdenter(alleRessurser, navIdenterMedAktiveTilknytninger);
        List<RessursState> ressurserUtenAktiveTilknytninger = ressurserIkkeBlandtNavIdenter(alleRessurser, navIdenterMedAktiveTilknytninger);

        List<String> enhetIderEnheterIkkeAktiveEllerUtenOrganisering = enheterIkkeAktiveEllerUtenOrganisering.stream().map(OrgEnhetState::id).toList();

        List<RessursState> ressurserMedAktiveTilknytningerTilEnheterIkkeAktiveEllerUtenOrganisering =
                ressursKoblingForEnhetIder(ressurserMedAktiveTilknytninger, aktiveOrgtilknytningState, enhetIderEnheterIkkeAktiveEllerUtenOrganisering);

        int aktiveTilknytningerSize = aktiveOrgtilknytningState.size();
        int aktiveTilknytningerUtoverEnhetPeriodeSize = aktiveOrgtilknytningerStateUtoverEnhetPeriode.size();

        return new RessursOrgtilknytningKoblingInfo(
                alleRessurser.size(),
                aktiveTilknytningerSize,
                ressurserMedAktiveTilknytninger.size(),
                ressurserUtenAktiveTilknytninger.size(),
                ressurserMedAktiveTilknytningerTilEnheterIkkeAktiveEllerUtenOrganisering.size(),
                aktiveTilknytningerUtoverEnhetPeriodeSize
        );
    }

    public List<RessursMedEnhet> ressurserMedAktivOrgtilknytningTilIkkeAktivEnhet(boolean inkluderEksterne, boolean inkluderHelfo) {

        List<RessursState> alleRessurser = inkluderEksterne
                ? ressursStateRepository.getAll().stream().toList()
                : ressursStateRepository.getAll().stream().filter(r -> !r.sektor().equals(Sektor.EKSTERN)).toList();

        List<OrgEnhetState> alleEnheter = inkluderHelfo
                ? orgEnhetStateRepository.getAll().stream().toList()
                : orgEnhetStateRepository.getAll().stream().filter(e -> !e.navn().startsWith("HELFO")).toList();

        List<OrgEnhetState> enheterIkkeAktiveEllerUtenOrganisering;
        log.info("Finner ressurser knyttet til ikke-aktiv enhet med ny implementasjon");

        List<OrganiseringState> aktiveOrganiseringer = organiseringStateRepository.getOrganiseringer();

        List<String> aktiveOverEnheterUniqueIder = uniqueIdForOverEnhet(aktiveOrganiseringer);
        List<String> aktiveUnderEnheterUniqueIder = uniqueIdForUnderEnhet(aktiveOrganiseringer);

        List<String> aktiveOrganiseringerUniqueIder = new ArrayList<>(aktiveOverEnheterUniqueIder);
        aktiveOrganiseringerUniqueIder.addAll(aktiveUnderEnheterUniqueIder);

        enheterIkkeAktiveEllerUtenOrganisering = enheterIkkeBlantEnhetIder(alleEnheter, aktiveOrganiseringerUniqueIder);

        var alleOrgtilknytningState = orgtilknytningStateRepository.getAll();
        var aktiveOrgtilknytningState = aktiveOrgtilknytninger(alleOrgtilknytningState);


        List<String> navIdenterMedAktiveTilknytninger = aktiveOrgtilknytningState.stream().map(OrgtilknytningState::navident).toList();
        List<RessursState> ressurserMedAktiveTilknytninger = ressurserForNavIdenter(alleRessurser, navIdenterMedAktiveTilknytninger);

        List<String> enhetIderEnheterIkkeAktiveEllerUtenOrganisering = enheterIkkeAktiveEllerUtenOrganisering.stream().map(OrgEnhetState::id).toList();
        List<RessursState> ressurserMedAktiveTilknytningerTilEnheterIkkeAktiveEllerUtenOrganisering = ressursKoblingDagensDatoForEnhetIder(
                ressurserMedAktiveTilknytninger,
                aktiveOrgtilknytningState,
                enhetIderEnheterIkkeAktiveEllerUtenOrganisering
        );

        List<RessursMedEnhet> ressursMedEnhet = new ArrayList<>();
        ressurserMedAktiveTilknytningerTilEnheterIkkeAktiveEllerUtenOrganisering.forEach(ressurs -> aktiveOrgtilknytningState.stream()
                .filter(ot -> ot.navident().equals(ressurs.navident())).forEach(ot -> {
                    var orgEnhet = orgEnhetStateRepository.findById(ot.orgEnhetId());
                    ressursMedEnhet.add(new RessursMedEnhet(
                            ressurs.navident(),
                            ressurs.fid(),
                            ressurs.visningsnavn().getFormattedVisningsnavn(),
                            ressurs.startdato().orElse(null),
                            ressurs.sluttdato().orElse(null),
                            ressurs.sektor(),
                            orgEnhet.map(OrgEnhetState::id).orElse(""),
                            orgEnhet.map(OrgEnhetState::navn).orElse(""),
                            orgEnhet.map(OrgEnhetState::gyldigFom).orElse(null),
                            orgEnhet.map(OrgEnhetState::gyldigTom).orElse(null),
                            orgEnhet.filter(orgEnhetState -> orgEnhetState.uniqueAgressoId().isPresent())
                                    .flatMap(OrgEnhetState::uniqueAgressoId).orElse("")
                    ));
                }));
        return ressursMedEnhet;
    }

    public List<RessursMedEnhet> ressurserMedEnhetUtoverRessursSluttdato() {
        var alleOrgtilknytningState = orgtilknytningStateRepository.getAll().stream().toList();
        log.info("alleOrgtilknytningState, size: {}", alleOrgtilknytningState.size());

        var tilknytningerUtoverRessursPeriode = orgtilknytningerUtoverRessursPeriode(alleOrgtilknytningState);
        log.info("tilknytningerUtoverRessursPeriode, size: {}", tilknytningerUtoverRessursPeriode.size());

        List<RessursMedEnhet> ressursMedEnhet = new ArrayList<>();
        tilknytningerUtoverRessursPeriode.forEach(t -> {
            var oe = orgEnhetStateRepository.findById(t.orgEnhetId());
            var ressurs = ressursStateRepository.findByNavident(t.navident());
            ressursMedEnhet.add(new RessursMedEnhet(
                    ressurs.map(RessursState::navident).orElse(""),
                    ressurs.map(RessursState::fid).orElse(""),
                    ressurs.map(ressursState -> ressursState.visningsnavn().getFormattedVisningsnavn()).orElse(""),
                    ressurs.flatMap(RessursState::startdato).orElse(null),
                    ressurs.flatMap(RessursState::sluttdato).orElse(null),
                    ressurs.map(RessursState::sektor).orElse(null),
                    oe.map(OrgEnhetState::id).orElse(""),
                    oe.map(OrgEnhetState::navn).orElse(""),
                    oe.map(OrgEnhetState::gyldigFom).orElse(null),
                    oe.map(OrgEnhetState::gyldigTom).orElse(null),
                    oe.flatMap(OrgEnhetState::uniqueAgressoId).orElse("")
            ));
        });
        return ressursMedEnhet;
    }

    public List<RessursState> finnDuplikateRessurserPaaSammeNavident() {
        var alleRessurser = ressursStateRepository.getAll();

        Set<String> fidNavidenter = new HashSet<>();
        return alleRessurser.stream()
                .filter(n -> !fidNavidenter.add(n.navident()))
                .toList();
    }

    public List<RessursOrgtilknytningUlikSluttdato> ressurserMedUlikOrgTilknytningSluttDato() {
        Map<String, RessursState> alleRessurserSomErAktive = ressursStateRepository.findAllActiveRessurser();
        Map<String, Collection<OrgtilknytningState>> alleOrgTilknytninger =
                getOrgtilknytningerForNavidenter(alleRessurserSomErAktive.keySet(), gyldigTomBefore2099);
        Map<String, Collection<OrgtilknytningState>> sisteOrgtilknytninger = getSisteOrgtilknytninger(alleOrgTilknytninger);

        List<RessursOrgtilknytningUlikSluttdato> ulikeSluttdatoer = new ArrayList<>();

        for (OrgtilknytningState orgtilknytning : sisteOrgtilknytninger.values().stream().flatMap(Collection::stream).toList()) {
            var ressurs = alleRessurserSomErAktive.get(orgtilknytning.navident());
            if (!orgtilknytning.gyldigTom().equals(ressurs.sluttdato())) {
                ulikeSluttdatoer.add(getRessursOrgtilknytningUlikSluttdato(ressurs, orgtilknytning));
            }
        }
        return ulikeSluttdatoer;
    }

    private Map<String, Collection<OrgtilknytningState>> getSisteOrgtilknytninger(Map<String, Collection<OrgtilknytningState>> alleOrgTilknytninger) {
        Map<String, Collection<OrgtilknytningState>> result = new HashMap<>();
        for (Map.Entry<String, Collection<OrgtilknytningState>> entry : alleOrgTilknytninger.entrySet()) {
            String key = entry.getKey();
            Collection<OrgtilknytningState> orgtilknytningState = entry.getValue();

            Optional<LocalDate> latestDate = orgtilknytningState.stream()
                    .map(ot -> ot.gyldigTom().orElse(LocalDate.MAX))
                    .max(LocalDate::compareTo);

            Collection<OrgtilknytningState> filteredOrgtilknytningState = orgtilknytningState.stream()
                    .filter(org -> (org.gyldigTom().isEmpty() && latestDate.equals(Optional.of(LocalDate.MAX))) || org.gyldigTom().equals(latestDate))
                    .toList();

            if (!filteredOrgtilknytningState.isEmpty()) {
                result.put(key, filteredOrgtilknytningState);
            }
        }
        return result;
    }

    private RessursOrgtilknytningUlikSluttdato getRessursOrgtilknytningUlikSluttdato(RessursState ressurs, OrgtilknytningState orgtilknytning) {
        return RessursOrgtilknytningUlikSluttdato.builder()
                .navident(ressurs.navident())
                .orgEnhetId(orgtilknytning.orgEnhetId())
                .ressursSluttdato(ressurs.sluttdato().isPresent() ? ressurs.sluttdato().get() : null)
                .orgtilknytningSluttdato(orgtilknytning.gyldigTom().isPresent() ? orgtilknytning.gyldigTom().get() : null)
                .build();
    }

    public List<RessursMedFlereDOL> ressursMedFlereEnnEnDOL() {
        Map<String, RessursState> alleRessurserSomErAktive = ressursStateRepository.findAllActiveRessurser();
        Map<String, Collection<OrgtilknytningState>> alleOrgtilknytningerKobletTilRessurs =
                getOrgtilknytningerForNavidenter(alleRessurserSomErAktive.keySet(), gyldigTomMindreEnnSeksMndSidenEllerIkkeSatt);

        Map<String, List<OrgtilknytningState>> overlappendeDOLList = alleOrgtilknytningerKobletTilRessurs.entrySet().stream()
                .filter(orgtilknytning -> orgtilknytning.getValue().size() > 1)
                .filter(orgtilknytning -> validerOmRessursHarFlereDol(orgtilknytning.getValue()))
                .flatMap(orgtilknytning ->
                        finnDOLMedOverlapp(orgtilknytning.getValue()).stream().filter(Objects::nonNull))
                .collect(Collectors.groupingBy(OrgtilknytningState::navident, Collectors.toList()));

        List<RessursMedFlereDOL> ressursMedFlereDOL = new ArrayList<>();

        overlappendeDOLList.forEach((navident, orgtilknytningStates) -> {
            List<String> OTer = new ArrayList<>();
            orgtilknytningStates.forEach(orgtilknytningState -> {
                String ot = String.format("%s %s (%s -> %s)",
                        orgtilknytningState.fid(),
                        orgtilknytningState.orgEnhetId(),
                        orgtilknytningState.gyldigFom(),
                        orgtilknytningState.gyldigTom().isPresent() ? orgtilknytningState.gyldigTom().get() : "");
                OTer.add(ot);
            });

            var ressurs = RessursMedFlereDOL.builder()
                    .navident(navident)
                    .otErMedOverlapp(String.join(", ", OTer))
                    .build();

            ressursMedFlereDOL.add(ressurs);
        });

        return ressursMedFlereDOL;
    }

    private List<OrgtilknytningState> finnDOLMedOverlapp(Collection<OrgtilknytningState> orgtilknytningStates) {
        List<OrgtilknytningState> DOLSortedByFomDato = orgtilknytningStates.stream()
                .filter(OrgtilknytningState::erDagligOppfolging)
                .sorted(Comparator.comparing(OrgtilknytningState::gyldigFom))
                .toList();

        List<OrgtilknytningState> overlappendeTilknytninger = new ArrayList<>();

        for (int i = 0; i < DOLSortedByFomDato.size(); i++) {
            for (int j = i + 1; j < DOLSortedByFomDato.size(); j++) {
                OrgtilknytningState orgtilknytningStateEn = DOLSortedByFomDato.get(i);
                OrgtilknytningState orgtilknytningStateTo = DOLSortedByFomDato.get(j);
                
                LocalDate startdatoEn = orgtilknytningStateEn.gyldigFom();
                LocalDate sluttdatoEn = orgtilknytningStateEn.gyldigTom().orElse(LocalDate.MAX);
                LocalDate startdatoTo = orgtilknytningStateTo.gyldigFom();
                LocalDate sluttdatoTo = orgtilknytningStateTo.gyldigTom().orElse(LocalDate.MAX);

                if (!startdatoEn.isAfter(sluttdatoTo) && !sluttdatoEn.isBefore(startdatoTo)) {
                    overlappendeTilknytninger.add(orgtilknytningStateEn);
                    overlappendeTilknytninger.add(orgtilknytningStateTo);
                }
            }
        }

        return overlappendeTilknytninger.stream().distinct().toList();
    }

    private final Predicate<OrgtilknytningState> gyldigTomMindreEnnSeksMndSidenEllerIkkeSatt = orgtilknytningState -> {
        if (orgtilknytningState.gyldigTom() != null && orgtilknytningState.gyldigTom().isPresent()) {
            return PeriodeUtil.innenforSisteSeksMaaneder(orgtilknytningState.gyldigTom().get());
        } else {
            return true;
        }
    };

    private boolean validerOmRessursHarFlereDol(Collection<OrgtilknytningState> orgtilknytningStates) {
        return orgtilknytningStates.stream()
                .filter(OrgtilknytningState::erDagligOppfolging)
                .count() >= 2;
    }

    private Map<String, Collection<OrgtilknytningState>> getOrgtilknytningerForNavidenter(Set<String> navidenter, Predicate<OrgtilknytningState> filter) {
        return navidenter.stream().collect(Collectors.toMap(navident -> navident, navident -> {
            Collection<OrgtilknytningState> value = orgtilknytningStateRepository.findByNavident(navident);
            return value == null ? Collections.emptyList() : value.stream().filter(filter).toList();
        }));
    }

    private List<String> uniqueIdForOverEnhet(List<OrganiseringState> organiseringer) {
        return organiseringer.stream().map(OrganiseringState::idOver).distinct().toList();
    }

    private List<String> uniqueIdForUnderEnhet(List<OrganiseringState> organiseringer) {
        return organiseringer.stream().map(OrganiseringState::id).distinct().toList();
    }

    private static List<OrgEnhetState> enheterIkkeBlantEnhetIder(List<OrgEnhetState> enheter, List<String> enehtIder) {
        return enheter.stream().filter(e -> !enehtIder.contains(e.id())).toList();
    }

    private List<OrgtilknytningState> aktiveOrgtilknytninger(Collection<OrgtilknytningState> orgtilknytninger) {
        return orgtilknytninger.stream().filter(ot ->
                ot.gyldigFom().isBefore(LocalDate.now().plusDays(1)) &&
                        (ot.gyldigTom().isEmpty() || ot.gyldigTom().get().isAfter(LocalDate.now()))
        ).toList();
    }

    private List<RessursState> ressurserForNavIdenter(List<RessursState> ressurser, List<String> navIdenter) {
        return ressurser.stream().filter(r -> navIdenter.contains(r.navident())).toList();
    }

    private List<RessursState> ressurserIkkeBlandtNavIdenter(List<RessursState> ressurser, List<String> navIdenter) {
        return ressurser.stream().filter(r -> !navIdenter.contains(r.navident())).toList();
    }

    private List<RessursState> ressursKoblingForEnhetIder(List<RessursState> ressurser, List<OrgtilknytningState> orgtilknytninger, List<String> enhetIder) {
        List<OrgtilknytningState> orgtilknytningerForAngitteEnheter =
                orgtilknytninger.stream().filter(ot -> enhetIder.contains(ot.orgEnhetId())).toList();
        return ressurser.stream().filter(r -> orgtilknytningerForAngitteEnheter.stream().anyMatch(ot -> ot.navident().equals(r.navident()))).toList();
    }

    private List<RessursState> ressursKoblingDagensDatoForEnhetIder(
            List<RessursState> ressurser,
            List<OrgtilknytningState> orgtilknytninger,
            List<String> enhetIder
    ) {
        List<OrgtilknytningState> orgtilknytningerForAngitteEnheter =
                orgtilknytninger.stream()
                        .filter(ot -> enhetIder.contains(ot.orgEnhetId()))
                        .filter(RessursSanityCheckService::isCurrentPeriod)
                        .toList();
        return ressurser.stream().filter(r -> orgtilknytningerForAngitteEnheter.stream().anyMatch(ot -> ot.navident().equals(r.navident()))).toList();
    }

    public static boolean isCurrentPeriod(OrgtilknytningState orgtilknytning) {
        return isCurrentOrFuturePeriod(orgtilknytning) && orgtilknytning.gyldigFom().isBefore(tomorrow());
    }

    public static boolean isCurrentOrFuturePeriod(OrgtilknytningState orgtilknytning) {
        return orgtilknytning.gyldigTom().isEmpty() || orgtilknytning.gyldigTom().get().isAfter(yesterday());
    }

    private List<OrgtilknytningState> orgtilknytningerUtoverRessursPeriode(List<OrgtilknytningState> orgtilknytninger) {
        return orgtilknytninger.stream()
                .filter(t -> {
                    var ressurs = ressursStateRepository.findByNavident(t.navident());
                    if (ressurs.isEmpty()) {
                        return false;
                    }
                    if (ressurs.get().sluttdato().isPresent() && ressurs.get().sluttdato().get().isBefore(t.gyldigFom())) {
                        return true; // ressurs.sluttDato < tilknytning.gyldigFom
                    }
                    if (t.gyldigTom().isPresent() && ressurs.get().startdato().isPresent() && ressurs.get().startdato().get().isAfter(t.gyldigTom().get())) {
                        return true; // ressurs.startDato > tilknytning.gyldigTom
                    }
                    return t.gyldigTom().isEmpty() && ressurs.get().sluttdato().isPresent(); // ressurs avsluttet > tilknytning ikke avsluttet
                }).toList();
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
    
    private final Predicate<OrgtilknytningState> gyldigTomBefore2099 = orgtilknytningState -> 
                    orgtilknytningState.gyldigTom().isPresent() && 
                    orgtilknytningState.gyldigTom().get().isBefore(LocalDate.of(2099, 1, 1));

    public List<RessursState> ressurserMedBlankMellomnavn() {
        return ressursStateRepository.getAll().stream()
                .filter(r -> r.visningsnavn().mellomnavn().isPresent())
                .filter(r -> r.visningsnavn().mellomnavn().get().isBlank())
                .toList();
    }
}
