package no.nav.nom.apps.api.orgtilknytning;

import io.getunleash.Unleash;
import no.nav.nom.apps.api.orgtilknytning.contract.Orgtilknytning;
import no.nav.nom.apps.api.orgtilknytning.contract.OrgtilknytningService;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.state.OrgtilknytningStateRepository;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

@Service
public class OrgtilknytningStateService implements OrgtilknytningService {

    private final OrgtilknytningStateRepository orgtilknytningStateRepository;

    public OrgtilknytningStateService(OrgtilknytningStateRepository orgtilknytningStateRepository) {
        this.orgtilknytningStateRepository = orgtilknytningStateRepository;
    }

    @Override
    public Collection<OrgtilknytningState> getAllKoblinger() {
        return orgtilknytningStateRepository.getAll();
    }

    @Override
    public Collection<OrgtilknytningState> getKoblingerByNavIdent(String navident) {
        return orgtilknytningStateRepository.findByNavident(navident);
    }

    @Override
    public <T> Map<String, List<T>> getKoblingerByNavidenter(Set<String> navidenter, Function<OrgtilknytningState, T> transformer) {
        // TODO: Implement
        return new HashMap<>();
    }

    @Override
    public Collection<Orgtilknytning> getAllKoblingerOld() {
        throw new NotImplementedException("Nye service klasse implementerer ikke gamle metoder");
    }

    @Override
    public List<Orgtilknytning> getKoblingerByNavIdentOld(String navIdent) {
        throw new NotImplementedException("Nye service klasse implementerer ikke gamle metoder");
    }

    @Override
    public <T> Map<String, List<T>> getKoblingerByNavidenterOld(Set<String> navidenter, Function<Orgtilknytning, T> transformer) {
        throw new NotImplementedException("Nye service klasse implementerer ikke gamle metoder");
    }
}
