package no.nav.nom.apps.api.dvh.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;

@Getter
public class DvhLeder {

    String navIdent;
    String orgEnhetNr;
    String orgNiv;
    String orgenhetNavn;

    public DvhLeder(JsonNode node) {
        this.navIdent = node.get("LEDER_NAVIDENT").asText();
        this.orgEnhetNr = node.get("ORGNODE_KODE").asText();
        this.orgNiv = node.get("ORGNODE_NIVA").asText();
    }

    public void setOrgenhetNavn(String orgenhetNavn){
        this.orgenhetNavn = orgenhetNavn;
    }
}
