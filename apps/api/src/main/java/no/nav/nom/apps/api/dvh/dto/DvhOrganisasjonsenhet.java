package no.nav.nom.apps.api.dvh.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Value;

import java.time.LocalDate;

@Value
public class DvhOrganisasjonsenhet {

    String agressoId;
    String orgNiv;
    String navn;
    String agressoOrgType;
    LocalDate gyldigFom;
    LocalDate gyldigTom;

    public DvhOrganisasjonsenhet(JsonNode node) {
        this.agressoId = node.get("ORGNODE_KODE").asText();
        this.orgNiv = node.get("ORGNODE_NIVA").asText();
        this.navn = node.get("ORGNODE_NAVN").asText();
        this.agressoOrgType = node.get("agresso_orgnode_type").asText();
        gyldigFom = LocalDate.now().minusDays(1);
        gyldigTom = null;

        //this.gyldigFom = LocalDate.parse(node.get("GYLDIG_FRA").asText());
        //val gyldigTomText = node.get("GYLDIG_TIL");
        //this.gyldigTom = gyldigTomText != null ? LocalDate.parse(gyldigTomText.asText()) : null;
    }
}
