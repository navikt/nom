package no.nav.nom.apps.api.kobling.contract;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface KoblingService {
    Collection<Kobling> getAllKoblinger();
    List<Kobling> getKoblingerByNavIdent(String navIdent);
    Map<String, List<Kobling>> getKoblingerByNavidenter(Set<String> navidenter);
}
