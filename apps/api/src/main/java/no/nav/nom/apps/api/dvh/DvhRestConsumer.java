package no.nav.nom.apps.api.dvh;

import io.getunleash.Unleash;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.authorize.redigereressurs.AuthorizedNavident;
import no.nav.nom.apps.api.clients.dvhords.DvhOrdsService;
import no.nav.nom.apps.api.common.testidenter.ManuelleRessurserOgLedereTilState;
import no.nav.nom.apps.api.dvh.dto.DvhTelefon;
import no.nav.nom.apps.api.organisering.OrganiseringService;
import no.nav.nom.apps.api.orgenhet.OrgEnhetTypeHandler;
import no.nav.nom.apps.api.orgenhet.implementations.dvh.v2.DvhOrgenhetsLast;
import no.nav.nom.apps.api.orgtilknytning.dvh.EksterneRessurserLoader;
import no.nav.nom.apps.api.orgtilknytning.dvh.RessursStillingLoader;
import no.nav.nom.apps.api.orgtilknytning.job.OTUtenforHierarki;
import no.nav.nom.apps.api.ressurs.implementations.dvh.DvhRessurs;
import no.nav.nom.apps.api.ressurs.implementations.dvh.RessursHendelseForEventsourceService;
import no.nav.nom.apps.api.telefon.Telefon;
import no.nav.nom.apps.api.telefon.TelefonService;
import no.nav.nom.contract.nom.TelefonType;
import no.nav.nom.contract.nom.eventsource.Jobbtelefon;
import no.nav.nom.libs.state.authorize.NomStateDelegertLederListener;
import no.nav.nom.libs.state.organisering.NomStateOrganiseringListener;
import no.nav.nom.libs.state.orgenhet.NomStateOrgEnhetListener;
import no.nav.nom.libs.state.orgenhetleder.NomStateOrgEnhetLederListener;
import no.nav.nom.libs.state.orgtilknytning.NomStateOrgtilknytningListener;
import no.nav.nom.libs.state.ressurs.NomStateRessursListener;
import no.nav.nom.libs.state.ressurs.mastered.NomStateMasteredRessursListener;
import no.nav.nom.libs.utils.common.platform.LeaderElection;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static no.nav.nom.apps.api.common.AppConfig.TEAM_LOGS;

@Slf4j
@Component
@RequiredArgsConstructor
public class DvhRestConsumer {
    public static final String CACHE = "cache-source-dvh";
    private final long LOAD_INTERVAL = 30 * 60 * 1000; // sync hvert 30min

    private final DvhService dvhService;
    private final NomStateRessursListener nomStateRessursListener;
    private final NomStateOrgtilknytningListener nomStateOrgtilknytningListener;
    private final NomStateOrganiseringListener nomStateOrganiseringListener;
    private final NomStateOrgEnhetListener nomStateOrgEnhetListener;
    private final NomStateMasteredRessursListener nomStateMasteredRessursListener;
    private final RessursHendelseForEventsourceService ressursHendelseForEventsourceService;
    private final LeaderElection leaderElection;
    private final DvhOrgenhetsLast dvhOrgenhetsLast;
    private final OrgEnhetTypeHandler orgEnhetTypeHandler;

    private final RessursStillingLoader ressursStillingLoader;
    private final EksterneRessurserLoader eksterneRessurserLoader;
    private final OrganiseringService organiseringService;
    private final AuthorizedNavident authorizedNavident;

    private final ManuelleRessurserOgLedereTilState manuelleRessurserOgLedereTilState;

    private final DvhStartupLoadMonitor dvhStartupLoadMonitor;
    private final NomStateDelegertLederListener nomStateDelegertLederListener;
    private final DvhOrdsService dvhOrdsService;

    private final OTUtenforHierarki otUtenforHierarki;
    private final TelefonService telefonService;
    private final NomStateOrgEnhetLederListener nomStateOrgEnhetLederListener;
    private final Unleash unleash;

