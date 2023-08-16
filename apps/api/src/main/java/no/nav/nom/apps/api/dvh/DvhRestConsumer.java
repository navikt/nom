package no.nav.nom.apps.api.dvh;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.nom.apps.api.clients.nomproxy.NomProxyClient;
import no.nav.nom.apps.api.dvh.dto.DvhLeder;
import no.nav.nom.apps.api.dvh.dto.DvhOrganisering;
import no.nav.nom.apps.api.dvh.dto.DvhTelefon;
import no.nav.nom.apps.api.orgenhet.OrgEnhetTypeAndNivaaResolver;
import no.nav.nom.apps.api.orgenhet.implementations.dvh.v2.DvhDataLoader;
import no.nav.nom.apps.api.orgtilknytning.dvh.RessursStillingLoader;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.state.OrgtilknytningStateListener;
import no.nav.nom.apps.api.ressurs.RessursHendelseForEventsourceService;
import no.nav.nom.apps.api.ressurs.eventsourcing.state.RessursStateListener;
import no.nav.nom.apps.api.ressurs.implementations.dvh.DvhRessurs;
import no.nav.nom.libs.utils.common.platform.LeaderElection;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@Component
@RequiredArgsConstructor
public class DvhRestConsumer {
    public static final String CACHE = "cache-source-dvh";
    // increase while db is slow
    private final long LOAD_INTERVAL = 30 * 60 * 1000; // sync hvert 30min
    private final DvhRepository dvhRepository;
    private final DvhService dvhService;
    private final NomProxyClient nomProxyClient;
    private final AtomicBoolean initialLoadComplete = new AtomicBoolean(false);
    private final RessursStateListener ressursStateListener;
    private final OrgtilknytningStateListener orgtilknytningStateListener;
    private final RessursHendelseForEventsourceService ressursHendelseForEventsourceService;
    private final LeaderElection leaderElection;
    private final DvhDataLoader dvhDataLoader;
    private final OrgEnhetTypeAndNivaaResolver orgEnhetTypeAndNivaaResolver;

    private final RessursStillingLoader ressursStillingLoader;

    public boolean initloadComplete() {
        return initialLoadComplete.get();
    }


    @Scheduled(fixedDelay = LOAD_INTERVAL)
    public void scheduledLoadKoblingerLedereOgTlfFromDvh() {
        while (!ressursStateListener.initloadComplete() && !orgtilknytningStateListener.initloadComplete()) {
            try {
                Thread.sleep(5_000);
            } catch (Exception ignored) {
            }
        }
        loadKoblingerLedereOgTlfFromDvh();

        //TODO: Foreløpig settes den bak loadKoblinger siden den er avhengig at koblinger er til stede.
        resolveOrgEnhetsTypes();
    }

    @Scheduled(cron = "${app.dvh-load.cron-ressurs-og-orgenhet}")
    public void scheduledLoadRessurserOgOrgEnheterFromDvh() {
        while (!ressursStateListener.initloadComplete()) {
            try {
                Thread.sleep(5_000);
            } catch (Exception ignored) {
            }
        }
        loadRessurserOgOrgEnheter();
    }

    public void loadRessurserOgOrgEnheter() {
        if (leaderElection.isLeader()) {
            log.info("Laster ressurser fra dvh");
            loadRessurserFromDvh();

            log.info("Laster orgenheter fra dvh");
            dvhDataLoader.load();
        }
    }


    public void loadKoblingerLedereOgTlfFromDvh() {
        try {
            log.info("Laster Orgtilnytninger fra dvh");
            loadOrgtilknytningerFromDvh();
            log.info("Laster ledere og Organisering fra dvh");
            loadLedereAndOrganiseringerFromDvh();

            if (!initialLoadComplete.get()) {
                initialLoadComplete.set(true);
                log.info("Dvh load complete");
            }

            // manglende tlf er ikke kritisk nok til å stoppe applikasjonen fra å være "ready"
            log.info("Laster tlf fra dvh");
            loadTlfFromDvh();
        } catch (Exception e) {
            log.error("Failure loading data from DVH", e);
        }
    }

    @SneakyThrows
    public void loadRessurserFromDvh() {
        var jsonRessurser = dvhExchange("/proxy/dvh/json/Hr/NOM2/NOM_ANSATTE_BUCKETS_v1");

        val dvhRessurs = StreamSupport.stream(jsonRessurser.get("NOM_ANSATTE_BUCKETS_v1Result").spliterator(), false)
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
                            log.warn("Forkastet lesing av ansatt med personident " + personIdent + " har flere aktive navidenter: " + navIdenter);
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
    }

    private void loadOrgtilknytningerFromDvh() {
        if (initialLoadComplete.get() && leaderElection.isLeader()) {
            lastOrgtilknytningerTilEvtsrc();
        }
    }

    public void lastOrgtilknytningerTilEvtsrc() {
        try {
            ressursStillingLoader.loadAndProcess();
        } catch(Exception e) {
            log.error("Feilet i last av ressursStillinger", e);
        }
    }

    private void loadTlfFromDvh() {
        val jsonTlfNr = dvhExchange("/proxy/dvh/json/Hr/NOM2/NOM_TLF_v1");
        val tlfRader = StreamSupport.stream(jsonTlfNr.get("NOM_TLF_v1Result").spliterator(), false)
                .filter(this::validateTlf)
                .map(DvhTelefon::new)
                .collect(Collectors.toList());
        log.info("Telefonnr lastet: Antall rader = " + tlfRader.size());
        dvhRepository.saveTlf(tlfRader);
    }

    private boolean validateTlf(JsonNode jsonNode) {
        val isSecretNumber = !jsonNode.hasNonNull("issecretnumber") || (jsonNode.hasNonNull("issecretnumber") && !jsonNode.get("issecretnumber").asBoolean());
        val userRef1 = jsonNode.get("userreference1");

        return isSecretNumber && (userRef1.asText().startsWith("PT-") || userRef1.asText().startsWith("PK-"));
    }

    private void loadLedereAndOrganiseringerFromDvh() {
        val jsonOrgenheter = dvhExchange("/proxy/dvh/json/Hr/NOM2/NOM_HRORG_HIERARKI2_v2");
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
        dvhRepository.saveOrganiseringer(organiseringer);
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

    private void resolveOrgEnhetsTypes(){
        orgEnhetTypeAndNivaaResolver.resolve();
    }
}
