package no.nav.nom.apps.nom.ressurser.ressurser;

import lombok.RequiredArgsConstructor;
import no.nav.nom.contract.nom.dto.Ressurstype;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
@RequiredArgsConstructor
public class RepositoryMetricFactory {

    private final RessursRepository ressursRepository;

    private final AtomicLong activeInternGauge;

    private final AtomicLong inactiveInternGauge;

    private final AtomicLong activeEksternGauge;

    private final AtomicLong inactiveEksternGauge;

    @Scheduled(cron = "${app.scheduledTasks.repositoryMetrics}")
    private void scheduledMetricExport(){
        activeInternGauge.set(ressursRepository.countActiveByRessurstype(Ressurstype.INTERN.toString()));
        inactiveInternGauge.set(ressursRepository.countInactiveByRessurstype(Ressurstype.INTERN.toString()));
        activeEksternGauge.set(ressursRepository.countActiveByRessurstype(Ressurstype.EKSTERN.toString()));
        inactiveEksternGauge.set(ressursRepository.countInactiveByRessurstype(Ressurstype.EKSTERN.toString()));
    }
}
