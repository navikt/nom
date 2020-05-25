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
        meterRegistry.gauge("database_ressurs_gauge", Tags.of("type", Ressurstype.INTERN.toString(), "status", "active"), repository.countAllValid(Ressurstype.INTERN.toString()));
        meterRegistry.gauge("database_ressurs_gauge", Tags.of("type", Ressurstype.EKSTERN.toString(), "status", "active"), repository.countAllValid(Ressurstype.EKSTERN.toString()));
    }
}
