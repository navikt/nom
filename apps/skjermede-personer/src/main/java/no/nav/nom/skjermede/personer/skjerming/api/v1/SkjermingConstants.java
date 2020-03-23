package no.nav.nom.skjermede.personer.skjerming.api.v1;

public class SkjermingConstants {
    public static final String BASE_URL = "/api/v1/skjerming";
    public static final String BY_NAVIDENT = "{navident}";
    public static final String BY_ANSATT = "ansatt";
    public static final String FAMILIEMEDLEMER = "familiemedlemer";
    public static final String NOT_FOUND_MESSAGE = "Skjerming for NAV ident %s ikke funnet";
    public static final String UNAUTHORIZED_MESSAGE = "Tilgang nektet";
    public static final String DUPLICATE_SKJERMING_MESSAGE = "Personen er allerede skjermet";
}
