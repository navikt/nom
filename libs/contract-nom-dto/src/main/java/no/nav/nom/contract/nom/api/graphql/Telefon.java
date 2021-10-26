package no.nav.nom.contract.nom.api.graphql;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Telefon {
    String nummer;
    TelefonType type;
    String beskrivelse;
}
