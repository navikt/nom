package no.nav.nom.apps.api.dvh.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.nom.apps.api.telefon.Telefon;
import no.nav.nom.contract.nom.TelefonType;
import no.nav.nom.contract.nom.eventsource.Jobbtelefon;

@Data
@Builder
@Slf4j
public class DvhTelefon {
    String navIdent;
    String nummer;
    DvhTelefonType type;
    String beskrivelse;

    public static DvhTelefon fromOrds(JsonNode jsonNode) {
        val userref1 = jsonNode.get("userrefeerence1").asText();
        DvhTelefonType type;
        if (userref1.startsWith("PK-")) {
            type = DvhTelefonType.KONTORTELEFON;
        } else if (userref1.startsWith("PT-")) {
            type = DvhTelefonType.TJENESTETELEFON;
        } else {
            // TODO, håndtere bedre?
            log.error("DvhTelefon.type satt til null! " + jsonNode.get("userreference1").asText());
            return null;
        }

        return DvhTelefon.builder()
            .navIdent(jsonNode.get("navident").asText())
            .nummer(jsonNode.get("msisdn").asText())
            .type(type)
            .beskrivelse(jsonNode.get("typename").asText())
            .build();
    }

    public Jobbtelefon toJobbtelefon() {
        return new Jobbtelefon(nummer, switch (type) {
            case TJENESTETELEFON -> TelefonType.NAV_TJENESTE_TELEFON;
            case KONTORTELEFON -> TelefonType.NAV_KONTOR_TELEFON;
        });
    }

    public Telefon toTelefon() {
        return new Telefon(navIdent, nummer, switch (type) {
            case TJENESTETELEFON -> TelefonType.NAV_TJENESTE_TELEFON;
            case KONTORTELEFON -> TelefonType.NAV_KONTOR_TELEFON;
        });
    }

    public enum DvhTelefonType {
        TJENESTETELEFON,
        KONTORTELEFON
    }
}
