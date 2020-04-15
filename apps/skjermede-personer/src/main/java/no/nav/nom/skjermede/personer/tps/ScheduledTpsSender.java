package no.nav.nom.skjermede.personer.tps;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.skjermede.personer.skjerming.SkjermetPersonRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("!local & !test")
@RequiredArgsConstructor
public class ScheduledTpsSender {

    private final TpsService tpsService;
    private final SkjermetPersonRepository skjermetPersonRepository;

    @Scheduled(fixedDelay = 5000, initialDelay = 5000)
    private void sendTilTps() {
        skjermetPersonRepository.getAllIkkeSendtTilTps().stream().forEach(tpsService::sendAndUpdate);
    }
}
