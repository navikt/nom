package no.nav.nom.apps.nom.ressurser.ressurser;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.RequiredArgsConstructor;
import no.nav.nom.contract.nom.dto.Ressurstype;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Profile("!local")
@RequiredArgsConstructor
@Component
public class RepositoryMetricFactory {

    private final MeterRegistry meterRegistry;

    private final RessursRepository repository;

    @Scheduled(cron = "${app.scheduledTasks.repositoryMetrics}")
    private void scheduledMetricExport(){
        meterRegistry.gauge("database.ressurs.count", Tags.of("count", "distinct", "type", Ressurstype.INTERN.toString(), "validation", "valid"), repository.countDistinctByFunksjonellIdAndValid(Ressurstype.INTERN.toString()));
        meterRegistry.gauge("database.ressurs.count", Tags.of("count", "distinct", "type", Ressurstype.EKSTERN.toString(), "validation", "valid"), repository.countDistinctByFunksjonellIdAndValid(Ressurstype.EKSTERN.toString()));
    }
}
