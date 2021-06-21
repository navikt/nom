package no.nav.nom.apps.api.graphql.request.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;

import java.util.UUID;

@Data
@Builder
@With
@AllArgsConstructor
@NoArgsConstructor
public class BestillingMetadata {

    UUID requestSetId;
    Aktivering aktivering;
    String beskrivelse;

    // GraphQL default on enum seems to not work
    public boolean isImmediate() {
        return getAktivering() == Aktivering.IMMEDIATE;
    }

    public Aktivering getAktivering() {
        return aktivering == null ? Aktivering.IMMEDIATE : aktivering;
    }

    public enum Aktivering {
        IMMEDIATE,
        FOR_APPROVAL
    }

    public BestillingMetadata copy() {
        return withAktivering(getAktivering());
    }
}
