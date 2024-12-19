package no.nav.nom.apps.api.orgtilknytning.dvh;

import com.fasterxml.jackson.databind.JsonNode;
import io.getunleash.Unleash;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.nom.apps.api.clients.dvhords.DvhOrdsService;
import no.nav.nom.apps.api.clients.nomproxy.NomProxyClient;
import no.nav.nom.apps.api.common.AppProperties;
import no.nav.nom.apps.api.ressurs.implementations.dvh.DvhRessurs;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static no.nav.nom.apps.api.common.AppConfig.SECURE_LOGS;

@Slf4j
@Service
public class EksterneRessurserLoader {
    private final NomProxyClient nomProxyClient;
    private final AppProperties appProperties;
    private final DvhOrdsService dvhOrdsService;
    private final Unleash unleash;
    private EksterneRessurserProcessor eksterneRessurserProcessor;

    public EksterneRessurserLoader(NomProxyClient nomProxyClient, AppProperties appProperties, EksterneRessurserProcessor eksterneRessurserProcessor,
                                   DvhOrdsService dvhOrdsService,
                                   Unleash unleash
    ) {
        this.nomProxyClient = nomProxyClient;
        this.appProperties = appProperties;
        this.eksterneRessurserProcessor = eksterneRessurserProcessor;
        this.dvhOrdsService = dvhOrdsService;
        this.unleash = unleash;
    }

    public void loadAndProcess() {
        List<DvhEksternRessurs> eksterneRessurser = load();
        eksterneRessurser.forEach(eksterneRessurserProcessor::process);
    }

    private List<DvhEksternRessurs> load() {
        log.info("Laster eksterneRessurser fra {}", appProperties.clients().nomProxy().eksterneRessurserPath());
        LocalDateTime start = LocalDateTime.now();

        List<DvhEksternRessurs> eksterneRessurser;
        if (unleash.isEnabled("nom.dvh.last_eksterne_fra_ords")) {
            eksterneRessurser = dvhOrdsService.getEksterneTilknytninger();
        } else {
            val jsonEksterneRessurser = dvhExchange(appProperties.clients().nomProxy().eksterneRessurserPath());
            val listField = jsonEksterneRessurser.fields().next();
            log.debug(SECURE_LOGS, "Lastet eksterneRessurser: Første field: {} med {} ant noder", listField.getKey(), listField.getValue().size());

            eksterneRessurser = StreamSupport.stream(jsonEksterneRessurser.get("NOM_EKSTERNE_V1Response").spliterator(), true)
                .map(DvhEksternRessurs::fromJson)
                .collect(Collectors.toList());
        }

//        getOrdsEksterneRessurserAndCompare(eksterneRessurser);

        log.info("Lastet {} eksterneRessurser. Tok {}", eksterneRessurser.size(), durationAsH24(start));
        return eksterneRessurser;
    }

    private void getOrdsEksterneRessurserAndCompare(List<DvhEksternRessurs> tdvDvhRessurser) {
        try {
            var ordsDvhRessurser = dvhOrdsService.getEksterneTilknytninger();

            boolean erLikt = true;
            if (ordsDvhRessurser.size() != tdvDvhRessurser.size()) {
                log.error("Antall eksterne ressurser fra TDV og ORDS er ulik. TDV: {}, ORDS: {}", tdvDvhRessurser.size(), ordsDvhRessurser.size());
                erLikt = false;
            }

            var ordsMap = ordsDvhRessurser.stream().collect(Collectors.toMap(DvhEksternRessurs::navident, Function.identity()));
            int idxSkip = tdvDvhRessurser.size()/200;
            for (int i = 0; i < 200; i++) {
                int idx = i * idxSkip;
                var dvhRessursTdv = tdvDvhRessurser.get(idx);
                var dvhRessursOrds = ordsMap.get(dvhRessursTdv.navident());
                if (dvhRessursOrds == null || !DvhEksternRessurs.compare(dvhRessursTdv, dvhRessursOrds)) {
                    log.error("ekstern ressurs fra TDV og ORDS er ulik. \n\tTDV: \n\t{}, \n\tORDS: \n\t{}"
                        , dvhRessursTdv.payload()
                        , dvhRessursOrds == null ? null : dvhRessursOrds.payload());
                    erLikt = false;
                }
            }
            if (erLikt) {
                log.info("Last av eksterne ressurser fra ORDS (nom_ords_ansatte_buckets.json) er lik last fra TDV (NOM_ANSATTE_BUCKETS_v2)");
            } else {
                log.error("Last av eksterne ressurser fra ORDS (nom_ords_ansatte_buckets.json) er annerledes enn last fra TDV (NOM_ANSATTE_BUCKETS_v2)");
            }
        } catch (Exception e) {
            log.error("Feilet i last av eksterne ressurser fra ORDS (nom_ords_ansatte_buckets.json)", e);
        }
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
