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
@AllArgsConstructor
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
    public String uniqueId() { return uniqueAgressoIdOver() + "-" + uniqueAgressoIdUnder();  }

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

    public static boolean compare(DvhOrganisering tdvOrganisering, DvhOrganisering ordsOrganisering) {
        return Objects.equals(tdvOrganisering.getAgressoIdOver(), ordsOrganisering.getAgressoIdOver()) &&
            Objects.equals(tdvOrganisering.getOrgNivOver(), ordsOrganisering.getOrgNivOver()) &&
            Objects.equals(tdvOrganisering.getNavnOver(), ordsOrganisering.getNavnOver()) &&
            Objects.equals(tdvOrganisering.getAgressoIdUnder(), ordsOrganisering.getAgressoIdUnder()) &&
            Objects.equals(tdvOrganisering.getOrgNivUnder(), ordsOrganisering.getOrgNivUnder()) &&
            Objects.equals(tdvOrganisering.getNavnUnder(), ordsOrganisering.getNavnUnder());
    }

}
