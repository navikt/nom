package no.nav.nom.apps.api.kobling;

import lombok.RequiredArgsConstructor;
import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.kobling.contract.Kobling;
import no.nav.nom.apps.api.kobling.contract.KoblingService;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

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
}
