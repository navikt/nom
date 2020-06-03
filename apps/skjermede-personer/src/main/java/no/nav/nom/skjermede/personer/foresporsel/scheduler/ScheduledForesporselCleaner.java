package no.nav.nom.skjermede.personer.foresporsel.scheduler;

import lombok.RequiredArgsConstructor;
import no.nav.nom.skjermede.personer.common.LeaderElector;
import no.nav.nom.skjermede.personer.foresporsel.ForesporselRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Profile("!local & !test")
@RequiredArgsConstructor
public class ScheduledForesporselCleaner {

    private final ForesporselRepository foresporselRepository;
    private final LeaderElector leaderElector;

    @Scheduled(cron = "0 30 0 * * *")
    public void scheduledCleaner() {
        if (leaderElector.isLeader()) {
            foresporselRepository.deleteAllByEpostSendtBeforeOrBekreftetTrue(LocalDateTime.now().minusDays(14));
        }
    }
}
