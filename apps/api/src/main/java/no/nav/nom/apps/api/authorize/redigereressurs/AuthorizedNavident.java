package no.nav.nom.apps.api.authorize.redigereressurs;

import io.getunleash.Unleash;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.authorize.email.TilgangDelegertEmailService;
import no.nav.nom.apps.api.authorize.eventsourcing.producer.AuthorizeCommandService;
import no.nav.nom.apps.api.authorize.eventsourcing.producer.commands.AvsluttDelegertAuthorization;
import no.nav.nom.apps.api.authorize.eventsourcing.producer.commands.FornyDelegertAuthorization;
import no.nav.nom.apps.api.authorize.eventsourcing.producer.commands.OpprettDelegertAuthorization;
import no.nav.nom.apps.api.authorize.redigereressurs.api.app.LederTilgangForOrgenhetRequest;
import no.nav.nom.apps.api.common.DateUtils;
import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.dvh.dto.DvhLeder;
import no.nav.nom.apps.api.graphql.common.SecurityUtil;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil;
import no.nav.nom.contract.nom.NomNivaa;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.Origination;
import no.nav.nom.contract.nom.state.LederTilgangState;
import no.nav.nom.contract.nom.state.OrganiseringState;
import no.nav.nom.libs.state.authorize.DelegertLederStateRepository;
import no.nav.nom.libs.state.organisering.OrganiseringStateRepository;
import no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateRepository;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
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

import static no.nav.nom.apps.api.graphql.common.SecurityUtil.getAuthenticatedNavIdent;

/**
 *  Denne tjenesten inneholder alle som har til å redigere ressurser under gitt orgenhet i hierarkiet.
 *  Alle ledere på en enhet fra og med 'Arbeidsområde' type og nedover har tilgang for ressurser som har en tilknytning til en av disse enhetene og nedover.
 *  Per nå sjekker den at orgtilknytning isCurrentOrFuturePeriod
 *
 *  Disse samme lederne har også mulighet til å gi andre ansatte tilgang til å redigere ressurser på samme orgenhet eller er valgt nivå under der igjen
 *  Disse tildelte rettighetene ka bestå også etter lederbytte, men må revideres på jevnlig basis. (Bekreftes på nytt, hvis ikke vil tilgangen tas vekk)
 */
@Slf4j
@Service
public class AuthorizedNavident {
    private final Unleash unleash;
    // Per leder for alle orgenheter på ARBEIDSOMRÅDE nivå og nedover. For den enkelte leder er alle under-nivåer lagt til.
    Map<String, HashSet<String>> lederForOrgenhetSomKanRedigere = new ConcurrentHashMap<>();
    // Delegert lederskap. Delegert leder for orgenhet, og alle underliggende enheter
    private final AuthorizeCommandService authorizeCommandService;
    private final DelegertLederStateRepository delegertLederStateRepository;
    private final OrgtilknytningStateRepository orgtilknytningStateRepository;
    private final OrganiseringStateRepository organiseringStateRepository;
    private final RessursStateRepository ressursStateRepository;
    private final OrgenhetService orgenhetService;
    private final DvhRepository dvhRepository;
    private final TilgangDelegertEmailService tilgangDelegertEmailService;

    public AuthorizedNavident(
        AuthorizeCommandService authorizeCommandService,
        DelegertLederStateRepository delegertLederStateRepository,
        OrgtilknytningStateRepository orgtilknytningStateRepository,
        OrganiseringStateRepository organiseringStateRepository,
        RessursStateRepository ressursStateRepository,
        OrgenhetService orgenhetService,
        DvhRepository dvhRepository,
        TilgangDelegertEmailService tilgangDelegertEmailService,
        Unleash unleash
    ) {
        this.authorizeCommandService = authorizeCommandService;
        this.delegertLederStateRepository = delegertLederStateRepository;
        this.orgtilknytningStateRepository = orgtilknytningStateRepository;
        this.organiseringStateRepository = organiseringStateRepository;
        this.ressursStateRepository = ressursStateRepository;
        this.orgenhetService = orgenhetService;
        this.dvhRepository = dvhRepository;
        this.tilgangDelegertEmailService = tilgangDelegertEmailService;
        this.unleash = unleash;
    }

