package no.nav.nom.apps.api.ressurs.api.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.common.SwaggerConfig;
import no.nav.nom.apps.api.graphql.common.SecurityUtil;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.RessursCommadService;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OppdaterPersonaliaCommand;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OppdaterPersonidentCommand;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.SlettRessursCommand;
import no.nav.nom.apps.api.ressurs.eventsourcing.state.RessursStateRepository;
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
@PreAuthorize("hasAuthority('ADMIN')")
public class ManuelleRessursEvtsrcMeldinger {

    private final RessursCommadService ressursCommadService;
    private final RessursStateRepository ressursStateRepository;

    public ManuelleRessursEvtsrcMeldinger(RessursCommadService ressursCommadService, RessursStateRepository ressursStateRepository) {
        this.ressursCommadService = ressursCommadService;
        this.ressursStateRepository = ressursStateRepository;
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
    public ResponseEntity<String> lagSlettRessursMelding(
        @Parameter(description = "FID for ressursen som skal slettes") @RequestParam String fid,
        @Parameter(description = "Skriv CONFIRM for å bekrefte!") @RequestParam String confirm,
        @NonNull JwtAuthenticationToken token
    ) {
        log.debug("lagSlettRessursMelding : " + token.getName());

        if (!validFid(fid)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("FID er ikke gyldig");
        }

        var ressursState = ressursStateRepository.findByFid(fid);
        if (ressursState.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Finner ingen ressurs med FID: " +  HtmlUtils.htmlEncode(fid));
        }

        if (!"CONFIRM".equals(confirm)) {
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body("Ressurs med FID %s er funnet for navident %s og personident %s. Men vil ikke slettes da handling ikke er bekreftet".formatted(
                    HtmlUtils.htmlEncode(fid),
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
                    SecurityUtil.getAuthenticatedNavIdent(),
                    "Admin swagger/rest"
                )
            )
        );

        return ResponseEntity.ok()
            .body("Ressurs med FID %s er funnet for navident %s og personident %s. Slett event er nå sendt".formatted(
                HtmlUtils.htmlEncode(fid),
                ressursState.get().navident(),
                ressursState.get().personident()
            ));
    }

    @Operation(
        description = "Lag en Oppdater personident for ressurs Eventsrc melding",
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
    @PostMapping(value = "/oppdaterPersonidentForRessurs")
    public ResponseEntity<String> oppdaterPersonidentForRessurs(
        @Parameter(description = "FID for ressursen som skal få ny personident") @RequestParam String fid,
        @Parameter(description = "Ny personident (fnr)") @RequestParam String nyPersonident,
        @Parameter(description = "Skriv CONFIRM for å bekrefte!") @RequestParam String confirm,
        @NonNull JwtAuthenticationToken token
    ) {
        log.debug("oppdaterPersonidentForRessurs : " + token.getName());

        if (!validFid(fid)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("FID er ikke gyldig");
        }

        if (!validPersonident(nyPersonident)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Personident er ikke gyldig");
        }

        var ressursState = ressursStateRepository.findByFid(fid);
        if (ressursState.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Finner ingen ressurs med FID: " +  HtmlUtils.htmlEncode(fid));
        }

        if (!"CONFIRM".equals(confirm)) {
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body("Ressurs med FID %s er funnet for navident %s og personident %s. Men vil ikke endres da handling ikke er bekreftet".formatted(
                    HtmlUtils.htmlEncode(fid),
                    ressursState.get().navident(),
                    ressursState.get().personident()
                ));
        }

        ressursCommadService.oppdaterPersonidentCommand(
            new OppdaterPersonidentCommand(fid, nyPersonident),
            new InputMetadata(
                "/api/admin/manuelleEvtsrcMeldinger",
                "oppdaterPersonidentForRessurs",
                fid,
                new Origination(
                    SecurityUtil.getAuthenticatedNavIdent(),
                    "Admin swagger/rest"
                )
            )
        );

        return ResponseEntity.ok()
            .body("Ressurs med FID %s er funnet for navident %s og personident %s. Endre personident til %s event er nå sendt".formatted(
                HtmlUtils.htmlEncode(fid),
                ressursState.get().navident(),
                ressursState.get().personident(),
                HtmlUtils.htmlEncode(nyPersonident)
            ));
    }

    @Operation(
        description = "Lag en Oppdater Personalia for ressurs Eventsrc melding",
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
    @PostMapping(value = "/oppdaterPersonaliaForRessurs")
    public ResponseEntity<String> oppdaterPersonidentForRessurs(
        @Parameter(description = "FID for ressursen som skal få endret personalia") @RequestParam String fid,
        @Parameter(description = "Fornavn") @RequestParam String fornavn,
        @Parameter(description = "Mellomnavn") @RequestParam(required = false) String mellomnavn,
        @Parameter(description = "Etternavn") @RequestParam(required = false) String etternavn,
        @Parameter(description = "Skriv CONFIRM for å bekrefte!") @RequestParam String confirm,
        @NonNull JwtAuthenticationToken token
    ) {
        log.debug("oppdaterPersonaliaForRessurs : " + token.getName());

        if (!validFid(fid)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("FID er ikke gyldig");
        }

        var ressursState = ressursStateRepository.findByFid(fid);
        if (ressursState.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Finner ingen ressurs med FID: " +  HtmlUtils.htmlEncode(fid));
        }

        if (!"CONFIRM".equals(confirm)) {
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body("Ressurs med FID %s er funnet for navident %s med navn %s. Men vil ikke endres da handling ikke er bekreftet".formatted(
                    HtmlUtils.htmlEncode(fid),
                    ressursState.get().navident(),
                    ressursState.get().visningsnavn().toString()
                ));
        }

        if (mellomnavn != null && mellomnavn.isBlank()) mellomnavn = null;

        ressursCommadService.oppdaterPersonaliaEvent(
            new OppdaterPersonaliaCommand(
                HtmlUtils.htmlEncode(fid),
                new Visningsnavn(fornavn, Optional.ofNullable(mellomnavn), etternavn)
                ),
            new InputMetadata(
                "/api/admin/manuelleEvtsrcMeldinger",
                "oppdaterPersonaliaForRessurs",
                fid,
                new Origination(
                    SecurityUtil.getAuthenticatedNavIdent(),
                    "Admin swagger/rest"
                )
            )
        );

        return ResponseEntity.ok()
            .body("Ressurs med FID %s er funnet for navident %s med navn %s. Endre personalia event er nå sendt".formatted(
                HtmlUtils.htmlEncode(fid),
                ressursState.get().navident(),
                ressursState.get().visningsnavn().toString()
            ));
    }

    private boolean validFid(String fid) {
        return fid.matches("[a-zA-Z0-9\\-]+");
    }

    private boolean validPersonident(String fid) {
        return fid.matches("[0-9]+");
    }
}
