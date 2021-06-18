package no.nav.nom.apps.api.graphql.request.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BestillingRequestMetadata {

    UUID requestSetId;
    Aktivering aktivering;
    String beskrivelse;

    // GraphQL default on enum seems to not work
    public boolean isImmediate() {
        return aktivering != Aktivering.FOR_APPROVAL;
    }

    public enum Aktivering {
        IMMEDIATE,
        FOR_APPROVAL
    }
}