    public List<LederTilgangState> getLederTilgangerForUser() {
        return delegertLederStateRepository.findByNavident(getAuthenticatedNavIdent());
    }

    public List<LederTilgangState> getLederTilgangGittAvBruker() {
        return delegertLederStateRepository.findDelegertAvNavident(getAuthenticatedNavIdent());
    }

    public boolean kanBrukerRedigereRessurs(String navident, String ressursNavident) {
        Set<String> orgenhetIder = new HashSet<>();
        synchronized (this) {
            var orgenheter = lederForOrgenhetSomKanRedigere.get(navident);
            if (orgenheter != null) {
                orgenhetIder.addAll(orgenheter);
            }
        }

        orgenhetIder.addAll(delegertLederStateRepository.aktiveDelegeringerForOrgenheterTilNavident(navident));
        return orgenhetIder.stream().anyMatch(orgenhetId -> erRessursInnunderOrgenhet(ressursNavident, orgenhetId));
    }

    public boolean kanBrukerOppretteRessurs(String navident, String opprettesIOrgenhetId) {
        if (opprettesIOrgenhetId == null) {
            return false;
        }

        Set<String> orgenhetIder = new HashSet<>();
        synchronized (this) {
            var orgenheter = lederForOrgenhetSomKanRedigere.get(navident);
            if (orgenheter != null) {
                orgenhetIder.addAll(orgenheter);
            }
        }

        orgenhetIder.addAll(delegertLederStateRepository.aktiveDelegeringerForOrgenheterTilNavident(navident));
        return orgenhetIder.stream().anyMatch(orgenhetId -> alleEnheterIHierarkiFra(orgenhetId).contains(opprettesIOrgenhetId));
    }


    public boolean addLederTilgangForOrgenhet(String leder, LederTilgangForOrgenhetRequest request) {
        sjekkParametereForAddLederTilgang(leder, request);

        var inputMetadata = new InputMetadata("REST", "addDelegatedAuthorization",
            request,
            new Origination(getAuthenticatedNavIdent(), SecurityUtil.getAuthenticatedAzpName())
        );

        OpprettDelegertAuthorization opprettDelegertAuthorization = OpprettDelegertAuthorization.builder()
            .av(leder)
            .til(request.delegertTilNavident())
            .forOrgenhet(request.orgenhetId())
            .gyldigFom(LocalDate.now())
            .gyldigTom(request.gyldigTom())
            .inputMetadata(inputMetadata)
            .build();

        authorizeCommandService.opprettDelegertAuthorization(opprettDelegertAuthorization);
        tilgangDelegertEmailService.notifyDelegertTilgangByEmail(leder, request);
        return true;
    }

    public boolean removeLederTilgangForOrgenhet(String leder, String delegertLeder, String orgenhetId) {
        sjekkParametereDelegertLeder(leder, delegertLeder, orgenhetId, LocalDate.now());

        var opprinneligDelegering = delegertLederStateRepository.lederTilgangForOrgenhetOgNavident(orgenhetId, delegertLeder);
        if (opprinneligDelegering.isEmpty()) {
            log.warn("Fant ingen delegert leder rolle for orgenhet {} og navident {}. Kan ikke avsluttes", orgenhetId, delegertLeder);
            return false;
        }

        var inputMetadata = new InputMetadata("AuthorizedNavident", "removeLederTilgangForOrgenhet",
            Map.of("leder", leder, "delegertLeder", delegertLeder, "orgenhetId", orgenhetId),
            new Origination("nom-api", "nom-api")
        );

        AvsluttDelegertAuthorization avsluttDelegertAuthorization = AvsluttDelegertAuthorization.builder()
            .av(leder)
            .til(delegertLeder)
            .forOrgenhet(orgenhetId)
            .gyldigFom(opprinneligDelegering.get().gyldigFom())
            .opprinneligGyldigTom(opprinneligDelegering.get().gyldigTom())
            .nyGyldigTom(LocalDate.now().minusDays(1))
            .inputMetadata(inputMetadata)
            .build();

        authorizeCommandService.avsluttDelegertAuthorization(opprinneligDelegering.get().fid(), avsluttDelegertAuthorization);
        return true;
    }

