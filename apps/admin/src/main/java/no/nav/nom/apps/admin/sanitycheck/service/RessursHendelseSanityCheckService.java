package no.nav.nom.apps.admin.sanitycheck.service;

import no.nav.nom.apps.admin.dto.ressurs.RessursHendelseKafkaDTO;
import no.nav.nom.apps.admin.events.ressurs.api.RessursHendelseApiRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RessursHendelseSanityCheckService {
    private final Logger log = LoggerFactory.getLogger(RessursHendelseSanityCheckService.class);

    private final RessursHendelseApiRepository hendelseApiRepository;


    public RessursHendelseSanityCheckService(RessursHendelseApiRepository hendelseApiRepository) {
        this.hendelseApiRepository = hendelseApiRepository;
    }

    public List<RessursHendelseKafkaDTO> finnDuplikateHendelser() {
        Instant start = Instant.now();
        List<RessursHendelseKafkaDTO> duplikater = getDuplikateHendelser(hendelseApiRepository.getRessursHendelser())
            .stream()
            .sorted(Comparator.comparing(RessursHendelseKafkaDTO::navident))
            .toList();

        Duration duration = Duration.between(start, Instant.now());
        String timeUsed = "%02d m %02d s %03d ms".formatted(duration.toMinutesPart(), duration.toSecondsPart(), duration.toMillisPart());

        log.debug("Antall Duplikate hendelser = {}, brukte {} ", duplikater.size(), timeUsed);

        return duplikater;
    }

    public List<RessursHendelseKafkaDTO> getDuplikateHendelser(List<RessursHendelseKafkaDTO> alleHendelser) {
        Map<String, List<RessursHendelseKafkaDTO>> hendelserPrNavident = alleHendelser.stream()
            .collect(Collectors.groupingBy(RessursHendelseKafkaDTO::navident, Collectors.mapping(Function.identity(), Collectors.toList())));

        return hendelserPrNavident.values().stream()
            .filter(hendelseList -> hendelseList.size() > 1)
            .flatMap(List::stream)
            .collect(Collectors.toList());
    }

}
