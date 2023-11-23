package no.nav.nom.apps.api.orgtilknytning.dvh;

import com.fasterxml.jackson.databind.JsonNode;
import io.getunleash.Unleash;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.nom.apps.api.clients.nomproxy.NomProxyClient;
import no.nav.nom.apps.api.common.AppProperties;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@Service
public class EksterneRessurserLoader {
    private final NomProxyClient nomProxyClient;
    private final AppProperties appProperties;
    private EksterneRessurserProcessor eksterneRessurserProcessor;

    public EksterneRessurserLoader(NomProxyClient nomProxyClient, AppProperties appProperties, EksterneRessurserProcessor eksterneRessurserProcessor, Unleash unleash) {
        this.nomProxyClient = nomProxyClient;
        this.appProperties = appProperties;
        this.eksterneRessurserProcessor = eksterneRessurserProcessor;
    }

    public void loadAndProcess() {
        List<DvhEksternRessurs> eksterneRessurser = load();
        eksterneRessurser.forEach(eksterneRessurserProcessor::process);
    }

    private List<DvhEksternRessurs> load() {
        log.info("Laster eksterneRessurser fra {}", appProperties.clients().nomProxy().eksterneRessurserPath());
        LocalDateTime start = LocalDateTime.now();

        val jsonEksterneRessurser = dvhExchange(appProperties.clients().nomProxy().eksterneRessurserPath());
        val listField = jsonEksterneRessurser.fields().next();
        log.debug("Lastet eksterneRessurser: Første field: {} med {} ant noder", listField.getKey(), listField.getValue().size());

        List<DvhEksternRessurs> eksterneRessurser = StreamSupport.stream(jsonEksterneRessurser.get("NOM_EKSTERNE_V1Response").spliterator(), true)
            .map(DvhEksternRessurs::fromJson)
            .collect(Collectors.toList());

        log.info("Lastet {} eksterneRessurser. Tok {}", eksterneRessurser.size(), durationAsH24(start));
        return eksterneRessurser;
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
