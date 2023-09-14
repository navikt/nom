package no.nav.nom.apps.api.rest.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.common.SwaggerConfig;
import no.nav.nom.apps.api.graphql.common.SecurityUtil;
import no.nav.nom.apps.api.ressurs.IRessursService;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.RessursCommadService;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.SlettRessursCommand;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.Origination;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Tag(name = "Admin for å lage manuelle Evtsrc meldinger")
@RestController
@RequestMapping("/api/admin/manuelleEvtsrcMeldinger")
public class ManuelleEvtsrcMeldinger {

    private final RessursCommadService ressursCommadService;
    private final IRessursService ressursService;

    public ManuelleEvtsrcMeldinger(RessursCommadService ressursCommadService, IRessursService ressursService) {
        this.ressursCommadService = ressursCommadService;
        this.ressursService = ressursService;
    }


    @Operation(
        description = "Lag en slett ressurs Eventsrc melding",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Success"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Not found")
        }
    )
    @PostMapping(value = "/lagSlettRessursMelding")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<String> lagSlettRessursMelding(
        @Parameter(description = "FID for ressursen som skal slettes") @RequestParam String fid,
        @Parameter(description = "Skriv CONFIRM for å bekrefte!") @RequestParam String confirm,
        @NonNull JwtAuthenticationToken token
    ) {
        log.debug("lagSlettRessursMelding : " + token.getName());
        var ressursState = ressursService.findByFid(fid);
        if (ressursState.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Finner ingen ressurs med FID: " + fid);
        }

        if (!"CONFIRM".equals(confirm)) {
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body("Ressurs med FID %s er funnet for navident %s og personident %s. Men vil ikke slettes da handling ikke er bekreftet".formatted(
                    fid,
                    ressursState.get().navident(),
                    ressursState.get().personident()
                ));
        }

        ressursCommadService.slettRessursCommand(
            new SlettRessursCommand(fid),
            new InputMetadata(
                "/api/admin/manuelleEvtsrcMeldinger",
                "lagSlettRessursMelding",
                fid,
                new Origination(
                    SecurityUtil.getUserFromToken(token).orElseThrow(),
                    "Admin swagger/rest"
                )
            )
        );

        return ResponseEntity.ok()
            .body("Ressurs med FID %s er funnet for navident %s og personident %s. Men vil ikke slettes da handling ikke er bekreftet".formatted(
                fid,
                ressursState.get().navident(),
                ressursState.get().personident()
            ));
    }
}
