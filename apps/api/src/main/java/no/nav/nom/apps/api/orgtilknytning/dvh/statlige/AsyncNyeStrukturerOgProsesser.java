package no.nav.nom.apps.api.orgtilknytning.dvh.statlige;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.dvh.dto.DvhRessursStilling;
import no.nav.nom.apps.api.orgtilknytning.dvh.StrukturerteStillingPerioderForNavident;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
@Slf4j
public class AsyncNyeStrukturerOgProsesser {
    private final NyeRessursStillingStrukturerer nyeRessursStillingStrukturerer;
    private final NyeRessursStillingProcessor nyeRessursStillingProcessor;
    final KafkaTemplate<String, StrukturerteStillingPerioderForNavident> dvhLastResultProducerTemplate;


    public AsyncNyeStrukturerOgProsesser(
            NyeRessursStillingStrukturerer nyeRessursStillingStrukturerer,
            NyeRessursStillingProcessor nyeRessursStillingProcessor,
            KafkaTemplate<String, StrukturerteStillingPerioderForNavident> dvhLastResultProducerTemplate
    ) {
        this.nyeRessursStillingStrukturerer = nyeRessursStillingStrukturerer;
        this.nyeRessursStillingProcessor = nyeRessursStillingProcessor;
        this.dvhLastResultProducerTemplate = dvhLastResultProducerTemplate;
    }

    @Async
    public CompletableFuture<Map<String, StrukturerteStillingPerioderForNavident>> asyncStrukturerStillingPerioderPerNavident(List<DvhRessursStilling> ressursStillinger) {
        LocalDateTime start = LocalDateTime.now();

        var result = nyeRessursStillingProcessor.process(
            nyeRessursStillingStrukturerer.strukturerStilingPerioderPerNavident(ressursStillinger)
        );

        persistDvhLastResult(result);
        log.info("Lastet og prosessert ressursStillinger på nye måten. Tok {}", durationAsH24(start));

        return CompletableFuture.completedFuture(result);
    }

    private void persistDvhLastResult(Map<String, StrukturerteStillingPerioderForNavident> result) {
        result.forEach(dvhLastResultProducerTemplate::sendDefault);
    }

    private String durationAsH24(LocalDateTime start) {
        Duration duration = Duration.between(start, LocalDateTime.now());
        return  "%02d:%02d:%02d".formatted(duration.toHoursPart(), duration.toMinutesPart(), duration.toSecondsPart());
    }
}
