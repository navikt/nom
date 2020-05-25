package no.nav.nom.skjermede.personer.skjerming;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Profile("!local & !test")
@RequiredArgsConstructor
@Component
public class RepositoryMetricFactory {

    private final MeterRegistry meterRegistry;

    private final SkjermetPersonRepository repository;

    @Scheduled(cron = "${app.scheduledTasks.repositoryMetrics}")
    private void scheduledMetricExport(){
        meterRegistry.gauge("database.skjermet.gauge", Tags.of("type", "ansatt", "status", "active"), repository.countAllActiveSkjermetAnsatte());
        meterRegistry.gauge("database.skjermet.gauge", Tags.of("type", "familie", "status", "active"), repository.countAllActiveSkjermetFamilie());
    }
}
