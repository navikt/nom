package no.nav.nom.skjermede.personer.common;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import no.nav.nom.skjermede.personer.foresporsel.ForesporselRepository;
import no.nav.nom.skjermede.personer.skjerming.SkjermetPersonRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Profile("!local & !test")
@Component
public class RepositoryMetricFactory {

    private final MeterRegistry meterRegistry;

    private final SkjermetPersonRepository skjermetPersonRepository;

    private final ForesporselRepository foresporselRepository;

    private final AtomicLong ansattActive;

    private final AtomicLong familieActive;

    private final AtomicLong foresporselActive;

    public RepositoryMetricFactory(MeterRegistry meterRegistry, SkjermetPersonRepository skjermetPersonRepository, ForesporselRepository foresporselRepository){
        this.meterRegistry = meterRegistry;
        this.skjermetPersonRepository = skjermetPersonRepository;
        this.foresporselRepository = foresporselRepository;

        ansattActive = meterRegistry.gauge("database.skjermet.gauge", Tags.of("type", "ansatt", "status", "aktiv"), new AtomicLong(0));
        familieActive = meterRegistry.gauge("database.skjermet.gauge", Tags.of("type", "familie", "status", "aktiv"), new AtomicLong(0));
        foresporselActive = meterRegistry.gauge("foresporsel.gauge", Tags.of("status", "aktiv"), new AtomicLong(0));
    }

    @Scheduled(cron = "${app.scheduledTasks.repositoryMetrics}")
    private void scheduledMetricExport(){
        ansattActive.set(skjermetPersonRepository.countAllActiveSkjermetAnsatte());
        familieActive.set(skjermetPersonRepository.countAllActiveSkjermetFamilie());
        foresporselActive.set(foresporselRepository.countAllByBekreftetFalse());
    }
}
