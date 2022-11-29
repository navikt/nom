package no.nav.nom.apps.api.kobling;

import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.kobling.contract.Kobling;
import no.nav.nom.apps.api.kobling.contract.KoblingService;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

@Service
public class KoblingDvhService implements KoblingService {

    private final DvhRepository dvhRepository;

    public KoblingDvhService(DvhRepository dvhRepository) {
        this.dvhRepository = dvhRepository;
    }

    @Override
    public Collection<Kobling> getAllKoblinger() {
        return dvhRepository.getKoblinger();
    }

    @Override
    public List<Kobling> getKoblingerByNavIdent(String navIdent) {
        return getAllKoblinger().stream().filter(s -> s.getNavIdent().equals(navIdent)).toList();
    }

    @Override
    public <T> Map<String, List<T>> getKoblingerByNavidenter(Set<String> navidenter, Function<Kobling, T> transformer) {
        return getAllKoblinger().stream()
                .filter(kobling -> navidenter.contains(kobling.getNavIdent()))
                .collect(groupingBy(Kobling::getNavIdent, mapping(transformer, toList())));
    }

}
