package no.nav.nom.apps.nom.unit4.unit4.dto;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Unit4Status {
    AKTIV("N"),
    PARKERT("P");
    //SPERRET("S") BRUKES IKKE
    //AVSLUTTET("T") BRUKES IKKE


    private final String value;

    Unit4Status(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
