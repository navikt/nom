package no.nav.nom.apps.navident.common;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class MetricConfig {

    private final MeterRegistry meterRegistry;

    @Bean
    public Counter eventSourceProducedCounter(){
        return Counter
                .builder("kafka.messages.produced.count")
                .tag("topic", "nav-ident")
                .register(meterRegistry);
    }

    @Bean
    public Counter eventSourceConsumedCounter(){
        return Counter
                .builder("kafka.messages.consumed.count")
                .tag("topic", "nav-ident")
                .register(meterRegistry);
    }
}
