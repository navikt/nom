package no.nav.nom.apps.api.dvh.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

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
            .navIdent(node.get("leder_navident").asText())
            .orgEnhetNr(node.get("orgnode_kode").asText())
            .orgNiv(node.get("orgnode_niva").asText())
            .orgenhetNavn(node.get("orgnode_navn").asText())
            .build();
    }
}