    public boolean renewLederTilgangForOrgenhet(String leder, LederTilgangForOrgenhetRequest request) {
        sjekkParametereDelegertLeder(leder, request.delegertTilNavident(), request.orgenhetId(), request.gyldigTom());

        var inputMetadata = new InputMetadata("REST", "renewLederTilgangForOrgenhet",
            request,
            new Origination(getAuthenticatedNavIdent(), SecurityUtil.getAuthenticatedAzpName())
        );

        var opprinneligDelegering = delegertLederStateRepository.lederTilgangForOrgenhetOgNavident(request.orgenhetId(), request.delegertTilNavident());
        if (opprinneligDelegering.isEmpty()) {
            log.warn("Fant ingen delegert leder rolle for orgenhet {} og navident {}. Kan ikke fornyes", request.orgenhetId(), request.delegertTilNavident());
            return false;
        }

        FornyDelegertAuthorization fornyDelegertAuthorization = FornyDelegertAuthorization.builder()
            .av(leder)
            .til(request.delegertTilNavident())
            .forOrgenhet(request.orgenhetId())
            .gyldigFom(opprinneligDelegering.get().gyldigFom())
            .opprinneligGyldigTom(opprinneligDelegering.get().gyldigTom())
            .nyGyldigTom(request.gyldigTom())
            .inputMetadata(inputMetadata)
            .build();

        authorizeCommandService.fornyDelegertAuthorization(opprinneligDelegering.get().fid(), fornyDelegertAuthorization);
        return true;
    }

    public void sjekkParametereForAddLederTilgang(String leder, LederTilgangForOrgenhetRequest request) {
        sjekkParametereDelegertLeder(leder, request.delegertTilNavident(), request.orgenhetId(), request.gyldigTom());
        sjekkOmOrgenhetAlleredeDelegertTilNavident(request);
    }

    private void sjekkGyldigTomDato(LocalDate localDate) {
        if (localDate == null) {
            throw new IllegalArgumentException("Gyldig tom dato kan ikke være null");
        }
        if (localDate.isBefore(LocalDate.now().minusDays(1))) {
            throw new IllegalArgumentException("Gyldig tom dato kan ikke være før gårsdagens dato");
        }
    }

    public void sjekkParametereDelegertLeder(String leder, String delegertLeder, String orgenhetId, LocalDate gyldigTom) {
        sjekkGyldigTomDato(gyldigTom);

        if (leder == null || delegertLeder == null || orgenhetId == null) {
            throw new IllegalArgumentException("Leder, delegert leder og orgenhetId kan ikke være null");
        }

        if (!erRessursAktiv(delegertLeder)) {
            throw new IllegalArgumentException("Kan ikke delegere rediger-ressurs rettighet til navident "
                + delegertLeder + " da denne ikke finnes eller ikke er aktiv");
        }

        if (erEkstern(delegertLeder)) {
            throw new IllegalArgumentException("Eksterne medarbeidere kan ikke få delegert tilgang til å redigere.");
        }

        if (leder.equals(delegertLeder)) {
            throw new IllegalArgumentException("Leder og delegert leder kan ikke være samme person");
        }

        orgenhetService.getById(orgenhetId)
            .orElseThrow(() -> new IllegalArgumentException("Orgenhet " + orgenhetId + " finnes ikke i hierarkiet"));

        if (unleash.isEnabled("nom-api.driftsenhet-delegering-losning")) {
            alleOrgenheterMellomDriftsenhetOgArbeidsomraade().stream()
                .filter(orgenhetId::equals)
                .findAny().orElseThrow(() ->
                    new IllegalArgumentException("Kan ikke delegere eller endre delegert leder rettighet for ressurs-redigering tilhørende orgenhet " + orgenhetId +
                        " da denne ikke er  DRIFTSENHET, ARBEIDSOMRÅDE eller mellom disse organisasjonstreet"));
            if (!harLederDelegeringsRettForOrgenhet(leder, orgenhetId)) {
                throw new NotAuthorizedToDelegateLeder("Leder " + leder + " har ikke rett til å delegere eller endre delegert leder rettighet for ressurs-redigering tilhørende orgenhet " + orgenhetId);
            }
        } else {
            alleEnheterIHierarkiFraArbeidsomraade().stream()
                .filter(orgenhetId::equals)
                .findAny().orElseThrow(() ->
                    new IllegalArgumentException("Kan ikke delegere eller endre delegert leder rettighet for ressurs-redigering tilhørende orgenhet " + orgenhetId
                        + " da denne ikke er under ARBEIDSOMRÅDE i organisasjonstreet"));
            if (!harLederDelegeringsRettForOrgenhetOld(leder, orgenhetId)) {
                throw new NotAuthorizedToDelegateLeder("Leder " + leder + " har ikke rett til å delegere eller endre delegert leder rettighet for ressurs-redigering tilhørende orgenhet " + orgenhetId);
            }
        }
    }
    public void sjekkOmOrgenhetAlleredeDelegertTilNavident(LederTilgangForOrgenhetRequest request) {
        sjekkOmOrgenhetAlleredeDelegertTilNavident(request.orgenhetId(), request.delegertTilNavident());
    }

