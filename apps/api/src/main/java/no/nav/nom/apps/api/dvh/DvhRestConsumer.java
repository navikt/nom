package no.nav.nom.apps.api.dvh;

import com.fasterxml.jackson.databind.JsonNode;
import io.getunleash.Unleash;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.nom.apps.api.authorize.redigereressurs.AuthorizedNavident;
import no.nav.nom.apps.api.clients.nomproxy.NomProxyClient;
import no.nav.nom.apps.api.common.AppProperties;
import no.nav.nom.apps.api.common.testidenter.ManuelleRessurserOgLedereTilState;
import no.nav.nom.apps.api.dvh.dto.DvhLeder;
import no.nav.nom.apps.api.dvh.dto.DvhOrganisering;
import no.nav.nom.apps.api.dvh.dto.DvhTelefon;
import no.nav.nom.apps.api.organisering.eventsourcing.OrganiseringService;
import no.nav.nom.apps.api.orgenhet.OrgEnhetTypeHandler;
import no.nav.nom.apps.api.orgenhet.eventsourcing.state.OrgEnhetStateListener;
import no.nav.nom.apps.api.orgenhet.implementations.dvh.v2.DvhOrgenhetsLast;
import no.nav.nom.apps.api.orgtilknytning.dvh.EksterneRessurserLoader;
import no.nav.nom.apps.api.orgtilknytning.dvh.KommunaleRessurserLoader;
import no.nav.nom.apps.api.orgtilknytning.dvh.RessursStillingLoader;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.state.OrgtilknytningStateListener;
import no.nav.nom.apps.api.ressurs.eventsourcing.state.RessursStateListener;
import no.nav.nom.apps.api.ressurs.implementations.dvh.DvhRessurs;
import no.nav.nom.apps.api.ressurs.implementations.dvh.RessursHendelseForEventsourceService;
import no.nav.nom.libs.state.orgenhet.NomStateOrgEnhetListener;
import no.nav.nom.libs.state.orgtilknytning.NomStateOrgtilknytningListener;
import no.nav.nom.libs.state.ressurs.NomStateRessursListener;
import no.nav.nom.libs.utils.common.platform.LeaderElection;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static no.nav.nom.apps.api.common.AppConfig.SECURE_LOGS;
import static no.nav.nom.apps.api.ressurs.implementations.dvh.DvhRessurs.RemedyRessursType.ANNEN_STATLIG;

@Slf4j
@Component
@RequiredArgsConstructor
public class DvhRestConsumer {
    public static final String CACHE = "cache-source-dvh";
    public static final String UNLEASH_PRODUSERE_NYE_ORGANISERING_MELDINGER = "nom.organisering.enable_new_topics";
    private final long LOAD_INTERVAL = 30 * 60 * 1000; // sync hvert 30min
    private final AppProperties appProperties;

    private final DvhRepository dvhRepository;
    private final DvhService dvhService;
    private final NomProxyClient nomProxyClient;
    private final RessursStateListener ressursStateListener;
    private final NomStateRessursListener nomStateRessursListener;
    private final OrgtilknytningStateListener orgtilknytningStateListener;
    private final NomStateOrgtilknytningListener nomStateOrgtilknytningListener;
    private final OrgEnhetStateListener orgEnhetStateListener;
    private final NomStateOrgEnhetListener nomStateOrgEnhetListener;
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
                    "Ressurs state: {}  Orgtilkn. state: {}  Orgenhet state: {}",
                    ressursStateListener.initloadComplete(),
                    orgtilknytningStateListener.initloadComplete(),
                    orgEnhetStateListener.initloadComplete()
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
            updateOrgtilknytningerStateFromDvh();

            updateOrgtilknytningerForEksterneFromDvh();

            loadRessurserFromDvh();

            dvhOrgenhetsLast.lastOrgenhetsDataFraDvhTilEvtsrc();

            orgEnhetTypeHandler.updateOrgEnhetsTypeForLeafs();
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
            var jsonRessurser = dvhExchange(appProperties.clients().nomProxy().ansatteBucketsPath());

