package no.nav.nom.apps.api.hierarki;

public record NomId(String nomId) {
    public NomId{
        // todo, validation
    }
    public static NomId create(String oeId) {
        return new NomId(oeId);
    }
}
