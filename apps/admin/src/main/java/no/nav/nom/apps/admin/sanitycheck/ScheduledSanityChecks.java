package no.nav.nom.apps.admin.sanitycheck;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.admin.sanitycheck.service.RessursSanityCheckService;
import no.nav.nom.contract.nom.state.RessursState;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.List;

@Slf4j
public class ScheduledSanityChecks {
    private RessursSanityCheckService ressursSanityCheckService;

    @Scheduled(cron = "* */15 * * * *") // Every 15 minutes
    public void runDuplicateRessursSanityCheck() {
        List<RessursState>  result = ressursSanityCheckService.finnDuplikateRessurserPaaSammeNavident();
        if (!result.isEmpty()) {
            log.error("ALERT: Sanity check fant duplikate ressurser på samme navident: {}", result.stream().map(RessursState::navident).toList());
        }
    }
}
