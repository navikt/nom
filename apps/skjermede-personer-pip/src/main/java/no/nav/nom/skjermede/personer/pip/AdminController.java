package no.nav.nom.skjermede.personer.pip;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;

@RestController("/admin")
@AllArgsConstructor
public class AdminController {
    private final Cache cache;
    private final MeterRegistry metric;

    @GetMapping("/size")
    public int isSkjermet(@RequestParam String personident, @RequestParam String apiKey) {
        validate(apiKey);
        metric.counter("calls").increment();
        return cache.size();
    }

    @GetMapping("/get")
    public HashMap<String, Skjermingsperiode> getAll(@RequestParam String apiKey) {
        validate(apiKey);
        return cache.getAll();
    }

    private void validate(String apiKey) throws RuntimeException {
        if(!apiKey.equals("key")) { throw new RuntimeException("Wrong API key"); }
    }
}
