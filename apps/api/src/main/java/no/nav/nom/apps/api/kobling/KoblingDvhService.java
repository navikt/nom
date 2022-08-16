package no.nav.nom.apps.api.kobling;

import lombok.RequiredArgsConstructor;
import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.kobling.contract.Kobling;
import no.nav.nom.apps.api.kobling.contract.KoblingService;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KoblingDvhService implements KoblingService {

    private final DvhRepository dvhRepository;

    @Override
    public Collection<Kobling> getAllKoblinger() {
        return dvhRepository.getKoblinger();
    }

    @Override
    public List<Kobling> getKoblingerByNavIdent(String navIdent) {
        return getAllKoblinger().stream()
                .filter(s -> s.getNavIdent().equals(navIdent))
                .toList();
    }

    @Override
    public Map<String, List<Kobling>> getKoblingerByNavidenter(Set<String> navidenter) {
        return getAllKoblinger().stream()
                .filter(kobling -> navidenter.contains(kobling.getNavIdent()))
                .collect(Collectors.groupingBy(Kobling::getNavIdent));
    }

}
