package no.nav.nom.apps.api.orgtilknytning.dvh;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.clients.dvhords.DvhOrdsService;
import no.nav.nom.apps.api.dvh.dto.DvhRessursStilling;
import no.nav.nom.apps.api.orgtilknytning.dvh.statlige.AsyncNyeStrukturerOgProsesser;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class RessursStillingLoader {
    private final AsyncNyeStrukturerOgProsesser asyncNyeStrukturerOgProsesser;
    private final DvhOrdsService dvhOrdsService;

    public RessursStillingLoader(
        AsyncNyeStrukturerOgProsesser asyncNyeStrukturerOgProsesser, DvhOrdsService dvhOrdsService
    ) {
        this.asyncNyeStrukturerOgProsesser = asyncNyeStrukturerOgProsesser;
        this.dvhOrdsService = dvhOrdsService;
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
                log.debug("Feil ved NYE lasting og prosessering av ressursStillinger for navident {}: {}", navident, sspfn.errors());
            }
        });
    }

    public List<DvhRessursStilling> load() {
        LocalDateTime start = LocalDateTime.now();

        var ressursStillinger = dvhOrdsService.getStatligeTilknytninger();

        log.info("Lastet {} ressursStillinger. Tok {}", ressursStillinger.size(), durationAsH24(start));
        return ressursStillinger;
    }

    private String durationAsH24(LocalDateTime start) {
        Duration duration = Duration.between(start, LocalDateTime.now());
        return  "%02d:%02d:%02d".formatted(duration.toHoursPart(), duration.toMinutesPart(), duration.toSecondsPart());
    }
}