            val dvhRessurs = StreamSupport.stream(jsonRessurser.get("NOM_ANSATTE_BUCKETS_v2Response").spliterator(), false)
                .map(DvhRessurs::new)
                .distinct()
                .collect(Collectors.groupingBy(DvhRessurs::getPersonIdent))
                .values()
                .stream()
                .peek(l -> {
                    if (l.size() > 1) {
                        l.removeAll(l.stream().filter(Predicate.not(DvhRessurs::getRemedyAktiv)).toList());
                        if (l.size() > 1) {
                            val personIdent = l.get(0).getPersonIdent();
                            val navIdenter = l.stream().map(DvhRessurs::getNavIdent).toList();
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
        log.info("Oppdater skygge Orgtilknytninger for kommunale fra dvh");
        kommunaleRessurserLoader.loadAndProcess();
    }

    public void loadTlfFromDvh() {
        log.info("Laster tlf fra dvh");
        try {
            val jsonTlfNr = dvhExchange(appProperties.clients().nomProxy().tlfPath());
            val tlfRader = StreamSupport.stream(jsonTlfNr.get("NOM_TLF_v1Result").spliterator(), false)
                .filter(this::validateTlf)
                .map(DvhTelefon::new)
                .collect(Collectors.toList());
            log.info("Telefonnr lastet: Antall rader = " + tlfRader.size());
            dvhRepository.saveTlf(tlfRader);
        } catch (Exception e) {
            log.error("Failure loading tlf data from DVH", e);
        }
    }

    private boolean validateTlf(JsonNode jsonNode) {
        val isSecretNumber = !jsonNode.hasNonNull("issecretnumber") || (jsonNode.hasNonNull("issecretnumber") && !jsonNode.get("issecretnumber").asBoolean());
        val userRef1 = jsonNode.get("userreference1");

        return isSecretNumber && (userRef1.asText().startsWith("PT-") || userRef1.asText().startsWith("PK-"));
    }

    public void loadLedereAndOrganiseringerFromDvh() {
        log.info("Laster ledere og Organisering fra dvh");
        try {
            val jsonOrgenheter = dvhExchange(appProperties.clients().nomProxy().orgHierarkiPath());
            val ledere = new LinkedList<DvhLeder>();
            val organiseringer = new LinkedList<DvhOrganisering>();

            StreamSupport.stream(jsonOrgenheter.get("NOM_HRORG_HIERARKI2_v2Response").spliterator(), false)
                .filter(this::determineEnhetActive)
                .filter(this::determineEnhetNotIgnored)
                .forEach(e -> {
                    if (validateOrganisering(e)) {
                        organiseringer.add(new DvhOrganisering(e));
                    }

                    if (validateLeder(e)) {
                        ledere.add(new DvhLeder(e));
                    }
                });

            dvhService.saveLedere(ledere);
            dvhService.saveOrganiseringerAndSetOrganiserteOrgenheter(organiseringer);

            if (unleash.isEnabled(UNLEASH_PRODUSERE_NYE_ORGANISERING_MELDINGER)) {
                organiseringService.saveOrganiseringer(organiseringer);
            }
        } catch (Exception e) {
            log.error("Failure loading ledere og Organisering data from DVH", e);
        }
    }

    private boolean determineEnhetNotIgnored(JsonNode jsonNode) {
        // 'deltar i rekruttering' og 'nav ventelønn' er kun for intern bruk i agresso.
        val orgnodeKode = jsonNode.get("ORGNODE_KODE").asText();
        val deltarIRekruttering = orgnodeKode.matches("0+");
        val navVenteloenn = orgnodeKode.equals("9999920");
        return !(deltarIRekruttering || navVenteloenn);
    }

    private JsonNode dvhExchange(String urlPath) {
        return nomProxyClient.getWebClient().get()
            .uri(urlPath)
            .retrieve()
            .bodyToMono(JsonNode.class)
            .block();

    }

    private boolean determineEnhetActive(JsonNode e) {
        return e.get("ORGNODE_STATUS").asText().equals("N");
    }

    private boolean validateOrganisering(JsonNode e) {
        final var validate = e.hasNonNull("ORGNODE_KODE_OVER") && e.hasNonNull("ORGNODE_KODE")
            && e.hasNonNull("ORGNODE_NIVA_OVER") && e.hasNonNull("ORGNODE_NIVA");
        if (!validate) {
            log.info("Ugyldig organisering fra dvh, ORGNODE_KODE_OVER: {}, ORGNODE_KODE: {}", getAsText(e, "ORGNODE_KODE_OVER"), getAsText(e, "ORGNODE_KODE"));
        }
        return validate;
    }

    private boolean validateLeder(JsonNode e) {
        final var validate = e.hasNonNull("LEDER_NAVIDENT") && e.hasNonNull("ORGNODE_KODE") && e.hasNonNull("ORGNODE_NIVA");
        if (!validate) {
            log.info("Ugyldig leder fra dvh, LEDER_NAVIDENT: {}, ORGNODE_KODE: {}", getAsText(e, "LEDER_NAVIDENT"), getAsText(e, "ORGNODE_KODE"));
        }
        return validate;
    }

    private String getAsText(JsonNode e, String field) {
        JsonNode jsonNode = e.get(field);
        return jsonNode == null ? "<null>" : jsonNode.asText();
    }

    private boolean isInitialStateReady() {
        return ressursStateListener.initloadComplete()
            && orgtilknytningStateListener.initloadComplete()
            && orgEnhetStateListener.initloadComplete()
            && nomStateRessursListener.hasReadToEndOnStartup()
            && nomStateOrgEnhetListener.hasReadToEndOnStartup()
            && nomStateOrgtilknytningListener.hasReadToEndOnStartup();
    }

    private String durationAsH24(LocalDateTime start) {
        Duration duration = Duration.between(start, LocalDateTime.now());
        return "%02d:%02d:%02d".formatted(duration.toHoursPart(), duration.toMinutesPart(), duration.toSecondsPart());
    }
}
