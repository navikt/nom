package no.nav.nom.apps.api.telefon;

import lombok.Builder;
import no.nav.nom.contract.nom.TelefonType;

@Builder(toBuilder = true)
public record Telefon(
        String navident,
        String nummer,
        TelefonType type
) {
}
