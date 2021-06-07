package no.nav.nom.apps.api.dvh.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Value;

import java.time.LocalDate;

@Value
public class DvhOrganisering {

    String overenhetNummer;
    String underenhetNummer;
    LocalDate gyldigFom;
    LocalDate gyldigTom;

    public DvhOrganisering(JsonNode node) {
        overenhetNummer = node.get("ORGNODE_KODE_OVER").asText();
        underenhetNummer = node.get("ORGNODE_KODE").asText();

        gyldigFom = LocalDate.now().minusDays(1);
        gyldigTom = null;
        //gyldigFom = LocalDate.parse(node.get("GYLDIG_FRA_ORG").asText());
        //gyldigTom = LocalDate.parse(node.get("GYLDIG_TIL_ORG").asText());
    }
}
