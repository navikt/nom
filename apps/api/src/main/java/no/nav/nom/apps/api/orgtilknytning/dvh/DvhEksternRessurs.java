package no.nav.nom.apps.api.orgtilknytning.dvh;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JsonNode;

import java.time.LocalDate;

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

    public static DvhEksternRessurs fromOrds(JsonNode json) {
        try {
            return new DvhEksternRessurs(
                json.toPrettyString(),
                json.get("nav_ident").asString(),
                getNullableStringNode(json, "remedy_enhet_id"),
                getNullableStringNode(json, "orgenhet_paa_ressurs"),
                getNullableStringNode(json, "orgenhet_paa_ressurs_niva"),
                LocalDate.parse(json.get("remedy_start_dato").asString()),
                getOrNullIfMaxDate(json, "remedy_slutt_dato"),
                "JA".equals(json.get("remedy_aktiv").asString())
            );
        } catch (Exception e) {
            log.error("Unable to parse DvhEksternRessurs from {}", json.toString(), e);
            throw new IllegalArgumentException("Cannot parse json for ekstern ressurs:\n%s".formatted(json.toString()), e);
        }
    }

    private static LocalDate getOrNullIfMaxDate(JsonNode json, String field_name) {
        JsonNode node = json.get(field_name);
        LocalDate date = null;
        if (!node.isNull()) {
            date = LocalDate.parse(node.asString());
        }
        return MAX_DATE.equals(date) ? null : date;
    }

    private static String getNullableStringNode(JsonNode json, String field_name) {
        JsonNode node = json.get(field_name);
        return node.isNull() ? null : node.asString();
    }
}
