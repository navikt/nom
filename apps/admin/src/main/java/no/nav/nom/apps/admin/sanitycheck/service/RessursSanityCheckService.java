package no.nav.nom.apps.admin.sanitycheck.service;

import no.nav.nom.apps.admin.sanitycheck.dto.ressurs.RessursMedEnhet;
import no.nav.nom.apps.admin.sanitycheck.dto.ressurs.RessursOrgtilknytningKoblingInfo;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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

    public static LocalDate today() { return LocalDate.now(); }

    public static LocalDate yesterday() { return today().minusDays(1); }

    public static LocalDate tomorrow() { return today().plusDays(1); }

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
}
