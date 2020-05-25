package no.nav.nom.apps.nom.ressurser.ressurser;

import io.micrometer.core.instrument.MeterRegistry;
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

    private final RessursRepository ressursRepository;

    @Scheduled(cron = "${app.scheduledTasks.repositoryMetrics}")
    private void scheduledMetricExport(){
        meterRegistry.counter("database_ressurs_count", "type", Ressurstype.INTERN.toString(), "status", "any").increment(ressursRepository.countAllByRessurstype(Ressurstype.INTERN));
        meterRegistry.counter("database_ressurs_count", "type", Ressurstype.INTERN.toString(), "status", "inactive").increment(ressursRepository.countInactiveByRessurstype(Ressurstype.INTERN.toString()));
        meterRegistry.counter("database_ressurs_count", "type", Ressurstype.EKSTERN.toString(), "status", "any").increment(ressursRepository.countAllByRessurstype(Ressurstype.EKSTERN));
        meterRegistry.counter("database_ressurs_count", "type", Ressurstype.EKSTERN.toString(), "status", "inactive").increment(ressursRepository.countInactiveByRessurstype(Ressurstype.EKSTERN.toString()));
    }
}
