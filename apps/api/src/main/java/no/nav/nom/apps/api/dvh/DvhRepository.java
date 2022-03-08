package no.nav.nom.apps.api.dvh;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.nom.apps.api.dvh.dto.DvhLeder;
import no.nav.nom.apps.api.dvh.dto.DvhOrganisering;
import no.nav.nom.apps.api.dvh.dto.DvhTelefon;
import no.nav.nom.apps.api.kobling.contract.Kobling;
import no.nav.nom.apps.api.orgenhet.contract.Orgenhet;
import no.nav.nom.apps.api.ressurs.contract.RessursService;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Getter
@Component
@AllArgsConstructor
public class DvhRepository {

    private final RessursService ressursService;

    private Map<String, Map<String, Orgenhet>> enheterByNivaByAgressoId;
    private List<DvhLeder> ledere;
    private List<DvhOrganisering> organiseringer;
    private List<Kobling> koblinger;
    private List<DvhTelefon> navidentTelefonNrMap;

    public Optional<Orgenhet> getEnhetByAgressoIdAndNiva(String agressoId, String orgNiv) {
        return Optional.ofNullable(enheterByNivaByAgressoId.containsKey(orgNiv) ? enheterByNivaByAgressoId.get(orgNiv).get(agressoId) : null);
    }

    public List<DvhOrganisering> getOrganiseringer(String agressoId, String orgNiv) {
        return organiseringer.stream()
                .filter(e -> e.containsEnhet(agressoId, orgNiv))
                .toList();
    }

    public List<Kobling> getKoblingerByAgressoId(String agressoId, String orgNiv) {
        return koblinger.stream()
                .filter(x -> x.getAgressoId().equals(agressoId) && x.getOrgNiv().equals(orgNiv))
                .toList();
    }

    public List<DvhLeder> getLederByAgressoIdAndNiv(String agressoId, String orgNiv) {
        return ledere.stream()
                .filter(l -> l.getOrgEnhetNr().equals(agressoId) && l.getOrgNiv().equals(orgNiv))
                .toList();
    }

    public List<Orgenhet> getEnheterByLeder(String lederNavIdent) {
        return ledere.stream()
                .filter(l -> l.getNavIdent().equals(lederNavIdent))
                .map(l -> getEnhetByAgressoIdAndNiva(l.getOrgEnhetNr(), l.getOrgNiv()).orElse(null))
                .filter(Objects::nonNull)
                .toList();
    }

    public void saveOrganisasjonsenheter(Map<String, Map<String, Orgenhet>> orgenheter) {
        enheterByNivaByAgressoId = orgenheter;
    }

    public void saveLedere(List<DvhLeder> ledere) {
        this.ledere = ledere.stream()
                .filter(l -> {
                    val enhet = getEnhetByAgressoIdAndNiva(l.getOrgEnhetNr(), l.getOrgNiv());
                    val ressurs = ressursService.findByNavIdent(l.getNavIdent());
                    if(enhet.isEmpty() && ressurs.isEmpty()){
                        log.info("Leder relasjon mislyktes, enhet: "+l.getOrgEnhetNr()+", navIdent: "+l.getNavIdent()+", "+"Enhet og ressurs mangler");
                    } else if(enhet.isEmpty()){
                        log.info("Leder relasjon mislyktes, enhet: "+l.getOrgEnhetNr()+", navIdent: "+l.getNavIdent()+", "+"Enhet mangler");
                    } else if(ressurs.isEmpty()){
                        log.info("Leder relasjon mislyktes, enhet: "+l.getOrgEnhetNr()+", navIdent: "+l.getNavIdent()+", "+"Ressurs mangler");
                    }
                    return enhet.isPresent() && ressurs.isPresent();
                })
                .toList();
    }

    public void saveOrganiseringer(List<DvhOrganisering> organiseringer) {
        this.organiseringer = organiseringer.stream()
                .filter(o -> {
                    val overEnhet = getEnhetByAgressoIdAndNiva(o.getAgressoIdOver(), o.getOrgNivOver());
                    val underEnhet = getEnhetByAgressoIdAndNiva(o.getAgressoIdUnder(), o.getOrgNivUnder());
                    if (overEnhet.isEmpty() && underEnhet.isEmpty()) {
                        log.info("Organisering relasjon mislyktes, overenhet: " + o.getAgressoIdOver() + ", underenhet: " + o.getAgressoIdUnder() + ", " + "Over- og underenhet mangler");
                    } else if (overEnhet.isEmpty()) {
                        log.info("Organisering relasjon mislyktes, overenhet: " + o.getAgressoIdOver() + ", underenhet: " + o.getAgressoIdUnder() + ", " + "Overenhet mangler");
                    } else if (underEnhet.isEmpty()) {
                        log.info("Organisering relasjon mislyktes, overenhet: " + o.getAgressoIdOver() + ", underenhet: " + o.getAgressoIdUnder() + ", " + "Underenhet mangler");
                    } else {
                        return true;
                    }
                    return false;
                })
                .collect(Collectors.toList());
    }

    public void saveKoblinger(List<Kobling> koblinger) {

        val list = Collections.synchronizedList(new LinkedList<Kobling>());

        val bulkSize = 10000;
        for(int i = 0; i<koblinger.size(); i+=bulkSize){
            val selection = koblinger.subList(i, Math.min(i + bulkSize, koblinger.size()));
            val ressursByNavIdent = ressursService.findByNavIdentBulk(selection.stream().map(Kobling::getNavIdent).toList());

            selection.forEach(k -> {
                        val organisasjonsenhet = getEnhetByAgressoIdAndNiva(k.getAgressoId(), k.getOrgNiv()).orElse(null);
                        val dvhRessurs = ressursByNavIdent.get(k.getNavIdent());
                        if (organisasjonsenhet == null && dvhRessurs == null) {
                            log.info("Kobling relasjon mislyktes, enhet: "+k.getAgressoId() + ", navIdent: " + k.getNavIdent() + ", " + "Enhet og ressurs mangler");
                        } else if (organisasjonsenhet == null) {
                            log.info("Kobling relasjon mislyktes, enhet: "+k.getAgressoId() + ", navIdent: " + k.getNavIdent() + ", " + "Enhet mangler");
                        } else if (dvhRessurs == null) {
                            log.info("Kobling relasjon mislyktes, enhet: "+k.getAgressoId() + ", navIdent: " + k.getNavIdent() + ", " + "Ressurs mangler");
                        } else {
                            list.add(k);
                        }
                    }
            );
        }

        this.koblinger = list;
    }

    public void saveTlf(List<DvhTelefon> tlfList){
        this.navidentTelefonNrMap = tlfList;
    }

    public void clearRepository() {
        enheterByNivaByAgressoId.clear();
        organiseringer.clear();
        koblinger.clear();
    }

    public List<DvhTelefon> getTelefonNrListByNavIdent(String navIdent) {
        val lst = this.navidentTelefonNrMap.stream().filter(it -> it.getNavIdent().equals(navIdent));
        return lst.toList();
    }
}
