package no.nav.nom.apps.api.orgtilknytning.dvh;

import com.fasterxml.jackson.databind.JsonNode;
import io.getunleash.Unleash;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.nom.apps.api.clients.dvhords.DvhOrdsService;
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
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@Service
public class RessursStillingLoader {
    private final NomProxyClient nomProxyClient;
    private final AppProperties appProperties;
    private final String ressursStillingerResponseNode;
    private final AsyncNyeStrukturerOgProsesser asyncNyeStrukturerOgProsesser;
    private final DvhOrdsService dvhOrdsService;
    private final Unleash unleash;

    public RessursStillingLoader(
        NomProxyClient nomProxyClient, AppProperties appProperties,
        @Value("${app.dvh-load.nom-ressurser-stillinger-respons-navn}") String ressursStillingerResponseNode,
        AsyncNyeStrukturerOgProsesser asyncNyeStrukturerOgProsesser, DvhOrdsService dvhOrdsService, Unleash unleash
    ) {
        this.nomProxyClient = nomProxyClient;
        this.appProperties = appProperties;
        this.ressursStillingerResponseNode = ressursStillingerResponseNode;
        this.asyncNyeStrukturerOgProsesser = asyncNyeStrukturerOgProsesser;
        this.dvhOrdsService = dvhOrdsService;
        this.unleash = unleash;
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
        LocalDateTime start = LocalDateTime.now();

        List<DvhRessursStilling> ressursStillinger;
        if (unleash.isEnabled("nom.dvh.last_ressursstillinger_fra_ords")) {
            // TODO: Fix paginering
            ressursStillinger = dvhOrdsService.getStatligeTilknytninger();
        } else {
            log.info("Laster ressursStillinger fra {}", appProperties.clients().nomProxy().ressursStillingerPath());
            val jsonRessursStillinger = dvhExchange(appProperties.clients().nomProxy().ressursStillingerPath());
            ressursStillinger = StreamSupport.stream(jsonRessursStillinger.get(ressursStillingerResponseNode).spliterator(), true)
                .map(DvhRessursStilling::new)
                .collect(Collectors.toList());
        }

        getOrdsRessursStillingerAndCompare(ressursStillinger);

        log.info("Lastet {} ressursStillinger. Tok {}", ressursStillinger.size(), durationAsH24(start));
        return ressursStillinger;
    }

    private void getOrdsRessursStillingerAndCompare(List<DvhRessursStilling> tdvDvhRessursStillinger) {
        try {
            var ordsDvhRessursStillinger = dvhOrdsService.getStatligeTilknytninger();

            boolean erLikt = true;
            if (ordsDvhRessursStillinger.size() != tdvDvhRessursStillinger.size()) {
                log.error("Antall RessursStillinger fra TDV og ORDS er ulik. TDV: {}, ORDS: {}", tdvDvhRessursStillinger.size(), ordsDvhRessursStillinger.size());
                erLikt = false;
            }

            var ordsMap = ordsDvhRessursStillinger.stream().collect(Collectors.toMap(this::getRessursStillingUniqueKey, Function.identity()));
            int idxSkip = tdvDvhRessursStillinger.size()/200;
            for (int i = 0; i < 200; i++) {
                int idx = i * idxSkip;
                var dvhRessursStillingTdv = tdvDvhRessursStillinger.get(idx);
                var dvhRessursStillingOrds = ordsMap.get(getRessursStillingUniqueKey(dvhRessursStillingTdv));
                if (dvhRessursStillingOrds == null || !DvhRessursStilling.compare(dvhRessursStillingTdv, dvhRessursStillingOrds)) {
                    log.error("RessursStillinger fra TDV og ORDS er ulik. \n\tTDV: \n\t{}, \n\tORDS: \n\t{}"
                        , dvhRessursStillingTdv.getPayload()
                        , dvhRessursStillingOrds != null ? dvhRessursStillingOrds.getPayload() : null);
                    erLikt = false;
                }
            }
            if (erLikt) {
                log.info("Last av RessursStillinger fra ORDS (NOM_ORDS_RESSURS_STILLINGER.json) er lik last fra TDV (NOM_RESSURS_STILLINGER)");
            } else {
                log.error("Last av RessursStillinger fra ORDS (NOM_ORDS_RESSURS_STILLINGER.json) er annerledes enn last fra TDV (NOM_RESSURS_STILLINGER)");
            }
        } catch (Exception e) {
            log.error("Feilet i last av RessursStillinger fra ORDS (NOM_ORDS_RESSURS_STILLINGER.json)", e);
        }
    }

    private String getRessursStillingUniqueKey(DvhRessursStilling dvhRessursStilling) {
        return dvhRessursStilling.getNavident() + "-" + dvhRessursStilling.getStillingAgressoId() + "-" + dvhRessursStilling.getStillingOrgenhetNavn() + "-" + dvhRessursStilling.getStillingDatoFom() + "-" + dvhRessursStilling.getStillingDatoTom();
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
