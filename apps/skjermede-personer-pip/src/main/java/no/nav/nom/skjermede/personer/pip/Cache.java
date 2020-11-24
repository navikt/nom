package no.nav.nom.skjermede.personer.pip;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
@NoArgsConstructor
public class Cache {

    static private final ConcurrentHashMap<String, Skjermingsperiode> skjermedePersoner = new ConcurrentHashMap<>();

    // Må holde en referanse til objektet, blir ellers tatt av garbage collector, og stopper eksportere metrikker.
    private Gauge cacheGauge;

    @Autowired
    public Cache(MeterRegistry meterRegistry){
        cacheGauge = Gauge
                .builder("skjermede.personer.cache.gauge", skjermedePersoner, ConcurrentHashMap::size)
                .description("Skjermede personer cache info")
                .register(meterRegistry);
    }

    public boolean isSkjermet(String personident) {
        val periode = skjermedePersoner.get(personident);

        return (periode != null) &&
                (periode.getFra().isBefore(LocalDateTime.now())) &&
                (periode.getTil() == null || periode.getTil().isAfter(LocalDateTime.now()));
    }

    public ConcurrentHashMap<String, Skjermingsperiode> getAll() {
        return skjermedePersoner;
    }

    public int size() {
        return skjermedePersoner.size();
    }

    void store(String personident, Skjermingsperiode periode) {
            skjermedePersoner.put(personident, periode);
    }

}
