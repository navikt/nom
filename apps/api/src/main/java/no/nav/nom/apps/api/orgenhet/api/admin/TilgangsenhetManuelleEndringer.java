package no.nav.nom.apps.api.orgenhet.api.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.common.SwaggerConfig;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.OrgEnhetCommandService;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.commands.EndreTilgangsenhet;
import no.nav.nom.apps.api.remedy.RemedyEnhetMapperService;
import no.nav.nom.apps.api.rest.common.StandardResponse;
import no.nav.nom.apps.api.tilgangsenhet.TilgangsenhetRepository;
import no.nav.nom.apps.api.tilgangsenhet.exception.InvalidTilgangsenhetException;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.Origination;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;

import static java.util.Objects.isNull;
import static no.nav.nom.apps.api.graphql.common.SecurityUtil.getAuthenticatedNavIdent;

@Slf4j
@RestController
@RequestMapping("/api/admin/tilgangsenhetednringer")
@PreAuthorize("hasAuthority('ADMIN')")
public class TilgangsenhetManuelleEndringer {

    private final OrgEnhetCommandService orgEnhetCommandService;
    private final OrgenhetService orgenhetService;
    private final TilgangsenhetRepository tilgangsenhetRepository;
    private final RemedyEnhetMapperService remedyEnhetMapperService;

    public TilgangsenhetManuelleEndringer(
        OrgEnhetCommandService orgEnhetCommandService, OrgenhetService orgenhetService,
        TilgangsenhetRepository tilgangsenhetRepository, RemedyEnhetMapperService remedyEnhetMapperService
    ) {
        this.orgEnhetCommandService = orgEnhetCommandService;
        this.orgenhetService = orgenhetService;
        this.tilgangsenhetRepository = tilgangsenhetRepository;
        this.remedyEnhetMapperService = remedyEnhetMapperService;
    }

    @Operation(
        description = "Endre ",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @PatchMapping
    public void setTilgangsenhet(@RequestBody TilgangsenhetEndring tilgangsenhetEndring) {
        if (tilgangsenhetEndring.tilgangsenhetId == null || !tilgangsenhetRepository.isValid(tilgangsenhetEndring.tilgangsenhetId())) {
            throw new InvalidTilgangsenhetException(String.format("Invalid tilgangsenhet: %s", tilgangsenhetEndring.tilgangsenhetId()));
        }

        var inputMetadata = new InputMetadata(
            "TilgangsenhetManuelleEndringer",
            "setTilgangsenhet",
            tilgangsenhetEndring,
            new Origination(
                getAuthenticatedNavIdent(),
                "ADMIN-API"
            )
        );

        var orgenhetState = orgenhetService.getById(tilgangsenhetEndring.orgEnhetId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("Orgenhet ID %s not found", tilgangsenhetEndring.orgEnhetId())));

        orgEnhetCommandService.endreTilgangsenhet(
            orgenhetState.getFid(), new EndreTilgangsenhet(
                tilgangsenhetEndring.tilgangsenhetId(),
                inputMetadata
            )
        );
    }

    @Operation(
        description = "Fjern",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @PatchMapping("/remove/{orgEnhetId}")
    public void removeTilgangsenhet(@PathVariable String orgEnhetId) {

        var inputMetadata = new InputMetadata(
            "TilgangsenhetManuelleEndringer",
            "removeTilgangsenhet",
            orgEnhetId,
            new Origination(
                getAuthenticatedNavIdent(),
                "ADMIN-API"
            )
        );

        var orgenhetState = orgenhetService.getById(orgEnhetId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, String.format("Orgenhet ID %s not found", orgEnhetId)));

        orgEnhetCommandService.endreTilgangsenhet(
            orgenhetState.getFid(), new EndreTilgangsenhet(
                null,
                inputMetadata
            )
        );
    }

    @Operation(
        description = "Skript for å mappe remedyEnhetsId til tilgangsenhetId i OrgEnhetState",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @PostMapping("/populate")
    public String populerTilgangsenheter(@RequestParam boolean dryRun) {
        var tilgangsenhetEndringer = orgenhetService.getAktiveEnheterIOrganisasjonen().stream()
            .filter(orgEnhet -> isNull(orgEnhet.getTilgangsenhetId()) || orgEnhet.getTilgangsenhetId().isBlank())
            .map(orgEnhet -> remedyEnhetMapperService.findByNomId(orgEnhet.getId()).orElse(null))
            .filter(Objects::nonNull)
            .toList();


        if (dryRun) {
            var dryRunOutcome = new StringBuilder();
            tilgangsenhetEndringer.forEach(endring -> dryRunOutcome.append(String.format("%s -> %s\n", endring.nomId(), endring.remedyEnhetId())));
            return dryRunOutcome.toString();
        } else {
            tilgangsenhetEndringer.forEach(
                endring -> {
                    var inputMetadata = new InputMetadata(
                        "tilgangsenhet-endring-admin",
                        "endreTilgangsenhet",
                        endring,
                        new Origination(getAuthenticatedNavIdent(), "ADMIN-API")
                    );

                    orgEnhetCommandService.endreTilgangsenhet(
                        orgenhetService.getById(endring.nomId()).get().getFid(), new EndreTilgangsenhet(
                            endring.remedyEnhetId(),
                            inputMetadata
                        )
                    );
                }
            );

            return "Populated successfully";
        }
    }

    public record TilgangsenhetEndring(
        String orgEnhetId,
        String tilgangsenhetId
    ) {}

    @ExceptionHandler({InvalidTilgangsenhetException.class, ResponseStatusException.class})
    private ResponseEntity<StandardResponse> handleServerError(Exception exception) {
        log.error(exception.getMessage(), exception);

        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(StandardResponse.builder().message(exception.getMessage()).build());
    }
}
