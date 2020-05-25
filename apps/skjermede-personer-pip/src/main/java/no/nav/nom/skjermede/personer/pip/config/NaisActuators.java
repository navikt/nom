package no.nav.nom.skjermede.personer.pip.config;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal")
public class NaisActuators {

    @GetMapping("/isAlive")
    public void isAlive() {}

    @GetMapping("/isReady")
    public void isReady() {}
}

