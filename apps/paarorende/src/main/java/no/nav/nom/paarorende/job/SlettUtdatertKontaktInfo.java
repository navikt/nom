package no.nav.nom.paarorende.job;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.paarorende.jpa.KontaktinfoRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SlettUtdatertKontaktInfo {

    private static final long TWICE_DAYLY = 12*60*60*1000;

    private final KontaktinfoRepository kontaktinfoRepository;

    public SlettUtdatertKontaktInfo(KontaktinfoRepository kontaktinfoRepository) {
        this.kontaktinfoRepository = kontaktinfoRepository;
    }

    @Scheduled(initialDelay = 5000, fixedDelay = TWICE_DAYLY)
    public void run() {
        var antallSlettet = kontaktinfoRepository.deleteOldKontaktInfo();

        if (antallSlettet > 0) {
            log.info("Slettet {} kontaktinfo", antallSlettet);
        }
    }
}
