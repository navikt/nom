package no.nav.nom.apps.api.dvh;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.nom.apps.api.dvh.dto.DvhLeder;
import no.nav.nom.apps.api.kobling.DvhKobling;
import no.nav.nom.apps.api.kobling.contract.Kobling;
import no.nav.nom.apps.api.ressurs.implementations.RessursService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DvhService {

    private final DvhRepository dvhRepository;
    private final RessursService ressursService;

    public void saveKoblinger(List<DvhKobling> koblinger) {

        val list = Collections.synchronizedList(new LinkedList<Kobling>());

        val bulkSize = 10000;
        for(int i = 0; i<koblinger.size(); i+=bulkSize){
            val selection = koblinger.subList(i, Math.min(i + bulkSize, koblinger.size()));
            val ressursByNavIdent = ressursService.findByNavIdentBulk(selection.stream().map(Kobling::getNavIdent).toList());

            selection.forEach(k -> {
                        val organisasjonsenhet = dvhRepository.getEnhetByAgressoIdAndNiva(k.getAgressoId(), k.getOrgNiv()).orElse(null);
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
        dvhRepository.saveLedere(ledere.stream()
                    .filter(l -> {
                        val enhet = dvhRepository.getEnhetByAgressoIdAndNiva(l.getOrgEnhetNr(), l.getOrgNiv());
                        val ressurs = ressursService.findByNavIdent(l.getNavIdent());
                        if(enhet.isEmpty() && ressurs.isEmpty()){
                            log.info("Leder relasjon mislyktes, enhet: "+l.getOrgEnhetNr()+", navIdent: "+l.getNavIdent()+", "+"Enhet og ressurs mangler");
                        } else if(enhet.isEmpty()){
                            log.info("Leder relasjon mislyktes, enhet: "+l.getOrgEnhetNr()+", navIdent: "+l.getNavIdent()+", "+"Enhet mangler");
                        } else if(ressurs.isEmpty()){
                            log.info("Leder relasjon mislyktes, enhet: "+l.getOrgEnhetNr()+", navIdent: "+l.getNavIdent()+", "+"Ressurs mangler");
                        }
                        if(enhet.isPresent() && ressurs.isPresent()){
                            l.setOrgenhetNavn(enhet.get().getNavn());
                            return true;
                        }
                        return false;
                    })
                    .toList()
        );
    }
}
