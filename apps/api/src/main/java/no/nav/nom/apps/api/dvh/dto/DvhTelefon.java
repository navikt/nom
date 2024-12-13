package no.nav.nom.apps.api.dvh.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.util.Objects;

@Data
@Builder
@AllArgsConstructor
@Slf4j
public class DvhTelefon {
    String navIdent;
    String nummer;
    DvhTelefonType type;
    String beskrivelse;

    public DvhTelefon() {}

    public DvhTelefon(JsonNode node) {
        this.navIdent = node.get("navident").asText();
        this.nummer = node.get("msisdn").asText();

        val userref1 = node.get("userreference1").asText();

        if (userref1.startsWith("PK-")) {
            this.type = DvhTelefonType.KONTORTELEFON;
        } else if (userref1.startsWith("PT-")) {
            this.type = DvhTelefonType.TJENESTETELEFON;
        } else {
            // TODO, håndtere bedre?
            log.error("DvhTelefon.type satt til null! " + node.get("userreference1").asText());
            this.type = null;
        }

        this.beskrivelse = node.get("typename").asText();
    }

    public static boolean compare(DvhTelefon dvhTelefonTdv, DvhTelefon dvhTelefonOrds) {
        return Objects.equals(dvhTelefonTdv.getNavIdent(), dvhTelefonOrds.getNavIdent()) &&
            Objects.equals(dvhTelefonTdv.getNummer(), dvhTelefonOrds.getNummer()) &&
            Objects.equals(dvhTelefonTdv.getType(), dvhTelefonOrds.getType()) &&
            Objects.equals(dvhTelefonTdv.getBeskrivelse(), dvhTelefonOrds.getBeskrivelse());
    }

    public Object geUniqueKey() {
        return navIdent + "-" + nummer;
    }

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

    public enum DvhTelefonType {
        TJENESTETELEFON,
        KONTORTELEFON
    }
}
