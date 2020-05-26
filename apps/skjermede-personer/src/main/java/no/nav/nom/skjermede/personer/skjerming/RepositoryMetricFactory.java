package no.nav.nom.skjermede.personer.skjerming;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Profile("!local & !test")
@Component
public class RepositoryMetricFactory {

    private final MeterRegistry meterRegistry;

    private final SkjermetPersonRepository skjermetPersonRepository;

    private final AtomicLong ansattActive;

    private final AtomicLong familieActive;

    public RepositoryMetricFactory(MeterRegistry meterRegistry, SkjermetPersonRepository skjermetPersonRepository){
        this.meterRegistry = meterRegistry;
        this.skjermetPersonRepository = skjermetPersonRepository;

        ansattActive = meterRegistry.gauge("database.skjermet.gauge", Tags.of("type", "ansatt", "status", "aktiv"), new AtomicLong(0));
        familieActive = meterRegistry.gauge("database.skjermet.gauge", Tags.of("type", "familie", "status", "aktiv"), new AtomicLong(0));
    }

    @Scheduled(cron = "${app.scheduledTasks.repositoryMetrics}")
    private void scheduledMetricExport(){
        ansattActive.set(skjermetPersonRepository.countAllActiveSkjermetAnsatte());
        familieActive.set(skjermetPersonRepository.countAllActiveSkjermetFamilie());
    }
}
