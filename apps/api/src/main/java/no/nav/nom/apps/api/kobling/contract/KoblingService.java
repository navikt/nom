package no.nav.nom.apps.api.kobling.contract;

import java.util.Collection;
import java.util.List;

public interface KoblingService {
    Collection<Kobling> getAllKoblinger();
    List<Kobling> getKoblingerByNavIdent(String navIdent);
}
