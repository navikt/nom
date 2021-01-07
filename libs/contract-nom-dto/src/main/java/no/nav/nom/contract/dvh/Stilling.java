package no.nav.nom.contract.dvh;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Value
@Builder
public class Stilling {
    public static final String TYPE = "no.nav.nom.dvh:stilling";

    @JsonProperty("RESOURCE_ID")
    String resourceId;

    @JsonProperty("FRA_DATO")
    LocalDate fraDato;

    @JsonProperty("TIL_DATO")
    LocalDate tilDato;

    @JsonProperty("ANSETTELSESTYPE_BESK")
    String ansettelsesTypeBesk;

    @JsonProperty("ORGENHET")
    String orgEnhet;

    @JsonProperty("ORGENHET_BESK")
    String orgEnhetBesk;

    @JsonProperty("STILLING_I_BUNN")
    double stillingIBunn;

    @JsonProperty("PERMISJON")
    String permisjon;

    @JsonProperty("LAST_UPDATE")
    LocalDateTime timestamp;

}
