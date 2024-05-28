package no.nav.nom.apps.api.ressurs;

public interface NomMastered {
    String UNLEASH_DO_NOT_UPDATE_MASTERED_RESSURS = "nom.master-ressurs.ikke-oppdater-fra-dvh";

    boolean markRessursAsMasteredInNom(String navident);

    boolean unmarkRessursAsMasteredInNom(String navident);

    boolean isMastered(String navident);
}
