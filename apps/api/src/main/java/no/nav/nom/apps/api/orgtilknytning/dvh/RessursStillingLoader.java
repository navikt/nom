package no.nav.nom.apps.api.orgtilknytning.dvh;

import com.fasterxml.jackson.databind.JsonNode;
import io.getunleash.Unleash;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.nom.apps.api.clients.nomproxy.NomProxyClient;
import no.nav.nom.apps.api.common.AppProperties;
import no.nav.nom.apps.api.dvh.dto.DvhRessursStilling;
import no.nav.nom.apps.api.orgtilknytning.dvh.statlige.AsyncNyeStrukturerOgProsesser;
import no.nav.nom.apps.api.ressurs.NomMastered;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static no.nav.nom.apps.api.common.AppConfig.SECURE_LOGS;
import static no.nav.nom.apps.api.orgtilknytning.dvh.statlige.NyeRessursStillingProcessor.LAGRE_ORGTILKNYTNINGER_I_NY_PROSESSERING_FOR_DVH_LAST;

@Slf4j
@Service
public class RessursStillingLoader {
    private final RessursStillingStrukturerer ressursStillingStrukturerer;
    private final RessursStillingProcessor ressursStillingProcessor;
    private final RessursStillingLoadStatus ressursStillingLoadStatus;
    private final NomProxyClient nomProxyClient;
    private final AppProperties appProperties;
    private final String ressursStillingerResponseNode;
    private final AsyncNyeStrukturerOgProsesser asyncNyeStrukturerOgProsesser;
    private final RessursStateRepository ressursStateRepository;
    private final NomMastered nomMastered;
    private final Unleash unleash;

    public RessursStillingLoader(
        RessursStillingStrukturerer ressursStillingStrukturerer,
        RessursStillingProcessor ressursStillingProcessor,
        RessursStillingLoadStatus ressursStillingLoadStatus, NomProxyClient nomProxyClient, AppProperties appProperties,
        @Value("${app.dvh-load.nom-ressurser-stillinger-respons-navn}") String ressursStillingerResponseNode,
        AsyncNyeStrukturerOgProsesser asyncNyeStrukturerOgProsesser,
        RessursStateRepository ressursStateRepository, NomMastered nomMastered, Unleash unleash
    ) {
        this.ressursStillingStrukturerer = ressursStillingStrukturerer;
        this.ressursStillingProcessor = ressursStillingProcessor;
        this.ressursStillingLoadStatus = ressursStillingLoadStatus;
        this.nomProxyClient = nomProxyClient;
        this.appProperties = appProperties;
        this.ressursStillingerResponseNode = ressursStillingerResponseNode;
        this.asyncNyeStrukturerOgProsesser = asyncNyeStrukturerOgProsesser;
        this.ressursStateRepository = ressursStateRepository;
        this.nomMastered = nomMastered;
        this.unleash = unleash;
    }

    public void loadAndProcess() {
        LocalDateTime start = LocalDateTime.now();

        ressursStillingLoadStatus.startNewLoad();

        List<DvhRessursStilling> ressursStillinger = load();

        var nyeStillingPerioderPerNavidentFuture = asyncNyeStrukturerOgProsesser.asyncStrukturerStillingPerioderPerNavident(ressursStillinger);

        Map <String, StrukturerteStillingPerioderForNavident> gmlStillingPerioderPerNavident = null;
        if (!unleash.isEnabled(LAGRE_ORGTILKNYTNINGER_I_NY_PROSESSERING_FOR_DVH_LAST) ) {
            Map <String, List<StillingPeriode>> stillingPerioderPerNavident = ressursStillingStrukturerer.strukturerStilingPerioderPerNavident(ressursStillinger);
            gmlStillingPerioderPerNavident = ressursStillingProcessor.process(stillingPerioderPerNavident);
            log.info("Lastet og prosessert ressursStillinger {} på gamlemåten. Tok {}", gmlStillingPerioderPerNavident.size(), durationAsH24(start));
        }

        try {
            var nyeStillingPerioderPerNavident = nyeStillingPerioderPerNavidentFuture.get();
            log.info("Lastet og prosessert ressursStillinger {} på nye måten. Tok {}", nyeStillingPerioderPerNavident.size(), durationAsH24(start));
            if (!unleash.isEnabled(LAGRE_ORGTILKNYTNINGER_I_NY_PROSESSERING_FOR_DVH_LAST) ) {
                diffNyGmlResult(gmlStillingPerioderPerNavident, nyeStillingPerioderPerNavident);
            }
            loggErrorsFraNyLast(nyeStillingPerioderPerNavident);
        } catch (Exception e) {
            log.error("Feil ved lasting og prosessering av ressursStillinger på nye måten", e);
        }

        if (!unleash.isEnabled(LAGRE_ORGTILKNYTNINGER_I_NY_PROSESSERING_FOR_DVH_LAST) ) {
            log.info("Lastet og prosessert ressursStillinger på både nye og gamlemåten. Tok {}", durationAsH24(start));
        }

        ressursStillingLoadStatus.endLoad();
    }

    private void loggErrorsFraNyLast(Map<String, StrukturerteStillingPerioderForNavident> nyeStillingPerioderPerNavident) {
        nyeStillingPerioderPerNavident.forEach((navident, sspfn) -> {
            if (!sspfn.errors().isEmpty()) {
                log.error("Feil ved NYE lasting og prosessering av ressursStillinger for navident {}: {}", navident, sspfn.errors());
            }
        });
    }

    private void diffNyGmlResult(Map<String, StrukturerteStillingPerioderForNavident> gmlStillingPerioderPerNavident, Map<String, StrukturerteStillingPerioderForNavident> nyeStillingPerioderPerNavident) {
        log.info("Diffing ny og gammel prosessering av ressursStillinger");
        gmlStillingPerioderPerNavident.forEach((navident, gmlSSPFN) -> {
            var nySSPFN = nyeStillingPerioderPerNavident.get(navident);
            var ressurs = ressursStateRepository.findByNavident(navident);
            if (ressurs.isEmpty() || nomMastered.isMastered(navident)) {
                log.debug("Gammel prosessering har navident {} som ikke finnes i ny prosessering", navident);
            }
            if (nySSPFN == null) {
                if (ressurs.isPresent() && ressurs.get().isStatlig()) {
                    log.info("Ny prosessering mangler for navident {}", navident);
                }
            } else {
                var differ = nySSPFN.diff(gmlSSPFN, log.isDebugEnabled());
                if (!differ.isEmpty()) {
                    log.info("Forskjell i prosessering av ressursStillinger for navident {}: {}", navident, differ);
                }
                var nyLastAktiveOgFremtidigeEndringer = nySSPFN.aktiveOgFremtidigeEndringer();
                if (!nyLastAktiveOgFremtidigeEndringer.isEmpty()) {
                    if (log.isDebugEnabled()) {
                        log.info("Aktive og fremtidige endringer i ny last for navident {}: {}", navident, nyLastAktiveOgFremtidigeEndringer);
                    } else {
                        log.info("Aktive og fremtidige endringer i ny last for navident {}", navident);
                    }
                }
            }
        });
        nyeStillingPerioderPerNavident.forEach((navident, nySSPFN) -> {
            if (!gmlStillingPerioderPerNavident.containsKey(navident)) {
                log.info("Ny prosessering har navident {} som ikke finnes i gammel prosessering", navident);
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
