package no.nav.nom.apps.api.ressurs;

import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class RessursService {
    private final RessursStateRepository ressursStateRepository;

    public RessursService(RessursStateRepository ressursStateRepository) {
        this.ressursStateRepository = ressursStateRepository;
    }

    public Optional<RessursState> findByNavident(String navident) {
        return ressursStateRepository.findByNavident(navident);
    }

    public Map<String, List<RessursState>> findAllGroupedByFysiskBrukersted() {
        return ressursStateRepository.getAll().stream().collect(Collectors.groupingBy(RessursState::primaryFysiskBrukersted));
    }
}
