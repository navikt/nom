package no.nav.nom.apps.api.dvh.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import tools.jackson.databind.JsonNode;

import java.time.LocalDate;
import java.util.Objects;

@Getter
@Builder(toBuilder = true)
@ToString
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

    public static DvhOrganisering fromOrds(JsonNode jsonNode) {
        return DvhOrganisering.builder()
            .agressoIdOver(jsonNode.get("orgnode_kode_over").asString())
            .orgNivOver(jsonNode.get("orgnode_niva_over").asString())
            .navnOver(jsonNode.get("orgnode_navn_over").asString())
            .agressoIdUnder(jsonNode.get("orgnode_kode").asString())
            .orgNivUnder(jsonNode.get("orgnode_niva").asString())
            .navnUnder(jsonNode.get("orgnode_navn").asString())
            .gyldigFom(LocalDate.now().minusDays(1))
            .build();
    }
}
