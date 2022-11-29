package no.nav.nom.apps.api.common;

import com.github.f4b6a3.ulid.UlidCreator;

public class PrefixedUlidCreator {

    private static final String RESSURS_ULID_PREFIX = "ressurs-";
    private static final String ORGENHET_ULID_PREFIX = "orgEnhet-";
    private static final String EVENT_ULID_PREFIX = "event-";

    private static String createPrefixedUlid(String prefix) {
        return prefix + UlidCreator.getUlid().toString();
    }

    public static String createRessursUlid() {
        return createPrefixedUlid(RESSURS_ULID_PREFIX);
    }

    public static String createOrgEnhetUlid() {
        return createPrefixedUlid(ORGENHET_ULID_PREFIX);
    }

    public static String createEventUlid() {
        return createPrefixedUlid(EVENT_ULID_PREFIX);
    }
}