    public void sjekkOmOrgenhetAlleredeDelegertTilNavident(String orgenhetId, String delegertTilNavident) {
        if (delegertLederStateRepository.erOrgenhetDelegertTilNavident(orgenhetId, delegertTilNavident)) {
            throw new IllegalArgumentException("Delegert leder " + delegertTilNavident + " er allerede aktiv for orgenhet " +orgenhetId);
        }
    }

    private boolean erRessursAktiv(String delegertLeder) {
        if (ressursStateRepository.findByNavident(delegertLeder).isEmpty()) return false;
        var ressurs = ressursStateRepository.findByNavident(delegertLeder).get();
        return DateUtils.isPeriodActive(ressurs.startdato().orElse(LocalDate.now()), ressurs.sluttdato().orElse(null));
    }

    private boolean erEkstern(String delegertLeder) {
        var ident = ressursStateRepository.findByNavident(delegertLeder);
        return ident.isPresent() && ident.get().isEkstern();
    }

    public boolean harLederDelegeringsRettForOrgenhetOld(String leder, String orgenhetId) {
        return lederForOrgenhetSomKanRedigere.containsKey(leder)
            && lederForOrgenhetSomKanRedigere.get(leder).stream()
            .anyMatch(o -> alleEnheterIHierarkiFra(orgenhetId).contains(o));
    }


    public boolean harLederDelegeringsRettForOrgenhet(String leder, String orgenhetId) {
        var orgenhet = orgenhetService.getById(orgenhetId).orElseThrow();
        var orgenheter = new HashSet<String>();
        return lederForOrgenhetSomKanRedigere.containsKey(leder)
            && lederForOrgenhetSomKanRedigere.get(leder).stream()
                .anyMatch(o -> {
                    alleEnheterIHierarkiFraArbeidsomraadeTilDriftsenhet(orgenhet, orgenheter);
                    return orgenheter.contains(o);
                });
    }

    /**
     * Bør kalles når ledere i organisasjonen oppdateres og også når organisasjon oppdateres.
     * Per nå skjer alt dette via DvhRestConsumer på samme syklus.
     */
    public void oppdaterLedereForOrgenheter() {
        log.info("Oppdaterer ledere for orgenheter i redigere ressurs auth liste");
        var enheterIHierarkiFraDriftsenhet = unleash.isEnabled("nom-api.driftsenhet-delegering-losning") ? alleEnheterIHierarkiFraDriftsenhet() : alleEnheterIHierarkiFraArbeidsomraade();
        Map<String, HashSet<String>> nyLederForOrgenhetListe = new HashMap<>();

        enheterIHierarkiFraDriftsenhet.forEach(
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
            lederForOrgenhetSomKanRedigere.clear();
            lederForOrgenhetSomKanRedigere.putAll(nyLederForOrgenhetListe);
        }
        log.info("Oppdatert {} ledere for {} orgenheter i redigere ressurs auth liste", 
            lederForOrgenhetSomKanRedigere.size(), orgenheterMedAuthedLederCount()
        );
    }

