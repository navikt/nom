package no.nav.nom.apps.api.dvh;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.nom.apps.api.dvh.dto.DvhLeder;
import no.nav.nom.apps.api.dvh.dto.DvhTelefon;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;


@Slf4j
@Getter
@Component
@RequiredArgsConstructor
public class DvhRepository {
    private List<DvhTelefon> navidentTelefonNrList = Collections.emptyList();

    private final List<Supplier<Boolean>> listeners = new ArrayList<>();

    public void saveTlf(List<DvhTelefon> tlfList) {
        if (tlfList == null || tlfList.isEmpty()) {
            log.warn("Mottok tom eller null liste for telefonnummer fra DVH");
            return;
        }
        this.navidentTelefonNrList = tlfList;
        this.updateListeners();
    }

    public void clearRepository() {
        this.updateListeners();
    }

    public List<DvhTelefon> getTelefonNrListByNavIdent(String navIdent) {
        val lst = this.navidentTelefonNrList.stream().filter(it -> it.getNavIdent().equals(navIdent));
        return lst.toList();
    }

    public Optional<DvhTelefon> getTelefonByNummer(String telefonNummer) {
        // Phone numbers in DVH includes country code. If the requested phone-number does not include a country code we assume 47.
        var formattedNumber = telefonNummer.length() == 8 ? "47".concat(telefonNummer) : telefonNummer;
        return navidentTelefonNrList.stream().filter(it -> it.getNummer().equals(formattedNumber)).findFirst();
    }

    public void addUpdateListener(Supplier<Boolean> callback) {
        this.listeners.add(callback);
    }

    private void updateListeners() {
        for (var listener : this.listeners) {
            listener.get();
        }
    }
}
