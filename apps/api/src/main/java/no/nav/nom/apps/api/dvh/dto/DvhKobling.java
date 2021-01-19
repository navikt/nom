package no.nav.nom.apps.api.dvh.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Value;
import lombok.val;

import java.time.LocalDate;

@Value
public class DvhKobling {

    private String ressursId;
    private String agressoId;
    private LocalDate gyldigFom;
    private LocalDate gyldigTom;

    public DvhKobling(JsonNode node) {
        ressursId = node.get("RESOURCE_ID").asText();
        agressoId = node.get("ORGENHET").asText();
        gyldigFom = LocalDate.parse(node.get("FRA_DATO").asText());
        val gyldigTomText = node.get("TIL_DATO").asText();
        gyldigTom = gyldigTomText != null ? LocalDate.parse(gyldigTomText) : null;
    }
}
