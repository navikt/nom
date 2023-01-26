package no.nav.nom.apps.api.orgTilknytning;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.val;
import no.nav.nom.apps.api.orgTilknytning.contract.Orgtilknytning;

import java.time.LocalDate;
import java.util.Optional;

@Getter
public class DvhOrgtilknytning implements Orgtilknytning {

    String navIdent;
    String agressoId;
    String orgNiv = "ORGENHET";
    String orgenhetNavn = null;
    LocalDate gyldigFom;
    Optional<LocalDate> gyldigTom;

    public DvhOrgtilknytning(JsonNode node) {
        navIdent = node.get("NAV_ID").asText();
        agressoId = node.get("ORGENHET").asText();
        gyldigFom = LocalDate.parse(node.get("FRA_DATO").asText());
        val gyldigTomText = node.get("TIL_DATO").asText();
        gyldigTom = Optional.ofNullable(
            gyldigTomText != null ?
                (LocalDate.parse(gyldigTomText).equals(LocalDate.parse("2099-12-31")) ? null : LocalDate.parse(gyldigTomText))
                : null);

    }

    public void setOrgenhetNavn(String orgenhetNavn) {
        this.orgenhetNavn = orgenhetNavn;
    }
}
