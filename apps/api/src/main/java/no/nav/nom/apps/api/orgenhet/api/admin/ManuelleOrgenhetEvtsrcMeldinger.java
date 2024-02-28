package no.nav.nom.apps.api.orgenhet.api.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.common.SwaggerConfig;
import no.nav.nom.apps.api.graphql.common.SecurityUtil;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.OrgEnhetCommandService;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.commands.AvvikleOrgEnhet;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.FjernOrgtilknytning;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.Origination;
import org.flywaydb.core.internal.util.HtmlUtils;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Slf4j
@Tag(name = "Admin for å lage manuelle Evtsrc meldinger")
@RestController
@RequestMapping("/api/admin/manuelleEvtsrcMeldinger")
@PreAuthorize("hasAuthority('ADMIN')")
public class ManuelleOrgenhetEvtsrcMeldinger {
    private final OrgEnhetCommandService orgEnhetCommandService;

    private final OrgenhetService orgenhetService;

    public ManuelleOrgenhetEvtsrcMeldinger(OrgEnhetCommandService orgEnhetCommandService, OrgenhetService orgenhetService) {
        this.orgEnhetCommandService = orgEnhetCommandService;
        this.orgenhetService = orgenhetService;
    }


    @Operation(
        description = "Lag en avsluttOrgenhet Eventsrc melding",
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
    @PostMapping(value = "/avvikleOrgenhet")
    public ResponseEntity<String> avvikleOrgenhet(
        @Parameter(description = "Orgenhet Id") @RequestParam String orgenhetId,
        @Parameter(description = "Gyldig til og med") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate gylidgTom,
        @Parameter(description = "Skriv CONFIRM for å bekrefte!") @RequestParam String confirm,
        @NonNull JwtAuthenticationToken token
    ) {
        var orgenhet = orgenhetService.getById(orgenhetId).orElse(null);
        if (orgenhet == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Finner ingen orgenhet med ID: " +  HtmlUtils.htmlEncode(orgenhetId));
        }
        if (orgenhet.getGyldigFom().isAfter(gylidgTom)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Enhetens gyldigFom (" + orgenhet.getGyldigFom() + ") er etter ny gyldigTOM (" + HtmlUtils.htmlEncode(gylidgTom.toString()) + ") dato");
        }

        if (!"CONFIRM".equals(confirm)) {
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body("Orgenhet %s - %s [%s - %s] kan avsluttes på dato %s. Men vil ikke utføres da handling ikke er bekreftet".formatted(
                    HtmlUtils.htmlEncode(orgenhetId), HtmlUtils.htmlEncode(orgenhet.getNavn()),
                    orgenhet.getGyldigFom(), orgenhet.getGyldigTom().isPresent() ? orgenhet.getGyldigTom() : "", HtmlUtils.htmlEncode(gylidgTom.toString())));
        }

        orgEnhetCommandService.avikleOrgEnhet(
            orgenhet.getFid(),
            new AvvikleOrgEnhet(
                gylidgTom,
                new InputMetadata(
                    "/api/admin/manuelleEvtsrcMeldinger",
                    "avvikleOrgenhet",
                    orgenhet.getId(),
                    new Origination(
                        SecurityUtil.getAuthenticatedNavIdent(),
                        "Admin swagger/rest"
                    )
                )
            )
        );
        return ResponseEntity.ok()
            .body("Orgenhet %s - %s [%s - %s] avsluttes på dato %s.".formatted(
                HtmlUtils.htmlEncode(orgenhetId), HtmlUtils.htmlEncode(orgenhet.getNavn()),
                orgenhet.getGyldigFom(), orgenhet.getGyldigTom().isPresent() ? orgenhet.getGyldigTom() : "", HtmlUtils.htmlEncode(gylidgTom.toString())));
    }

}
