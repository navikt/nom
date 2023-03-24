package no.nav.nom.apps.api.orgTilknytning.dvh;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.getunleash.Unleash;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.nom.apps.api.common.AppProperties;
import no.nav.nom.apps.api.common.security.TokenRepository;
import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.dvh.dto.DvhRessursStilling;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Spliterator;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@Service
public class RessursStillingLoader {
    public static final String UNLEASH_LAST_NY_ORGTILKNYTNING = "nom.orgtilknytning.last";
    private final TokenRepository tokenRepository;
    private final DvhRepository dvhRepository;
    private final RestTemplate restTemplate;
    private final AppProperties appProperties;
    private final String ressursStillingerResponseNode;

//    @Value("${app.kafka.orgEnhet.stateTopic}") String stateTopic,



    private final Unleash unleash;

    public RessursStillingLoader(TokenRepository tokenRepository, DvhRepository dvhRepository, RestTemplate restTemplate, AppProperties appProperties,
                                 @Value("${app.dvh-load.nom-ressurser-stillinger-respons-navn}") String ressursStillingerResponseNode,
                                 Unleash unleash) {
        this.tokenRepository = tokenRepository;
        this.dvhRepository = dvhRepository;
        this.restTemplate = restTemplate;
        this.appProperties = appProperties;
        this.ressursStillingerResponseNode = ressursStillingerResponseNode;
        this.unleash = unleash;
    }

    public void load() {
        if (unleash.isEnabled(UNLEASH_LAST_NY_ORGTILKNYTNING)) {
            log.info("Laster ressursStillinger fra {}", appProperties.proxy().ressursStillingerPath());
            LocalDateTime start = LocalDateTime.now();
            val jsonRessursStillinger = dvhExchange(appProperties.proxy().ressursStillingerPath());
            val listField = jsonRessursStillinger.fields().next();
            log.info("Lastet ressursStillinger: Første field: {} med {} ant noder", listField.getKey(), ((ArrayNode)listField.getValue()).size());
            List<DvhRessursStilling> ressursStillinger = StreamSupport.stream(jsonRessursStillinger.get(ressursStillingerResponseNode).spliterator(), true)
                .map(DvhRessursStilling::new)
                .collect(Collectors.toList());
            log.info("Lastet {} ressursStillinger. Tok {}", ressursStillinger.size(), durationAsH24(start));
        }
    }

    // Shameless duplication from DvhRestConsumer
    private JsonNode dvhExchange(String urlPath) {
        val headers = new HttpHeaders();
        headers.setBearerAuth(tokenRepository.getSystemAadToken(appProperties.proxy().aadClientId()).serialize());

        return restTemplate.exchange(appProperties.proxy().baseUrl() + urlPath, HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class).getBody();
    }

    private String durationAsH24(LocalDateTime start) {
        Duration duration = Duration.between(start, LocalDateTime.now());
        return  "%02d:%02d:%02d".formatted(duration.toHoursPart(), duration.toMinutesPart(), duration.toSecondsPart());
    }
}
