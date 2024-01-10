package no.nav.nom.apps.api.dvh.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@AllArgsConstructor
@ToString
public class DvhLeder {

    String navIdent;
    String orgEnhetNr;
    String orgNiv;
    String orgenhetNavn;

    public DvhLeder(JsonNode node) {
        this.navIdent = node.get("LEDER_NAVIDENT").asText();
        this.orgEnhetNr = node.get("ORGNODE_KODE").asText();
        this.orgNiv = node.get("ORGNODE_NIVA").asText();
        this.orgenhetNavn = node.get("ORGNODE_NAVN").asText();
    }
}
