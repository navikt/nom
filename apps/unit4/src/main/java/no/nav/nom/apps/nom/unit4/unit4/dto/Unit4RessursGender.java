package no.nav.nom.apps.nom.unit4.unit4.dto;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Unit4RessursGender {
    MALE("M"),
    FEMALE("F");

    private final String value;

    Unit4RessursGender(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

}
