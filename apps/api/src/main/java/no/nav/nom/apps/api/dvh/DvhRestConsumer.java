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
import no.nav.nom.apps.api.orgtilknytning.dvh.KommunaleRessurserLoader;
import no.nav.nom.apps.api.orgtilknytning.dvh.RessursStillingLoader;
import no.nav.nom.apps.api.orgtilknytning.job.OTUtenforHierarki;
import no.nav.nom.apps.api.ressurs.implementations.dvh.DvhRessurs;
import no.nav.nom.apps.api.ressurs.implementations.dvh.RessursHendelseForEventsourceService;
import no.nav.nom.apps.api.telefon.TelefonService;
import no.nav.nom.contract.nom.TelefonType;
import no.nav.nom.libs.state.authorize.NomStateDelegertLederListener;
import no.nav.nom.libs.state.organisering.NomStateOrganiseringListener;
import no.nav.nom.libs.state.orgenhet.NomStateOrgEnhetListener;
import no.nav.nom.libs.state.orgtilknytning.NomStateOrgtilknytningListener;
import no.nav.nom.libs.state.ressurs.NomStateRessursListener;
import no.nav.nom.libs.state.ressurs.mastered.NomStateMasteredRessursListener;
import no.nav.nom.libs.utils.common.platform.LeaderElection;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static no.nav.nom.apps.api.common.AppConfig.SECURE_LOGS;
import static no.nav.nom.apps.api.ressurs.implementations.dvh.DvhRessurs.RemedyRessursType.ANNEN_STATLIG;

@Slf4j
@Component
@RequiredArgsConstructor
public class DvhRestConsumer {
    public static final String CACHE = "cache-source-dvh";
    private final long LOAD_INTERVAL = 30 * 60 * 1000; // sync hvert 30min

    private final DvhRepository dvhRepository;
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
    private final KommunaleRessurserLoader kommunaleRessurserLoader;
    private final AuthorizedNavident authorizedNavident;

    private final ManuelleRessurserOgLedereTilState manuelleRessurserOgLedereTilState;

    private final DvhStartupLoadMonitor dvhStartupLoadMonitor;
    private final NomStateDelegertLederListener nomStateDelegertLederListener;
    private final DvhOrdsService dvhOrdsService;

