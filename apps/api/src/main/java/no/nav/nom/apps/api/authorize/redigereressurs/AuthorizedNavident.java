package no.nav.nom.apps.api.authorize.redigereressurs;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.common.DateUtils;
import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.dvh.dto.DvhLeder;
import no.nav.nom.apps.api.organisering.eventsourcing.state.OrganiseringStateRepository;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.state.OrgtilknytningStateRepository;
import no.nav.nom.apps.api.ressurs.eventsourcing.state.RessursStateRepository;
import no.nav.nom.contract.nom.NomNivaa;
import no.nav.nom.contract.nom.state.OrganiseringState;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 *  Denne tjenesten inneholder alle som har til å redigere ressurser under gitt orgenhet i hierarkiet.
 *  Alle ledere på en enhet fra og med 'Arbeidsområde' type og nedover har tilgang for ressurser som har en tilknytning til en av disse enhetene og nedover.
 *  TODO: Må vurdere om det gjelder for aktive orgtilknytninger etc. i forhold til bruksmønster på endring hvor ressursen går fra en enhet til en annen
 *      --> Per nå sjekker den at orgtilknytning isCurrentOrFuturePeriod
 *
 *  Disse samme lederne har også mulighet til å gi andre ansatte tilgang til å redigere ressurser på samme orgenhet eller er valgt nivå under der igjen
 *  Disse tildelte rettighetene ka bestå også etter lederbytte, men må revideres på jevnlig basis. (Bekreftes på nytt, hvis ikke vil tilgangen tas vekk)
 */
@Slf4j
@Service
public class AuthorizedNavident {
    // Per leder for alle orgenheter på ARBEIDSOMRÅDE nivå og nedover. For den enkelte leder er alle under-nivåer lagt til.
    Map<String, HashSet<String>> lederForOrgenhet = new ConcurrentHashMap<>();
    // Delegert lederskap. Delegert leder for orgenhet, og alle underliggende enheter
    Map<String, HashSet<String>> delegertLederForOrgenhet = new ConcurrentHashMap<>();

    private final OrgtilknytningStateRepository orgtilknytningStateRepository;
    private final OrganiseringStateRepository organiseringStateRepository;
    private final RessursStateRepository ressursStateRepository;
    private final OrgenhetService orgenhetService;
    private final DvhRepository dvhRepository;

    public AuthorizedNavident(
            OrgtilknytningStateRepository orgtilknytningStateRepository,
            OrganiseringStateRepository organiseringStateRepository, RessursStateRepository ressursStateRepository,
            OrgenhetService orgenhetService,
            DvhRepository dvhRepository
    ) {
        this.orgtilknytningStateRepository = orgtilknytningStateRepository;
        this.organiseringStateRepository = organiseringStateRepository;
        this.ressursStateRepository = ressursStateRepository;
        this.orgenhetService = orgenhetService;
        this.dvhRepository = dvhRepository;
    }

    public boolean kanBrukerRedigereRessurs(String lederNavident, String ressursNavident) {
        Set<String> orgenhetIder = new HashSet<>();
        synchronized (this) {
            var orgenheter = lederForOrgenhet.get(lederNavident);
            if (orgenheter != null) {
                orgenhetIder.addAll(orgenheter);
            }
        }
        var delegerteOrgenheter = delegertLederForOrgenhet.get(lederNavident);
        if (delegerteOrgenheter != null) {
            orgenhetIder.addAll(delegerteOrgenheter);
        }
        return orgenhetIder.stream().anyMatch(orgenhetId -> erRessursInnunderOrgenhet(ressursNavident, orgenhetId));
    }

