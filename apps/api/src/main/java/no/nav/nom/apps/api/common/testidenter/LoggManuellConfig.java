package no.nav.nom.apps.api.common.testidenter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class LoggManuellConfig {
    @Autowired
    ManuelleRessurserOgLedereProperties cfg;

    @EventListener
    public void handleContextRefresh(ContextRefreshedEvent event) {
        log.info("Manuelle ressurser og ledere er aktivert: {}", cfg.enabled());
        if (cfg.enabled()) {
            log.info("Manuelle ressurser: {}", cfg.ressurser());
            log.info("Manuelle ledere: {}", cfg.ledere());
        }
    }
}