    private final OTUtenforHierarki otUtenforHierarki;
    private final TelefonService telefonService;
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
                    "Ressurs state: {}  Orgtilkn. state: {} Organisering state: {} Orgenhet state: {}",
                    nomStateMasteredRessursListener.hasReadToEndOnStartup(),
                    nomStateOrgtilknytningListener.hasReadToEndOnStartup(),
                    nomStateOrganiseringListener.hasReadToEndOnStartup(),
                    nomStateOrgEnhetListener.hasReadToEndOnStartup()
                );
            } catch (Exception logMe) {
                log.error("Waiting for initialState last feilet", logMe);
            }
        }

        log.info("State topics er lest, fortsetter schedulert oppdatering fra DVH etter {}", durationAsH24(start));

        var ledereLastOk = loadLedereAndOrganiseringerFromDvh();
        loadTlfFromDvh();

        manuelleRessurserOgLedereTilState.oppdater();

        authorizedNavident.oppdaterLedereForOrgenheter();

        if (!dvhStartupLoadMonitor.hasCompleted() && ledereLastOk) { // Pod kan markeres som klar etter oppstart
            dvhStartupLoadMonitor.setCompleted();
            log.info("Initial oppdatering fra DVH er klar. Etter {}", durationAsH24(start));
        }

        updateShadowOrgtilknytningerForKommunaleFromDvh();

        if (leaderElection.isLeader()) { // Oppdater state fra dvh - holder at en Pod gjør dette
            dvhOrgenhetsLast.lastOrgenhetsDataFraDvhTilEvtsrc();

            // Sikre 'riktig orgenhet+organisering' før Sjekk på ot utenfor hierarki
            loadLedereAndOrganiseringerFromDvh();
            // Finn OTer utenfor hierarki - og autoflytt om mulig
            findOTUtenforHierarki();

            orgEnhetTypeHandler.updateOrgEnhetsTypeForLeafs();

            loadRessurserFromDvh();

            updateOrgtilknytningerStateFromDvh();

            updateOrgtilknytningerForEksterneFromDvh();
        }

        log.info("Ferdig med schedulert oppdatering fra DVH. Tok {}", durationAsH24(start));
    }

    private boolean skipScheduledRunIfDuringDvhUpdate() {
        // Vill alltid kjøres på deploy
        if (!dvhStartupLoadMonitor.hasCompleted()) {
            log.info("Oppstartsinnlesning - kan ikke skippe schedulert oppdatering");
            return false;
        }
        // Dvh oppdateres ifølge Henrik ca. 02.00. 14.00 og 18.00.
        // Har observert inkonsistens i periodene 01:30-03:00, 12:30-15:00, og 17:00-18:30
        // Velger å kun kjøre i timene 22, 05, 10 og 16
        // Kan kjøres manuelt fra Swagger/admin
        List<Integer> runHours = Arrays.asList(5, 10, 16, 22);
        if (runHours.contains(LocalDateTime.now().getHour())) {
            log.info("Kjører schedulert oppdatering fra DVH da timen {} er i runHours {}", LocalDateTime.now().getHour(), runHours);
            return false;
        }
        log.info("Skipper schedulert oppdatering fra DVH da timen {} er ikke er i runHours {}", LocalDateTime.now().getHour(), runHours);
        return true;
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
                            var personIdent = l.get(0).getPersonIdent();
                            var navIdenter = l.stream().map(DvhRessurs::getNavIdent).toList();
                            log.warn(SECURE_LOGS, "Forkastet lesing av ansatt med personident " + personIdent + " har flere aktive navidenter: " + navIdenter);
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
            dvhRepository.saveAnnenStatNavIdenter(findAnnenStatNavIdenter(dvhRessurs));
        } catch (Exception e) {
            log.error("Feilet i last av ressurser fra dvh", e);
        }
    }

    private Set<String> findAnnenStatNavIdenter(List<DvhRessurs> dvhRessurs) {
        return dvhRessurs.stream()
            .filter(r -> r.getRemedyAnsattType().equals(ANNEN_STATLIG.beskrivelse))
            .map(DvhRessurs::getNavIdent)
            .collect(Collectors.toSet());
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

    public void updateShadowOrgtilknytningerForKommunaleFromDvh() {
        try {
            log.info("Oppdater skygge Orgtilknytninger for kommunale fra dvh");
            kommunaleRessurserLoader.loadAndProcess();
        }  catch (Exception e) {
            log.error("Failure loading skygge-orgtilknytninger data from DVH", e);
        }
    }

    public void loadTlfFromDvh() {
        log.info("Laster tlf fra dvh");
        try {
            var tlfRader = dvhOrdsService.getTelefoner();
            if (unleash.isEnabled("nom-api.ny-dvh-telefon-persistering")) {
                List<DvhTelefon> dvhTelefonerMedEndring = getDvhTelefoner(tlfRader);

                if (!dvhTelefonerMedEndring.isEmpty()) telefonService.oppdaterRessursStateJobbtelefonBulk(dvhTelefonerMedEndring);
            }
            log.info("Telefonnr lastet: Antall rader = " + tlfRader.size());
            dvhRepository.saveTlf(tlfRader);
        } catch (Exception e) {
            log.error("Failure loading tlf data from DVH", e);
        }
    }

    private List<DvhTelefon> getDvhTelefoner(List<DvhTelefon> tlfRader) {
        List<DvhTelefon> dvhTelefonerMedEndring = new ArrayList<>();
        var sortedTlfRader = tlfRader.stream().collect(Collectors.groupingBy(DvhTelefon::getNavIdent));
        sortedTlfRader.forEach((navident, dvhTelefoner) -> {
            if (dvhTelefoner.size() > 1) {
                if (dvhTelefoner.stream().allMatch(dvhTelefon -> dvhTelefon.getType().equals(DvhTelefon.DvhTelefonType.TJENESTETELEFON))) dvhTelefoner.sort(Comparator.comparing(DvhTelefon::getNummer));
                else if (dvhTelefoner.stream().allMatch(dvhTelefon -> dvhTelefon.getType().equals(DvhTelefon.DvhTelefonType.KONTORTELEFON))) dvhTelefoner.sort(Comparator.comparing(DvhTelefon::getNummer));
                else dvhTelefoner.sort(Comparator.comparing((DvhTelefon t) -> t.getType() != DvhTelefon.DvhTelefonType.TJENESTETELEFON));
            }

            var ressursStateTelefonnummer = telefonService.getRessursStateTelefonnummer(navident);
            if (isNull(ressursStateTelefonnummer)
                || !ressursStateTelefonnummer.nummer().equals(dvhTelefoner.getFirst().getNummer())
                || !compareTelefonType(dvhTelefoner.getFirst().getType(), ressursStateTelefonnummer.type())) {
                dvhTelefonerMedEndring.add(dvhTelefoner.getFirst());
            }
        });
        return dvhTelefonerMedEndring;
    }

    public boolean loadLedereAndOrganiseringerFromDvh() {
        log.info("Laster ledere og Organisering fra dvh");
        try {
            var ledere = dvhOrdsService.getLedere();
            var organiseringer = dvhOrdsService.getOrganiseringer();

            dvhService.saveLedere(ledere);
            dvhService.setOrganiserteOrgenheter(organiseringer); // TODO: Denne burde løses annerledes - via lytting på endringer i organiseringState

            organiseringService.saveOrganiseringer(organiseringer);
            return true;
        } catch (Exception e) {
            log.error("Failure loading ledere og Organisering data from DVH", e);
        }
        return false;
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
            && nomStateDelegertLederListener.hasReadToEndOnStartup();
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
