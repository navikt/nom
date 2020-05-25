package no.nav.nom.skjermede.personer.pip;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class PipController {
    private final Cache cache;
    private final MeterRegistry metric;

    @GetMapping("/skjermet")
    public boolean isSkjermet(@RequestParam String personident) {
        metric.counter("calls").increment();
        return cache.isSkjermet(personident);
    }
}
