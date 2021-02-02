package no.nav.nom.apps.api.dvh.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Value;

@Value
public class DvhLeder {

    private String ressursId;
    private String orgEnhetNr;

    public DvhLeder(JsonNode node) {
        this.ressursId = node.get("leder_resource_id").asText();
        this.orgEnhetNr = node.get("orgnode_kode").asText();
    }
}
