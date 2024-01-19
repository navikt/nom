package no.nav.nom.apps.api.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

import static no.nav.nom.apps.api.common.AppConfig.SECURE_LOGS;

@Component
@Slf4j
public class InitialSecureLogsStatement {
    @PostConstruct
    public void init() {
        log.info(SECURE_LOGS, "Securelogs check - application started");
    }
}