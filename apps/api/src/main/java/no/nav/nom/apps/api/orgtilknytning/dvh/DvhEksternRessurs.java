package no.nav.nom.apps.api.orgtilknytning.dvh;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.Optional;

@Slf4j
public record DvhEksternRessurs(
    String payload,
    String navident,
    String remedyEnhetId,
    String orgenhetPaaRessurs,
    String orgenhetPaaRessursNivaa,
    LocalDate remedyStart,
    LocalDate remedySlutt,
    boolean remedyAktiv
) {
    private static final LocalDate MAX_DATE = LocalDate.of(2099, 12, 31);

//    public DvhEksternRessurs(JsonNode json) {
//        this(
//            json.toString(),
//            json.get("NAV_IDENT").asText(),
//            getNullableStringNode(json, "REMEDY_ENHET_ID"),
//            getNullableStringNode(json, "ORGENHET_PAA_RESSURS"),
//            getNullableStringNode(json, "ORGENHET_PAA_RESSURS_NIVA"),
//            LocalDate.parse(json.get("REMEDY_START_DATO").asText()),
//            getOrNullIfMaxDate(LocalDate.parse(json.get("REMEDY_SLUTT_DATO").asText())),
//            "JA".equals(json.get("REMEDY_AKTIV").asText())
//        );
//    }

    public static DvhEksternRessurs fromJson(JsonNode json) {
        try {
            return new DvhEksternRessurs(
                json.toString(),
                json.get("NAV_IDENT").asText(),
                getNullableStringNode(json, "REMEDY_ENHET_ID"),
                getNullableStringNode(json, "ORGENHET_PAA_RESSURS"),
                getNullableStringNode(json, "ORGENHET_PAA_RESSURS_NIVA"),
                LocalDate.parse(json.get("REMEDY_START_DATO").asText()),
                getOrNullIfMaxDate(LocalDate.parse(json.get("REMEDY_SLUTT_DATO").asText())),
                "JA".equals(json.get("REMEDY_AKTIV").asText())
            );
        } catch (Exception e) {
            log.error("Unable to parse DvhEksternRessurs from {}", json.toString(), e);
            return null;
        }
    }

    private static LocalDate getOrNullIfMaxDate(LocalDate date) {
        return MAX_DATE.equals(date) ? null : date;
    }

    private static String getNullableStringNode(JsonNode json, String field_name) {
        JsonNode node = json.get(field_name);
        return node.isNull() ? null : node.asText();
    }
}
