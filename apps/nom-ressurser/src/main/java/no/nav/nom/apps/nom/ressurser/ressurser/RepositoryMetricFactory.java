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

    private final AtomicLong inactiveIntern;

    private final AtomicLong inactiveEkstern;

    @Autowired
    public RepositoryMetricFactory(MeterRegistry meterRegistry, RessursRepository ressursRepository){
        this.meterRegistry = meterRegistry;
        this.ressursRepository = ressursRepository;
        anyIntern = meterRegistry.gauge("database.ressurs.gauge", Tags.of("type", Ressurstype.INTERN.toString(), "status", "any"), new AtomicLong(0));
        inactiveIntern = meterRegistry.gauge("database.ressurs.gauge", Tags.of("type", Ressurstype.INTERN.toString(), "status", "inactive"), new AtomicLong(0));
        anyEkstern = meterRegistry.gauge("database.ressurs.gauge", Tags.of("type", Ressurstype.EKSTERN.toString(), "status", "any"), new AtomicLong(0));
        inactiveEkstern = meterRegistry.gauge("database.ressurs.gauge", Tags.of("type", Ressurstype.EKSTERN.toString(), "status", "inactive"), new AtomicLong(0));
    }

    @Scheduled(cron = "${app.scheduledTasks.repositoryMetrics}")
    private void scheduledMetricExport(){
        anyIntern.set(ressursRepository.countAllByRessurstype(Ressurstype.INTERN));
        anyEkstern.set(ressursRepository.countAllByRessurstype(Ressurstype.EKSTERN));
        inactiveIntern.set(ressursRepository.countInactiveByRessurstype(Ressurstype.INTERN.toString()));
        inactiveEkstern.set(ressursRepository.countInactiveByRessurstype(Ressurstype.EKSTERN.toString()));
    }
}
