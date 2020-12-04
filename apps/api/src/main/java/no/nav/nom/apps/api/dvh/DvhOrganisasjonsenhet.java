package no.nav.nom.apps.api.dvh;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Value;
import lombok.val;

import java.time.LocalDate;

@Value
public class DvhOrganisasjonsenhet {
    private String nummer;
    private String navn;
    private LocalDate gyldigFom;
    private LocalDate gyldigTom;

    DvhOrganisasjonsenhet(JsonNode node) {
        this.nummer = node.get("ORGNODE_KODE").asText();
        this.navn = node.get("ORGNODE_NAVN").asText();
        this.gyldigFom = LocalDate.parse(node.get("GYLDIG_FRA").asText());
        val gyldigTomText = node.get("GYLDIG_TIL");
        this.gyldigTom = gyldigTomText != null ? LocalDate.parse(gyldigTomText.asText()) : null;
    }
}
