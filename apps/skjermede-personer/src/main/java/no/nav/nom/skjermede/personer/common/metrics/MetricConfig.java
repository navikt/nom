package no.nav.nom.skjermede.personer.common.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.atomic.AtomicLong;

@Configuration
@RequiredArgsConstructor
public class MetricConfig {

    private final MeterRegistry meterRegistry;

    @Bean
    public AtomicLong ansattActiveGauge(){
        return meterRegistry.gauge("database.skjermet.gauge", Tags.of("type", "ansatt", "status", "aktiv"), new AtomicLong(0));
    }

    @Bean
    public AtomicLong familieActiveGauge(){
        return meterRegistry.gauge("database.skjermet.gauge", Tags.of("type", "familie", "status", "aktiv"), new AtomicLong(0));
    }

    @Bean
    public AtomicLong foresporselActiveGauge(){
        return meterRegistry.gauge("foresporsel.gauge", Tags.of("epost", "sendt"), new AtomicLong(0));
    }

    @Bean
    public AtomicLong foresporselEpostNullGauge(){
        return meterRegistry.gauge("foresporsel.gauge", Tags.of("epost", "null"), new AtomicLong(0));
    }

    @Bean
    public Counter foresporselGodkjentCounter(){
        return Counter
                .builder("foresporsel.count")
                .tags("status", "godkjent")
                .register(meterRegistry);
    }

    @Bean
    public Counter foresporselOpprettetCounter(){
        return Counter
                .builder("foresporsel.count")
                .tags("status", "opprettet")
                .register(meterRegistry);
    }

    @Bean
    public Counter foresporselSlettetCounter(){
        return Counter
                .builder("foresporsel.count")
                .tags("status", "slettet")
                .register(meterRegistry);
    }

    @Bean
    public Counter tpsEndringCounter(){
        return Counter
                .builder("tps.messages.sent.count")
                .tags("type", "endring")
                .register(meterRegistry);
    }

    @Bean
    public Counter tpsOpphorCounter(){
        return Counter
                .builder("tps.messages.sent.count")
                .tags("type", "opphor")
                .register(meterRegistry);
    }

    @Bean
    public Counter kafkaSkjermetCounter(){
        return Counter
                .builder("kafka.messages.produced.count")
                .tags("topic", "aapen-skjermede-personer-v1")
                .register(meterRegistry);
    }
}
