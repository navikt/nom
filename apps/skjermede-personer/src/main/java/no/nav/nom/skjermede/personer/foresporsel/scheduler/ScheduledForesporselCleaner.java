package no.nav.nom.skjermede.personer.foresporsel.scheduler;

import lombok.RequiredArgsConstructor;
import no.nav.nom.skjermede.personer.common.platform.LeaderElector;
import no.nav.nom.skjermede.personer.foresporsel.ForesporselRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@Profile("!local & !test")
@RequiredArgsConstructor
public class ScheduledForesporselCleaner {

    private final ForesporselRepository foresporselRepository;
    private final LeaderElector leaderElector;

    @Transactional(value = "transactionManager")
    @Scheduled(cron = "0 30 0 * * *")
    public void scheduledCleaner() {
        if (leaderElector.isLeader()) {
            foresporselRepository.deleteAllByOpprettetDatoBeforeOrBekreftetTrue(LocalDateTime.now().minusDays(14));
        }
    }
}
