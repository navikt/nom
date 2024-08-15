package no.nav.nom.libs.state.ressurs.mastered;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface MasteredRessursStateRepository {
    int count();

    boolean isMastered(String navident);

    List<String> navidenter();

    Map<String, LocalDateTime> getMasteredNavidenter();

    void update(String navident, LocalDateTime since);

    void clear();
}