    public List<IOrgenhet> isAuthorizedToDelegate(String lederNavident) {
        var orgenheterTilLeder = orgenhetService.getEnheterByLeder(lederNavident);
        var enheterMellomDriftOgArbomr = alleOrgenheterMellomDriftsenhetOgArbeidsomraade();

        return orgenheterTilLeder.stream()
            .filter(orgenhet -> enheterMellomDriftOgArbomr.contains(orgenhet.getId()))
            .toList();
    }

    private int orgenheterMedAuthedLederCount() {
        return lederForOrgenhetSomKanRedigere.values().stream().mapToInt(Collection::size).sum();
    }

    private int orgenheterMedDelegertAuthedLederCount() {
        return delegertLederStateRepository.count();
    }

    private void addLederForOrgenhetToList(Map<String, HashSet<String>> lederForEnhet, String navident, String orgenhetId) {
        HashSet<String> orgenheter = lederForEnhet.containsKey(navident) ? lederForEnhet.get(navident) : new HashSet<>();
        orgenheter.add(orgenhetId);
        lederForEnhet.put(navident, orgenheter);
    }

    private boolean erRessursInnunderOrgenhet(String ressursNavident, String orgenhetId) {
        List<String> orgenheterIHierarki = alleEnheterIHierarkiFra(orgenhetId);

        return orgtilknytningStateRepository
            .findByNavident(ressursNavident).stream()
            .filter(PeriodeUtil::isCurrentOrFuturePeriod)
            .anyMatch(ot -> orgenheterIHierarki.contains(ot.orgEnhetId()));
    }

    private List<String> alleEnheterIHierarkiFraArbeidsomraade() {
        var arbeidsomraadeEnheter = orgenhetService.getAllEnheterMedOrganisering().stream()
            .filter(orgenhet -> orgenhet.getNomNivaa().isPresent() && orgenhet.getNomNivaa().get().equals(NomNivaa.ARBEIDSOMRAADE));
        List<String> orgenheter = new ArrayList<>();
        arbeidsomraadeEnheter.forEach(orgenhet -> orgenheter.addAll(alleEnheterIHierarkiFra(orgenhet.getId())));
        return orgenheter;
    }

    private List<String> alleOrgenheterMellomDriftsenhetOgArbeidsomraade() {
        var arbeidsomraadeEnheter = orgenhetService.getAllEnheterMedOrganisering().stream()
            .filter(orgenhet -> orgenhet.getNomNivaa().isPresent() && orgenhet.getNomNivaa().get().equals(NomNivaa.ARBEIDSOMRAADE));

        Set<String> orgenheter = new HashSet<>();
        arbeidsomraadeEnheter.forEach(orgenhet -> alleEnheterIHierarkiFraArbeidsomraadeTilDriftsenhet(orgenhet, orgenheter));
        return orgenheter.stream().toList();
    }

    private void alleEnheterIHierarkiFraArbeidsomraadeTilDriftsenhet(IOrgenhet orgenhet, Set<String> orgenheter) {
        if (orgenhet.getNomNivaa().isPresent() && orgenhet.getNomNivaa().get().equals(NomNivaa.DRIFTSENHET)) {
            orgenheter.add(orgenhet.getId());
            return;
        }

        organiseringStateRepository.findActiveById(orgenhet.getId()).ifPresent(organiseringState -> {
            orgenheter.add(orgenhet.getId());
            var orgenhetTilIdOver = orgenhetService.getById(organiseringState.idOver());
            orgenhetTilIdOver.ifPresent(iOrgenhet -> alleEnheterIHierarkiFraArbeidsomraadeTilDriftsenhet(iOrgenhet, orgenheter));
        });
    }

    private List<String> alleEnheterIHierarkiFraDriftsenhet() {
        var driftsenhetEnheter = orgenhetService.getAllEnheterMedOrganisering().stream()
            .filter(orgenhet -> orgenhet.getNomNivaa().isPresent() && orgenhet.getNomNivaa().get().equals(NomNivaa.DRIFTSENHET));
        List<String> orgenheter = new ArrayList<>();
        driftsenhetEnheter.forEach(orgenhet -> orgenheter.addAll(alleEnheterIHierarkiFra(orgenhet.getId())));
        return orgenheter;
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
