package no.nav.nom.apps.api.dvh;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Value;

import java.time.LocalDate;

@Value
public class DvhOrganisasjonsenhet {
    private String nummer;
    private String navn;
    private LocalDate gyldigFom;
    private LocalDate gyldigTil;

    DvhOrganisasjonsenhet(JsonNode node) {
        this.nummer = node.get("ORGNODE_KODE").asText();
        this.navn = node.get("ORGNODE_NAVN").asText();
        this.gyldigFom = LocalDate.parse(node.get("GYLDIG_FRA").asText());
        this.gyldigTil = LocalDate.parse(node.get("GYLDIG_TIL").asText());
    }
}
