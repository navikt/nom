package no.nav.nom.apps.api.orgenhet.eventsourcing.state;

import io.getunleash.Unleash;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Profile("!test")
@Primary
public class ToggledOrgEnhetStateRepository implements OrgEnhetStateRepository {
    public static final String UNLEASH_USE_NEW_ORGENHET_STATE_REPOSITORY = "nom.orgenhet_state.use_new_repository";
    private final OrgEnhetStateRepository oldOrgEnhetStateRepository;
    private final OrgEnhetStateRepository nomStateOrgEnhetRepository;
    private final Unleash unleash;

    public ToggledOrgEnhetStateRepository(
        @Qualifier("oldOrgEnhetStateRepository") OrgEnhetStateRepository oldOrgEnhetStateRepository,
        @Qualifier("nomStateOrgEnhetRepository") OrgEnhetStateRepository nomStateOrgEnhetRepository,
        Unleash unleash
    ) {
        this.oldOrgEnhetStateRepository = oldOrgEnhetStateRepository;
        this.nomStateOrgEnhetRepository = nomStateOrgEnhetRepository;
        this.unleash = unleash;
    }

    @Override
    public Optional<OrgEnhetState> findByFid(String fid) {
        if (unleash.isEnabled(UNLEASH_USE_NEW_ORGENHET_STATE_REPOSITORY)) {
            return nomStateOrgEnhetRepository.findByFid(fid);
        } else {
            return oldOrgEnhetStateRepository.findByFid(fid);
        }
    }

    @Override
    public List<OrgEnhetState> findByFidIn(List<String> fids) {
        if (unleash.isEnabled(UNLEASH_USE_NEW_ORGENHET_STATE_REPOSITORY)) {
            return nomStateOrgEnhetRepository.findByFidIn(fids);
        } else {
            return oldOrgEnhetStateRepository.findByFidIn(fids);
        }
    }

    @Override
    public Optional<OrgEnhetState> findById(String id) {
        if (unleash.isEnabled(UNLEASH_USE_NEW_ORGENHET_STATE_REPOSITORY)) {
            return nomStateOrgEnhetRepository.findById(id);
        } else {
            return oldOrgEnhetStateRepository.findById(id);
        }
    }

    @Override
    public List<OrgEnhetState> findByIdIn(List<String> ids) {
        if (unleash.isEnabled(UNLEASH_USE_NEW_ORGENHET_STATE_REPOSITORY)) {
            return nomStateOrgEnhetRepository.findByIdIn(ids);
        } else {
            return oldOrgEnhetStateRepository.findByIdIn(ids);
        }
    }

    @Override
    public List<OrgEnhetState> findByIdIn(Set<String> ids) {
        if (unleash.isEnabled(UNLEASH_USE_NEW_ORGENHET_STATE_REPOSITORY)) {
            return nomStateOrgEnhetRepository.findByIdIn(ids);
        } else {
            return oldOrgEnhetStateRepository.findByIdIn(ids);
        }
    }

    @Override
    public List<OrgEnhetState> findAllByUniqueAgressoId(String id) {
        if (unleash.isEnabled(UNLEASH_USE_NEW_ORGENHET_STATE_REPOSITORY)) {
            return nomStateOrgEnhetRepository.findAllByUniqueAgressoId(id);
        } else {
            return oldOrgEnhetStateRepository.findAllByUniqueAgressoId(id);
        }
    }

    @Override
    public List<OrgEnhetState> findAllByAgressoIdIn(Set<String> agressoIds) {
        if (unleash.isEnabled(UNLEASH_USE_NEW_ORGENHET_STATE_REPOSITORY)) {
            return nomStateOrgEnhetRepository.findAllByAgressoIdIn(agressoIds);
        } else {
            return oldOrgEnhetStateRepository.findAllByAgressoIdIn(agressoIds);
        }
    }

    @Override
    public List<OrgEnhetState> findAllByAgressoIdAndNiv(String id) {
        if (unleash.isEnabled(UNLEASH_USE_NEW_ORGENHET_STATE_REPOSITORY)) {
            return nomStateOrgEnhetRepository.findAllByAgressoIdAndNiv(id);
        } else {
            return oldOrgEnhetStateRepository.findAllByAgressoIdAndNiv(id);
        }
    }

    @Override
    public Optional<OrgEnhetState> findNewestUniqueAgressoId(String id) {
        if (unleash.isEnabled(UNLEASH_USE_NEW_ORGENHET_STATE_REPOSITORY)) {
            return nomStateOrgEnhetRepository.findNewestUniqueAgressoId(id);
        } else {
            return oldOrgEnhetStateRepository.findNewestUniqueAgressoId(id);
        }
    }

    @Override
    public Collection<OrgEnhetState> getAll() {
        if (unleash.isEnabled(UNLEASH_USE_NEW_ORGENHET_STATE_REPOSITORY)) {
            return nomStateOrgEnhetRepository.getAll();
        } else {
            return oldOrgEnhetStateRepository.getAll();
        }
    }

    @Override
    public int count() {
        if (unleash.isEnabled(UNLEASH_USE_NEW_ORGENHET_STATE_REPOSITORY)) {
            return nomStateOrgEnhetRepository.count();
        } else {
            return oldOrgEnhetStateRepository.count();
        }
    }

    @Override
    public void update(OrgEnhetState newOrgEnhetState) {
        throw new RuntimeException("ERROR! Call to update() in ToggledOrgEnhetStateRepository - should not be called on implemented beans directly");
    }

    @Override
    public void clear() {
        throw new RuntimeException("ERROR! Call to clear() in ToggledOrgEnhetStateRepository - should not be called on implemented beans directly");
    }
}
