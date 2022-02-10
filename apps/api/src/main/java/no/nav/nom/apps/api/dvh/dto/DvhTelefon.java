package no.nav.nom.apps.api.dvh.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.nom.contract.nom.api.graphql.TelefonType;

@Data
@Slf4j
public class DvhTelefon {
    String navIdent;
    String nummer;
    DvhTelefonType type;
    String beskrivelse;

    public enum DvhTelefonType {
        TJENESTETELEFON,
        KONTORTELEFON
    }

    public DvhTelefon(JsonNode node){
        this.navIdent = node.get("navident").asText();
        this.nummer = node.get("msisdn").asText();

        val userref1 = node.get("userreference1").asText();

        if(userref1.startsWith("PK-")){
            this.type = DvhTelefonType.KONTORTELEFON;
        }else if(userref1.startsWith("PT-")){
            this.type = DvhTelefonType.TJENESTETELEFON;
        }else{
            // TODO, håndtere bedre?
            log.error("DvhTelefon.type satt til null! " + node.get("userreference1").asText() );
            this.type = null;
        }

        this.beskrivelse = node.get("typename").asText();


    }
}
