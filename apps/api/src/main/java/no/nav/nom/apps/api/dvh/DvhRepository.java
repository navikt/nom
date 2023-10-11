package no.nav.nom.apps.api.dvh;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.nom.apps.api.dvh.dto.DvhLeder;
import no.nav.nom.apps.api.dvh.dto.DvhOrganisering;
import no.nav.nom.apps.api.dvh.dto.DvhTelefon;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;


@Slf4j
@Getter
@Component
@RequiredArgsConstructor
public class DvhRepository {

    private List<DvhLeder> ledere = Collections.emptyList();
    private List<DvhOrganisering> organiseringer = Collections.emptyList();
    private List<DvhTelefon> navidentTelefonNrMap;

    private final List<Supplier<Boolean>> listeners = new ArrayList<>();

    public List<DvhOrganisering> getOrganiseringer(String agressoId, String orgNiv, String navn) {
        return organiseringer.stream().filter(e -> e.containsEnhet(agressoId, orgNiv, navn)).toList();
    }

    public List<DvhLeder> getLederByAgressoIdAndNiv(String agressoId, String orgNiv) {
        return ledere.stream().filter(l -> l.getOrgEnhetNr().equals(agressoId) && l.getOrgNiv().equals(orgNiv)).toList();
    }

    public void saveLedere(List<DvhLeder> ledere) {
        this.ledere = ledere;
        this.updateListeners();
    }

    protected void saveOrganiseringer(List<DvhOrganisering> organiseringer) {
        this.organiseringer = organiseringer;
        updateListeners();
    }

    public void saveTlf(List<DvhTelefon> tlfList) {
        this.navidentTelefonNrMap = tlfList;
        this.updateListeners();
    }

    public void clearRepository() {
        organiseringer = Collections.emptyList();
        this.updateListeners();
    }

    public List<DvhTelefon> getTelefonNrListByNavIdent(String navIdent) {
        val lst = this.navidentTelefonNrMap.stream().filter(it -> it.getNavIdent().equals(navIdent));
        return lst.toList();
    }

    public Optional<DvhTelefon> getTelefonByNummer(String telefonNummer) {
        // Phone numbers in DVH includes country code. If the requested phone-number does not include a country code we assume 47.
        var formattedNumber = telefonNummer.length() == 8 ? "47".concat(telefonNummer) : telefonNummer;
        return navidentTelefonNrMap.stream().filter(it -> it.getNummer().equals(formattedNumber)).findFirst();
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
