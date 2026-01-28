package no.nav.nom.apps.api.dvh.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import tools.jackson.databind.JsonNode;

@Getter
@Builder
@ToString
public class DvhLeder {
    String navIdent;
    String orgEnhetNr;
    String orgNiv;
    String orgenhetNavn;

    public static DvhLeder fromOrds(JsonNode node) {
        return DvhLeder.builder()
            .navIdent(node.get("leder_navident").asString())
            .orgEnhetNr(node.get("orgnode_kode").asString())
            .orgNiv(node.get("orgnode_niva").asString())
            .orgenhetNavn(node.get("orgnode_navn").asString())
            .build();
    }
}
