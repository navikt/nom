package no.nav.nom.apps.api.telefon;

import no.nav.nom.contract.nom.TelefonType;

public record Telefon(
        String navident,
        String nummer,
        TelefonType type
) {
}
