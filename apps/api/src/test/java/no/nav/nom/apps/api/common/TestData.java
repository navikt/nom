package no.nav.nom.apps.api.common;

import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.contract.nom.state.RessursState;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public class TestData {

    public static OrgtilknytningState orgtilknytningState() {
        return new OrgtilknytningState(
            UUID.randomUUID().toString(),
            "X123456",
            "orgEnhet123",
            false,
            LocalDate.now().minusMonths(1),
            Optional.empty()
        );
    }

    public static RessursState ressursState() {
        return new RessursState(
            UUID.randomUUID().toString(),
            "11111111111",
            "X123456",
            Sektor.NAV_STATLIG,
            new Visningsnavn("Nav", Optional.empty(), "Navesen"),
            Optional.empty(),
            Optional.empty()
        );
    }
}
