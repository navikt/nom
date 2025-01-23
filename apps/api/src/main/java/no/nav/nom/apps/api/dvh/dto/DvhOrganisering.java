package no.nav.nom.apps.api.dvh.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

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
            .agressoIdOver(jsonNode.get("orgnode_kode_over").asText())
            .orgNivOver(jsonNode.get("orgnode_niva_over").asText())
            .navnOver(jsonNode.get("orgnode_navn_over").asText())
            .agressoIdUnder(jsonNode.get("orgnode_kode").asText())
            .orgNivUnder(jsonNode.get("orgnode_niva").asText())
            .navnUnder(jsonNode.get("orgnode_navn").asText())
            .gyldigFom(LocalDate.now().minusDays(1))
            .build();
    }
}
