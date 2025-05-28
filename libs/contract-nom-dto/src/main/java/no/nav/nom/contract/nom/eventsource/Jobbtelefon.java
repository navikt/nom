package no.nav.nom.contract.nom.eventsource;

import no.nav.nom.contract.nom.TelefonType;

public record Jobbtelefon(
        String nummer,
        TelefonType type
) {
}
