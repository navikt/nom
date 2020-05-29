package no.nav.nom.apps.nom.ressurser.common;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.RequiredArgsConstructor;
import no.nav.nom.contract.nom.dto.Ressurstype;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.atomic.AtomicLong;

@Configuration
@RequiredArgsConstructor
public class MetricConfig {

    private final MeterRegistry meterRegistry;

    @Bean
    public AtomicLong activeInternGauge(){
        return meterRegistry.gauge("database.ressurs.gauge", Tags.of("type", Ressurstype.INTERN.toString(), "status", "aktiv"), new AtomicLong(0));
    }

    @Bean
    public AtomicLong inactiveInternGauge(){
        return meterRegistry.gauge("database.ressurs.gauge", Tags.of("type", Ressurstype.INTERN.toString(), "status", "inaktiv"), new AtomicLong(0));
    }

    @Bean
    public AtomicLong activeEksternGauge(){
        return meterRegistry.gauge("database.ressurs.gauge", Tags.of("type", Ressurstype.EKSTERN.toString(), "status", "aktiv"), new AtomicLong(0));
    }

    @Bean
    public AtomicLong inactiveEksternGauge(){
        return meterRegistry.gauge("database.ressurs.gauge", Tags.of("type", Ressurstype.EKSTERN.toString(), "status", "inaktiv"), new AtomicLong(0));
    }

    @Bean
    public Counter dvhReadCounter(){
        return Counter
                .builder("dvh.messages.read.count")
                .register(meterRegistry);
    }

    @Bean
    public Counter kakfaRessursCounter(){
        return Counter
                .builder("kafka.messages.produced.count")
                .tag("topic", "aapen-nom-ressurs-v1")
                .register(meterRegistry);
    }
}
