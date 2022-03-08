package no.nav.nom.apps.api.orgenhet.implementations;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Value;
import no.nav.nom.apps.api.orgenhet.contract.Orgenhet;
import org.apache.tomcat.jni.Local;

import java.time.LocalDate;
import java.util.Optional;

@Value
public class DvhOrganisasjonsenhet implements Orgenhet {

    String agressoId;
    String orgNiv;
    String navn;
    String agressoOrgType;
    LocalDate gyldigFom;
    Optional<LocalDate> gyldigTom;

    public DvhOrganisasjonsenhet(JsonNode node) {
        this.agressoId = node.get("ORGNODE_KODE").asText();
        this.orgNiv = node.get("ORGNODE_NIVA").asText();
        this.navn = node.get("ORGNODE_NAVN").asText();
        this.agressoOrgType = node.get("agresso_orgnode_type").asText();
        gyldigFom = LocalDate.now().minusDays(1);
        gyldigTom = Optional.empty();

        //this.gyldigFom = LocalDate.parse(node.get("GYLDIG_FRA").asText());
        //val gyldigTomText = node.get("GYLDIG_TIL");
        //this.gyldigTom = gyldigTomText != null ? LocalDate.parse(gyldigTomText.asText()) : null;
    }
}
