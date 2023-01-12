package no.nav.nom.apps.nom.unit4.unit4.dto;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Unit4RessursType {
    FAST_ANSATTE("F"),
    KOMMUNALT_ANSATT("N"),
    EKSTERN("E"),
    MIDLERTIDIG_ANSATTE("M"),
    LEDER("B"),
    IKKJE_RELEVANT("I"),
    KOMMUNAL_LEDER("K"),
    LESE_OG_SEKRETAERHJELP("L"),
    EKSTERN_KONSULENT("O"),
    PENSJONISTER("P"),
    LAERLING("R"),
    STUDENT("S"),
    TOLK("T"),
    VENTELOENN("V"),
    TIMELOENNA("Y");

    private final String value;

    Unit4RessursType(String value) {
        this.value = value;
    }

    @JsonValue // Jackson will serialize this enum as the value of the field
    public String getValue() {
        return value;
    }
}
