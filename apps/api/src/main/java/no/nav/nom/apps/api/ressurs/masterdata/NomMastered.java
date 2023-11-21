package no.nav.nom.apps.api.ressurs.masterdata;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.ressurs.eventsourcing.state.RessursStateRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class NomMastered {
    public static final String UNLEASH_DO_NOT_UPDATE_MASTERED_RESSURS = "nom.master-ressurs.ikke-oppdater-fra-dvh";
    final Map<String, LocalDateTime> masteredNavidenter = new ConcurrentHashMap<>();
    final RessursStateRepository ressursStateRepository;
    final KafkaTemplate<String, LocalDateTime> nomMasteredRessursProducerTemplate;

    public NomMastered(RessursStateRepository ressursStateRepository, KafkaTemplate<String, LocalDateTime> nomMasteredRessursProducerTemplate) {
        this.ressursStateRepository = ressursStateRepository;
        this.nomMasteredRessursProducerTemplate = nomMasteredRessursProducerTemplate;
    }

    public boolean markRessursAsMasteredInNom(String navident) {
        var now = LocalDateTime.now();
        var ressursState = ressursStateRepository.findByNavident(navident);
        if (ressursState.isEmpty()) {
            log.warn("RessursState for navident {} finnes ikke, kan ikke legges til som mastret.", navident);
            return false;
        }
        if (!masteredNavidenter.containsKey(navident)) {
            postMasteredMessage(navident, now);
        }
        return true;
    }

    public boolean unmarkRessursAsMasteredInNom(String navident) {
        var ressursState = ressursStateRepository.findByNavident(navident);
        if (ressursState.isEmpty()) {
            log.warn("RessursState for navident {} finnes ikke, kan ikke legges til som mastret.", navident);
            return false;
        }
        if (!masteredNavidenter.containsKey(navident)) {
            return false;
        }
        postRemoveMasteredMessage(navident);
        return true;
    }

    public int count() {
        return masteredNavidenter.size();
    }

    public boolean isMastered(String navident) {
        return masteredNavidenter.containsKey(navident);
    }

    public List<String> navidenter() {
        return masteredNavidenter.keySet().stream().toList();
    }

    void updateRessursAsMasteredInNomFromKafka(String navident, LocalDateTime since) {
        var ressursState = ressursStateRepository.findByNavident(navident);
        if (ressursState.isEmpty()) {
            log.warn("RessursState for navident {} finnes ikke, kan ikke oppdateres som mastret.", navident);
            return;
        }
        masteredNavidenter.put(navident, since);
    }

    void removeRessursAsMasteredInNomFromKafka(String navident) { // Tombstones
        var ressursState = ressursStateRepository.findByNavident(navident);
        if (ressursState.isEmpty()) {
            log.warn("RessursState for navident {} finnes ikke, kan ikke fjernes som mastret.", navident);
            return;
        }
        masteredNavidenter.remove(navident);
    }

    private void postMasteredMessage(String navident, LocalDateTime since) {
        nomMasteredRessursProducerTemplate.sendDefault(navident, since);
    }

    private void postRemoveMasteredMessage(String navident) {
        nomMasteredRessursProducerTemplate.sendDefault(navident, null);
    }

    public void clear() {
        masteredNavidenter.clear();
    }
}
