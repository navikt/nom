package no.nav.nom.apps.api.ressurs;

public interface NomMastered {
    boolean markRessursAsMasteredInNom(String navident);

    boolean unmarkRessursAsMasteredInNom(String navident);

    boolean isMastered(String navident);
}
