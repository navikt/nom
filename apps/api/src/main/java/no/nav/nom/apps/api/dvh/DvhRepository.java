package no.nav.nom.apps.api.dvh;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.nom.apps.api.dvh.dto.DvhLeder;
import no.nav.nom.apps.api.dvh.dto.DvhOrganisering;
import no.nav.nom.apps.api.dvh.dto.DvhTelefon;
import no.nav.nom.apps.api.kobling.contract.Kobling;
import no.nav.nom.apps.api.orgenhet.contract.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.contract.IOrgenhetService;
import no.nav.nom.apps.api.ressurs.implementations.dvh.DvhRessurs;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

@Slf4j
@Getter
@Component
@RequiredArgsConstructor
public class DvhRepository {

    private final IOrgenhetService orgenhetService;
    private List<DvhLeder> ledere;
    private List<DvhOrganisering> organiseringer;
    private List<Kobling> koblinger;
    private List<DvhTelefon> navidentTelefonNrMap;

    private final List<Supplier<Boolean>> listeners = new ArrayList<>();

    public List<DvhOrganisering> getOrganiseringer(String agressoId, String orgNiv, String navn) {
        return organiseringer.stream().filter(e -> e.containsEnhet(agressoId, orgNiv, navn)).toList();
    }

    public List<Kobling> getKoblingerByAgressoId(String agressoId, String orgNiv) {
        return koblinger.stream().filter(x -> x.getAgressoId().equals(agressoId) && x.getOrgNiv().equals(orgNiv)).toList();
    }

    public List<DvhLeder> getLederByAgressoIdAndNiv(String agressoId, String orgNiv) {
        return ledere.stream().filter(l -> l.getOrgEnhetNr().equals(agressoId) && l.getOrgNiv().equals(orgNiv)).toList();
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
        this.updateListeners();
    }

    public void saveOrganiseringer(List<DvhOrganisering> organiseringer) {
        val cachedEnheter = orgenhetService.getAllEnheter().stream()
                .collect(Collectors.groupingBy(IOrgenhet::getUniqueAgressoId))
                .values().stream()
                .map(l -> l.stream().max(Comparator.comparing(IOrgenhet::getGyldigFom)))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(toMap(IOrgenhet::getUniqueAgressoId, Function.identity()));

        this.organiseringer = organiseringer.stream().filter(o -> {
                    final var overEnhet = cachedEnheter.get(o.getAgressoIdOver() + "-" + o.getNavnOver() + "-" + o.getOrgNivOver());
                    final var underEnhet = cachedEnheter.get(o.getAgressoIdUnder() + "-" + o.getNavnUnder() + "-" + o.getOrgNivUnder());
                    if (overEnhet == null && underEnhet == null) {
                        log.info(
                                "Organisering relasjon mislyktes, overenhet: {}, underenhet: {}, Over- og Underenhet mangler",
                                o.getAgressoIdOver(),
                                o.getAgressoIdUnder()
                        );
                    } else if (overEnhet == null) {
                        log.info("Organisering relasjon mislyktes, overenhet: {}, underenhet: {}, Overenhet mangler", o.getAgressoIdOver(), o.getAgressoIdUnder());
                    } else if (underEnhet == null) {
                        log.info("Organisering relasjon mislyktes, overenhet: {}, underenhet: {}, Underenhet mangler", o.getAgressoIdOver(), o.getAgressoIdUnder());
                    } else {
                        return true;
                    }
                    return false;
                })
                .collect(toList());
        this.updateListeners();
    }

    public void saveKoblinger(List<Kobling> koblinger) {
        this.koblinger = koblinger;
        this.updateListeners();
    }


    public void saveTlf(List<DvhTelefon> tlfList) {
        this.navidentTelefonNrMap = tlfList;
        this.updateListeners();
    }

    public void clearRepository() {
        if (organiseringer != null) {
            organiseringer.clear();
        }
        koblinger = Collections.emptyList();
        this.updateListeners();
    }

    public List<DvhTelefon> getTelefonNrListByNavIdent(String navIdent) {
        val lst = this.navidentTelefonNrMap.stream().filter(it -> it.getNavIdent().equals(navIdent));
        return lst.toList();
    }

    public void addUpdateListener(Supplier<Boolean> callback) {
        this.listeners.add(callback);
    }

    private void updateListeners() {
        for(var listener: this.listeners){
            listener.get();
        }
    }
}
