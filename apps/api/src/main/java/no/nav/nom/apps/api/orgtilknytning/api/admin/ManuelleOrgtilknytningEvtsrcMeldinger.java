package no.nav.nom.apps.api.orgtilknytning.api.admin;

import com.google.common.base.Strings;
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
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.EndreDagligOppfolgingOrgtilknytningCommand;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.EndreOrgtilknytningDatoCommand;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.EndreOrgtilknytningSektorCommand;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.FjernOrgtilknytning;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.Origination;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Objects.isNull;

@Slf4j
@Tag(name = "Admin for å lage manuelle Evtsrc meldinger")
@RestController
@RequestMapping("/api/admin/manuelleEvtsrcMeldinger")
@PreAuthorize("hasAuthority('ADMIN')")
public class ManuelleOrgtilknytningEvtsrcMeldinger {

    private final OrgtilknytningCommandService orgtilknytningCommandService;

    private final OrgtilknytningService orgtilknytningService;

    public ManuelleOrgtilknytningEvtsrcMeldinger(OrgtilknytningCommandService orgtilknytningCommandService, OrgtilknytningService orgtilknytningService) {
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
    public ResponseEntity<String> fjernOrgtilknytning(
        @Parameter(description = "FID for orgtilknytningen som skal slettes") @RequestParam String fid,
        @Parameter(description = "Skriv CONFIRM for å bekrefte!") @RequestParam String confirm,
        @NonNull JwtAuthenticationToken token
    ) {
        if (!validFid(fid)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("FID er ikke gyldig");
        }

        var orgtilknytning = orgtilknytningService.findByFid(fid);
        if (orgtilknytning.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Finner ingen orgtilknytning med FID: " + HtmlUtils.htmlEscape(fid));
        }

        if (!"CONFIRM".equals(confirm)) {
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body("Orgtilknytning med FID %s er funnet.. Men vil ikke slettes da handling ikke er bekreftet".formatted(HtmlUtils.htmlEscape(fid)));
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
        return ResponseEntity.ok().body("Orgtilknytning med FID %s er funnet. Fjern event er nå sendt".formatted(HtmlUtils.htmlEscape(fid)));
    }

    @Operation(
        description = "Lager fjernOrgtilknytning Eventsrc meldinger for liste av orgtilknytning FID's",
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
    @PostMapping(value = "/fjernOrgtilknytninger")
    public ResponseEntity<String> fjernOrgtilknytning(
        @Parameter(description = "Skriv CONFIRM for å bekrefte!") @RequestParam String confirm,
        @Parameter(description = "Liste over FID's å slette") @RequestBody List<String> otFids
    ) {
        StringBuffer response = new StringBuffer();
        AtomicBoolean hasError = new AtomicBoolean(false);

        otFids.forEach(fid -> {
            if (!validFid(fid)) {
                response.append("FID %s er ikke gyldig".formatted(HtmlUtils.htmlEscape(fid))).append("\n");
                hasError.set(true);
            }

            var orgtilknytning = orgtilknytningService.findByFid(fid);
            if (orgtilknytning.isEmpty()) {
                response.append("Finner ingen orgtilknytning med FID: " + HtmlUtils.htmlEscape(fid)).append("\n");
                hasError.set(true);
            }

            if (!"CONFIRM".equals(confirm)) {
                response.append("Orgtilknytning med FID %s er funnet.. Men vil ikke slettes da handling ikke er bekreftet".formatted(HtmlUtils.htmlEscape(fid))).append("\n");
            }
        });
        if (!hasError.get() && "CONFIRM".equals(confirm)) {
            otFids.forEach(fid -> {
                var orgtilknytning = orgtilknytningService.findByFid(fid);

                orgtilknytningCommandService.fjernOrgtilknytning(
                    fid,
                    new FjernOrgtilknytning(
                        orgtilknytning.get().navident(),
                        orgtilknytning.get().orgEnhetId(),
                        orgtilknytning.get().gyldigFom(),
                        orgtilknytning.get().gyldigTom(),
                        new InputMetadata(
                            "/api/admin/manuelleEvtsrcMeldinger",
                            "fjernOrgtilknytninger",
                            fid,
                            new Origination(
                                SecurityUtil.getAuthenticatedNavIdent(),
                                "Admin swagger/rest"
                            )
                        ),
                        null
                    )
                );
            });
        }
        if (hasError.get() || !"CONFIRM".equals(confirm)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response.toString());
        }
        return ResponseEntity.ok().body("Fjern event er nå sendt for %s orgtilknytninger ".formatted(otFids.size()));
    }

    @Operation(
        description = "Lag en DagligOppfolgingEndret Eventsrc melding",
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
    @PostMapping(value = "/endreDagligOppfolgingOrgtilknytning")
    public ResponseEntity<String> endreDagligOppfolgingOrgtilknytning(
        @Parameter(description = "FID for orgtilknytningen som skal endre DOL") @RequestParam String fid,
        @Parameter(description = "Ny true/false for Daglig Oppfølging for orgtilknytningen") @RequestParam Boolean erDagligOppfolging,
        @Parameter(description = "Skriv CONFIRM for å bekrefte!") @RequestParam String confirm,
        @NonNull JwtAuthenticationToken token
    ) {
        if (!validFid(fid)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("FID er ikke gyldig");
        }

        var orgtilknytning = orgtilknytningService.findByFid(fid);
        if (orgtilknytning.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Finner ingen orgtilknytning med FID: " +  HtmlUtils.htmlEscape(fid));
        }

        if (!"CONFIRM".equals(confirm)) {
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body("Orgtilknytning med FID %s er funnet.. Men vil ikke endres da handling ikke er bekreftet".formatted(HtmlUtils.htmlEscape(fid)));
        }

        orgtilknytningCommandService.endreDagligOppfolgingOrgtilknytning(
            new EndreDagligOppfolgingOrgtilknytningCommand(
                fid,
                orgtilknytning.get().navident(),
                orgtilknytning.get().orgEnhetId(),
                orgtilknytning.get().gyldigFom(),
                orgtilknytning.get().gyldigTom(),
                erDagligOppfolging,
                new InputMetadata(
                    "/api/admin/manuelleEvtsrcMeldinger",
                    "endreDagligOppfolgingOrgtilknytning",
                    fid,
                    new Origination(
                        SecurityUtil.getAuthenticatedNavIdent(),
                        "Admin swagger/rest"
                    )
                ),
                null
            )
        );
        return ResponseEntity.ok().body("Orgtilknytning med FID %s er endret med daglig oppfølging til {}.".formatted(HtmlUtils.htmlEscape(erDagligOppfolging.toString())));
    }

    @Operation(
        description = "Lag en OrgtilknytningSektorEndret Eventsrc melding",
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
    @PostMapping(value = "/endreSektorOrgtilknytning")
    public ResponseEntity<String> endreSektorOrgtilknytning(
        @Parameter(description = "FID for orgtilknytningen som skal endre sektor") @RequestParam String fid,
        @Parameter(description = "Ny sektor for orgtilknytningen") @RequestParam String sektor,
        @Parameter(description = "Navident for orgtilknytningen") @RequestParam(required = false) String navident,
        @Parameter(description = "orgenhetId for org") @RequestParam(required = false) String orgEnhetId,
        @Parameter(description = "Skriv CONFIRM for å bekrefte!") @RequestParam String confirm
    ) {
        if (!validFid(fid)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("FID er ikke gyldig");
        }

        var orgtilknytning = orgtilknytningService.findByFid(fid);
        if (orgtilknytning.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Finner ingen orgtilknytning med FID: " +  HtmlUtils.htmlEscape(fid));
        }

        if (!"CONFIRM".equals(confirm)) {
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body("Orgtilknytning med FID %s er funnet, men vil ikke endres da handling ikke er bekreftet".formatted(HtmlUtils.htmlEscape(fid)));
        }
        orgtilknytningCommandService.endreOrgtilknytningSektor(
            new EndreOrgtilknytningSektorCommand(
                fid,
                isNull(navident) ? orgtilknytning.get().navident() : navident,
                isNull(orgEnhetId) ? orgtilknytning.get().orgEnhetId() : orgEnhetId,
                Sektor.valueOf(sektor),
                new InputMetadata(
                    "/api/admin/manuelleEvtsrcMeldinger",
                    "endreSektorOrgtilknytning",
                    fid,
                    new Origination(
                        SecurityUtil.getAuthenticatedNavIdent(),
                        "Admin swagger/rest"
                    )
                ),
                null
            )
        );
        return ResponseEntity.ok().body("Orgtilknytning med FID %s er endret med sektor til %s.".formatted(HtmlUtils.htmlEscape(fid), HtmlUtils.htmlEscape(sektor)));
    }

    @Operation(
        description = "Lag en OrgtilknytningDatoEndret Eventsrc melding",
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
    @PostMapping(value = "/endreSluttdatoOrgtilknytning")
    public ResponseEntity<String> endreSluttdatoOrgtilknytning(
        @Parameter(description = "FID for orgtilknytningen som skal endre sluttdato") @RequestParam String fid,
        @Parameter(description = "Ny sluttdato for ressursen") @RequestParam String nySluttDato,
        @Parameter(description = "Skriv CONFIRM for å bekrefte!") @RequestParam String confirm
    ) {
        if (!validFid(fid)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("FID er ikke gyldig");
        }
        var orgtilknytning = orgtilknytningService.findByFid(fid);
        if (orgtilknytning.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Finner ingen orgtilknytning med FID: " +  HtmlUtils.htmlEscape(fid));
        }

        LocalDate nySluttDatoParsed = null;
        try {
            nySluttDatoParsed = Strings.isNullOrEmpty(nySluttDato) ? null : LocalDate.parse(nySluttDato);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Feil format på sluttdato");
        }

        if (nySluttDatoParsed != null && orgtilknytning.get().gyldigFom().isAfter(nySluttDatoParsed)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Ny sluttdato kan ikke være før startdato");
        }

        if (isNull(orgtilknytning.get().gyldigTom())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Orgtilknytning uten sluttdato kan ikke få endret sluttdato, den må avvikles i stedet");
        }

        if (!"CONFIRM".equals(confirm)) {
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body("Orgtilknytning med FID %s og sluttdato %s er funnet, men vil ikke endres da handling ikke er bekreftet".formatted(
                    HtmlUtils.htmlEscape(fid),
                    orgtilknytning.get().gyldigTom())
                );
        }

        orgtilknytningCommandService.endreOrgtilknytningDato(
            new EndreOrgtilknytningDatoCommand(
                fid,
                orgtilknytning.get().gyldigFom(),
                nySluttDatoParsed,
                new InputMetadata(
                    "/api/admin/manuelleEvtsrcMeldinger",
                    "endreSektorOrgtilknytning",
                    fid,
                    new Origination(
                        SecurityUtil.getAuthenticatedNavIdent(),
                        "Admin swagger/rest"
                    )
                )
            )
        );
        return ResponseEntity.ok()
            .body("Orgtilknytning med FID %s og sluttdato %s er funnet, og sluttdato er endret til %s".formatted(
                HtmlUtils.htmlEscape(fid),
                orgtilknytning.get().gyldigTom(),
                nySluttDato)
            );
    }

    private boolean validFid(String fid) {
        return fid.matches("[a-zA-Z0-9\\-]+");
    }
}
