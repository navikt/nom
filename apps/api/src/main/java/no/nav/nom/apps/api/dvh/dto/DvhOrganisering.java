package no.nav.nom.apps.api.dvh.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Value;

import java.time.LocalDate;

@Value
public class DvhOrganisering {

    private String overenhetNummer;
    private String underenhetNummer;
    private LocalDate gyldigFom;
    private LocalDate gyldigTom;

    public DvhOrganisering(JsonNode node) {
        overenhetNummer = node.get("ORGNODE_KODE_OVER").asText();
        underenhetNummer = node.get("ORGNODE_KODE").asText();
        gyldigFom = LocalDate.parse(node.get("GYLDIG_FRA_ORG").asText());
        gyldigTom = LocalDate.parse(node.get("GYLDIG_TIL_ORG").asText());
    }
}
