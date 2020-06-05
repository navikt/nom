package no.nav.nom.skjermede.personer.tps;

import lombok.RequiredArgsConstructor;
import no.nav.nom.skjermede.personer.common.LeaderElector;
import no.nav.nom.skjermede.personer.skjerming.SkjermetPersonRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Profile("!local & !test")
@RequiredArgsConstructor
public class ScheduledTpsSender {

    private final TpsService tpsService;
    private final LeaderElector leaderElector;
    private final SkjermetPersonRepository skjermetPersonRepository;

    @Scheduled(fixedDelay = 5000, initialDelay = 5000)
    private void sendTilTps() {
        if (leaderElector.isLeader()) {
            skjermetPersonRepository.getAllIkkeSendtTilTps().stream().forEach(tpsService::sendAndUpdate);
        }
    }
}
