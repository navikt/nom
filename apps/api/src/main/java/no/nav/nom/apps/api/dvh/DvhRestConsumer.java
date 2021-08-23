package no.nav.nom.apps.api.dvh;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.nom.apps.api.common.AppProperties;
import no.nav.nom.apps.api.common.security.TokenRepository;
import no.nav.nom.apps.api.dvh.dto.DvhKobling;
import no.nav.nom.apps.api.dvh.dto.DvhLeder;
import no.nav.nom.apps.api.dvh.dto.DvhOrganisasjonsenhet;
import no.nav.nom.apps.api.dvh.dto.DvhOrganisering;
import no.nav.nom.apps.api.dvh.dto.DvhRessurs;
import no.nav.nom.libs.utils.common.platform.LeaderElection;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.LivenessState;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@Component
@RequiredArgsConstructor
public class DvhRestConsumer {

    public static final String CACHE = "cache-source";
    // increase while db is slow
    private final long LOAD_INTERVAL = 30 * 60 * 1000; // sync hvert 30min

    private final TokenRepository tokenRepository;
    private final DvhRepository dvhRepository;
    private final RestTemplate restTemplate;
    private final AppProperties appProperties;
    private final AtomicBoolean initialLoadComplete = new AtomicBoolean(false);
    private final ApplicationEventPublisher appEventPublisher;
    private final RessursHendelseService ressursHendelseService;
    private final OrgenhetHendelseService orgenhetHendelseService;
    private final LeaderElection leaderElection;

    @Scheduled(fixedDelay = LOAD_INTERVAL)
    public void loadFromDvh() {
        try {
            loadRessurserFromDvh();
            loadOrgenhetFromDvh();
            loadKoblingerFromDvh();
        } catch (Exception e) {
            boolean firstLoad = !initialLoadComplete.get();
            if (firstLoad) {
                log.error("Failure loading data from DVH - setting LivenessState.BROKEN", e);
                AvailabilityChangeEvent.publish(appEventPublisher, CACHE, LivenessState.BROKEN);
            } else {
                log.error("Failure loading data from DVH", e);
            }
            return;
        }

        if (!initialLoadComplete.get()) {
            initialLoadComplete.set(true);
            AvailabilityChangeEvent.publish(appEventPublisher, CACHE, ReadinessState.ACCEPTING_TRAFFIC);
            log.info("Changing to ReadinessState.ACCEPTING_TRAFFIC");
        }
    }

    @SneakyThrows
    private void loadRessurserFromDvh() {
        if (!leaderElection.isLeader()) {
            return;
        }
        var jsonRessurser = dvhExchange("/proxy/ressurser");

        val dvhRessurs = StreamSupport.stream(jsonRessurser.get("NOM_ANSATTE_BUCKETS_v1Result").spliterator(), false)
                .map(DvhRessurs::new)
                .distinct()
                .collect(Collectors.groupingBy(DvhRessurs::getPersonIdent))
                .values().stream()
                .peek(l -> {
                    if(l.size() > 1){
                        l.removeAll(l.stream().filter(Predicate.not(DvhRessurs::getRemedyAktiv)).toList());
                        if(l.size() > 1){
                            val personIdent = l.get(0).getPersonIdent();
                            val navIdenter = l.stream().map(DvhRessurs::getNavIdent).toList();
                            log.warn("Forkastet lesing av ansatt med personident "+personIdent+" har flere aktive navidenter: "+navIdenter);
                            l.clear();
                        }
                    }
                })
                .flatMap(Collection::stream)
                .collect(Collectors.groupingBy(DvhRessurs::getNavIdent))
                .values().stream()
                .filter(l -> l.size() == 1)
                .flatMap(Collection::stream)
                .toList();

        ressursHendelseService.handterRessursIderFraDvh(dvhRessurs);
    }

    private void loadKoblingerFromDvh() {
        val jsonStillinger = dvhExchange("/proxy/stillinger");
        val koblinger = StreamSupport.stream(jsonStillinger.get("stilling2nomResult").spliterator(), false)
                .filter(this::validateKobling)
                .map(DvhKobling::new)
                .collect(Collectors.toList());

        dvhRepository.saveKoblinger(koblinger);
    }

