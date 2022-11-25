package no.nav.nom.contract.nom.api.graphql;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import no.nav.nom.contract.nom.TelefonType;

@Value
@Builder
@AllArgsConstructor
public class Telefon {
    String nummer;
    TelefonType type;
    String beskrivelse;
}
