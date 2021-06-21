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
public class Bestilling {

    UUID requestId;
    UUID requestSetId;
    BestillingMetadata metadata;
    BestillingType type;
    ObjectNode data;

    public enum BestillingType {
        OPPRETT_RESSURS,
        OPPRETT_ENHET
    }

}
