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

    public static final String UNLEASH_ORGTILKNYTNING_BRUK_EVTSRC_STATE = "nom.orgtilknytning.bruk_evtsrc_state";

    private final OrgtilknytningStateRepository orgtilknytningStateRepository;
    private final OrgtilknytningDvhService orgtilknytningDvhService;
    private final Unleash unleash;

    public OrgtilknytningStateService(OrgtilknytningStateRepository orgtilknytningStateRepository, OrgtilknytningDvhService orgtilknytningDvhService, Unleash unleash) {
        this.orgtilknytningStateRepository = orgtilknytningStateRepository;
        this.orgtilknytningDvhService = orgtilknytningDvhService;
        this.unleash = unleash;
    }

    @Override
    public Collection<OrgtilknytningState> getAllKoblinger() {
        // TODO: Implement
        return new ArrayList<>();
    }

    @Override
    public List<OrgtilknytningState> getKoblingerByNavIdent(String navIdent) {
        // TODO: Implement
        return new ArrayList<>();
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
