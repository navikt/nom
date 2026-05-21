package no.nav.nom.apps.api.orgenhet.api.admin;

import no.nav.nom.apps.api.graphql.common.SecurityUtil;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.OrgEnhetCommandService;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.commands.EndreTilgangsenhet;
import no.nav.nom.apps.api.tilgangsenhet.InvalidTilgangsenhetException;
import no.nav.nom.apps.api.tilgangsenhet.TilgangsenhetRepository;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.Origination;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static no.nav.nom.apps.api.graphql.common.SecurityUtil.getAuthenticatedNavIdent;

@RestController
@RequestMapping("/api/admin/tilgangsenhetednringer")
@PreAuthorize("hasAuthority('ADMIN')")
public class TilgangsenhetManuelleEndringer {

    private final OrgEnhetCommandService orgEnhetCommandService;
    private final OrgenhetService orgenhetService;
    private final TilgangsenhetRepository tilgangsenhetRepository;

    public TilgangsenhetManuelleEndringer(OrgEnhetCommandService orgEnhetCommandService, OrgenhetService orgenhetService,
                                          TilgangsenhetRepository tilgangsenhetRepository
    ) {
        this.orgEnhetCommandService = orgEnhetCommandService;
        this.orgenhetService = orgenhetService;
        this.tilgangsenhetRepository = tilgangsenhetRepository;
    }

    @PostMapping
    public void setTilgangsenhet(@RequestBody TilgangsenhetEndring tilgangsenhetEndring) {
        if (tilgangsenhetEndring.tilgangsenhetId == null || !tilgangsenhetRepository.isValid(tilgangsenhetEndring.tilgangsenhetId)) {
            throw new InvalidTilgangsenhetException(String.format("Invalid tilgangsenhet: ‰s", "tilgangsenhetEndring.tilgangsenhetId"));
        }

        var inputMetadata = new InputMetadata(
            "tilgangsenhet-endring-admin",
            "endreTilgangsenhet",
            tilgangsenhetEndring,
            new Origination(
                getAuthenticatedNavIdent(),
                SecurityUtil.getAuthenticatedAzpName()
            )
        );

        var orgenhetState = orgenhetService.getById(tilgangsenhetEndring.id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("Orgenhet ID %s not found", tilgangsenhetEndring.id)));

        orgEnhetCommandService.endreTilgangsenhet(
            orgenhetState.getFid(), new EndreTilgangsenhet(
                tilgangsenhetEndring.tilgangsenhetId(),
                inputMetadata
            )
        );
    }

    public record TilgangsenhetEndring(
        String id,
        String tilgangsenhetId
    ) {}

}
