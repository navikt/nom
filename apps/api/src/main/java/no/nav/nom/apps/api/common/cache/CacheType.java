package no.nav.nom.apps.api.common.cache;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum CacheType {
    OTENDRINGER("no.nav.nom.cache.type:OTENDRINGER");

    String cacheType;

    CacheType(String cacheType) {
        this.cacheType = cacheType;
    }

    @JsonValue
    public String getCacheType() {
        return cacheType;
    }

    public static CacheType fromString(String value) {
        return Arrays.stream(CacheType.values())
            .filter(v -> v.getCacheType().equals(value))
            .findAny()
            .orElseThrow(() -> new IllegalArgumentException("No enum with value " + value + " found"));
    }

    public static String toString(CacheType type) {
        return type.getCacheType();
    }
}
