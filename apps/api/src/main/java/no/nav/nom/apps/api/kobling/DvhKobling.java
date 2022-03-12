package no.nav.nom.apps.api.kobling;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Value;
import lombok.val;
import no.nav.nom.apps.api.kobling.contract.Kobling;

import java.time.LocalDate;
import java.util.Optional;

@Getter
public class DvhKobling implements Kobling {

    String navIdent;
    String agressoId;
    String orgNiv = "ORGENHET";
    String orgenhetNavn = null;
    LocalDate gyldigFom;
    LocalDate gyldigTom;

    public DvhKobling(JsonNode node) {
        navIdent = node.get("NAV_ID").asText();
        agressoId = node.get("ORGENHET").asText();
        gyldigFom = LocalDate.parse(node.get("FRA_DATO").asText());
        val gyldigTomText = node.get("TIL_DATO").asText();
        gyldigTom = gyldigTomText != null ? LocalDate.parse(gyldigTomText) : null;
    }

    @Override
    public Optional<LocalDate> getGyldigTom(){
        return Optional.ofNullable(gyldigTom);
    }

    public void setOrgenhetNavn(String orgenhetNavn){
        this.orgenhetNavn = orgenhetNavn;
    }
}
