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
import java.util.stream.Collectors;

@Slf4j
@Getter
@Component
@AllArgsConstructor
public class DvhRepository {

    private Map<String, Map<String, IOrgenhet>> enheterByNivaByAgressoId;
    private List<DvhLeder> ledere;
    private List<DvhOrganisering> organiseringer;
    private List<Kobling> koblinger;
    private List<DvhTelefon> navidentTelefonNrMap;
    private Map<String, RessursI> ressursByNavIdent;
    private List<DvhOrgenhetV2> orgenhetV2;
    private final Map<String, LocalDate> sluttdatoByPersonIdent = new HashMap<>();

    public Optional<IOrgenhet> getEnhetByAgressoIdAndNiva(String agressoId, String orgNiv) {
        return Optional.ofNullable(enheterByNivaByAgressoId.containsKey(orgNiv) ? enheterByNivaByAgressoId.get(orgNiv).get(agressoId) : null);
    }

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
                .map(l -> getEnhetByAgressoIdAndNiva(l.getOrgEnhetNr(), l.getOrgNiv()).orElse(null))
                .filter(Objects::nonNull)
                .toList();
    }

    public void saveOrganisasjonsenheter(Map<String, Map<String, IOrgenhet>> orgenheter) {
        enheterByNivaByAgressoId = orgenheter;
    }

    public void saveLedere(List<DvhLeder> ledere) {
        this.ledere = ledere;
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
                        o.setNavnOver(overEnhet.get().getNavn());
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
        enheterByNivaByAgressoId.clear();
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

    public void saveOrgenhetV2(List<DvhOrgenhetV2> orgenheter){
        DvhOrgenhetV2 fakeNAV = new DvhOrgenhetV2("NAV", "ORGNIV0", "NAV", null, "200012", "209912", "N", null);
        organiseringer = orgenheter.stream()
                .map(o -> fakeOverenhet(o, fakeNAV))
                .toList();

        val removedDuplicates = orgenheter.stream()
                .collect(Collectors.groupingBy(e -> e.getAgressoId()+e.getOrgNiv()+e.getNavn(), Collectors.toList()))
                .values().stream()
                .map(l -> l.size() == 1 ? l.get(0) : l.stream().max(Comparator.comparing(DvhOrgenhetV2::getGyldigFom)).stream().findFirst().get())
                .collect(Collectors.toList());

        organiseringer = removedDuplicates.stream()
                .map(o -> fakeOverenhet(o, fakeNAV))
                .toList();

        val orgenheterWithFake = new LinkedList<>(removedDuplicates);
        orgenheterWithFake.add(fakeNAV);
        orgenhetV2 = orgenheterWithFake;
    }

    private DvhOrganisering fakeOverenhet(DvhOrgenhetV2 dvhOrgenhetV2, DvhOrgenhetV2 fakeNAV){
        return new DvhOrganisering(fakeNAV.getAgressoId(), fakeNAV.getOrgNiv(), fakeNAV.getNavn(),
                dvhOrgenhetV2.getAgressoId(), dvhOrgenhetV2.getOrgNiv(), dvhOrgenhetV2.getNavn(),
                LocalDate.now().minusDays(1), null);
    }
}
