package no.nav.nom.apps.api.authorize.redigereressurs.api.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.authorize.eventsourcing.producer.AuthorizeCommandService;
import no.nav.nom.apps.api.authorize.eventsourcing.producer.commands.AvsluttDelegertAuthorization;
import no.nav.nom.apps.api.authorize.eventsourcing.producer.commands.FornyDelegertAuthorization;
import no.nav.nom.apps.api.authorize.eventsourcing.producer.commands.OpprettDelegertAuthorization;
import no.nav.nom.apps.api.authorize.redigereressurs.AuthorizedNavident;
import no.nav.nom.apps.api.common.SwaggerConfig;
import no.nav.nom.apps.api.graphql.common.SecurityUtil;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.rest.common.StandardResponse;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.Origination;
import no.nav.nom.libs.state.authorize.DelegertLederStateRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Tag(name = "Admin for å lage manuelle Evtsrc meldinger")
@RestController
@RequestMapping("/api/admin/manuelleEvtsrcMeldinger")
@PreAuthorize("hasAuthority('ADMIN')")
public class ManuelleAutorizedEvtsrcMeldinger {

    private final AuthorizeCommandService authorizeCommandService;
    private final DelegertLederStateRepository delegertLederStateRepository;
    private final OrgenhetService orgenhetService;
    private final AuthorizedNavident authorizedNavident;
    @Value("${app.orgenhet.helfo}") private String helfoOrgenhetId;
    @Value("${app.orgenhet.skatt}") private String skattOrgenhetId;

    public ManuelleAutorizedEvtsrcMeldinger(AuthorizeCommandService authorizeCommandService, DelegertLederStateRepository delegertLederStateRepository,
                                            OrgenhetService orgenhetService, AuthorizedNavident authorizedNavident
    ) {
        this.authorizeCommandService = authorizeCommandService;
        this.delegertLederStateRepository = delegertLederStateRepository;
        this.orgenhetService = orgenhetService;
        this.authorizedNavident = authorizedNavident;
    }

