package no.nav.nom.apps.api;

import no.nav.nom.apps.api.common.PrefixedUlidCreator;
import no.nav.nom.contract.nom.IdentType;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.TelefonType;
import no.nav.nom.contract.nom.eventsource.Jobbtelefon;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

@Component
public class TestUtil {
    private final RessursStateRepository ressursStateRepository;

    public TestUtil(RessursStateRepository ressursStateRepository) {
        this.ressursStateRepository = ressursStateRepository;
    }

    public RessursState createRessursInState(String suffix) {
        final var ressursState = createRessursState(suffix);
        ressursStateRepository.update(ressursState);

        return ressursState;
    }

    static RessursState createRessursState(String suffix) {
        return new RessursState(
            PrefixedUlidCreator.createRessursUlid(),
            "testPersonIdent" + suffix,
            "testNavIdent" + suffix,
            Sektor.NAV_STATLIG,
            new Visningsnavn(
                "fornavn" + suffix,
                Optional.of("mellomnavn" + suffix),
                "etternavn" + suffix
            ),
            new Jobbtelefon("12345678", TelefonType.NAV_TJENESTE_TELEFON),
            Optional.empty(),
            null,
            Optional.of(LocalDate.now()),
            null,
            Collections.emptyList(),
            IdentType.STANDARD,
            null
        );
    }
}