    public boolean addDelegertLederForOrgenhet(String leder, String delegertLeder, String orgenhetId) {
        sjekkParametereForAddRemoveDelegertLeder(leder, delegertLeder, orgenhetId);

        // TODO: Invoker evtSrc, oppdater delegertLederForOrgenhet async i bakhånd, Ta med DATO!!

        HashSet<String> orgenheterForDelegertLeder = delegertLederForOrgenhet.containsKey(delegertLeder) ? delegertLederForOrgenhet.get(delegertLeder) : new HashSet<>();
        boolean added = orgenheterForDelegertLeder.add(orgenhetId);
        delegertLederForOrgenhet.put(delegertLeder, orgenheterForDelegertLeder);
        return added;
    }

    public boolean removeDelegertLederForOrgenhet(String leder, String delegertLeder, String orgenhetId) {
        sjekkParametereForAddRemoveDelegertLeder(leder, delegertLeder, orgenhetId);

        // TODO: Invoker evtSrc, oppdater delegertLederForOrgenhet async i bakhånd, Ta med DATO!!
        boolean removed = false;
        if (delegertLederForOrgenhet.containsKey(delegertLeder)) {
            removed = delegertLederForOrgenhet.get(delegertLeder).remove(orgenhetId);
        }
        return removed;
    }

    public boolean renewDelegertLederForOrgenhet(String leder, String delegertLeder, String orgenhetId) {
        // TODO: Implementer renewDelegertLederForOrgenhet
        throw new NotImplementedException("Ikke implementert enda");
    }

    private void sjekkParametereForAddRemoveDelegertLeder(String leder, String delegertLeder, String orgenhetId) {
        if (leder == null || delegertLeder == null || orgenhetId == null) {
            throw new IllegalArgumentException("Leder, delegert leder og orgenhetId kan ikke være null");
        }

        if (!erRessursAktiv(delegertLeder)) {
            throw new IllegalArgumentException("Kan ikke delegere rediger-ressurs rettighet til navident "
                + delegertLeder + " da denne ikke finnes eller ikke er aktiv");
        }

        if (leder.equals(delegertLeder)) {
            throw new IllegalArgumentException("Leder og delegert leder kan ikke være samme person");
        }

        orgenhetService.getById(orgenhetId)
            .orElseThrow(() -> new IllegalArgumentException("Orgenhet " + orgenhetId + " finnes ikke i hierarkiet"));

        alleEnheterIHierarkiFraArbeidsomraade().stream()
            .filter(orgenhetId::equals)
            .findAny().orElseThrow(() ->
                new IllegalArgumentException("Kan ikke delegere eller endre delegert leder rettighet for ressurs-redigering tilhørende orgenhet " + orgenhetId
                    + " da denne ikke er under ARBEIDSOMRÅDE i organisasjonstreet"));


        if (!harLederDelegeringsRettForOrgenhet(leder, orgenhetId)) {
            throw new NotAuthorizedToDelegateLeder("Leder " + leder + " har ikke rett til å delegere eller endre delegert leder rettighet for ressurs-redigering tilhørende orgenhet " + orgenhetId);
        }
    }

    private boolean erRessursAktiv(String delegertLeder) {
        if (ressursStateRepository.findByNavident(delegertLeder).isEmpty()) return false;
        var ressurs = ressursStateRepository.findByNavident(delegertLeder).get();
        return DateUtils.isPeriodActive(ressurs.startdato().orElse(LocalDate.now()), ressurs.sluttdato().orElse(null));
    }

    public boolean harLederDelegeringsRettForOrgenhet(String leder, String orgenhetId) {
        return lederForOrgenhet.containsKey(leder)
            && lederForOrgenhet.get(leder).stream()
                .anyMatch(o -> alleEnheterIHierarkiFra(orgenhetId).contains(o));
    }

