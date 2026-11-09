package no.nav.nom.apps.api.ressurs.masterdata;

import io.getunleash.Unleash;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.ressurs.NomMastered;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import no.nav.nom.libs.state.ressurs.mastered.MasteredRessursStateRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
public class NomMasteredService implements NomMastered {
    public static final String UNLEASH_BEHANDLE_ALLE_SOM_MASTRET_I_NOM = "nom-api.behandle_alle_ressurser_som_mastret_i_nom";

    final MasteredRessursStateRepository masteredRessursStateRepository;
    final RessursStateRepository ressursStateRepository;
    final KafkaTemplate<String, LocalDateTime> nomMasteredRessursProducerTemplate;
    private final Unleash unleash;

    public NomMasteredService(
        MasteredRessursStateRepository masteredRessursStateRepository,
        RessursStateRepository ressursStateRepository,
        KafkaTemplate<String, LocalDateTime> nomMasteredRessursProducerTemplate, Unleash unleash
    ) {
        this.masteredRessursStateRepository = masteredRessursStateRepository;
        this.ressursStateRepository = ressursStateRepository;
        this.nomMasteredRessursProducerTemplate = nomMasteredRessursProducerTemplate;
        this.unleash = unleash;
    }

    @Override
    public boolean markRessursAsMasteredInNom(String navident) {
        if (unleash.isEnabled(UNLEASH_BEHANDLE_ALLE_SOM_MASTRET_I_NOM, false)) {
            return true;
        }

        var now = LocalDateTime.now();
        var ressursState = ressursStateRepository.findByNavident(navident);
        if (ressursState.isEmpty()) {
            log.warn("RessursState for navident {} finnes ikke, kan ikke legges til som mastret.", navident);
            return false;
        }
        if (!masteredRessursStateRepository.isMastered(navident)) {
            postMasteredMessage(navident, now);
        }
        return true;
    }

    @Override
    public boolean unmarkRessursAsMasteredInNom(String navident) {
        if (unleash.isEnabled(UNLEASH_BEHANDLE_ALLE_SOM_MASTRET_I_NOM, false)) {
            return true;
        }

        var ressursState = ressursStateRepository.findByNavident(navident);
        if (ressursState.isEmpty()) {
            log.warn("RessursState for navident {} finnes ikke, kan ikke legges til som mastret.", navident);
            return false;
        }
        if (!masteredRessursStateRepository.isMastered(navident)) {
            return false;
        }
        postRemoveMasteredMessage(navident);
        return true;
    }

    @Override
    public boolean isMastered(String navident) {
        if (unleash.isEnabled(UNLEASH_BEHANDLE_ALLE_SOM_MASTRET_I_NOM, false)) {
            return true;
        }

        return masteredRessursStateRepository.isMastered(navident);
    }

    private void postMasteredMessage(String navident, LocalDateTime since) {
        nomMasteredRessursProducerTemplate.sendDefault(navident, since);
    }

    private void postRemoveMasteredMessage(String navident) {
        nomMasteredRessursProducerTemplate.sendDefault(navident, null);
    }
}
