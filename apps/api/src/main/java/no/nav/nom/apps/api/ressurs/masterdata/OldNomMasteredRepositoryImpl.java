package no.nav.nom.apps.api.ressurs.masterdata;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import no.nav.nom.libs.state.ressurs.mastered.MasteredRessursStateRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component("OldNomMasteredRepository")
@Slf4j
public class OldNomMasteredRepositoryImpl implements MasteredRessursStateRepository {
    protected final Map<String, LocalDateTime> masteredNavidenter = new ConcurrentHashMap<>();

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
    public void update(String navident, LocalDateTime since) {
        if (since == null) {
            removeRessursAsMasteredInNom(navident);
        } else {
            updateRessursAsMasteredInNom(navident, since);
        }
    }

    private void updateRessursAsMasteredInNom(String navident, LocalDateTime since) {
        masteredNavidenter.put(navident, since);
    }

    private void removeRessursAsMasteredInNom(String navident) { // Tombstones
        masteredNavidenter.remove(navident);
    }

    @Override
    public void clear() {
        masteredNavidenter.clear();
    }
}
