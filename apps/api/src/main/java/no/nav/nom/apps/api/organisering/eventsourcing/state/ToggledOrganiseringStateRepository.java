package no.nav.nom.apps.api.organisering.eventsourcing.state;

import io.getunleash.Unleash;
import no.nav.nom.contract.nom.state.OrganiseringState;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.libs.state.organisering.OrganiseringStateRepository;
import no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Profile("!test")
@Primary
public class ToggledOrganiseringStateRepository implements OrganiseringStateRepository {
    public static final String UNLEASH_USE_NEW_ORGANISERNIG_STATE_REPOSITORY = "nom.organisering_state.use_new_repository";
    private final OrganiseringStateRepository oldOrganiseringStateRepository;
    private final OrganiseringStateRepository nomStateOrganiseringRepository;
    private final Unleash unleash;

    public ToggledOrganiseringStateRepository(
        @Qualifier("oldOrganiseringStateRepository") OrganiseringStateRepository oldOrganiseringStateRepository,
        @Qualifier("nomStateOrganiseringRepository") OrganiseringStateRepository nomStateOrganiseringRepository,
        Unleash unleash
    ) {
        this.oldOrganiseringStateRepository = oldOrganiseringStateRepository;
        this.nomStateOrganiseringRepository = nomStateOrganiseringRepository;
        this.unleash = unleash;
    }

    @Override
    public Optional<OrganiseringState> findByFid(String fid) {
        if (unleash.isEnabled(UNLEASH_USE_NEW_ORGANISERNIG_STATE_REPOSITORY)) {
            return nomStateOrganiseringRepository.findByFid(fid);
        } else {
            return oldOrganiseringStateRepository.findByFid(fid);
        }
    }

    @Override
    public Optional<OrganiseringState> findActiveByFid(String fid) {
        if (unleash.isEnabled(UNLEASH_USE_NEW_ORGANISERNIG_STATE_REPOSITORY)) {
            return nomStateOrganiseringRepository.findActiveByFid(fid);
        } else {
            return oldOrganiseringStateRepository.findActiveByFid(fid);
        }
    }

    @Override
    public Optional<OrganiseringState> findActiveById(String id) {
        if (unleash.isEnabled(UNLEASH_USE_NEW_ORGANISERNIG_STATE_REPOSITORY)) {
            return nomStateOrganiseringRepository.findActiveById(id);
        } else {
            return oldOrganiseringStateRepository.findActiveById(id);
        }
    }

    @Override
    public Optional<OrganiseringState> findById(String id) {
        if (unleash.isEnabled(UNLEASH_USE_NEW_ORGANISERNIG_STATE_REPOSITORY)) {
            return nomStateOrganiseringRepository.findById(id);
        } else {
            return oldOrganiseringStateRepository.findById(id);
        }
    }

    @Override
    public void logRepositoryState() {
        if (unleash.isEnabled(UNLEASH_USE_NEW_ORGANISERNIG_STATE_REPOSITORY)) {
            nomStateOrganiseringRepository.logRepositoryState();
        } else {
            oldOrganiseringStateRepository.logRepositoryState();
        }
    }

    @Override
    public Map<String, OrganiseringState> getActiveOrganisering() {
        if (unleash.isEnabled(UNLEASH_USE_NEW_ORGANISERNIG_STATE_REPOSITORY)) {
            return nomStateOrganiseringRepository.getActiveOrganisering();
        } else {
            return oldOrganiseringStateRepository.getActiveOrganisering();
        }
    }

    @Override
    public List<OrganiseringState> getOrganiseringer() {
        if (unleash.isEnabled(UNLEASH_USE_NEW_ORGANISERNIG_STATE_REPOSITORY)) {
            return nomStateOrganiseringRepository.getOrganiseringer();
        } else {
            return oldOrganiseringStateRepository.getOrganiseringer();
        }
    }

    @Override
    public List<OrganiseringState> getOrganiseringerContainingId(String id) {
        if (unleash.isEnabled(UNLEASH_USE_NEW_ORGANISERNIG_STATE_REPOSITORY)) {
            return nomStateOrganiseringRepository.getOrganiseringerContainingId(id);
        } else {
            return oldOrganiseringStateRepository.getOrganiseringerContainingId(id);
        }
    }

    @Override
    public void clear() {
        throw new RuntimeException("ERROR! Call to clear() in ToggledOrganiseringStateRepository - should not be called on implemented beans directly");
    }

    @Override
    public void update(OrganiseringState organiseringState) {
        throw new RuntimeException("ERROR! Call to update() in ToggledOrganiseringStateRepository - should not be called on implemented beans directly");

    }

    @Override
    public void putEventAggregate(OrganiseringState organiseringState) {
        throw new RuntimeException("ERROR! Call to putEventAggregate() in ToggledOrganiseringStateRepository - should not be called on implemented beans directly");
    }

    @Override
    public void removeEventAggregate(OrganiseringState organiseringState) {
        throw new RuntimeException("ERROR! Call to removeEventAggregate() in ToggledOrganiseringStateRepository - should not be called on implemented beans directly");
    }

    @Override
    public int count() {
        return 0;
    }
}
