package no.nav.nom.apps.api.ressurs.masterdata;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class NomMasteredImpl implements NomMastered {
    protected final Map<String, LocalDateTime> masteredNavidenter = new ConcurrentHashMap<>();
    final RessursStateRepository ressursStateRepository;
    final KafkaTemplate<String, LocalDateTime> nomMasteredRessursProducerTemplate;

    public NomMasteredImpl(
        RessursStateRepository ressursStateRepository,
        KafkaTemplate<String, LocalDateTime> nomMasteredRessursProducerTemplate
    ) {
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
        if (!masteredNavidenter.containsKey(navident)) {
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
        if (!masteredNavidenter.containsKey(navident)) {
            return false;
        }
        postRemoveMasteredMessage(navident);
        return true;
    }

    @Override
    public int count() {
        return masteredNavidenter.size();
    }

    @Override
    public boolean isMastered(String navident) {
        return masteredNavidenter.containsKey(navident);
    }

    @Override
    public List<String> navidenter() {
        return masteredNavidenter.keySet().stream().toList();
    }

    @Override
    public void updateRessursAsMasteredInNomFromKafka(String navident, LocalDateTime since) {
        masteredNavidenter.put(navident, since);
    }

    @Override
    public void removeRessursAsMasteredInNomFromKafka(String navident) { // Tombstones
        masteredNavidenter.remove(navident);
    }

    private void postMasteredMessage(String navident, LocalDateTime since) {
        nomMasteredRessursProducerTemplate.sendDefault(navident, since);
    }

    private void postRemoveMasteredMessage(String navident) {
        nomMasteredRessursProducerTemplate.sendDefault(navident, null);
    }

    @Override
    public void clear() {
        masteredNavidenter.clear();
    }
}
