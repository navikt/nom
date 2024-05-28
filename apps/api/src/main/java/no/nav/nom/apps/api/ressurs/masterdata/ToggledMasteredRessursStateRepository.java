package no.nav.nom.apps.api.ressurs.masterdata;

import io.getunleash.Unleash;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import no.nav.nom.libs.state.ressurs.mastered.MasteredRessursStateRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

@Service
@Profile("!test")
@Primary
public class ToggledMasteredRessursStateRepository implements MasteredRessursStateRepository {
    public static final String UNLEASH_USE_NEW_MASTERED_RESSURS_STATE_REPOSITORY = "nom.mastered_ressurs_state.use_new_repository";
    private final MasteredRessursStateRepository oldMasteredRessursStateRepository;
    private final MasteredRessursStateRepository nomStateMasteredRessursRepository;
    private final Unleash unleash;

    public ToggledMasteredRessursStateRepository(
        @Qualifier("OldNomMasteredRepository") MasteredRessursStateRepository oldMasteredRessursStateRepository,
        @Qualifier("nomStateMasteredRessursRepository") MasteredRessursStateRepository nomStateMasteredRessursRepository,
        Unleash unleash
    ) {
        this.oldMasteredRessursStateRepository = oldMasteredRessursStateRepository;
        this.nomStateMasteredRessursRepository = nomStateMasteredRessursRepository;
        this.unleash = unleash;
    }


    @Override
    public int count() {
        if (unleash.isEnabled(UNLEASH_USE_NEW_MASTERED_RESSURS_STATE_REPOSITORY)) {
            return nomStateMasteredRessursRepository.count();
        } else {
            return oldMasteredRessursStateRepository.count();
        }
    }

    @Override
    public boolean isMastered(String navident) {
        if (unleash.isEnabled(UNLEASH_USE_NEW_MASTERED_RESSURS_STATE_REPOSITORY)) {
            return nomStateMasteredRessursRepository.isMastered(navident);
        } else {
            return oldMasteredRessursStateRepository.isMastered(navident);
        }
    }

    @Override
    public List<String> navidenter() {
        if (unleash.isEnabled(UNLEASH_USE_NEW_MASTERED_RESSURS_STATE_REPOSITORY)) {
            return nomStateMasteredRessursRepository.navidenter();
        } else {
            return oldMasteredRessursStateRepository.navidenter();
        }
    }

    @Override
    public void update(String navident, LocalDateTime since) {
        throw new RuntimeException("ERROR! Call to update() in ToggledRessursStateRepository - should not be called on implemented beans directly");
    }

    @Override
    public void clear() {
        throw new RuntimeException("ERROR! Call to clear() in ToggledRessursStateRepository - should not be called on implemented beans directly");
    }
}
