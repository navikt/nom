package no.nav.nom.apps.api.organisering.api.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.common.SwaggerConfig;
import no.nav.nom.apps.api.graphql.common.SecurityUtil;
import no.nav.nom.apps.api.organisering.eventsourcing.producer.OrganiseringEventCommandService;
import no.nav.nom.apps.api.organisering.eventsourcing.producer.commands.AvsluttOrganiseringCmd;
import no.nav.nom.apps.api.organisering.eventsourcing.producer.commands.EndreOrganiseringCmd;
import no.nav.nom.apps.api.organisering.eventsourcing.producer.commands.OpprettOrganiseringCmd;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.Origination;
import no.nav.nom.libs.state.organisering.OrganiseringStateRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

import java.time.LocalDate;
import java.util.Map;

@Slf4j
@Tag(name = "Admin for å lage manuelle Evtsrc meldinger")
@RestController
@RequestMapping("/api/admin/manuelleEvtsrcMeldinger")
@PreAuthorize("hasAuthority('ADMIN')")
public class ManuelleOrganiseringEvtsrcMeldinger {

    private final OrganiseringEventCommandService organiseringEventCommandService;
    private final OrganiseringStateRepository organiseringRepository;

    public ManuelleOrganiseringEvtsrcMeldinger(
        OrganiseringEventCommandService organiseringEventCommandService,
        OrganiseringStateRepository organiseringRepository
    ) {
        this.organiseringEventCommandService = organiseringEventCommandService;
        this.organiseringRepository = organiseringRepository;
    }