    @Scheduled(fixedDelay = LOAD_INTERVAL) // Run on startup and every 30 min
    public void scheduledOppdateringFraDvh() {

        log.info("Starter schedulert oppdatering fra DVH");
        if (skipScheduledRunIfDuringDvhUpdate()) {
            return;
        }
        
        LocalDateTime start = LocalDateTime.now();

        while (!isInitialStateReady()) { // Wait on startup until state topics are read
            try {
                Thread.sleep(5_000);
                log.debug("InitialState er ikke klar enda");
                log.debug(
                    "\n Ressurs state: {} \n Orgtilkn. state: {} \n Organisering state: {} \n Orgenhet state: {} \n OrgEnhetLederState: {}",
                    nomStateMasteredRessursListener.hasReadToEndOnStartup(),
                    nomStateOrgtilknytningListener.hasReadToEndOnStartup(),
                    nomStateOrganiseringListener.hasReadToEndOnStartup(),
                    nomStateOrgEnhetListener.hasReadToEndOnStartup(),
                    nomStateOrgEnhetLederListener.hasReadToEndOnStartup()
                );
            } catch (Exception logMe) {
                log.error("Waiting for initialState last feilet", logMe);
            }
        }

        if (!unleash.isEnabled("nom-api.dvh-last")) {
            if (!dvhStartupLoadMonitor.hasCompleted()) {
                dvhStartupLoadMonitor.setCompleted();
            }
            loadOrganiseringerFromDvh(false);
            manuelleRessurserOgLedereTilState.oppdater();
            authorizedNavident.oppdaterLedereForOrgenheter();
            
            log.info("DVH lasten er skrudd av i Unleash, State Topics er lest inn, går ut av schedulert oppdatering fra DVH etter {}", durationAsH24(start));
            return;
        }

        log.info("State topics er lest, fortsetter schedulert oppdatering fra DVH etter {}", durationAsH24(start));

        List<DvhTelefon> dvhTelefoner = loadTlfFromDvh();

        // Denne kommer før oppdatering av orgenheter - gjør at noen organiseringer feiler når ny enhet ikke er opprettet enda
        // Men - vi må ha den her så lenge saveOrganiseringer(...) til orgenhetService benyttes - slik at hver pod er oppdatert med denne infoen.
        // Skal fjernes når det over er tatt vekk
        // Legger inn lesningen av denne under i lederElection delen for å sikre oppdaterte enheter først
        loadOrganiseringerFromDvh(false); // Hvis dette er leader og schedulert, skal ikke oppdaterring gjøres - altså force er false

        if (!dvhStartupLoadMonitor.hasCompleted() && dvhTelefoner != null && !dvhTelefoner.isEmpty()) { // Pod kan markeres som klar etter oppstart
            dvhStartupLoadMonitor.setCompleted();
            log.info("Initial oppdatering fra DVH er klar. Etter {}", durationAsH24(start));
        }

        if (leaderElection.isLeader() && erKlokkeslettISafeTidsromForKonsistentLastFraDVH()) { // Oppdater state fra dvh - holder at en Pod gjør dette
            log.info("Start leader schedulert oppdatering fra DVH");
            dvhOrgenhetsLast.lastOrgenhetsDataFraDvhTilEvtsrc();

            // Sikre 'riktig orgenhet+organisering' før Sjekk på ot utenfor hierarki
            loadLedereFromDvh();

            // Trenger en dummy pause her for å sikre at state for orgenheter er oppdatert
            // Dette for å hindre - "Organisering relasjon mislyktes, overenhet" i setOrganiserteOrgenheter(...)
            // I think....
            try {
                Thread.sleep(15_000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            loadOrganiseringerFromDvh(true); // Her kan leader oppdatere

            // Finn OTer utenfor hierarki - og autoflytt om mulig
            findOTUtenforHierarki();

            orgEnhetTypeHandler.updateOrgEnhetsTypeForLeafs();

            loadRessurserFromDvh();

            updateOrgtilknytningerStateFromDvh();

            updateOrgtilknytningerForEksterneFromDvh();
            if (dvhTelefoner != null && !dvhTelefoner.isEmpty()) {
                oppdaterTelefonerFraDvh(dvhTelefoner);
            }
        }

        manuelleRessurserOgLedereTilState.oppdater();

        // TODO - denne kan i sekundær Pod (ikke leader) bli kjørt rett før leader pod har oppdatert ny info
        //  - som gjør at sekundær pod er out of synch egentlig inntil denne trigges på nytt
        // Men dette er best effort akkurat nå
        authorizedNavident.oppdaterLedereForOrgenheter();

        log.info("Ferdig med schedulert oppdatering fra DVH. Tok {}", durationAsH24(start));
    }

    private boolean skipScheduledRunIfDuringDvhUpdate() {
        // Vill alltid kjøres på deploy
        if (!dvhStartupLoadMonitor.hasCompleted()) {
            log.info("Oppstartsinnlesning - kan ikke skippe schedulert oppdatering");
            return false;
        }

        return !erKlokkeslettISafeTidsromForKonsistentLastFraDVH();
    }

    private boolean erKlokkeslettISafeTidsromForKonsistentLastFraDVH() {
        // Dvh oppdateres ifølge Henrik ca. 02.00. 14.00 og 18.00.
        // Har observert inkonsistens i periodene 01:30-03:00, 12:30-15:00, og 17:00-18:30
        // Velger å kun kjøre i timene 22, 05, 10 og 16
        // Kan kjøres manuelt fra Swagger/admin
        List<Integer> runHours = Arrays.asList(5, 10, 16, 22);
        var safe = runHours.contains(LocalDateTime.now().getHour());
        log.info("Vi er {} safe tidsrom for oppdatering fra DVH da timen {} er i runHours {}", safe ? "i" : "ikke i", LocalDateTime.now().getHour(), runHours);
        return safe;
    }

    public void loadRessurserOgOrgEnheter() {
        if (leaderElection.isLeader()) {
            loadRessurserFromDvh();

            dvhOrgenhetsLast.lastOrgenhetsDataFraDvhTilEvtsrc();
        }
    }

    @SneakyThrows
    public void loadRessurserFromDvh() {
        log.info("Laster ressurser fra dvh");
        try {
            var dvhRessurser = dvhOrdsService.getRessurser();

            var dvhRessurs = dvhRessurser.stream()
                .distinct()
                .collect(Collectors.groupingBy(DvhRessurs::getPersonIdent))
                .values()
                .stream()
                .peek(l -> {
                    if (l.size() > 1) {
                        l.removeAll(l.stream().filter(Predicate.not(DvhRessurs::getRemedyAktiv)).toList());
                        if (l.size() > 1) {
                            var personIdent = l.getFirst().getPersonIdent();
                            var navIdenter = l.stream().map(DvhRessurs::getNavIdent).toList();
                            log.warn(TEAM_LOGS, "Forkastet lesing av ansatt med personident " + personIdent + " har flere aktive navidenter: " + navIdenter);
                            l.clear();
                        }
                    }
                })
                .flatMap(Collection::stream)
                .collect(Collectors.groupingBy(DvhRessurs::getNavIdent))
                .values()
                .stream()
                .filter(l -> l.size() == 1)
                .flatMap(Collection::stream)
                .toList();

            ressursHendelseForEventsourceService.handterRessursIderFraDvh(dvhRessurs);
        } catch (Exception e) {
            log.error("Feilet i last av ressurser fra dvh", e);
        }
    }

    private void updateOrgtilknytningerStateFromDvh() {
        if (dvhStartupLoadMonitor.hasCompleted() && leaderElection.isLeader()) {
            log.info("Oppdater Orgtilnytninger state fra dvh");
            lastOrgtilknytningerTilEvtsrc();
        }
    }

    public void lastOrgtilknytningerTilEvtsrc() {
        try {
            ressursStillingLoader.loadAndProcess();
        } catch (Exception e) {
            log.error("Feilet i last av ressursStillinger", e);
        }
    }

    public void updateOrgtilknytningerForEksterneFromDvh() {
        log.info("Oppdater Orgtilknytninger for eksterne fra dvh");
        eksterneRessurserLoader.loadAndProcess();
    }

    public List<DvhTelefon> loadTlfFromDvh() {
        log.info("Laster tlf fra dvh");
        try {
            List<DvhTelefon> tlfRader = dvhOrdsService.getTelefoner();
            log.info("Telefonnr lastet: Antall rader fra DVH = " + tlfRader.size());
            return tlfRader;
        } catch (Exception e) {
            log.error("Failure loading tlf data from DVH", e);
        }
        return List.of();
    }


    public void oppdaterTelefonerFraDvh(List<DvhTelefon> tlfRader) {
        Map<String, List<DvhTelefon>> dvhTelefonerMap = tlfRader.stream().collect(Collectors.groupingBy(DvhTelefon::getNavIdent));

        Map<String, Telefon> eksisterendeTelefoner = telefonService.getExistingTelefoner();

        // Oppdaterer eksisterende telefoner
        log.info("Fant {} eksisterende telefoner og {} telefoner fra dvh", eksisterendeTelefoner.size(), dvhTelefonerMap.size());
        eksisterendeTelefoner.forEach((String fid, Telefon telefon) -> {
            List<DvhTelefon> dvhTelefoner = dvhTelefonerMap.remove(telefon.navident());
            oppdaterTelefonOmEndret(fid, telefon.navident(), toJobbtelefon(telefon), dvhTelefoner);
        });

        // Legger til ny telefoner
        log.info("Sitter igjen med {} nye telefoner fra dvh", dvhTelefonerMap.size());
        dvhTelefonerMap.forEach((String navident, List<DvhTelefon> dvhTelefoner) -> telefonService.findRessursStateByNavident(navident)
            .ifPresent(ressursState -> oppdaterTelefonOmEndret(ressursState.fid(), navident, null, dvhTelefoner)));
    }

    private void oppdaterTelefonOmEndret (String fid, String navident, Jobbtelefon telefon, List<DvhTelefon> dvhTelefoner) {
        try {
            if (dvhTelefoner != null && !dvhTelefoner.isEmpty()) {
                if (dvhTelefoner.size() > 1) {
                    // Sorterer til en fast rekkefølge med tjenestetelefonnummer først for å unngå skifte mellom flere telefonnummer av samme type
                    dvhTelefoner.sort(Comparator
                        .comparing((DvhTelefon t) -> t.getType() != DvhTelefon.DvhTelefonType.TJENESTETELEFON)
                        .thenComparing(DvhTelefon::getNummer));
                }
                DvhTelefon dvhTelefon = dvhTelefoner.getFirst();

                // Ny telefon eller endret telefon i DVH
                if (telefon == null || !dvhTelefon.getNummer().equals(telefon.nummer()) || !compareTelefonType(dvhTelefon.getType(), telefon.type())) {
                    log.info("Telefon {}, dvhTelefon {}", telefon, dvhTelefon);
                    telefonService.oppdaterRessursStateJobbtelefon(fid, navident, dvhTelefon.toJobbtelefon());
                }
            } else {
                // Telefon er fjernet fra DVH
                log.info("Sletter telefon {}", telefon);
                telefonService.oppdaterRessursStateJobbtelefon(fid, navident, null);
            }
        } catch (Exception e) {
            log.error("Feilet i oppdatering av telefon for fid {}: ", fid, e);
        }
    }

    private Jobbtelefon toJobbtelefon(Telefon telefon) {
        return new Jobbtelefon(telefon.nummer(), telefon.type());
    }

    public void loadLedereFromDvh() {
        log.info("Laster ledere fra dvh");
        try {
            var ledere = dvhOrdsService.getLedere();
            dvhService.saveLedere(ledere);
        } catch (Exception e) {
            log.error("Failure loading ledere data from DVH", e);
        }
    }

    public void loadOrganiseringerFromDvh() {
        loadOrganiseringerFromDvh(false);
    }

    public void loadOrganiseringerFromDvh(boolean forceEvtSrcUpdate) {
        log.info("Laster Organisering fra dvh");
        try {
            var organiseringer = dvhOrdsService.getOrganiseringer();

            dvhService.setOrganiserteOrgenheter(organiseringer); // TODO: Denne burde løses annerledes - via lytting på endringer i organiseringState

            if (forceEvtSrcUpdate) { // Kun leader schedulert trigget eller trigget fra swagger admin
                log.info("Oppdaterer evtsrc med nye Organiseringer fra dvh");
                organiseringService.saveOrganiseringer(organiseringer);
            }
        } catch (Exception e) {
            log.error("ALERT! Failure loading Organisering data from DVH", e);
        }
    }

    public void findOTUtenforHierarki() {
        log.info("Finn OTer utenfor hierarki");
        try {
            otUtenforHierarki.findOTUtenforHierarkiFromDataLast();
        } catch (Exception e) {
            log.error("Feilet i 'Finn OTer utenfor hierarki'", e);
        }
    }

    private boolean isInitialStateReady() {
        return  nomStateRessursListener.hasReadToEndOnStartup()
            && nomStateOrgEnhetListener.hasReadToEndOnStartup()
            && nomStateOrganiseringListener.hasReadToEndOnStartup()
            && nomStateOrgtilknytningListener.hasReadToEndOnStartup()
            && nomStateMasteredRessursListener.hasReadToEndOnStartup()
            && nomStateDelegertLederListener.hasReadToEndOnStartup()
            && nomStateOrgEnhetLederListener.hasReadToEndOnStartup();
    }

    private String durationAsH24(LocalDateTime start) {
        Duration duration = Duration.between(start, LocalDateTime.now());
        return "%02d:%02d:%02d".formatted(duration.toHoursPart(), duration.toMinutesPart(), duration.toSecondsPart());
    }

    private boolean compareTelefonType(DvhTelefon.DvhTelefonType dvhTelefonType, TelefonType telefonType) {
        if (dvhTelefonType.equals(DvhTelefon.DvhTelefonType.TJENESTETELEFON) && telefonType.equals(TelefonType.NAV_TJENESTE_TELEFON)) return true;
        return dvhTelefonType.equals(DvhTelefon.DvhTelefonType.KONTORTELEFON) && telefonType.equals(TelefonType.NAV_KONTOR_TELEFON);
    }
}
