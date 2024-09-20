package no.nav.nom.apps.api.common;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class MetricConfig {

    private final MeterRegistry meterRegistry;
    private final RessursStateRepository ressursStateRepository;

    public MetricConfig(MeterRegistry meterRegistry, RessursStateRepository ressursStateRepository) {
        this.meterRegistry = meterRegistry;
        this.ressursStateRepository = ressursStateRepository;
        registerRessursMetrics();
    }

    private final static String METRICS_GAUGE_RESSURSER = "nom_ressurser_state";

    private void registerRessursMetrics() {
        Gauge.builder(METRICS_GAUGE_RESSURSER, ressursStateRepository, RessursStateRepository::count)         .tags("type", "alle") .tags("utvalg", "alle")     .register(meterRegistry);
        Gauge.builder(METRICS_GAUGE_RESSURSER, ressursStateRepository, RessursStateRepository::countEksterne) .tags("type", "alle") .tags("utvalg", "eksterne") .register(meterRegistry);
        Gauge.builder(METRICS_GAUGE_RESSURSER, ressursStateRepository, RessursStateRepository::countKommunale).tags("type", "alle") .tags("utvalg", "kommunale").register(meterRegistry);
        Gauge.builder(METRICS_GAUGE_RESSURSER, ressursStateRepository, RessursStateRepository::countStatlige) .tags("type", "alle") .tags("utvalg", "statlige") .register(meterRegistry);

        Gauge.builder(METRICS_GAUGE_RESSURSER, ressursStateRepository, RessursStateRepository::countIkkeAvsluttede)         .tags("type", "ikke_avsluttede") .tags("utvalg", "alle")     .register(meterRegistry);
        Gauge.builder(METRICS_GAUGE_RESSURSER, ressursStateRepository, RessursStateRepository::countIkkeAvsluttedeEksterne) .tags("type", "ikke_avsluttede") .tags("utvalg", "eksterne") .register(meterRegistry);
        Gauge.builder(METRICS_GAUGE_RESSURSER, ressursStateRepository, RessursStateRepository::countIkkeAvsluttedeKommunale).tags("type", "ikke_avsluttede") .tags("utvalg", "kommunale").register(meterRegistry);
        Gauge.builder(METRICS_GAUGE_RESSURSER, ressursStateRepository, RessursStateRepository::countIkkeAvsluttedeStatlige) .tags("type", "ikke_avsluttede") .tags("utvalg", "statlige") .register(meterRegistry);

        Gauge.builder(METRICS_GAUGE_RESSURSER, ressursStateRepository, RessursStateRepository::countFremtidige)         .tags("type", "fremtidige") .tags("utvalg", "alle")     .register(meterRegistry);
        Gauge.builder(METRICS_GAUGE_RESSURSER, ressursStateRepository, RessursStateRepository::countFremtidigeEksterne) .tags("type", "fremtidige") .tags("utvalg", "eksterne") .register(meterRegistry);
        Gauge.builder(METRICS_GAUGE_RESSURSER, ressursStateRepository, RessursStateRepository::countFremtidigeKommunale).tags("type", "fremtidige") .tags("utvalg", "kommunale").register(meterRegistry);
        Gauge.builder(METRICS_GAUGE_RESSURSER, ressursStateRepository, RessursStateRepository::countFremtidigeStatlige) .tags("type", "fremtidige") .tags("utvalg", "statlige") .register(meterRegistry);
    }

    // OLD BELOW

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

//


}
