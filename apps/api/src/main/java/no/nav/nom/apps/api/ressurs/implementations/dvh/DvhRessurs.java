package no.nav.nom.apps.api.ressurs.implementations.dvh;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Value;
import no.nav.nom.apps.api.ressurs.contract.IRessurs;

import java.util.UUID;

@Value
public class DvhRessurs implements IRessurs {

    String navIdent;
    String personIdent;
    String remedyAnsattType;
    Boolean remedyAktiv;
    JsonNode rawData;

    public DvhRessurs(JsonNode node) {
        navIdent = node.get("NAV_IDENT").asText();
        personIdent = node.get("FNR").asText();
        remedyAktiv = node.hasNonNull("REMEDY_AKTIV") && node.get("REMEDY_AKTIV").asText().equals("JA");
        remedyAnsattType = node.get("ANSATT_TYPE").asText();
        this.rawData = node;
    }

    @Override
    public UUID getId() {
        return null;
    }
}
