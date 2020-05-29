package no.nav.nom.skjermede.personer.common;

import lombok.RequiredArgsConstructor;
import no.nav.nom.skjermede.personer.foresporsel.ForesporselRepository;
import no.nav.nom.skjermede.personer.skjerming.SkjermetPersonRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
@RequiredArgsConstructor
public class RepositoryMetricFactory {

    private final SkjermetPersonRepository skjermetPersonRepository;

    private final ForesporselRepository foresporselRepository;

    private final AtomicLong ansattActiveGauge;

    private final AtomicLong familieActiveGauge;

    private final AtomicLong foresporselActiveGauge;

    @Scheduled(cron = "${app.scheduledTasks.repositoryMetrics}")
    private void scheduledMetricExport(){
        ansattActiveGauge.set(skjermetPersonRepository.countAllActiveSkjermetAnsatte());
        familieActiveGauge.set(skjermetPersonRepository.countAllActiveSkjermetFamilie());
        foresporselActiveGauge.set(foresporselRepository.countAllByBekreftetFalse());
    }
}
