package no.nav.nom.apps.api.dvh;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.nom.apps.api.dvh.dto.DvhLeder;
import no.nav.nom.apps.api.dvh.dto.DvhOrganisering;
import no.nav.nom.apps.api.dvh.dto.DvhTelefon;
import no.nav.nom.apps.api.kobling.contract.Kobling;
import no.nav.nom.apps.api.orgenhet.contract.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.contract.IOrgenhetService;
import no.nav.nom.apps.api.orgenhet.implementations.dvh.v2.DvhOrgenhetV2;
import no.nav.nom.apps.api.ressurs.contract.RessursI;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Getter
@Component
@AllArgsConstructor
public class DvhRepository {

    private List<DvhLeder> ledere;
    private List<DvhOrganisering> organiseringer;
    private List<Kobling> koblinger;
    private List<DvhTelefon> navidentTelefonNrMap;
    private Map<String, RessursI> ressursByNavIdent;
    private List<DvhOrgenhetV2> orgenhetV2;
    private final Map<String, LocalDate> sluttdatoByPersonIdent = new HashMap<>();
    private final IOrgenhetService orgenhetService;

    public List<DvhOrganisering> getOrganiseringer(String agressoId, String orgNiv, String navn) {
        return organiseringer.stream()
                .filter(e -> e.containsEnhet(agressoId, orgNiv, navn))
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

    public List<IOrgenhet> getEnheterByLeder(String lederNavIdent) {
        return ledere.stream()
                .filter(l -> l.getNavIdent().equals(lederNavIdent))
                .map(l -> orgenhetService.getEnhetByAgressoIdNivaAndName(l.getOrgEnhetNr(), l.getOrgNiv(), l.getOrgenhetNavn()).orElse(null))
                .filter(Objects::nonNull)
                .toList();
    }

    public void saveLedere(List<DvhLeder> ledere) {
        this.ledere = ledere;
    }

    public void saveOrganiseringer(List<DvhOrganisering> organiseringer) {
        val cachedEnheter = orgenhetService.getAllEnheter().stream()
                .collect(Collectors.groupingBy(IOrgenhet::getUniqueAgressoId)).values().stream()
                .map(l -> l.stream().max(Comparator.comparing(IOrgenhet::getGyldigFom)))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toMap(IOrgenhet::getUniqueAgressoId, Function.identity()));
        this.organiseringer = organiseringer.stream()
                .filter(o -> {
                    val overEnhet = cachedEnheter.get(o.getAgressoIdOver()+"-"+o.getNavnOver()+"-"+o.getOrgNivOver());
                    val underEnhet = cachedEnheter.get(o.getAgressoIdUnder()+"-"+o.getNavnUnder()+"-"+o.getOrgNivUnder());
                    if (overEnhet == null && underEnhet == null) {
                        log.info("Organisering relasjon mislyktes, overenhet: " + o.getAgressoIdOver() + ", underenhet: " + o.getAgressoIdUnder() + ", " + "Over- og underenhet mangler");
                    } else if (overEnhet == null) {
                        log.info("Organisering relasjon mislyktes, overenhet: " + o.getAgressoIdOver() + ", underenhet: " + o.getAgressoIdUnder() + ", " + "Overenhet mangler");
                    } else if (underEnhet == null) {
                        log.info("Organisering relasjon mislyktes, overenhet: " + o.getAgressoIdOver() + ", underenhet: " + o.getAgressoIdUnder() + ", " + "Underenhet mangler");
                    } else {
                        return true;
                    }
                    return false;
                })
                .collect(Collectors.toList());
    }

    public void saveKoblinger(List<Kobling> koblinger) {
        this.koblinger = koblinger;
    }

    public void saveTlf(List<DvhTelefon> tlfList){
        this.navidentTelefonNrMap = tlfList;
    }

    public void clearRepository() {
        organiseringer.clear();
        koblinger.clear();
        sluttdatoByPersonIdent.clear();
    }

    public List<DvhTelefon> getTelefonNrListByNavIdent(String navIdent) {
        val lst = this.navidentTelefonNrMap.stream().filter(it -> it.getNavIdent().equals(navIdent));
        return lst.toList();
    }

    public void setRessursByNavIdent(Map<String, RessursI> ressursByNavIdent){
        this.ressursByNavIdent = ressursByNavIdent;
    }
}
