package no.nav.nom.apps.nom.ressurser.ressurser;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import no.nav.nom.contract.nom.dto.Ressurstype;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Profile("!local")
@Component
public class RepositoryMetricFactory {

    private final MeterRegistry meterRegistry;

    private final RessursRepository ressursRepository;

    private final AtomicLong anyIntern;

    private final AtomicLong anyEkstern;

    private final AtomicLong activeIntern;

    private final AtomicLong activeEkstern;

    @Autowired
    public RepositoryMetricFactory(MeterRegistry meterRegistry, RessursRepository ressursRepository){
        this.meterRegistry = meterRegistry;
        this.ressursRepository = ressursRepository;
        anyIntern = meterRegistry.gauge("database.ressurs.gauge", Tags.of("type", Ressurstype.INTERN.toString(), "status", "any"), new AtomicLong(0));
        activeIntern = meterRegistry.gauge("database.ressurs.gauge", Tags.of("type", Ressurstype.INTERN.toString(), "status", "aktiv"), new AtomicLong(0));
        anyEkstern = meterRegistry.gauge("database.ressurs.gauge", Tags.of("type", Ressurstype.EKSTERN.toString(), "status", "any"), new AtomicLong(0));
        activeEkstern = meterRegistry.gauge("database.ressurs.gauge", Tags.of("type", Ressurstype.EKSTERN.toString(), "status", "aktiv"), new AtomicLong(0));
    }

    @Scheduled(cron = "${app.scheduledTasks.repositoryMetrics}")
    private void scheduledMetricExport(){
        anyIntern.set(ressursRepository.countAllByRessurstype(Ressurstype.INTERN));
        anyEkstern.set(ressursRepository.countAllByRessurstype(Ressurstype.EKSTERN));
        activeIntern.set(ressursRepository.countActiveByRessurstype(Ressurstype.INTERN.toString()));
        activeEkstern.set(ressursRepository.countActiveByRessurstype(Ressurstype.EKSTERN.toString()));
    }
}
