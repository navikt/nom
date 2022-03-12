package no.nav.nom.apps.api.dvh.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Value;

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

    public DvhOrganisering(String agressoIdOver, String orgNivOver, String navnOver,
                           String agressoIdUnder, String orgNivUnder, String navnUnder,
                           LocalDate gyldigFom, LocalDate gyldigTom){
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
        agressoIdUnder = node.get("ORGNODE_KODE").asText();
        orgNivUnder = node.get("ORGNODE_NIVA").asText();
        navnUnder = node.get("ORGNODE_NAVN").asText();

        gyldigFom = LocalDate.now().minusDays(1);
        gyldigTom = null;
        //gyldigFom = LocalDate.parse(node.get("GYLDIG_FRA_ORG").asText());
        //gyldigTom = LocalDate.parse(node.get("GYLDIG_TIL_ORG").asText());
    }

    public boolean containsEnhet(String agressoId, String orgNiv, String navn){
        return (Objects.equals(agressoId, agressoIdOver) && Objects.equals(orgNiv, orgNivOver) && Objects.equals(navn, navnOver)) ||
                (Objects.equals(agressoId, agressoIdUnder) && Objects.equals(orgNiv, orgNivUnder) && Objects.equals(navn, navnUnder));
    }

    public void setNavnOver(String navnOver){
        this.navnOver = navnOver;
    }

}
