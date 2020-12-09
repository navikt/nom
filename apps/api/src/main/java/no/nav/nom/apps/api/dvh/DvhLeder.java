package no.nav.nom.apps.api.dvh;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Value;

@Value
public class DvhLeder {

    private String ressursId;
    private String orgEnhetNr;

    DvhLeder(JsonNode node) {
        this.ressursId = node.get("LEDER_RESOURCE_ID").asText();
        this.orgEnhetNr = node.get("ORGNODE_KODE").asText();
    }
}
