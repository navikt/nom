package no.nav.nom.apps.api.ressurs;

import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;

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
        return ressursStateRepository.findAllActiveRessurser().values().stream()
            .filter(ressursState -> nonNull(ressursState.primaryFysiskBrukersted()) && !ressursState.primaryFysiskBrukersted().isBlank())
            .collect(Collectors.groupingBy(RessursState::primaryFysiskBrukersted));
    }

    public Map<String, RessursState> findAllActiveRessurser() {
        return ressursStateRepository.findAllActiveRessurser();
    }
}
