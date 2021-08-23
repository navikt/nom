package no.nav.nom.apps.api.dvh.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Value;

import java.time.LocalDate;
import java.util.Objects;

@Value
public class DvhOrganisering {

    String agressoIdOver;
    String orgNivOver;
    String agressoIdUnder;
    String orgNivUnder;
    LocalDate gyldigFom;
    LocalDate gyldigTom;

    public DvhOrganisering(JsonNode node) {
        agressoIdOver = node.get("ORGNODE_KODE_OVER").asText();
        orgNivOver = node.get("ORGNODE_NIVA_OVER").asText();
        agressoIdUnder = node.get("ORGNODE_KODE").asText();
        orgNivUnder = node.get("ORGNODE_NIVA").asText();

        gyldigFom = LocalDate.now().minusDays(1);
        gyldigTom = null;
        //gyldigFom = LocalDate.parse(node.get("GYLDIG_FRA_ORG").asText());
        //gyldigTom = LocalDate.parse(node.get("GYLDIG_TIL_ORG").asText());
    }

    public boolean containsEnhet(String agressoId, String orgNiv){
        return (Objects.equals(agressoId, agressoIdOver) && Objects.equals(orgNiv, orgNivOver)) ||
                (Objects.equals(agressoId, agressoIdUnder) && Objects.equals(orgNiv, orgNivUnder));
    }

}
