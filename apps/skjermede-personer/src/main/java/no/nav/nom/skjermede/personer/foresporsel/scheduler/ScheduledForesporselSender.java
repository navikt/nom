package no.nav.nom.skjermede.personer.foresporsel.scheduler;

import lombok.RequiredArgsConstructor;
import no.nav.nom.skjermede.personer.foresporsel.ForesporselService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ScheduledForesporselSender {

    private final ForesporselService foresporselService;

    @Scheduled(fixedRate = 60000, initialDelay = 30000)
    public void scheduledSender() {
        foresporselService.sendForesporsler();
    }
}
