package no.nav.nom.skjermede.personer.pip;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
@NoArgsConstructor
public class Cache {

    static private final HashMap<String, Skjermingsperiode> skjermedePersoner = new HashMap<>();

    // Må holde en referanse til objektet, blir ellers tatt av garbage collector, og stopper eksportere metrikker.
    private Gauge cacheGauge;

    @Autowired
    public Cache(MeterRegistry meterRegistry){
        cacheGauge = Gauge
                .builder("skjermede.personer.cache.gauge", skjermedePersoner, HashMap::size)
                .description("Skjermede personer cache info")
                .register(meterRegistry);
    }

    boolean isSkjermet(String personident) {
        val periode = skjermedePersoner.get(personident);

        return (periode != null) &&
                (periode.fra().isBefore(LocalDateTime.now())) &&
                (periode.til() == null || periode.til().isAfter(LocalDateTime.now()));
    }

    HashMap<String, Skjermingsperiode> getAll() {
        return skjermedePersoner;
    }

    int size() {
        return skjermedePersoner.size();
    }

    void store(String personident, Skjermingsperiode periode) {
        if(periode.til() == null || periode.til().isAfter(LocalDateTime.now())) {
            skjermedePersoner.put(personident, periode);
        }
    }

}
