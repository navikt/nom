package no.nav.nom.apps.api.authorize.eventsourcing.state;

import io.getunleash.Unleash;
import no.nav.nom.contract.nom.state.LederTilgangState;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.libs.state.authorize.DelegertLederStateRepository;
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
public class ToggledDelegertLederStateRepository implements DelegertLederStateRepository {
    public static final String UNLEASH_USE_NEW_DELEGERT_LEDER_STATE_REPOSITORY = "nom.delegert_leder_state.use_new_repository";
    private final DelegertLederStateRepository oldDelegertLederStateRepository;
    private final DelegertLederStateRepository nomStateDelegertLederRepository;
    private final Unleash unleash;

    public ToggledDelegertLederStateRepository(
        @Qualifier("oldDelegertLederStateRepository") DelegertLederStateRepository oldDelegertLederStateRepository,
        @Qualifier("nomStateDelegertLederRepository") DelegertLederStateRepository nomStateDelegertLederRepository,
        Unleash unleash
    ) {
        this.oldDelegertLederStateRepository = oldDelegertLederStateRepository;
        this.nomStateDelegertLederRepository = nomStateDelegertLederRepository;
        this.unleash = unleash;
    }


    @Override
    public Optional<LederTilgangState> findByFid(String fid) {
        if (unleash.isEnabled(UNLEASH_USE_NEW_DELEGERT_LEDER_STATE_REPOSITORY)) {
            return nomStateDelegertLederRepository.findByFid(fid);
        } else {
            return oldDelegertLederStateRepository.findByFid(fid);
        }
    }

    @Override
    public List<LederTilgangState> findByNavident(String navident) {
        if (unleash.isEnabled(UNLEASH_USE_NEW_DELEGERT_LEDER_STATE_REPOSITORY)) {
            return nomStateDelegertLederRepository.findByNavident(navident);
        } else {
            return oldDelegertLederStateRepository.findByNavident(navident);
        }
    }

    @Override
    public List<LederTilgangState> findDelegertAvNavident(String navident) {
        if (unleash.isEnabled(UNLEASH_USE_NEW_DELEGERT_LEDER_STATE_REPOSITORY)) {
            return nomStateDelegertLederRepository.findDelegertAvNavident(navident);
        } else {
            return oldDelegertLederStateRepository.findDelegertAvNavident(navident);
        }
    }

    @Override
    public List<String> aktiveDelegeringerForOrgenheterTilNavident(String navident) {
        if (unleash.isEnabled(UNLEASH_USE_NEW_DELEGERT_LEDER_STATE_REPOSITORY)) {
            return nomStateDelegertLederRepository.aktiveDelegeringerForOrgenheterTilNavident(navident);
        } else {
            return oldDelegertLederStateRepository.aktiveDelegeringerForOrgenheterTilNavident(navident);
        }
    }

    @Override
    public boolean erOrgenhetDelegertTilNavident(String orgenhetId, String navident) {
        if (unleash.isEnabled(UNLEASH_USE_NEW_DELEGERT_LEDER_STATE_REPOSITORY)) {
            return nomStateDelegertLederRepository.erOrgenhetDelegertTilNavident(orgenhetId, navident);
        } else {
            return oldDelegertLederStateRepository.erOrgenhetDelegertTilNavident(orgenhetId, navident);
        }
    }

    @Override
    public Optional<LederTilgangState> lederTilgangForOrgenhetOgNavident(String orgenhetId, String navident) {
        if (unleash.isEnabled(UNLEASH_USE_NEW_DELEGERT_LEDER_STATE_REPOSITORY)) {
            return nomStateDelegertLederRepository.lederTilgangForOrgenhetOgNavident(orgenhetId, navident);
        } else {
            return oldDelegertLederStateRepository.lederTilgangForOrgenhetOgNavident(orgenhetId, navident);
        }
    }

    @Override
    public Collection<LederTilgangState> getAll() {
        if (unleash.isEnabled(UNLEASH_USE_NEW_DELEGERT_LEDER_STATE_REPOSITORY)) {
            return nomStateDelegertLederRepository.getAll();
        } else {
            return oldDelegertLederStateRepository.getAll();
        }
    }

    @Override
    public int count() {
        return 0;
    }

    @Override
    public void update(LederTilgangState lederTilgangState) {
        throw new RuntimeException("ERROR! Call to update() in ToggledDelegertLederStateRepository - should not be called on implemented beans directly");
    }

    @Override
    public void clear() {
        throw new RuntimeException("ERROR! Call to clear() in ToggledDelegertLederStateRepository - should not be called on implemented beans directly");
    }
}