    @Operation(
        description = "Lag en AvsluttAuthorization Eventsrc melding med avslutningsdato i dag",
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
    @PostMapping(value = "/avsluttAuthorization")
    public ResponseEntity<String> avsluttAuthorization(
        @Parameter(description = "FID for authorization/delegering som skal avsluttes") @RequestParam String fid,
        @Parameter(description = "Skriv CONFIRM for å bekrefte!") @RequestParam String confirm,
        @NonNull JwtAuthenticationToken token
    ) {
        if (!validFid(fid)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("FID er ikke gyldig");
        }

        var authorization = delegertLederStateRepository.findByFid(fid).orElse(null);
        if (authorization == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Finner ingen authorization/delegering med FID: " +  HtmlUtils.htmlEscape(fid));
        }

        if (!"CONFIRM".equals(confirm)) {
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body("Authorization/delegering med FID %s er funnet.. Men vil ikke avsluttes da handling ikke er bekreftet".formatted(HtmlUtils.htmlEscape(fid)));
        }

        var inputMetadata = new InputMetadata(
            "/api/admin/manuelleEvtsrcMeldinger",
            "avsluttAuthorization",
            fid,
            new Origination(
                SecurityUtil.getAuthenticatedNavIdent(),
                "Admin swagger/rest"
            )
        );

        AvsluttDelegertAuthorization avsluttDelegertAuthorization = AvsluttDelegertAuthorization.builder()
            .av(authorization.av())
            .til(authorization.til())
            .forOrgenhet(authorization.orgenhetId())
            .gyldigFom(authorization.gyldigFom())
            .opprinneligGyldigTom(authorization.gyldigTom())
            .nyGyldigTom(LocalDate.now())
            .inputMetadata(inputMetadata)
            .build();

        authorizeCommandService.avsluttDelegertAuthorization(authorization.fid(), avsluttDelegertAuthorization);

        return ResponseEntity.ok().body("Authorization/delegering med FID %s er funnet. Avslutt event på dagens dato er nå sendt".formatted(HtmlUtils.htmlEscape(fid)));
    }

    @Operation(
        description = "Lag en Authorization/delegering Eventsrc melding på vegne av en leder",
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
    @PostMapping(value = "/opprettAuthorization")
    public ResponseEntity<String> opprettAuthorization(
        @Parameter(description = "Navident på Leder delegeringen er for") @RequestParam String lederNavident,
        @Parameter(description = "Navident som skal få tildelt delegeringen") @RequestParam String delegertNavident,
        @Parameter(description = "OrgenhetId som delegeringen skal gjelde") @RequestParam String orgenhetId,
        @Parameter(description = "Gyldig til og med dato for delegeringen") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate gyldigTom,
        @Parameter(description = "Skriv CONFIRM for å bekrefte!") @RequestParam String confirm,
        @NonNull JwtAuthenticationToken token
    ) {
        var orgenhet = orgenhetService.getById(orgenhetId);
        if (orgenhet.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Finner ingen orgenhet med ID: " +  HtmlUtils.htmlEscape(orgenhetId));
        }

        //TODO (hamkel): Fjern dette når ORG-2848 er ferdig implementert
        if (!orgenhetId.equals(helfoOrgenhetId) && !orgenhetId.equals(skattOrgenhetId)) {
            authorizedNavident.sjekkParametereDelegertLeder(lederNavident, delegertNavident, orgenhetId, gyldigTom);
        }
        authorizedNavident.sjekkOmOrgenhetAlleredeDelegertTilNavident(orgenhetId, delegertNavident);

        if (!"CONFIRM".equals(confirm)) {
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body("Authorization/Delegering kan gis - parameterene er korrekte.. Men vil ikke endres da handling ikke er bekreftet");
        }

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("leder", lederNavident);
        arguments.put("Delegert til", delegertNavident);
        arguments.put("Orgenhet", orgenhetId);
        arguments.put("Gyldig til", gyldigTom);

        var inputMetadata = new InputMetadata(
            "/api/admin/manuelleEvtsrcMeldinger",
            "opprettAuthorization",
            arguments,
            new Origination(
                SecurityUtil.getAuthenticatedNavIdent(),
                "Admin swagger/rest"
            )
        );

        OpprettDelegertAuthorization opprettDelegertAuthorization = OpprettDelegertAuthorization.builder()
            .av(lederNavident)
            .til(delegertNavident)
            .forOrgenhet(orgenhetId)
            .gyldigFom(LocalDate.now())
            .gyldigTom(gyldigTom)
            .inputMetadata(inputMetadata)
            .build();

        var fid = authorizeCommandService.opprettDelegertAuthorization(opprettDelegertAuthorization);

        return ResponseEntity.ok().body("Authorization/delegering med FID %s er opprettet".formatted(fid));
    }

    @Operation(
        description = "Forny en Authorization/delegering Eventsrc melding på vegne av en leder med ny gyldig til og med dato",
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
    @PostMapping(value = "/fornyAuthorization")
    public ResponseEntity<String> fornyAuthorization(
        @Parameter(description = "FID for opprinnelig delegeringen") @RequestParam String fid,
        @Parameter(description = "Ny gyldig til og med dato for delegeringen") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate nyGyldigTom,
        @Parameter(description = "Skriv CONFIRM for å bekrefte!") @RequestParam String confirm,
        @NonNull JwtAuthenticationToken token
    ) {
        if (!validFid(fid)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("FID er ikke gyldig");
        }
        
        String finalFid = fid;
        var delegering = delegertLederStateRepository.findByFid(fid)
            .orElseThrow(() -> new RuntimeException("Finner ingen authorization/delegering med FID: " +  HtmlUtils.htmlEscape(finalFid)));

        if (!"CONFIRM".equals(confirm)) {
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body("Authorization/Delegering kan endres - parameterene er korrekte.. Men vil ikke endres da handling ikke er bekreftet");
        }

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("leder", delegering.av());
        arguments.put("Delegert til", delegering.til());
        arguments.put("Orgenhet", delegering.orgenhetId());
        arguments.put("gyldigFom", delegering.gyldigFom());
        arguments.put("gyldigTom", delegering.gyldigTom());
        arguments.put("Ny Gyldig til", nyGyldigTom);

        var inputMetadata = new InputMetadata(
            "/api/admin/manuelleEvtsrcMeldinger",
            "fornyAuthorization",
            arguments,
            new Origination(
                SecurityUtil.getAuthenticatedNavIdent(),
                "Admin swagger/rest"
            )
        );

        FornyDelegertAuthorization fornyDelegertAuthorization = FornyDelegertAuthorization.builder()
            .av(delegering.av())
            .til(delegering.til())
            .forOrgenhet(delegering.orgenhetId())
            .gyldigFom(delegering.gyldigFom())
            .opprinneligGyldigTom(delegering.gyldigTom())
            .nyGyldigTom(nyGyldigTom)
            .inputMetadata(inputMetadata)
            .build();

        fid = authorizeCommandService.fornyDelegertAuthorization(delegering.fid(), fornyDelegertAuthorization);

        return ResponseEntity.ok().body("Authorization/delegering med FID %s er fornyet".formatted(fid));
    }

    private boolean validFid(String fid) {
        return fid.matches("[a-zA-Z0-9\\-]+");
    }

    @ExceptionHandler(NullPointerException.class)
    private ResponseEntity<StandardResponse> handleServerError(NullPointerException exception) {
        log.error("NullPointerException", exception);

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(StandardResponse.builder().message("Server error").build());
    }

    @ExceptionHandler(Exception.class)
    private ResponseEntity<StandardResponse> handleServerError(Exception exception) {
        log.error("Intern feil for sending av manuell melding", exception);

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(StandardResponse.builder().message("Server error").build());
    }
}


