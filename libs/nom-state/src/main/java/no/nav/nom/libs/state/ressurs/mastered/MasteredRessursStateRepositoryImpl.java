package no.nav.nom.libs.state.ressurs.mastered;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Comparator.comparing;

@Service
@Slf4j
public class MasteredRessursStateRepositoryImpl implements MasteredRessursStateRepository {
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

    public Map<String, LocalDateTime> getMasteredNavidenter() {
        return masteredNavidenter;
    }

    @Override
    public void update(String navident, LocalDateTime since) {
        if (since == null) {
            removeRessursAsMasteredInNom(navident);
        } else {
            updateRessursAsMasteredInNom(navident, since);
        }
    }

    public void updateRessursAsMasteredInNom(String navident, LocalDateTime since) {
        masteredNavidenter.put(navident, since);
    }

    public void removeRessursAsMasteredInNom(String navident) { // Tombstones
        masteredNavidenter.remove(navident);
    }

    @Override
    public void clear() {
        masteredNavidenter.clear();
    }
}