    private void loadOrgenhetFromDvh() {
        val jsonOrgenheter = dvhExchange("/proxy/enheter/v2");
        val orgenheter = new ConcurrentHashMap<String, Map<String, DvhOrganisasjonsenhet>>();
        val ledere = new LinkedList<DvhLeder>();
        val organiseringer = new LinkedList<DvhOrganisering>();

        StreamSupport.stream(jsonOrgenheter.get("NOM_HRORG_HIERARKI2_v2Response").spliterator(), false)
                .filter(this::determineEnhetActive)
                .filter(this::determineEnhetNotIgnored)
                .forEach(e -> {
                    if (validateEnhet(e)) {
                        val orgenhet = new DvhOrganisasjonsenhet(e);
                        if(!orgenheter.containsKey(orgenhet.getOrgNiv())){
                            orgenheter.put(orgenhet.getOrgNiv(), new ConcurrentHashMap<>(1));
                        }

                        orgenheter.get(orgenhet.getOrgNiv()).put(orgenhet.getAgressoId(), orgenhet);

                    }

                    if (validateOrganisering(e)) {
                        organiseringer.add(new DvhOrganisering(e));
                    }

                    if (validateLeder(e)) {
                        ledere.add(new DvhLeder(e));
                    }
                });

        if (leaderElection.isLeader()) {
            orgenhetHendelseService.handterOrgenhetFraDvh(orgenheter.values().stream()
                    .flatMap(map -> map.values().stream())
                    .collect(Collectors.toList())
            );
        }

        dvhRepository.saveOrganisasjonsenheter(orgenheter);
        dvhRepository.saveLedere(ledere);
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
        val headers = new HttpHeaders();
        headers.setBearerAuth(tokenRepository.getAadToken(appProperties.getProxy().getAadClientId()));

        return restTemplate.exchange(appProperties.getProxy().getBaseUrl() + urlPath, HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class).getBody();
    }

    private boolean determineEnhetActive(JsonNode e) {
        return e.get("ORGNODE_STATUS").asText().equals("N");
    }

    private boolean validateEnhet(JsonNode e) {
        val validate = e.hasNonNull("ORGNODE_KODE") && e.hasNonNull("ORGNODE_NAVN") && e.hasNonNull("ORGNODE_NIVA");
        if (validate) {
            return true;
        } else {
            log.info("Ugyldig enhet fra dvh, ORGNODE_KODE: " + getAsText(e, "ORGNODE_KODE") + ", ORGNODE_NAVN: " + getAsText(e, "ORGNODE_NAVN"));
            return false;
        }
    }

    private boolean validateOrganisering(JsonNode e) {
        val validate = e.hasNonNull("ORGNODE_KODE_OVER") && e.hasNonNull("ORGNODE_KODE") &&
                e.hasNonNull("ORGNODE_NIVA_OVER") && e.hasNonNull("ORGNODE_NIVA");
        if (validate) {
            return true;
        } else {
            log.info("Ugyldig organisering fra dvh, ORGNODE_KODE_OVER: " + getAsText(e, "ORGNODE_KODE_OVER") + ", ORGNODE_KODE: " + getAsText(e, "ORGNODE_KODE"));
            return false;
        }
    }

    private boolean validateLeder(JsonNode e) {
        val validate = e.hasNonNull("LEDER_NAVIDENT") && e.hasNonNull("ORGNODE_KODE") && e.hasNonNull("ORGNODE_NIVA");
        if (validate) {
            return true;
        } else {
            log.info("Ugyldig leder fra dvh, LEDER_NAVIDENT: " + getAsText(e, "LEDER_NAVIDENT") + ", ORGNODE_KODE: " + getAsText(e, "ORGNODE_KODE"));
            return false;
        }
    }

    private boolean validateKobling(JsonNode e) {
        val validate = e.hasNonNull("NAV_ID") && e.hasNonNull("FRA_DATO")
                && e.hasNonNull("ORGENHET");
        if (validate) {
            return true;
        } else {
            log.info("Ugyldig kobling fra dvh, NAV_ID: " + getAsText(e, "NAV_ID") + ", ORGENHET: " + getAsText(e, "ORGENHET")
                    + ", FRA_DATO: " + getAsText(e, "FRA_DATO"));
            return false;
        }
    }

    private String getAsText(JsonNode e, String field) {
        JsonNode jsonNode = e.get(field);
        return jsonNode == null ? "<null>" : jsonNode.asText();
    }
}
