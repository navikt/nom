package no.nav.nom.skjermede.personer.foresporsel.scheduler;

import lombok.RequiredArgsConstructor;
import no.nav.nom.skjermede.personer.foresporsel.ForesporselRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.Period;

@Component
@RequiredArgsConstructor
public class ScheduledForesporselCleaner {

    private final ForesporselRepository foresporselRepository;

    @Scheduled(cron = "0 30 0 * * *")
    public void scheduledCleaner() {
        foresporselRepository.deleteAllByEpostSendtBeforeAndBekreftetFalseOrBekreftetNull(LocalDateTime.now().minus(Period.ofDays(14)));
    }
}
