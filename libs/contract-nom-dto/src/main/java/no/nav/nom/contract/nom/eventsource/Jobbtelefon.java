package no.nav.nom.contract.nom.eventsource;

import no.nav.nom.contract.nom.TelefonType;

public record Jobbtelefon(
        String nummer,
        TelefonType type
) {
    public static String stripCountryCode(String nummer) {
        if (nummer.length() > 8 && nummer.startsWith("47")) {
            return nummer.substring(2);
        }
        return nummer;
    }
}
