package no.nav.nom.apps.api.kobling;

import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.kobling.contract.Kobling;
import no.nav.nom.apps.api.kobling.contract.KoblingService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class KoblingDvhService implements KoblingService {

    private final DvhRepository dvhRepository;

    private final Map<String, List<Kobling>> koblingMap;

    public KoblingDvhService(DvhRepository dvhRepository) {
        this.dvhRepository = dvhRepository;
        dvhRepository.addUpdateListener(this::updateSelf);
        this.koblingMap = new HashMap<>();
    }

    @Override
    public Collection<Kobling> getAllKoblinger() {
        return this.koblingMap.values().stream().flatMap(Collection::stream).toList();
    }

    @Override
    public List<Kobling> getKoblingerByNavIdent(String navIdent) {
        var hit = this.koblingMap.get(navIdent);
        return hit == null ? Collections.emptyList() : hit;
    }

    @Override
    public <T> Map<String, List<T>> getKoblingerByNavidenter(Set<String> navidenter, Function<Kobling, T> transformer) {
        return navidenter.stream().collect(Collectors.toMap(navident -> navident, navident -> {
            var value = this.koblingMap.get(navident);
            return value == null ? Collections.emptyList() : value.stream().map(transformer).toList();
        }));
    }

    private boolean updateSelf() {
        this.koblingMap.clear();
        for (var kobling : dvhRepository.getKoblinger()) {
            var key = kobling.getNavIdent();
            var coll = this.koblingMap.get(key);
            if (coll != null) {
                coll.add(kobling);
            } else {
                var lstnew = new ArrayList<Kobling>();
                lstnew.add(kobling);
                this.koblingMap.put(key, lstnew);
            }
        }
        return true;
    }

}
