package no.nav.nom.apps.api.dvh.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import no.nav.nom.apps.api.ressurs.implementations.dvh.DvhRessurs;

import java.util.Objects;

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

    public static DvhLeder fromOrds(JsonNode node) {
        return DvhLeder.builder()
            .navIdent(node.get("leder_navident").asText())
            .orgEnhetNr(node.get("orgnode_kode").asText())
            .orgNiv(node.get("orgnode_niva").asText())
            .orgenhetNavn(node.get("orgnode_navn").asText())
            .build();
    }

    public static boolean compare(DvhLeder tdvLeder, DvhLeder ordsLeder) {
        return Objects.equals(tdvLeder.getNavIdent(), ordsLeder.getNavIdent()) &&
            Objects.equals(tdvLeder.getOrgEnhetNr(), ordsLeder.getOrgEnhetNr()) &&
            Objects.equals(tdvLeder.getOrgNiv(), ordsLeder.getOrgNiv()) &&
            Objects.equals(tdvLeder.getOrgenhetNavn(), ordsLeder.getOrgenhetNavn());
    }

    public String getUniqueKey() {
        return navIdent + "-" + orgEnhetNr + "-" +  orgenhetNavn + "-" +  orgNiv;
    }
}
