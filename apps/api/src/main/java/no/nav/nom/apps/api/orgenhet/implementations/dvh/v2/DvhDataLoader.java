package no.nav.nom.apps.api.orgenhet.implementations.dvh.v2;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.nom.apps.api.common.AppProperties;
import no.nav.nom.apps.api.common.security.TokenRepository;
import no.nav.nom.apps.api.dvh.DvhRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.stream.StreamSupport;

@Slf4j
@Service
@Profile("!test")
@RequiredArgsConstructor
public class DvhDataLoader {

    private final long LOAD_INTERVAL = 30 * 60 * 1000; // sync hvert 30min
    private final String HISTORISK_ENEHT_PATH = "/proxy/dvh/json/Hr/NOM2/orgenhetStatus2nom";

    private final DvhRepository dvhRepository;
    private final AppProperties appProperties;
    private final TokenRepository tokenRepository;
    private final RestTemplate restTemplate;

    @Scheduled(fixedDelay = LOAD_INTERVAL)
    public void load(){
        val response = dvhExchange().get("orgenhetStatus2nomResponse").get("orgenhetStatus2nomResult");

        dvhRepository.saveOrgenhetV2(
            StreamSupport.stream(response.spliterator(), false)
                    .map(DvhOrgenhetV2::new)
                    .filter(e -> {
                        if(e.getGyldigFom() == null) log.warn("Manglende gyldigFra dato "+e);
                        if(e.getOrgenhetStatus().equals("P") && e.getGyldigTom().isEmpty())
                            log.warn("Parkert enhet uten gyldigTil dato! "+e);
                        if(e.getGyldigTom().isPresent() && e.getGyldigFom().isAfter(e.getGyldigTom().get())){
                            log.warn("Enhet med gyldigFom > gyldigTom "+e);
                        }
                        if(e.getOrgenhetStatus().equals("P") || e.getOrgenhetStatus().equals("N")) {
                            return true;
                        }
                        log.error("Mottatt enhet ugyldig status "+e);
                        return false;
                    })
                    .toList()
        );
    }


    private JsonNode dvhExchange() {
        val headers = new HttpHeaders();
        headers.setBearerAuth(tokenRepository.getAadToken(appProperties.getProxy().getAadClientId()));

        return restTemplate.exchange(appProperties.getProxy().getBaseUrl() + HISTORISK_ENEHT_PATH, HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class).getBody();
    }
}