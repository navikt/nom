package no.nav.nom.apps.api.hierarki;

public record NavIdent(String navIdent) {
    public NavIdent {
        if (navIdent == null || navIdent.isEmpty()) {
            throw new IllegalArgumentException("Bad navident. Cannot be null or empty.");
        }
        var formatOk = navIdent.matches("[a-zA-Z]\\d{6}");
        if (!formatOk) {
            throw new IllegalArgumentException(String.format("Bad navident %s. Wrong format",navIdent));
        }
    }

    public static NavIdent create(String navIdent) {
        return new NavIdent(navIdent);
    }
}
