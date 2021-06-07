package no.nav.nom.apps.api.dvh.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Value;

@Value
public class DvhLeder {

    String navIdent;
    String orgEnhetNr;

    public DvhLeder(JsonNode node) {
        this.navIdent = node.get("LEDER_NAVIDENT").asText();
        this.orgEnhetNr = node.get("ORGNODE_KODE").asText();
    }
}
