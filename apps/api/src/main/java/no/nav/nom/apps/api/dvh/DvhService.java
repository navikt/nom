package no.nav.nom.apps.api.dvh;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.nom.apps.api.dvh.dto.DvhLeder;
import no.nav.nom.apps.api.kobling.DvhKobling;
import no.nav.nom.apps.api.kobling.contract.Kobling;
import no.nav.nom.apps.api.orgenhet.contract.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.contract.IOrgenhetService;
import no.nav.nom.apps.api.ressurs.UnleashRessursServiceSelector;
import org.springframework.stereotype.Service;

import java.util.Collections;
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
    private final UnleashRessursServiceSelector unleashRessursServiceSelector;
    private final IOrgenhetService orgenhetService;

    public void saveKoblinger(List<DvhKobling> koblinger) {

        val list = Collections.synchronizedList(new LinkedList<Kobling>());
        val cachedEnheter = orgenhetService.getAllEnheter().stream()
                .collect(Collectors.groupingBy(e -> e.getAgressoId()+""+e.getOrgNiv())).values().stream()
                .map(l -> l.stream().max(Comparator.comparing(IOrgenhet::getGyldigFom)))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toMap(e -> e.getAgressoId()+""+e.getOrgNiv(), Function.identity()));


        val bulkSize = 10000;
        for(int i = 0; i<koblinger.size(); i+=bulkSize){
            val selection = koblinger.subList(i, Math.min(i + bulkSize, koblinger.size()));
            val ressursByNavIdent = unleashRessursServiceSelector.findByNavidentBulk(selection.stream().map(Kobling::getNavIdent).toList());

            selection.forEach(k -> {
                        val organisasjonsenhet = cachedEnheter.get(k.getAgressoId()+""+k.getOrgNiv());
                        val dvhRessurs = ressursByNavIdent.get(k.getNavIdent());
                        if (organisasjonsenhet == null && dvhRessurs == null) {
                            log.info("Kobling relasjon mislyktes, enhet: "+k.getAgressoId() + ", navIdent: " + k.getNavIdent() + ", " + "Enhet og ressurs mangler");
                        } else if (organisasjonsenhet == null) {
                            log.info("Kobling relasjon mislyktes, enhet: "+k.getAgressoId() + ", navIdent: " + k.getNavIdent() + ", " + "Enhet mangler");
                        } else if (dvhRessurs == null) {
                            log.info("Kobling relasjon mislyktes, enhet: "+k.getAgressoId() + ", navIdent: " + k.getNavIdent() + ", " + "Ressurs mangler");
                        } else {
                            k.setOrgenhetNavn(organisasjonsenhet.getNavn());
                            list.add(k);
                        }
                    }
            );
        }

        dvhRepository.saveKoblinger(list);
    }

    public void saveLedere(List<DvhLeder> ledere) {
        val cachedEnheter = orgenhetService.getAllEnheter().stream()
                .collect(Collectors.groupingBy(IOrgenhet::getUniqueAgressoId)).values().stream()
                .map(l -> l.stream().max(Comparator.comparing(IOrgenhet::getGyldigFom)))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toMap(IOrgenhet::getUniqueAgressoId, Function.identity()));

        dvhRepository.saveLedere(ledere.stream()
                    .filter(l -> {
                        val enhet = cachedEnheter.get(l.getOrgEnhetNr()+"-"+l.getOrgenhetNavn()+"-"+l.getOrgNiv());
                        val ressurs = unleashRessursServiceSelector.findByNavident(l.getNavIdent());
                        if(enhet == null && ressurs.isEmpty()){
                            log.info("Leder relasjon mislyktes, enhet: "+l.getOrgEnhetNr()+", navIdent: "+l.getNavIdent()+", "+"Enhet og ressurs mangler");
                        } else if(enhet == null){
                            log.info("Leder relasjon mislyktes, enhet: "+l.getOrgEnhetNr()+", navIdent: "+l.getNavIdent()+", "+"Enhet mangler");
                        } else if(ressurs.isEmpty()){
                            log.info("Leder relasjon mislyktes, enhet: "+l.getOrgEnhetNr()+", navIdent: "+l.getNavIdent()+", "+"Ressurs mangler");
                        }
                        if(enhet != null && ressurs.isPresent()){
                            return true;
                        }
                        return false;
                    })
                    .toList()
        );
    }
}
