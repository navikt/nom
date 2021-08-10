package no.nav.nom.apps.api.dvh.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Value;

@Value
public class DvhRessurs {

    String navIdent;
    String personIdent;
    Boolean remedyAktiv;

    public DvhRessurs(JsonNode node) {
        navIdent = node.get("NAV_IDENT").asText();
        personIdent = node.get("FNR").asText();
        remedyAktiv = node.get("REMEDY_AKTIV").asText().equals("JA");
    }
}
