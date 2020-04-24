package no.nav.nom.skjermede.personer.foresporsel.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.skjermede.personer.foresporsel.ForesporselRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduledForesporselCleaner {

    private final ForesporselRepository foresporselRepository;

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void scheduledCleaner() {
        foresporselRepository.deleteAllByEpostSendtBeforeOrBekreftetTrue(LocalDateTime.now().minusDays(14));
        log.info("Forespørsler som er bekreftet eller utgått ble slettet");
    }
}
