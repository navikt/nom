package no.nav.nom.apps.api.ressurs.masterdata;

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
    final MasteredRessursStateRepository masteredRessursStateRepository;
    final RessursStateRepository ressursStateRepository;
    final KafkaTemplate<String, LocalDateTime> nomMasteredRessursProducerTemplate;

    public NomMasteredService(
        MasteredRessursStateRepository masteredRessursStateRepository,
        RessursStateRepository ressursStateRepository,
        KafkaTemplate<String, LocalDateTime> nomMasteredRessursProducerTemplate
    ) {
        this.masteredRessursStateRepository = masteredRessursStateRepository;
        this.ressursStateRepository = ressursStateRepository;
        this.nomMasteredRessursProducerTemplate = nomMasteredRessursProducerTemplate;
    }

    @Override
    public boolean markRessursAsMasteredInNom(String navident) {
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
        return masteredRessursStateRepository.isMastered(navident);
    }

    private void postMasteredMessage(String navident, LocalDateTime since) {
        nomMasteredRessursProducerTemplate.sendDefault(navident, since);
    }

    private void postRemoveMasteredMessage(String navident) {
        nomMasteredRessursProducerTemplate.sendDefault(navident, null);
    }
}
