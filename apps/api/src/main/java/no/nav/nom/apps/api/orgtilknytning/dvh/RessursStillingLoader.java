package no.nav.nom.apps.api.orgtilknytning.dvh;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.nom.apps.api.clients.nomproxy.NomProxyClient;
import no.nav.nom.apps.api.common.AppProperties;
import no.nav.nom.apps.api.dvh.dto.DvhRessursStilling;
import no.nav.nom.apps.api.orgtilknytning.dvh.statlige.AsyncNyeStrukturerOgProsesser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static no.nav.nom.apps.api.common.AppConfig.SECURE_LOGS;

@Slf4j
@Service
public class RessursStillingLoader {
    private final NomProxyClient nomProxyClient;
    private final AppProperties appProperties;
    private final String ressursStillingerResponseNode;
    private final AsyncNyeStrukturerOgProsesser asyncNyeStrukturerOgProsesser;


    public RessursStillingLoader(
        NomProxyClient nomProxyClient, AppProperties appProperties,
        @Value("${app.dvh-load.nom-ressurser-stillinger-respons-navn}") String ressursStillingerResponseNode,
        AsyncNyeStrukturerOgProsesser asyncNyeStrukturerOgProsesser
    ) {
        this.nomProxyClient = nomProxyClient;
        this.appProperties = appProperties;
        this.ressursStillingerResponseNode = ressursStillingerResponseNode;
        this.asyncNyeStrukturerOgProsesser = asyncNyeStrukturerOgProsesser;
    }

    public void loadAndProcess() {
        LocalDateTime start = LocalDateTime.now();

        List<DvhRessursStilling> ressursStillinger = load();

        var nyeStillingPerioderPerNavidentFuture = asyncNyeStrukturerOgProsesser.asyncStrukturerStillingPerioderPerNavident(ressursStillinger);

        try {
            var nyeStillingPerioderPerNavident = nyeStillingPerioderPerNavidentFuture.get();
            log.info("Lastet og prosessert ressursStillinger {} på nye måten. Tok {}", nyeStillingPerioderPerNavident.size(), durationAsH24(start));
            loggErrorsFraNyLast(nyeStillingPerioderPerNavident);
        } catch (Exception e) {
            log.error("Feil ved lasting og prosessering av ressursStillinger på nye måten", e);
        }
    }

    private void loggErrorsFraNyLast(Map<String, StrukturerteStillingPerioderForNavident> nyeStillingPerioderPerNavident) {
        nyeStillingPerioderPerNavident.forEach((navident, sspfn) -> {
            if (!sspfn.errors().isEmpty()) {
                log.info("Feil ved NYE lasting og prosessering av ressursStillinger for navident {}: {}", navident, sspfn.errors());
            }
        });
    }

    public List<DvhRessursStilling> load() {
        log.info("Laster ressursStillinger fra {}", appProperties.clients().nomProxy().ressursStillingerPath());
        LocalDateTime start = LocalDateTime.now();
        val jsonRessursStillinger = dvhExchange(appProperties.clients().nomProxy().ressursStillingerPath());
        val listField = jsonRessursStillinger.fields().next();
        log.debug(SECURE_LOGS, "Lastet ressursStillinger: Første field: {} med {} ant noder", listField.getKey(), listField.getValue().size());
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
