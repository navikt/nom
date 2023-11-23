package no.nav.nom.apps.api.orgtilknytning.api.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.common.SwaggerConfig;
import no.nav.nom.apps.api.graphql.common.SecurityUtil;
import no.nav.nom.apps.api.orgtilknytning.OrgtilknytningService;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.OrgtilknytningCommandService;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.FjernOrgtilknytning;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OppdaterPersonaliaCommand;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OppdaterPersonidentCommand;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.SlettRessursCommand;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.Origination;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;
import org.flywaydb.core.internal.util.HtmlUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@Slf4j
@Tag(name = "Admin for å lage manuelle Evtsrc meldinger")
@RestController
@RequestMapping("/api/admin/manuelleEvtsrcMeldinger")
public class ManuelleEvtsrcMeldinger {

    private final OrgtilknytningCommandService orgtilknytningCommandService;

    private final OrgtilknytningService orgtilknytningService;

    public ManuelleEvtsrcMeldinger(OrgtilknytningCommandService orgtilknytningCommandService, OrgtilknytningService orgtilknytningService) {
        this.orgtilknytningCommandService = orgtilknytningCommandService;
        this.orgtilknytningService = orgtilknytningService;
    }

    @Operation(
        description = "Lag en fjernOrgtilknytning Eventsrc melding",
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
    @PostMapping(value = "/fjernOrgtilknytning")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<String> fjernOrgtilknytning(
        @Parameter(description = "FID for orgtilknytningen som skal slettes") @RequestParam String fid,
        @Parameter(description = "Skriv CONFIRM for å bekrefte!") @RequestParam String confirm,
        @NonNull JwtAuthenticationToken token
    ) {
        log.debug("fjernOrgtilknytning : " + token.getName());

        if (!validFid(fid)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("FID er ikke gyldig");
        }

        var orgtilknytning = orgtilknytningService.findByFid(fid);
        if (orgtilknytning.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Finner ingen orgtilknytning med FID: " +  HtmlUtils.htmlEncode(fid));
        }

        if (!"CONFIRM".equals(confirm)) {
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body("Orgtilknytning med FID %s er funnet.. Men vil ikke slettes da handling ikke er bekreftet".formatted(HtmlUtils.htmlEncode(fid)));
        }

        orgtilknytningCommandService.fjernOrgtilknytning(
            fid,
            new FjernOrgtilknytning(
                orgtilknytning.get().navident(),
                orgtilknytning.get().orgEnhetId(),
                orgtilknytning.get().gyldigFom(),
                orgtilknytning.get().gyldigTom(),
                new InputMetadata(
                    "/api/admin/manuelleEvtsrcMeldinger",
                    "fjernOrgtilknytning",
                    fid,
                    new Origination(
                        SecurityUtil.getAuthenticatedNavIdent(),
                        "Admin swagger/rest"
                    )
                ),
                null
            )
        );
        return ResponseEntity.ok().body("Orgtilknytning med FID %s er funnet. Fjern event er nå sendt".formatted(HtmlUtils.htmlEncode(fid)));
    }

    private boolean validFid(String fid) {
        return fid.matches("[a-zA-Z0-9\\-]+");
    }
}
