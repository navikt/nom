package no.nav.nom.apps.api.common;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.atomic.AtomicInteger;

@Configuration
@RequiredArgsConstructor
public class MetricConfig {

    private final MeterRegistry meterRegistry;

    // TODO Må telles opp
    @Bean
    public AtomicInteger ressurserGauge() {
        return meterRegistry.gauge("nom.hendelser", Tags.of("type", "ressurser"), new AtomicInteger(0));
    }

    // TODO Må telles opp
    @Bean
    public AtomicInteger ressurserHendelserGauge() {
        return meterRegistry.gauge("nom.hendelser", Tags.of("type", "ressurser.hendelser"), new AtomicInteger(0));
    }

    @Bean
    public AtomicInteger enheterGauge() {
        return meterRegistry.gauge("nom.hendelser", Tags.of("type", "enheter"), new AtomicInteger(0));
    }

    @Bean
    public AtomicInteger enheterHendelserGauge() {
        return meterRegistry.gauge("nom.hendelser", Tags.of("type", "enheter.hendelser"), new AtomicInteger(0));
    }
}
