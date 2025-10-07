package no.nav.nom.apps.api.dvh;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.nom.apps.api.dvh.dto.DvhLeder;
import no.nav.nom.apps.api.dvh.dto.DvhOrganisering;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;

@Slf4j
@Service
public class DvhService {

    private final DvhRepository dvhRepository;
    private final RessursStateRepository ressursStateRepository;
    private final OrgenhetService orgenhetService;

    public DvhService(
        DvhRepository dvhRepository,
        RessursStateRepository ressursStateRepository,
        OrgenhetService orgenhetService
    ) {
        this.dvhRepository = dvhRepository;
        this.ressursStateRepository = ressursStateRepository;
        this.orgenhetService = orgenhetService;
    }

    public void saveLedere(List<DvhLeder> ledere) {
        val cachedEnheter =
            orgenhetService.getAllEnheter().stream()
                .collect(Collectors.groupingBy(IOrgenhet::getUniqueAgressoId)).values().stream()
                .map(orgenheter -> orgenheter.stream()
                    .filter(o -> !o.endedOnStartDate()) // Ikke returner enheter som er opprettet og avsluttet samme dag
                    .max(Comparator.comparing(IOrgenhet::getGyldigFom))
                )
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
                    val ressurs = ressursStateRepository.findByNavident(l.getNavIdent());
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

        ledere.removeAll(enhetAndRessursMissing);
        ledere.removeAll(ressursMissing);
        ledere.removeAll(enhetMissing);

        log.info(enhetAndRessursMissing.size() + " leder koblinger hvor ressurs og enhet mangler");
        log.info(enhetMissing.size() + " leder koblinger hvor enhet mangler");
        log.info(ressursMissing.size() + " leder koblinger hvor ressurs mangler");

    }

    // TODO: Denne burde løses annerledes - via lytting på endringer i organiseringState
    public void setOrganiserteOrgenheter(List<DvhOrganisering> organiseringer) {
        val cachedEnheter =
            orgenhetService.getAllEnheter().stream()
                .collect(Collectors.groupingBy(IOrgenhet::getUniqueAgressoId))
                .values().stream()
                .map(orgenheter -> orgenheter.stream()
                    .filter(o -> !o.endedOnStartDate()) // Ikke returner enheter som er opprettet og avsluttet samme dag
                    .max(Comparator.comparing(IOrgenhet::getGyldigFom))
                )
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(toMap(IOrgenhet::getUniqueAgressoId, Function.identity()));

        var organiseringerMedMatchPaaOrgenhet = organiseringer.stream()
            .distinct()
            .filter(o -> {
                final var overEnhet = cachedEnheter.get(o.uniqueAgressoIdOver());
                final var underEnhet = cachedEnheter.get(o.uniqueAgressoIdUnder());
                if (overEnhet == null && underEnhet == null) {
                    log.info(
                        "Organisering relasjon mislyktes, overenhet: {}, underenhet: {}, Over- og Underenhet mangler",
                        o.uniqueAgressoIdOver(),
                        o.uniqueAgressoIdUnder()
                    );
                } else if (overEnhet == null) {
                    log.info(
                        "Organisering relasjon mislyktes, overenhet: {}, underenhet: {}, Overenhet mangler",
                        o.uniqueAgressoIdOver(),
                        o.uniqueAgressoIdUnder()
                    );
                } else if (underEnhet == null) {
                    log.info(
                        "Organisering relasjon mislyktes, overenhet: {}, underenhet: {}, Underenhet mangler",
                        o.uniqueAgressoIdOver(),
                        o.uniqueAgressoIdUnder()
                    );
                } else {
                    return true;
                }
                return false;
            })
            .toList();

        var overEnheter = organiseringerMedMatchPaaOrgenhet.stream().map(o -> cachedEnheter.get(o.uniqueAgressoIdOver())).toList();
        var underEnheter = organiseringerMedMatchPaaOrgenhet.stream().map(o -> cachedEnheter.get(o.uniqueAgressoIdUnder())).toList();

        // Add alle overEnheter og underEnheter to organiserteEnheter og behold unike enheter
        var organiserteEnheter = Stream.concat(overEnheter.stream(), underEnheter.stream())
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        orgenhetService.setOrganiserteEnheter(organiserteEnheter);
    }
}
