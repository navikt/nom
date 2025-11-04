package no.nav.nom.apps.api.ressurs;

import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class RessursService {
    private final RessursStateRepository ressursStateRepository;

    public RessursService(RessursStateRepository ressursStateRepository) {
        this.ressursStateRepository = ressursStateRepository;
    }

    public Optional<RessursState> findByNavident(String navident) {
        return ressursStateRepository.findByNavident(navident);
    }
}
