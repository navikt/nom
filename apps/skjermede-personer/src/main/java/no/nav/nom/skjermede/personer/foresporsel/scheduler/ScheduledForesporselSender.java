package no.nav.nom.skjermede.personer.foresporsel.scheduler;

import lombok.RequiredArgsConstructor;
import no.nav.nom.skjermede.personer.foresporsel.ForesporselService;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Profile("!local")
@RequiredArgsConstructor
public class ScheduledForesporselSender {

    private final ForesporselService foresporselService;

    @Scheduled(fixedDelay = 10000, initialDelay = 10000)
    public void scheduledSender() {
        foresporselService.sendForesporsler();
    }
}