    @Operation(
        description = "Lag en Avslutt organisering Eventsrc melding",
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
    @PostMapping(value = "/lagAvsluttOrganiseringMelding")
    public ResponseEntity<String> lagAvsluttOrganiseringMelding(
        @Parameter(description = "FID for organiseringen som skal avsluttes") @RequestParam String fid,
        @Parameter(description = "GyldigTom for organiseringen") @RequestParam(required = false) LocalDate gyldigTom,
        @Parameter(description = "Skriv CONFIRM for å bekrefte!") @RequestParam String confirm,
        @NonNull JwtAuthenticationToken token
    ) {
        log.debug("lagAvsluttOrganiseringMelding : {}", token.getName());

        if (!validFid(fid)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("FID er ikke gyldig");
        }

        if (gyldigTom == null) { gyldigTom = LocalDate.now(); }

        var organiseringState = organiseringRepository.findByFid(fid);
        if (organiseringState.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Finner ingen organisering med FID: " + HtmlUtils.htmlEscape(fid));
        }

        if (!"CONFIRM".equals(confirm)) {
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body("Organisering med FID %s er funnet for orgenhet %s under orgenhet %s. Men vil ikke avsluttes da handling ikke er bekreftet".formatted(
                    HtmlUtils.htmlEscape(fid),
                    organiseringState.get().id(),
                    organiseringState.get().idOver()
                ));
        }



        organiseringEventCommandService.avsluttOrganisering(
            fid,
            new AvsluttOrganiseringCmd(
                organiseringState.get().id(),
                gyldigTom,
                new InputMetadata(
                    "/api/admin/manuelleEvtsrcMeldinger",
                    "lagAvsluttOrganiseringMelding",
                    Map.of(
                        "fid", fid,
                        "gyldigTom", gyldigTom.toString()
                    ),
                    new Origination(
                        SecurityUtil.getAuthenticatedNavIdent(),
                        "Admin swagger/rest"
                    )
                )
            )
        );

        return ResponseEntity.ok()
            .body("Organisering med FID %s er funnet for orgenhet %s under orgenhet %s. Avslutt event er nå sendt".formatted(
                HtmlUtils.htmlEscape(fid),
                organiseringState.get().id(),
                organiseringState.get().idOver()
            ));
    }

    @Operation(
        description = "Lag en Endre organisering Eventsrc melding",
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
    @PostMapping(value = "/lagEndreOrganiseringMelding")
    public ResponseEntity<String> lagEndreOrganiseringMelding(
        @Parameter(description = "FID for organiseringen som skal endres") @RequestParam String fid,
        @Parameter(description = "Enhet ID for enheten som organiseres") @RequestParam(required = false) String id,
        @Parameter(description = "Navn for enheten som organiseres") @RequestParam(required = false) String navn,
        @Parameter(description = "Enhet ID for enheten som denne er organisert under") @RequestParam(required = false) String idOver,
        @Parameter(description = "Navn for enheten som denne er organisert under") @RequestParam(required = false) String navnOver,
        @Parameter(description = "GyldigFom for organiseringen") @RequestParam(required = false) LocalDate gyldigFom,
        @Parameter(description = "GyldigTom for organiseringen") @RequestParam(required = false) LocalDate gyldigTom,
        @Parameter(description = "Skriv CONFIRM for å bekrefte!") @RequestParam String confirm,
        @NonNull JwtAuthenticationToken token
    ) {
        log.debug("lagEndreOrganiseringMelding : {}", token.getName());

        if (!validFid(fid)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("FID er ikke gyldig");
        }

        var eksisterende = organiseringRepository.findByFid(fid);
        if (eksisterende.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Finner ingen organisering med FID: " + HtmlUtils.htmlEscape(fid));
        }

        if (id == null) { id = eksisterende.get().id(); }
        if (navn == null) { navn = eksisterende.get().navn(); }
        if (idOver == null) { idOver = eksisterende.get().idOver(); }
        if (navnOver == null) { navnOver = eksisterende.get().navnOver(); }
        if (gyldigFom == null) { gyldigFom = eksisterende.get().gyldigFom(); }

        if (!"CONFIRM".equals(confirm)) {
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body("Organisering med FID %s er funnet for orgenhet %s under orgenhet %s. Men vil ikke avsluttes da handling ikke er bekreftet".formatted(
                    HtmlUtils.htmlEscape(fid),
                    eksisterende.get().id(),
                    eksisterende.get().idOver()
                ));
        }

        organiseringEventCommandService.endreOrganisering(
            fid,
            new EndreOrganiseringCmd(
                id,
                navn,
                idOver,
                navnOver,
                gyldigFom,
                gyldigTom,
                new InputMetadata(
                    "/api/admin/manuelleEvtsrcMeldinger",
                    "lagEndreOrganiseringMelding",
                    Map.of(
                        "fid", fid,
                        "id", id,
                        "navn", navn,
                        "idOver", idOver,
                        "navnOver", navnOver,
                        "gyldigFom", gyldigFom == null ? "" : gyldigFom.toString(),
                        "gyldigTom", gyldigTom == null ? "" : gyldigTom.toString()
                    ),
                    new Origination(
                        SecurityUtil.getAuthenticatedNavIdent(),
                        "Admin swagger/rest"
                    )
                )
            )
        );

        return ResponseEntity.ok()
            .body("Organisering med FID %s er funnet for orgenhet %s under orgenhet %s. Avslutt event er nå sendt".formatted(
                HtmlUtils.htmlEscape(fid),
                eksisterende.get().id(),
                eksisterende.get().idOver()
            ));
    }

    @Operation(
        description = "Lag manuell OpprettOrganiseringCmd EvtSrc melding",
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
    @PostMapping("/lagOpprettOrganiseringCmd")
    public ResponseEntity<String> opprettOrganisering(
        @Parameter(description = "Enhet ID for enheten som organiseres") @RequestParam String id,
        @Parameter(description = "Navn for enheten som organiseres") @RequestParam String navn,
        @Parameter(description = "Enhet ID for enheten som denne er organisert under") @RequestParam String idOver,
        @Parameter(description = "Navn for enheten som denne er organisert under") @RequestParam String navnOver,
        @Parameter(description = "GyldigFom for organiseringen") @RequestParam LocalDate gyldigFom,
        @Parameter(description = "GyldigTom for organiseringen") @RequestParam(required = false) LocalDate gyldigTom,
        @Parameter(description = "Skriv CONFIRM for å bekrefte!") @RequestParam String confirm,
        @NonNull JwtAuthenticationToken token
    ) {
        if (!"CONFIRM".equals(confirm)) {
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body("Organisering med for orgEnhet med ID %s ble ikke opprettet da det ikke var bekfreftet".formatted(
                    HtmlUtils.htmlEscape(id)
                ));
        }

        organiseringEventCommandService.opprettOrganisering(
            new OpprettOrganiseringCmd(
                id,
                navn,
                idOver,
                navnOver,
                gyldigFom,
                gyldigTom,
                new InputMetadata(
                    "/api/admin/manuelleEvtsrcMeldinger",
                    "lagOpprettOrganiseringCmd",
                    Map.of(
                        "id", id,
                        "navn", navn,
                        "idOver", idOver,
                        "navnOver", navnOver,
                        "gyldigFom", gyldigFom == null ? "" : gyldigFom.toString(),
                        "gyldigTom", gyldigTom == null ? "" : gyldigTom.toString()
                    ),
                    new Origination(
                        SecurityUtil.getAuthenticatedNavIdent(),
                        "Admin swagger/rest"
                    )
                )
            )
        );

        return ResponseEntity.ok()
            .body("Organisering med OrgEnhet ID %s er nå opprettet.".formatted(
                HtmlUtils.htmlEscape(id)
            ));
    }

    private boolean validFid(String fid) {
        return fid.matches("[a-zA-Z0-9\\-]+");
    }
}
