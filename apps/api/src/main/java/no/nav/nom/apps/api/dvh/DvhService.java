package no.nav.nom.apps.api.dvh;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.nom.apps.api.common.DateUtils;
import no.nav.nom.apps.api.dvh.dto.DvhLeder;
import no.nav.nom.apps.api.orgTilknytning.DvhOrgtilknytning;
import no.nav.nom.apps.api.orgTilknytning.contract.Orgtilknytning;
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

import static java.util.stream.Collectors.toUnmodifiableList;

@Slf4j
@Service
@RequiredArgsConstructor
public class DvhService {

    private final DvhRepository dvhRepository;
    private final IRessursService ressursService;
    private final IOrgenhetService orgenhetService;

    public void saveOrgtilknytning(List<DvhOrgtilknytning> koblinger) {
        val cachedEnheter = orgenhetService.getAllEnheter().stream()
                .collect(Collectors.groupingBy(e -> e.getAgressoId() + "" + e.getOrgNiv())).values().stream()
                .map(l -> l.stream().max(Comparator.comparing(IOrgenhet::getGyldigFom)))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toMap(e -> e.getAgressoId() + "" + e.getOrgNiv(), Function.identity()));

        log.debug("saveOrgTilknyninger - cachedEnehter for 8435000ORGENHET is {}-{}", cachedEnheter.get("8435000ORGENHET").getId(), cachedEnheter.get("8435000ORGENHET").getNavn());
        final var orgenhetAndRessursMissing = new LinkedList<Orgtilknytning>();
        final var orgenhetMissing = new LinkedList<Orgtilknytning>();
        final var ressursMissing = new LinkedList<Orgtilknytning>();

        dvhRepository.saveKoblinger(
            koblinger.stream()
                .filter(k -> {
                    final var organisasjonsenhet = cachedEnheter.get(k.getAgressoId() + "" + k.getOrgNiv());
                    final var ressurs = ressursService.findByNavident(k.getNavIdent());
                    if (organisasjonsenhet == null && ressurs.isEmpty()) {
                        orgenhetAndRessursMissing.add(k);
                    } else if (organisasjonsenhet == null) {
                        orgenhetMissing.add(k);
                    } else if (ressurs.isEmpty()) {
                        ressursMissing.add(k);
                    } else {
                        k.setOrgenhetNavn(organisasjonsenhet.getNavn());
                        k.setOrgEnhetId(organisasjonsenhet.getId());
                        return true;
                    }
                    return false;
                })
                .collect(Collectors.groupingBy(orgTilknytning -> orgTilknytning.getNavIdent() + orgTilknytning.getOrgEnhetId()))
                .values().stream()
                .peek(orgTilknytninger -> {
                    for (int i = orgTilknytninger.size() - 1; i >= 0; i--) {
                        final var lastOrgtilknytning = orgTilknytninger.get(i);
                        for (int j = 0; j < i; j++) {
                            final var orgtilknytningCompare = orgTilknytninger.get(j);
                            boolean shouldMerge = DateUtils.isPeriodsOverlapping(
                                orgtilknytningCompare.getGyldigFom(),
                                orgtilknytningCompare.getGyldigTom(),
                                lastOrgtilknytning.getGyldigFom(),
                                lastOrgtilknytning.getGyldigTom()
                            );
                            if (shouldMerge) {
                                if(orgtilknytningCompare.getGyldigFom().isAfter(lastOrgtilknytning.getGyldigFom()))
                                    orgtilknytningCompare.setGyldigFom(lastOrgtilknytning.getGyldigFom());
                                orgTilknytninger.remove(lastOrgtilknytning);
                            }
                        }
                    }
                })
                .flatMap(List::stream)
                .collect(toUnmodifiableList())
        );

        log.info(orgenhetAndRessursMissing.size() + " koblinger hvor orgEnhet og ressurs mangler");
        log.info(orgenhetMissing.size() + " koblinger hvor orgEnhet mangler");
        log.info(ressursMissing.size() + " hvor ressurs mangler");
    }

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
                            if (enhet != null && ressurs.isPresent()) {
                                return true;
                            }
                            return false;
                        })
                        .toList()
        );

        log.info(enhetAndRessursMissing.size() + " leder koblinger hvor ressurs og enhet mangler");
        log.info(enhetMissing.size() + " leder koblinger hvor enhet mangler");
        log.info(ressursMissing.size() + " leder koblinger hvor ressurs mangler");
    }
}
