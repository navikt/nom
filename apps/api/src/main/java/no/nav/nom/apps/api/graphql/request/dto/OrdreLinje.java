package no.nav.nom.apps.api.graphql.request.dto;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrdreLinje {

    UUID ordreLinjeId;
    UUID ordreId;
    OrdreMetadata metadata;
    OrdreLinjeType type;
    ObjectNode data;

    public enum OrdreLinjeType {
        //OPPRETT,
        //ENDRE,
        //ANNULER
        OPPRETT_RESSURS,
        OPPRETT_ENHET
    }

}
