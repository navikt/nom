package no.nav.nom.libs.state.ressurs.mastered;

import java.time.LocalDateTime;
import java.util.List;

public interface MasteredRessursStateRepository {
    int count();

    boolean isMastered(String navident);

    List<String> navidenter();

    void update(String navident, LocalDateTime since);

    void clear();
}
