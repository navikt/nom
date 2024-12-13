package no.nav.nom.apps.api.dvh;

import io.getunleash.Unleash;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.authorize.redigereressurs.AuthorizedNavident;
import no.nav.nom.apps.api.clients.dvhords.DvhOrdsService;
import no.nav.nom.apps.api.common.testidenter.ManuelleRessurserOgLedereTilState;
import no.nav.nom.apps.api.dvh.dto.DvhLeder;
import no.nav.nom.apps.api.dvh.dto.DvhOrganisering;
import no.nav.nom.apps.api.dvh.dto.DvhTelefon;
import no.nav.nom.apps.api.organisering.eventsourcing.OrganiseringService;
import no.nav.nom.apps.api.orgenhet.OrgEnhetTypeHandler;
import no.nav.nom.apps.api.orgenhet.implementations.dvh.v2.DvhOrgenhetV2;
import no.nav.nom.apps.api.orgenhet.implementations.dvh.v2.DvhOrgenhetsLast;
import no.nav.nom.apps.api.orgtilknytning.dvh.EksterneRessurserLoader;
import no.nav.nom.apps.api.orgtilknytning.dvh.KommunaleRessurserLoader;
import no.nav.nom.apps.api.orgtilknytning.dvh.RessursStillingLoader;
import no.nav.nom.apps.api.ressurs.implementations.dvh.DvhRessurs;
import no.nav.nom.apps.api.ressurs.implementations.dvh.RessursHendelseForEventsourceService;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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
    private final Unleash unleash;
    private final NomStateDelegertLederListener nomStateDelegertLederListener;
    private final DvhOrdsService dvhOrdsService;
    private final DvhOldTdvService dvhOldTdvService;

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

        loadLedereAndOrganiseringerFromDvh();
        loadTlfFromDvh();

        updateShadowOrgtilknytningerForKommunaleFromDvh();
        manuelleRessurserOgLedereTilState.oppdater();

        authorizedNavident.oppdaterLedereForOrgenheter();

        if (!dvhStartupLoadMonitor.hasCompleted()) { // Pod kan markeres som klar etter oppstart
            dvhStartupLoadMonitor.setCompleted();
            log.info("Initial oppdatering fra DVH er klar. Etter {}", durationAsH24(start));
        }

        if (leaderElection.isLeader()) { // Oppdater state fra dvh - holder at en Pod gjør dette
            dvhOrgenhetsLast.lastOrgenhetsDataFraDvhTilEvtsrc();

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
            List<DvhRessurs> dvhRessurser;
            if (unleash.isEnabled("nom.dvh.last_ressurser_fra_ords")) {
                dvhRessurser = dvhOrdsService.getRessurser();
            } else {
                dvhRessurser = dvhOldTdvService.getTdvRessurser();
            }

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

            getOrdsJsonRessurserAndCompare(dvhRessurs);

            ressursHendelseForEventsourceService.handterRessursIderFraDvh(dvhRessurs);
            dvhRepository.saveAnnenStatNavIdenter(findAnnenStatNavIdenter(dvhRessurs));
        } catch (Exception e) {
            log.error("Feilet i last av ressurser fra dvh", e);
        }
    }

    private void getOrdsJsonRessurserAndCompare(List<DvhRessurs> tdvDvhRessurser) {
        try {
            var ordsDvhRessurser = dvhOrdsService.getRessurser().stream()
                .distinct()
                .collect(Collectors.groupingBy(DvhRessurs::getPersonIdent))
                .values()
                .stream()
                .peek(l -> {
                    if (l.size() > 1) {
                        l.removeAll(l.stream().filter(Predicate.not(DvhRessurs::getRemedyAktiv)).toList());
                        if (l.size() > 1) {
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

            boolean erLikt = true;
            if (ordsDvhRessurser.size() != tdvDvhRessurser.size()) {
                log.error("Antall ressurser fra TDV og ORDS er ulik. TDV: {}, ORDS: {}", tdvDvhRessurser.size(), ordsDvhRessurser.size());
                erLikt = false;
            }

            var ordsMap = ordsDvhRessurser.stream().collect(Collectors.toMap(DvhRessurs::getNavIdent, Function.identity()));
            int idxSkip = tdvDvhRessurser.size()/20;
            for (int i = 0; i < 20; i++) {
                int idx = i * idxSkip;
                var dvhRessursTdv = tdvDvhRessurser.get(idx);
                var dvhRessursOrds = ordsMap.get(dvhRessursTdv.getNavIdent());
                if (!DvhRessurs.compare(dvhRessursTdv, dvhRessursOrds)) {
                    log.error("Ressurs fra TDV og ORDS er ulik. \n\tTDV: \n\t{}, \n\tORDS: \n\t{}"
                        , dvhRessursTdv.getRawData().toPrettyString()
                        , dvhRessursOrds.getRawData().toPrettyString());
                    erLikt = false;
                }
            }
            if (erLikt) {
                log.info("Last av ressurser fra ORDS (nom_ords_ansatte_buckets.json) er lik last fra TDV (NOM_ANSATTE_BUCKETS_v2)");
            } else {
                log.error("Last av ressurser fra ORDS (nom_ords_ansatte_buckets.json) er annerledes enn last fra TDV (NOM_ANSATTE_BUCKETS_v2)");
            }
        } catch (Exception e) {
            log.error("Feilet i last av ressurser fra ORDS (nom_ords_ansatte_buckets.json)", e);
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
            List<DvhTelefon> tlfRader;
            if (unleash.isEnabled("nom.dvh.last_tlf_fra_ords")) {
                tlfRader = dvhOrdsService.getTelefoner();
            } else {
                tlfRader = dvhOldTdvService.getTdvTelefoner();
            }

            getOrdsTelefonerAndCompare(tlfRader);

            log.info("Telefonnr lastet: Antall rader = " + tlfRader.size());
            dvhRepository.saveTlf(tlfRader);
        } catch (Exception e) {
            log.error("Failure loading tlf data from DVH", e);
        }
    }

    private void getOrdsTelefonerAndCompare(List<DvhTelefon> tdvTelefoner) {
        try {
            var ordsTelefoner = dvhOrdsService.getTelefoner();

            boolean erLikt = true;
            if (ordsTelefoner.size() != tdvTelefoner.size()) {
                log.error("Antall telefoner fra TDV og ORDS er ulik. TDV: {}, ORDS: {}", tdvTelefoner.size(), ordsTelefoner.size());
                erLikt = false;
            }

            var ordsMap = ordsTelefoner.stream().collect(Collectors.toMap(DvhTelefon::geUniqueKey, Function.identity()));
            int idxSkip = tdvTelefoner.size()/20;
            for (int i = 0; i < 20; i++) {
                int idx = i * idxSkip;
                var dvhTelefonTdv = tdvTelefoner.get(idx);
                var dvhTelefonOrds = ordsMap.get(dvhTelefonTdv.geUniqueKey());
                if (dvhTelefonOrds == null || !DvhTelefon.compare(dvhTelefonTdv, dvhTelefonOrds)) {
                    log.error("Telefon fra TDV og ORDS er ulik. \n\tTDV: \n\t{}, \n\tORDS: \n\t{}"
                        , dvhTelefonTdv
                        , dvhTelefonOrds);
                    erLikt = false;
                }
            }
            if (erLikt) {
                log.info("Last av telefoner fra ORDS (NOM_ORDS_TLF.json) er lik last fra TDV (NOM_TLF_v1)");
            } else {
                log.error("Last av telefoner fra ORDS (NOM_ORDS_TLF.json) er annerledes enn last fra TDV (NOM_TLF_v1)");
            }
        } catch (Exception e) {
            log.error("Feilet i last av telefoner fra ORDS (NOM_ORDS_TLF.json)", e);
        }
    }


    public void loadLedereAndOrganiseringerFromDvh() {
        log.info("Laster ledere og Organisering fra dvh");
        try {
            List<DvhLeder> ledere;
            List<DvhOrganisering> organiseringer;
            if (unleash.isEnabled("nom.dvh.last_ledere_fra_ords")) {
                ledere = dvhOrdsService.getLedere();
            } else {
                ledere = dvhOldTdvService.getTdvLedere();
            }
            if (unleash.isEnabled("nom.dvh.last_organiseringer_fra_ords")) {
                organiseringer = dvhOrdsService.getOrganiseringer();
            } else {
                organiseringer = dvhOldTdvService.getTdvOrganiseringer();
            }

            dvhService.saveLedere(ledere);
            dvhService.setOrganiserteOrgenheter(organiseringer); // TODO: Denne burde løses annerledes - via lytting på endringer i organiseringState

            getOrdsJsonLedereAndOrganiseringAndCompare(ledere, organiseringer);

            organiseringService.saveOrganiseringer(organiseringer);
        } catch (Exception e) {
            log.error("Failure loading ledere og Organisering data from DVH", e);
        }
    }

    public void getOrdsJsonLedereAndOrganiseringAndCompare(List<DvhLeder> tdvLedere, List<DvhOrganisering> tdvOrganiseringer) {
        try {
            var ordsLedere = dvhOrdsService.getLedere();
            var ordsOrganiseringer = dvhOrdsService.getOrganiseringer();

            boolean erLikt = true;
            if (ordsLedere.size() != tdvLedere.size()) {
                log.error("Antall ledere fra TDV og ORDS er ulik. TDV: {}, ORDS: {}", tdvLedere.size(), ordsLedere.size());
                erLikt = false;
            }

            var ordsLederMap = ordsLedere.stream().collect(Collectors.toMap(DvhLeder::getUniqueKey, Function.identity()));
            int idxSkip = tdvLedere.size()/20;
            for (int i = 0; i < 20; i++) {
                int idx = i * idxSkip;
                var tdvLeder = tdvLedere.get(idx);
                var ordsLeder = ordsLederMap.get(tdvLeder.getUniqueKey());
                if (ordsLeder == null || !DvhLeder.compare(tdvLeder, ordsLeder)) {
                    log.error("Leder fra TDV og ORDS er ulik. \n\tTDV: \n\t{}, \n\tORDS: \n\t{}"
                        , tdvLeder
                        , ordsLeder);
                    erLikt = false;
                }
            }

            if (ordsOrganiseringer.size() != tdvOrganiseringer.size()) {
                log.error("Antall organiseringer fra TDV og ORDS er ulik. TDV: {}, ORDS: {}", tdvOrganiseringer.size(), ordsOrganiseringer.size());
                erLikt = false;
            }

            var ordsOrganiseringMap = ordsOrganiseringer.stream().collect(Collectors.toMap(DvhOrganisering::uniqueId, Function.identity()));
            idxSkip = tdvOrganiseringer.size()/20;
            for (int i = 0; i < 20; i++) {
                int idx = i * idxSkip;
                var tdvOrganisering = tdvOrganiseringer.get(idx);
                var ordsOrganisering = ordsOrganiseringMap.get(tdvOrganisering.uniqueId());
                if (ordsOrganisering == null || !DvhOrganisering.compare(tdvOrganisering, ordsOrganisering)) {
                    log.error("Organisering fra TDV og ORDS er ulik. \n\tTDV: \n\t{}, \n\tORDS: \n\t{}"
                        , tdvOrganisering
                        , ordsOrganisering);
                    erLikt = false;
                }
            }

            if (erLikt) {
                log.info("Last av ledere og organisering fra ORDS (nom_ords_hrorg_hierarki.json) er lik last fra TDV (NOM_ORDS_HRORG_HIERARKI)");
            } else {
                log.error("Last av ledere og organisering fra ORDS (nom_ords_hrorg_hierarki.json) er annerledes enn last fra TDV (NOM_ORDS_HRORG_HIERARKI)");
            }
        } catch (Exception e) {
            log.error("Feilet i last av  og organisering fra ORDS (nom_ords_hrorg_hierarki.json)", e);
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
}
