package no.nav.nom.apps.api.dvh.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;

import java.time.LocalDate;
import java.util.Objects;

@Getter
public class DvhOrganisering {

    String agressoIdOver;
    String orgNivOver;
    String navnOver;
    String agressoIdUnder;
    String orgNivUnder;
    String navnUnder;
    LocalDate gyldigFom;
    LocalDate gyldigTom;
    JsonNode rawData;

    public DvhOrganisering(
            String agressoIdOver,
            String orgNivOver,
            String navnOver,
            String agressoIdUnder,
            String orgNivUnder,
            String navnUnder,
            LocalDate gyldigFom,
            LocalDate gyldigTom
    ) {
        this.agressoIdOver = agressoIdOver;
        this.orgNivOver = orgNivOver;
        this.navnOver = navnOver;
        this.agressoIdUnder = agressoIdUnder;
        this.orgNivUnder = orgNivUnder;
        this.navnUnder = navnUnder;
        this.gyldigFom = gyldigFom;
        this.gyldigTom = gyldigTom;
    }

    public DvhOrganisering(JsonNode node) {
        agressoIdOver = node.get("ORGNODE_KODE_OVER").asText();
        orgNivOver = node.get("ORGNODE_NIVA_OVER").asText();
        navnOver = node.get("ORGNODE_NAVN_OVER").asText();
        agressoIdUnder = node.get("ORGNODE_KODE").asText();
        orgNivUnder = node.get("ORGNODE_NIVA").asText();
        navnUnder = node.get("ORGNODE_NAVN").asText();

        gyldigFom = LocalDate.now().minusDays(1);
        gyldigTom = null;
        //gyldigFom = LocalDate.parse(node.get("GYLDIG_FRA_ORG").asText());
        //gyldigTom = LocalDate.parse(node.get("GYLDIG_TIL_ORG").asText());
        this.rawData = node;
    }

    public boolean containsEnhet(String agressoId, String orgNiv, String navn) {
        return (Objects.equals(agressoId, agressoIdOver) && Objects.equals(orgNiv, orgNivOver) && Objects.equals(navn, navnOver))
                || (Objects.equals(agressoId, agressoIdUnder) && Objects.equals(orgNiv, orgNivUnder) && Objects.equals(navn, navnUnder));
    }

    public String uniqueAgressoIdUnder() {
        return agressoIdUnder + "-" + navnUnder + "-" + orgNivUnder;
    }
    public String uniqueAgressoIdOver() {
        return agressoIdOver + "-" + navnOver + "-" + orgNivOver;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DvhOrganisering that = (DvhOrganisering) o;
        return Objects.equals(agressoIdOver, that.agressoIdOver) && Objects.equals(orgNivOver, that.orgNivOver) && Objects.equals(
            navnOver,
            that.navnOver
        ) && Objects.equals(agressoIdUnder, that.agressoIdUnder) && Objects.equals(orgNivUnder, that.orgNivUnder) && Objects.equals(navnUnder, that.navnUnder);
    }

    @Override
    public int hashCode() {
        return Objects.hash(agressoIdOver, orgNivOver, navnOver, agressoIdUnder, orgNivUnder, navnUnder);
    }
}
