package no.nav.nom.apps.api.ressurs.masterdata;

import java.time.LocalDateTime;
import java.util.List;

public interface NomMastered {
    public static final String UNLEASH_DO_NOT_UPDATE_MASTERED_RESSURS = "nom.master-ressurs.ikke-oppdater-fra-dvh";

    boolean markRessursAsMasteredInNom(String navident);

    boolean unmarkRessursAsMasteredInNom(String navident);

    int count();

    boolean isMastered(String navident);

    List<String> navidenter();

    void updateRessursAsMasteredInNomFromKafka(String navident, LocalDateTime since);

    void removeRessursAsMasteredInNomFromKafka(String navident);

    void clear();
}
