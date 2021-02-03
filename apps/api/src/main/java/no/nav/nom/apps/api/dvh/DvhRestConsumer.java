package no.nav.nom.apps.api.dvh;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.nom.apps.api.common.AppProperties;
import no.nav.nom.apps.api.dvh.dto.DvhKobling;
import no.nav.nom.apps.api.dvh.dto.DvhLeder;
import no.nav.nom.apps.api.dvh.dto.DvhOrganisasjonsenhet;
import no.nav.nom.apps.api.dvh.dto.DvhOrganisering;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.LivenessState;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@Component
@RequiredArgsConstructor
public class DvhRestConsumer {
    public static final String CACHE = "cache-source";
    private final long ONE_HOUR = 60 * 60 * 1000;

    private final AadTokenRepository aadTokenRepository;
    private final DvhRepository dvhRepository;
    private final RestTemplate restTemplate;
    private final AppProperties appProperties;
    private final DvhStateListener dvhStateListener;
    private final AtomicBoolean initialLoadComplete = new AtomicBoolean(false);
    private final ApplicationEventPublisher appEventPublisher;

    @Scheduled(fixedDelay = ONE_HOUR)
    @SneakyThrows
    public void loadFromDvh(){
        while (!dvhStateListener.getInitialLoadStatus()){
            Thread.sleep(2000);
        }
        loadOrgenhetFromDvh();
        loadKoblingerFromDvh();

        if (!initialLoadComplete.get()) {
            initialLoadComplete.set(true);
            AvailabilityChangeEvent.publish(appEventPublisher, CACHE, LivenessState.CORRECT);
            log.info("Changing to LivenessState.CORRECT");
        }
    }

    private void loadKoblingerFromDvh(){
        val jsonStillinger = dvhExchange("/proxy/stillinger");
        val koblinger = StreamSupport.stream(jsonStillinger.get("stilling2nomResult").spliterator(), false)
                .filter(this::validateKobling)
                .map(DvhKobling::new)
                .collect(Collectors.toList());

        dvhRepository.saveKoblinger(koblinger);
    }

    private void loadOrgenhetFromDvh(){
        val jsonOrgenheter = dvhExchange("/proxy/enheter");
        val orgenheter = new HashMap<String, DvhOrganisasjonsenhet>(1000);
        val ledere = new HashMap<String, List<DvhLeder>>(1000);
        val organiseringerByEnhet = new ConcurrentHashMap<String, List<DvhOrganisering>>(1000);

        StreamSupport.stream(jsonOrgenheter.get("NOM_HRORG_HIERARKI_v1Result").spliterator(), false)
                .forEach(e -> {
                    if(validateEnhet(e)) {
                        val orgenhet = new DvhOrganisasjonsenhet(e);
                        orgenheter.put(orgenhet.getAgressoId(), orgenhet);
                    }

                    if(validateOrganisering(e)) {
                        val organisering = new DvhOrganisering(e);
                        putOrganisering(organiseringerByEnhet, organisering);
                    }

                    if(validateLeder(e)) {
                        val leder = new DvhLeder(e);
                        if (ledere.containsKey(leder.getOrgEnhetNr())) {
                            ledere.get(leder.getOrgEnhetNr()).add(leder);
                        } else {
                            ledere.put(leder.getOrgEnhetNr(), new LinkedList<>(List.of(leder)));
                        }
                    }
                });

        dvhRepository.saveOrganisasjonsenheter(orgenheter);
        dvhRepository.saveLedere(ledere);
        dvhRepository.saveOrganiseringer(organiseringerByEnhet);
    }

    private JsonNode dvhExchange(String urlPath){
        val headers = new HttpHeaders();
        headers.setBearerAuth(aadTokenRepository.getDvhToken());

        return restTemplate.exchange(appProperties.getDvh().getBaseUrl()+urlPath, HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class).getBody();
    }

    private void putOrganisering(Map<String, List<DvhOrganisering>> organiseringerByEnhet, DvhOrganisering organisering){
        if (organiseringerByEnhet.containsKey(organisering.getOverenhetNummer())){
            organiseringerByEnhet.get(organisering.getOverenhetNummer()).add(organisering);
        } else {
            organiseringerByEnhet.put(organisering.getOverenhetNummer(), new LinkedList<>(List.of(organisering)));
        }
        if (organiseringerByEnhet.containsKey(organisering.getUnderenhetNummer())){
            organiseringerByEnhet.get(organisering.getUnderenhetNummer()).add(organisering);
        } else {
            organiseringerByEnhet.put(organisering.getUnderenhetNummer(), new LinkedList<>(List.of(organisering)));
        }
    }

    private boolean validateEnhet(JsonNode e){
        val validate =  e.hasNonNull("orgnode_kode") && e.hasNonNull("orgnode_navn");
        if (validate){
            return true;
        } else {
            log.warn("Ugyldig enhet fra dvh, orgnode_kode: "+e.get("orgnode_kode").asText()+", orgnode_navn: "+e.get("orgnode_navn").asText());
            return false;
        }
    }

    private boolean validateOrganisering(JsonNode e){
        val validate = e.hasNonNull("orgnode_kode_over") && e.hasNonNull("orgnode_kode");
        if(validate){
            return true;
        } else {
            log.warn("Ugyldig organisering fra dvh, orgnode_kode_over: "+e.get("orgnode_kode_over").asText()+", orgnode_kode: "+e.get("orgnode_kode").asText());
            return false;
        }
    }

    private boolean validateLeder(JsonNode e){
        val validate = e.hasNonNull("leder_resource_id") && e.hasNonNull("orgnode_kode");
        if(validate){
            return true;
        } else {
            log.warn("Ugyldig leder fra dvh, leder_resource_id: "+e.get("leder_resource_id").asText()+", orgnode_kode: "+e.get("orgnode_kode").asText());
            return false;
        }
    }

    private boolean validateKobling(JsonNode e){
        val validate = e.hasNonNull("RESOURCE_ID") && e.hasNonNull("FRA_DATO")
                && e.hasNonNull("ORGENHET");
        if(validate){
            return true;
        } else {
            log.warn("Ugyldig kobling fra dvh, RESOURCE_ID: "+e.get("RESOURCE_ID").asText()+", ORGENHET: "+e.get("ORGENHET").asText()
            +", FRA_DATO: "+e.get("FRA_DATO").asText());
            return false;
        }
    }
}
