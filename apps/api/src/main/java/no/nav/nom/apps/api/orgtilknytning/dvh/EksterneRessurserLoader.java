package no.nav.nom.apps.api.orgtilknytning.dvh;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.clients.dvhords.DvhOrdsService;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class EksterneRessurserLoader {
    private final DvhOrdsService dvhOrdsService;
    private EksterneRessurserProcessor eksterneRessurserProcessor;

    public EksterneRessurserLoader(EksterneRessurserProcessor eksterneRessurserProcessor, DvhOrdsService dvhOrdsService) {
        this.eksterneRessurserProcessor = eksterneRessurserProcessor;
        this.dvhOrdsService = dvhOrdsService;
    }

    public void loadAndProcess() {
        List<DvhEksternRessurs> eksterneRessurser = load();
        eksterneRessurser.forEach(eksterneRessurserProcessor::process);
    }

    private List<DvhEksternRessurs> load() {
        LocalDateTime start = LocalDateTime.now();

        var eksterneRessurser = dvhOrdsService.getEksterneTilknytninger();

        log.info("Lastet {} eksterneRessurser. Tok {}", eksterneRessurser.size(), durationAsH24(start));
        return eksterneRessurser;
    }

    private String durationAsH24(LocalDateTime start) {
        Duration duration = Duration.between(start, LocalDateTime.now());
        return  "%02d:%02d:%02d".formatted(duration.toHoursPart(), duration.toMinutesPart(), duration.toSecondsPart());
    }
}
