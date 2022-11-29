package no.nav.nom.apps.api.dvh;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.nom.apps.api.dvh.dto.DvhLeder;
import no.nav.nom.apps.api.kobling.DvhKobling;
import no.nav.nom.apps.api.kobling.contract.Kobling;
import no.nav.nom.apps.api.orgenhet.contract.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.contract.IOrgenhetService;
import no.nav.nom.apps.api.ressurs.IRessursService;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toUnmodifiableList;

@Slf4j
@Service
@RequiredArgsConstructor
public class DvhService {

    private final DvhRepository dvhRepository;
    private final IRessursService ressursService;
    private final IOrgenhetService orgenhetService;

    public void saveKoblinger(List<DvhKobling> koblinger) {
        val cachedEnheter = orgenhetService.getAllEnheter().stream()
                .collect(Collectors.groupingBy(e -> e.getAgressoId() + "" + e.getOrgNiv())).values().stream()
                .map(l -> l.stream().max(Comparator.comparing(IOrgenhet::getGyldigFom)))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toMap(e -> e.getAgressoId() + "" + e.getOrgNiv(), Function.identity()));

        final var orgenhetAndRessursMissing = new LinkedList<Kobling>();
        final var orgenhetMissing = new LinkedList<Kobling>();
        final var ressursMissing = new LinkedList<Kobling>();

        dvhRepository.saveKoblinger(
                koblinger.stream()
                        .filter(k -> {
                            val organisasjonsenhet = cachedEnheter.get(k.getAgressoId() + "" + k.getOrgNiv());
                            val ressurs = ressursService.findByNavident(k.getNavIdent());
                            if (organisasjonsenhet == null && ressurs.isEmpty()) {
                                orgenhetAndRessursMissing.add(k);
                            } else if (organisasjonsenhet == null) {
                                orgenhetMissing.add(k);
                            } else if (ressurs.isEmpty()) {
                                ressursMissing.add(k);
                            } else {
                                k.setOrgenhetNavn(organisasjonsenhet.getNavn());
                                return true;
                            }
                            return false;
                        })
                        .collect(toUnmodifiableList())
        );

        log.warn(orgenhetAndRessursMissing.size() + " koblinger hvor orgEnhet og ressurs mangler");
        log.warn(orgenhetMissing.size() + " koblinger hvor orgEnhet");
        log.warn(ressursMissing.size() + " hvor ressurs mangler");
    }

    public void saveLedere(List<DvhLeder> ledere) {
        val cachedEnheter = orgenhetService.getAllEnheter().stream()
                .collect(Collectors.groupingBy(IOrgenhet::getUniqueAgressoId)).values().stream()
                .map(l -> l.stream().max(Comparator.comparing(IOrgenhet::getGyldigFom)))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toMap(IOrgenhet::getUniqueAgressoId, Function.identity()));

        dvhRepository.saveLedere(
                ledere.stream()
                        .filter(l -> {
                            val enhet = cachedEnheter.get(l.getOrgEnhetNr() + "-" + l.getOrgenhetNavn() + "-" + l.getOrgNiv());
                            val ressurs = ressursService.findByNavident(l.getNavIdent());
                            if (enhet == null && ressurs.isEmpty()) {
                                log.info("Leder relasjon mislyktes, enhet: " + l.getOrgEnhetNr() + ", navIdent: " + l.getNavIdent() + ", " + "Enhet og ressurs mangler");
                            } else if (enhet == null) {
                                log.info("Leder relasjon mislyktes, enhet: " + l.getOrgEnhetNr() + ", navIdent: " + l.getNavIdent() + ", " + "Enhet mangler");
                            } else if (ressurs.isEmpty()) {
                                log.info("Leder relasjon mislyktes, enhet: " + l.getOrgEnhetNr() + ", navIdent: " + l.getNavIdent() + ", " + "Ressurs mangler");
                            }
                            if (enhet != null && ressurs.isPresent()) {
                                return true;
                            }
                            return false;
                        })
                        .toList()
        );
    }
}
