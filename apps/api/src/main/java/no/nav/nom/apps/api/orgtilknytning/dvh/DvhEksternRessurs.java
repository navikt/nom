package no.nav.nom.apps.api.orgtilknytning.dvh;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDate;

public record DvhEksternRessurs(
    String payload,
    String navident,
    String remedyEnhetId,
    LocalDate remedyStart,
    LocalDate remedySlutt,
    boolean remedyAktiv
) {
    private static final LocalDate MAX_DATE = LocalDate.of(2099, 12, 31);
    public DvhEksternRessurs(JsonNode json) {
        this(
            json.toString(),
            json.get("NAV_IDENT").asText(),
            json.get("REMEDY_ENHET_ID").asText(),
            LocalDate.parse(json.get("REMEDY_START_DATO").asText()),
            getOrNullIfMaxDate(LocalDate.parse(json.get("REMEDY_SLUTT_DATO").asText())),
            "JA".equals(json.get("REMEDY_SLUTT_DATO").asText())
        );
    }

    private static LocalDate getOrNullIfMaxDate(LocalDate date) {
        return MAX_DATE.equals(date) ? null : date;
    }
}
