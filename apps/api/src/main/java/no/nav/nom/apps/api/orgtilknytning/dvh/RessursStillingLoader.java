package no.nav.nom.apps.api.orgtilknytning.dvh;

import com.fasterxml.jackson.databind.JsonNode;
import io.getunleash.Unleash;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.nom.apps.api.common.AppProperties;
import no.nav.nom.apps.api.common.security.TokenRepository;
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
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@Service
public class RessursStillingLoader {
    public static final String UNLEASH_LAST_NY_ORGTILKNYTNING = "nom.orgtilknytning.last";
    private final TokenRepository tokenRepository;
    private final RessursStillingStrukturerer ressursStillingStrukturerer;
    private final RessursStillingProcessor ressursStillingProcessor;
    private final RessursStillingLoadStatus ressursStillingLoadStatus;
    private final RestTemplate restTemplate;
    private final AppProperties appProperties;
    private final String ressursStillingerResponseNode;
    private final Unleash unleash;

    public RessursStillingLoader(TokenRepository tokenRepository,
                                 RessursStillingStrukturerer ressursStillingStrukturerer,
                                 RessursStillingProcessor ressursStillingProcessor,
                                 RessursStillingLoadStatus ressursStillingLoadStatus, RestTemplate restTemplate, AppProperties appProperties,
                                 @Value("${app.dvh-load.nom-ressurser-stillinger-respons-navn}") String ressursStillingerResponseNode,
                                 Unleash unleash) {
        this.tokenRepository = tokenRepository;
        this.ressursStillingStrukturerer = ressursStillingStrukturerer;
        this.ressursStillingProcessor = ressursStillingProcessor;
        this.ressursStillingLoadStatus = ressursStillingLoadStatus;
        this.restTemplate = restTemplate;
        this.appProperties = appProperties;
        this.ressursStillingerResponseNode = ressursStillingerResponseNode;
        this.unleash = unleash;
    }

    public void loadAndProcess() {
        if (unleash.isEnabled(UNLEASH_LAST_NY_ORGTILKNYTNING)) {
            ressursStillingLoadStatus.startNewLoad();

            List<DvhRessursStilling> ressursStillinger = load();

            Map <String, List<StillingPeriode>> stillingPerioderPerNavident = ressursStillingStrukturerer.StrukturerStilingPerioderPerNavident(ressursStillinger);

            ressursStillingProcessor.process(stillingPerioderPerNavident);

            ressursStillingLoadStatus.endLoad();
        }
    }

    protected List<DvhRessursStilling> load() {
        log.info("Laster ressursStillinger fra {}", appProperties.proxy().ressursStillingerPath());
        LocalDateTime start = LocalDateTime.now();
        val jsonRessursStillinger = dvhExchange(appProperties.proxy().ressursStillingerPath());
        val listField = jsonRessursStillinger.fields().next();
        log.debug("Lastet ressursStillinger: Første field: {} med {} ant noder", listField.getKey(), listField.getValue().size());
        List<DvhRessursStilling> ressursStillinger = StreamSupport.stream(jsonRessursStillinger.get(ressursStillingerResponseNode).spliterator(), true)
            .map(DvhRessursStilling::new)
            .collect(Collectors.toList());
        log.info("Lastet {} ressursStillinger. Tok {}", ressursStillinger.size(), durationAsH24(start));
        return ressursStillinger;
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
