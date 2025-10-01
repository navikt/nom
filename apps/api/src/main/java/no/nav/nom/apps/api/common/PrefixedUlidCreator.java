package no.nav.nom.apps.api.common;

import com.github.f4b6a3.ulid.UlidCreator;

public class PrefixedUlidCreator {

    private static final String RESSURS_ULID_PREFIX = "ressurs-";
    private static final String ORGENHET_ULID_PREFIX = "orgEnhet-";
    private static final String ORGTILKNYTNING_ULID_PREFIX = "orgtilknytning-";
    private static final String TMP_ORGTILKNYTNING_ULID_PREFIX = "tmp-orgtilknytning-";
    private static final String EVENT_ULID_PREFIX = "event-";
    private static final String ORGANISERING_ULID_PREFIX = "organisering-";
    private static final String AUTHORAZITION_ULID_PREFIX = "authorize-";
    private static final String ORGENHET_LEDER_PREFIX = "leder-";

    private static String createPrefixedUlid(String prefix) {
        return prefix + UlidCreator.getUlid().toString();
    }

    public static String createRessursUlid() {
        return createPrefixedUlid(RESSURS_ULID_PREFIX);
    }

    public static String createOrgEnhetUlid() {
        return createPrefixedUlid(ORGENHET_ULID_PREFIX);
    }

    public static String createOrgtilknytningUlid() {
        return createPrefixedUlid(ORGTILKNYTNING_ULID_PREFIX);
    }
    public static String createTmpOrgtilknytningUlid() {
        return createPrefixedUlid(TMP_ORGTILKNYTNING_ULID_PREFIX);
    }

    public static String createEventUlid() {
        return createPrefixedUlid(EVENT_ULID_PREFIX);
    }

    public static String createOrganiseringUlid() {
        return createPrefixedUlid(ORGANISERING_ULID_PREFIX);
    }

    public static String createAuthorizationUlid() {
        return createPrefixedUlid(AUTHORAZITION_ULID_PREFIX);
    }

    public static String createLederUlid() {
        return createPrefixedUlid(ORGENHET_LEDER_PREFIX);
    }
}
