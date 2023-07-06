package no.nav.nom.apps.api.orgtilknytning.dvh;

import com.fasterxml.jackson.databind.JsonNode;
import io.getunleash.Unleash;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.nom.apps.api.clients.nomproxy.NomProxyClient;
import no.nav.nom.apps.api.common.AppProperties;
import no.nav.nom.apps.api.dvh.dto.DvhRessursStilling;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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
    private final RessursStillingStrukturerer ressursStillingStrukturerer;
    private final RessursStillingProcessor ressursStillingProcessor;
    private final RessursStillingLoadStatus ressursStillingLoadStatus;
    private final NomProxyClient nomProxyClient;
    private final AppProperties appProperties;
    private final String ressursStillingerResponseNode;
    private final Unleash unleash;

    public RessursStillingLoader(RessursStillingStrukturerer ressursStillingStrukturerer,
                                 RessursStillingProcessor ressursStillingProcessor,
                                 RessursStillingLoadStatus ressursStillingLoadStatus, NomProxyClient nomProxyClient, AppProperties appProperties,
                                 @Value("${app.dvh-load.nom-ressurser-stillinger-respons-navn}") String ressursStillingerResponseNode,
                                 Unleash unleash) {
        this.ressursStillingStrukturerer = ressursStillingStrukturerer;
        this.ressursStillingProcessor = ressursStillingProcessor;
        this.ressursStillingLoadStatus = ressursStillingLoadStatus;
        this.nomProxyClient = nomProxyClient;
        this.appProperties = appProperties;
        this.ressursStillingerResponseNode = ressursStillingerResponseNode;
        this.unleash = unleash;
    }

    public void loadAndProcess() {
        if (unleash.isEnabled(UNLEASH_LAST_NY_ORGTILKNYTNING)) {
            ressursStillingLoadStatus.startNewLoad();

            List<DvhRessursStilling> ressursStillinger = load();

            Map <String, List<StillingPeriode>> stillingPerioderPerNavident = ressursStillingStrukturerer.strukturerStilingPerioderPerNavident(ressursStillinger);

            ressursStillingProcessor.process(stillingPerioderPerNavident);

            ressursStillingLoadStatus.endLoad();
        }
    }

    protected List<DvhRessursStilling> load() {
        log.info("Laster ressursStillinger fra {}", appProperties.clients().nomProxy().ressursStillingerPath());
        LocalDateTime start = LocalDateTime.now();
        val jsonRessursStillinger = dvhExchange(appProperties.clients().nomProxy().ressursStillingerPath());
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
        return nomProxyClient.getWebClient().get()
            .uri(urlPath)
            .retrieve()
            .bodyToMono(JsonNode.class)
            .block();
    }

    private String durationAsH24(LocalDateTime start) {
        Duration duration = Duration.between(start, LocalDateTime.now());
        return  "%02d:%02d:%02d".formatted(duration.toHoursPart(), duration.toMinutesPart(), duration.toSecondsPart());
    }
}
