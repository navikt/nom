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
        meterRegistry.gauge("database.skjermet.count", Tags.of("count", "distinct", "type", "ansatt", "validation", "valid"), repository.countDistinctSkjermetAnsatte());
        meterRegistry.gauge("database.skjermet.count", Tags.of("count", "distinct", "type", "familie", "validation", "valid"), repository.countDistinctSkjermetFamilie());
    }
}
