package no.nav.nom.apps.api.dvh;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.nom.apps.api.dvh.dto.DvhLeder;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.IOrgenhetService;
import no.nav.nom.apps.api.ressurs.IRessursService;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DvhService {

    private final DvhRepository dvhRepository;
    private final IRessursService ressursService;
    private final IOrgenhetService orgenhetService;

    public void saveLedere(List<DvhLeder> ledere) {
        val cachedEnheter = orgenhetService.getAllEnheter().stream()
                .collect(Collectors.groupingBy(IOrgenhet::getUniqueAgressoId)).values().stream()
                .map(l -> l.stream().max(Comparator.comparing(IOrgenhet::getGyldigFom)))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toMap(IOrgenhet::getUniqueAgressoId, Function.identity()));

        final var enhetAndRessursMissing = new LinkedList<DvhLeder>();
        final var enhetMissing = new LinkedList<DvhLeder>();
        final var ressursMissing = new LinkedList<DvhLeder>();

        dvhRepository.saveLedere(
                ledere.stream()
                        .filter(l -> {
                            val enhet = cachedEnheter.get(l.getOrgEnhetNr() + "-" + l.getOrgenhetNavn() + "-" + l.getOrgNiv());
                            val ressurs = ressursService.findByNavident(l.getNavIdent());
                            if (enhet == null && ressurs.isEmpty()) {
                                enhetAndRessursMissing.add(l);
                            } else if (enhet == null) {
                                enhetMissing.add(l);
                            } else if (ressurs.isEmpty()) {
                                ressursMissing.add(l);
                            }
                            return enhet != null && ressurs.isPresent();
                        })
                        .toList()
        );

        log.info(enhetAndRessursMissing.size() + " leder koblinger hvor ressurs og enhet mangler");
        log.info(enhetMissing.size() + " leder koblinger hvor enhet mangler");
        log.info(ressursMissing.size() + " leder koblinger hvor ressurs mangler");
    }
}
