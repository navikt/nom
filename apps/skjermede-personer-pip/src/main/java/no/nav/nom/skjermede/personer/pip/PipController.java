package no.nav.nom.skjermede.personer.pip;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class PipController {
    private final Cache cache;

    @GetMapping("/skjermet")
    public boolean isSkjermet(@RequestParam String personident) {
        return cache.isSkjermet(personident);
    }
}