    /**
     * Bør kalles når ledere oppdateres og også når organisasjon oppdateres.
     * Per nå skjer alt dette via DvhRestConsumer på samme syklus.
     */
    public void oppdaterLedereForOrgenheter() {
        log.info("Oppdaterer ledere for orgenheter i redigere ressurs auth liste");
        var enheterIHierarkiFraArbeidsomraade = alleEnheterIHierarkiFraArbeidsomraade();
        Map<String, HashSet<String>> nyLederForOrgenhetListe = new HashMap<>();

        enheterIHierarkiFraArbeidsomraade.forEach(
            enhetId ->  {
                var orgenhet = orgenhetService.getById(enhetId).get();
                var lederNavident = dvhRepository.getLedere().stream()
                    .filter(leder -> leder.getOrgEnhetNr().equals(orgenhet.getAgressoId()) && leder.getOrgNiv()
                        .equals(orgenhet.getOrgNiv()) && leder.getOrgenhetNavn().equals(orgenhet.getNavn()))
                    .findAny()
                    .map(DvhLeder::getNavIdent)
                    .orElse(null);
                if (lederNavident != null) {
                    addLederForOrgenhetToList(nyLederForOrgenhetListe, lederNavident, enhetId);
                }
            }
        );
        synchronized (this) {
            lederForOrgenhet.clear();
            lederForOrgenhet.putAll(nyLederForOrgenhetListe);
        }
        log.info("Oppdatert {} ledere for {} orgenheter i redigere ressurs auth liste", 
            lederForOrgenhet.size(), orgenheterMedAuthedLederCount()
        );
    }

    private int orgenheterMedAuthedLederCount() {
        return lederForOrgenhet.values().stream().mapToInt(Collection::size).sum();
    }

    private int orgenheterMedDelegertAuthedLederCount() {
        return delegertLederForOrgenhet.values().stream().mapToInt(Collection::size).sum();
    }

    private void addLederForOrgenhetToList(Map<String, HashSet<String>> lederForEnhet, String navident, String orgenhetId) {
        HashSet<String> orgenheter = lederForEnhet.containsKey(navident) ? lederForEnhet.get(navident) : new HashSet<>();
        orgenheter.add(orgenhetId);
        lederForEnhet.put(navident, orgenheter);
    }

    private List<String> alleEnheterIHierarkiFraArbeidsomraade() {
        var arbeidsomraadeEnheter = orgenhetService.getAllEnheterMedOrganisering().stream()
            .filter(orgenhet -> orgenhet.getNomNivaa().isPresent() && orgenhet.getNomNivaa().get().equals(NomNivaa.ARBEIDSOMRAADE));
        List<String> orgenheter = new ArrayList<>();
        arbeidsomraadeEnheter.forEach(orgenhet -> orgenheter.addAll(alleEnheterIHierarkiFra(orgenhet.getId())));
        return orgenheter;
    }


    private boolean erRessursInnunderOrgenhet(String ressursNavident, String orgenhetId) {
        List<String> orgenheterIHierarki = alleEnheterIHierarkiFra(orgenhetId);

        return orgtilknytningStateRepository
            .findByNavident(ressursNavident).stream()
            .filter(PeriodeUtil::isCurrentOrFuturePeriod)
            .anyMatch(ot -> orgenheterIHierarki.contains(ot.orgEnhetId()));
    }

    private List<String> alleEnheterIHierarkiFra(String orgenhetId) {
        var aktiveOrganiseringer = organiseringStateRepository.getActiveOrganisering().values();
        return alleEnheterIHierarkiFra(orgenhetId, aktiveOrganiseringer);
    }

    private List<String> alleEnheterIHierarkiFra(String orgenhetId, Collection<OrganiseringState> aktiveOrganiseringer) {
        List<String> orgenheter = new ArrayList<>();
        orgenheter.add(orgenhetId);

        underenheterFor(orgenhetId, aktiveOrganiseringer).forEach(id -> orgenheter.addAll(alleEnheterIHierarkiFra(id, aktiveOrganiseringer)));
        return orgenheter;
    }

    private List<String> underenheterFor(String orgenhetId, Collection<OrganiseringState> iOrganiseringer) {
        return iOrganiseringer.stream()
            .filter(organisering -> organisering.idOver().equals(orgenhetId))
            .map(OrganiseringState::id)
            .toList();
    }
}
