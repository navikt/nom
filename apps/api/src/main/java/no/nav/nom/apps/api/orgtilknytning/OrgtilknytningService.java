package no.nav.nom.apps.api.orgtilknytning;

import no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateRepository;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OrgtilknytningService {

    private final OrgtilknytningStateRepository orgtilknytningStateRepository;

    public OrgtilknytningService(OrgtilknytningStateRepository orgtilknytningStateRepository) {
        this.orgtilknytningStateRepository = orgtilknytningStateRepository;
    }

    public Collection<OrgtilknytningState> getAll() {
        return orgtilknytningStateRepository.getAll();
    }

    public Collection<OrgtilknytningState> getByNavIdent(String navident) {
        return orgtilknytningStateRepository.findByNavident(navident);
    }

    public List<OrgtilknytningState> getByEnhet(String id) {
        return getAll().stream()
            .filter(ot -> ot.orgEnhetId().equals(id))
            .toList();
    }

    public <T> Map<String, Collection<T>> getByNavidenter(Set<String> navidenter, Function<OrgtilknytningState, T> transformer) {
        return navidenter.stream().collect(Collectors.toMap(navident -> navident, navident -> {
            var value = this.getByNavIdent(navident);
            return value == null ? Collections.emptyList() : value.stream().map(transformer).toList();
        }));
    }

    public Optional<OrgtilknytningState> findByFid(String fid) {
        return orgtilknytningStateRepository.findByFid(fid);
    }

    public OrgtilknytningState getAktivDolOTForRessurs(String navident) {
        return orgtilknytningStateRepository.findByNavident(navident).stream()
            .filter(OrgtilknytningState::erDagligOppfolging)
            .filter(PeriodeUtil::isCurrentPeriod)
            .findFirst()
            .orElse(null);
    }
}
