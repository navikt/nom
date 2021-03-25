package no.nav.nom.apps.api.dvh.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Value;

@Value
public class DvhLeder {

    private String navIdent;
    private String orgEnhetNr;

    public DvhLeder(JsonNode node) {
        this.navIdent = node.get("leder_nav_id").asText();
        this.orgEnhetNr = node.get("orgnode_kode").asText();
    }
}
