package no.nav.nom.apps.api.orgtilknytning.eventsourcing.state;

import io.getunleash.Unleash;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Service
@Profile("!test")
@Primary
public class ToggledOrgtilknytningStateRepository implements OrgtilknytningStateRepository {
    public static final String UNLEASH_USE_NEW_ORGTILKNYTNING_STATE_REPOSITORY = "nom.orgtilknytning_state.use_new_repository";
    private final OrgtilknytningStateRepository oldOrgtilknytningStateRepository;
    private final OrgtilknytningStateRepository nomStateOrgtilknytningRepository;
    private final Unleash unleash;

    public ToggledOrgtilknytningStateRepository(
        @Qualifier("oldOrgtilknytningStateRepository") OrgtilknytningStateRepository oldOrgtilknytningStateRepository,
        @Qualifier("nomStateOrgtilknytningRepository") OrgtilknytningStateRepository nomStateOrgtilknytningRepository,
        Unleash unleash
    ) {
        this.oldOrgtilknytningStateRepository = oldOrgtilknytningStateRepository;
        this.nomStateOrgtilknytningRepository = nomStateOrgtilknytningRepository;
        this.unleash = unleash;
    }

    @Override
    public Optional<OrgtilknytningState> findByFid(String fid) {
        if (unleash.isEnabled(UNLEASH_USE_NEW_ORGTILKNYTNING_STATE_REPOSITORY)) {
            return nomStateOrgtilknytningRepository.findByFid(fid);
        } else {
            return oldOrgtilknytningStateRepository.findByFid(fid);
        }
    }

    @Override
    public List<OrgtilknytningState> findByFidIn(List<String> fids) {
        if (unleash.isEnabled(UNLEASH_USE_NEW_ORGTILKNYTNING_STATE_REPOSITORY)) {
            return nomStateOrgtilknytningRepository.findByFidIn(fids);
        } else {
            return oldOrgtilknytningStateRepository.findByFidIn(fids);
        }
    }

    @Override
    public Collection<OrgtilknytningState> findByNavident(String navident) {
        if (unleash.isEnabled(UNLEASH_USE_NEW_ORGTILKNYTNING_STATE_REPOSITORY)) {
            return nomStateOrgtilknytningRepository.findByNavident(navident);
        } else {
            return oldOrgtilknytningStateRepository.findByNavident(navident);
        }
    }

    @Override
    public List<OrgtilknytningState> findByNavidentIn(List<String> navidents) {
        if (unleash.isEnabled(UNLEASH_USE_NEW_ORGTILKNYTNING_STATE_REPOSITORY)) {
            return nomStateOrgtilknytningRepository.findByNavidentIn(navidents);
        } else {
            return oldOrgtilknytningStateRepository.findByNavidentIn(navidents);
        }
    }

    @Override
    public Collection<OrgtilknytningState> getAll() {
        if (unleash.isEnabled(UNLEASH_USE_NEW_ORGTILKNYTNING_STATE_REPOSITORY)) {
            return nomStateOrgtilknytningRepository.getAll();
        } else {
            return oldOrgtilknytningStateRepository.getAll();
        }
    }

    @Override
    public int count() {
        if (unleash.isEnabled(UNLEASH_USE_NEW_ORGTILKNYTNING_STATE_REPOSITORY)) {
            return nomStateOrgtilknytningRepository.count();
        } else {
            return oldOrgtilknytningStateRepository.count();
        }
    }

    @Override
    public void update(String fid, OrgtilknytningState newOrgtilknytningState) {
        throw new RuntimeException("ERROR! Call to update() in ToggledOrgtilknytningStateRepository - should not be called on implemented beans directly");
    }

    @Override
    public void clear() {
        throw new RuntimeException("ERROR! Call to clear() in ToggledOrgtilknytningStateRepository - should not be called on implemented beans directly");
    }
}
