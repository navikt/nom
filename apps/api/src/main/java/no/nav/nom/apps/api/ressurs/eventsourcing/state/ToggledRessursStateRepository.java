package no.nav.nom.apps.api.ressurs.eventsourcing.state;

import io.getunleash.Unleash;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

@Service
@Profile("!test")
@Primary
public class ToggledRessursStateRepository implements RessursStateRepository {
    public static final String UNLEASH_USE_NEW_RESSURS_STATE_REPOSITORY = "nom.ressurs_state.use_new_repository";
    private final RessursStateRepository oldRessursStateRepository;
    private final RessursStateRepository nomStateRessursRepository;
    private final Unleash unleash;

    public ToggledRessursStateRepository(
        @Qualifier("oldRessursStateRepository") RessursStateRepository oldRessursStateRepository,
        @Qualifier("nomStateRessursRepository") RessursStateRepository nomStateRessursRepository,
        Unleash unleash
    ) {
        this.oldRessursStateRepository = oldRessursStateRepository;
        this.nomStateRessursRepository = nomStateRessursRepository;
        this.unleash = unleash;
    }

    @Override
    public Optional<RessursState> findByFid(String fid) {
        if (unleash.isEnabled(UNLEASH_USE_NEW_RESSURS_STATE_REPOSITORY)) {
            return nomStateRessursRepository.findByFid(fid);
        } else {
            return oldRessursStateRepository.findByFid(fid);
        }
    }

    @Override
    public Optional<RessursState> findByPersonident(String personident) {
        if (unleash.isEnabled(UNLEASH_USE_NEW_RESSURS_STATE_REPOSITORY)) {
            return nomStateRessursRepository.findByPersonident(personident);
        } else {
            return oldRessursStateRepository.findByPersonident(personident);
        }
    }

    @Override
    public Optional<RessursState> findByNavident(String navident) {
        if (unleash.isEnabled(UNLEASH_USE_NEW_RESSURS_STATE_REPOSITORY)) {
            return nomStateRessursRepository.findByNavident(navident);
        } else {
            return oldRessursStateRepository.findByNavident(navident);
        }
    }

    @Override
    public <T> Optional<T> findByNavident(String navIdent, Function<RessursState, T> withMapper) {
        if (unleash.isEnabled(UNLEASH_USE_NEW_RESSURS_STATE_REPOSITORY)) {
            return nomStateRessursRepository.findByNavident(navIdent, withMapper);
        } else {
            return oldRessursStateRepository.findByNavident(navIdent, withMapper);
        }
    }

    @Override
    public Collection<RessursState> getAll() {
        if (unleash.isEnabled(UNLEASH_USE_NEW_RESSURS_STATE_REPOSITORY)) {
            return nomStateRessursRepository.getAll();
        } else {
            return oldRessursStateRepository.getAll();
        }
    }

    @Override
    public Collection<RessursState> getAllBareEksterne() {
        if (unleash.isEnabled(UNLEASH_USE_NEW_RESSURS_STATE_REPOSITORY)) {
            return nomStateRessursRepository.getAllBareEksterne();
        } else {
            return oldRessursStateRepository.getAllBareEksterne();
        }
    }

    @Override
    public Collection<RessursState> getAllByFid() {
        if (unleash.isEnabled(UNLEASH_USE_NEW_RESSURS_STATE_REPOSITORY)) {
            return nomStateRessursRepository.getAllByFid();
        } else {
            return oldRessursStateRepository.getAllByFid();
        }
    }

    @Override
    public int count() {
        if (unleash.isEnabled(UNLEASH_USE_NEW_RESSURS_STATE_REPOSITORY)) {
            return nomStateRessursRepository.count();
        } else {
            return oldRessursStateRepository.count();
        }
    }

    @Override
    public Map<String, RessursState> findByPersonidentBulk(List<String> personIdenter) {
        if (unleash.isEnabled(UNLEASH_USE_NEW_RESSURS_STATE_REPOSITORY)) {
            return nomStateRessursRepository.findByPersonidentBulk(personIdenter);
        } else {
            return oldRessursStateRepository.findByPersonidentBulk(personIdenter);
        }
    }

    @Override
    public Map<String, RessursState> findByNavidentBulk(List<String> navIdenter) {
        if (unleash.isEnabled(UNLEASH_USE_NEW_RESSURS_STATE_REPOSITORY)) {
            return nomStateRessursRepository.findByNavidentBulk(navIdenter);
        } else {
            return oldRessursStateRepository.findByNavidentBulk(navIdenter);
        }
    }

    @Override
    public void update(RessursState ressursEventData) {
        throw new RuntimeException("ERROR! Call to update() in ToggledRessursStateRepository - should not be called on implemented beans directly");
    }

    @Override
    public void clear() {
        throw new RuntimeException("ERROR! Call to clear() in ToggledRessursStateRepository - should not be called on implemented beans directly");
    }

    @Override
    public void remove(String fid) {
        throw new RuntimeException("ERROR! Call to remove() in ToggledRessursStateRepository - should not be called on implemented beans directly");
    }
}
