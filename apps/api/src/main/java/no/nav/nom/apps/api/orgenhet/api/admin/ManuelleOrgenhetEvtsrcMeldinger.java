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
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.commands.KorrigerOrgEnhet;
import no.nav.nom.apps.api.orgenhet.exceptions.OrgEnhetNotFoundException;
import no.nav.nom.apps.api.ressurs.api.exceptions.RessursNotFoundException;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.Origination;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
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
import org.springframework.web.util.HtmlUtils;

import java.time.LocalDate;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNullElse;
import static no.nav.nom.libs.utils.common.util.DateUtil.isActive;

@Slf4j
@Tag(name = "Admin for å lage manuelle Evtsrc meldinger")
@RestController
@RequestMapping("/api/admin/manuelleEvtsrcMeldinger")
@PreAuthorize("hasAuthority('ADMIN')")
public class ManuelleOrgenhetEvtsrcMeldinger {
    private final OrgEnhetCommandService orgEnhetCommandService;

    private final OrgenhetService orgenhetService;
    private final RessursStateRepository ressursStateRepository;

    public ManuelleOrgenhetEvtsrcMeldinger(
        OrgEnhetCommandService orgEnhetCommandService,
        OrgenhetService orgenhetService,
        RessursStateRepository ressursStateRepository
    ) {
        this.orgEnhetCommandService = orgEnhetCommandService;
        this.orgenhetService = orgenhetService;
        this.ressursStateRepository = ressursStateRepository;
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
        @Parameter(description = "Gyldig til og med") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate gyldigTom,
        @Parameter(description = "Skriv CONFIRM for å bekrefte!") @RequestParam String confirm,
        @NonNull JwtAuthenticationToken token
    ) {
        var orgenhet = orgenhetService.getById(orgenhetId).orElse(null);
        if (orgenhet == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Finner ingen orgenhet med ID: " +  HtmlUtils.htmlEscape(orgenhetId));
        }
        if (orgenhet.getGyldigFom().isAfter(gyldigTom)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Enhetens gyldigFom (" + orgenhet.getGyldigFom() + ") er etter ny gyldigTOM (" + HtmlUtils.htmlEscape(gyldigTom.toString()) + ") dato");
        }

        if (!"CONFIRM".equals(confirm)) {
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body("Orgenhet %s - %s [%s - %s] kan avsluttes på dato %s. Men vil ikke utføres da handling ikke er bekreftet".formatted(
                    HtmlUtils.htmlEscape(orgenhetId), HtmlUtils.htmlEscape(orgenhet.getNavn()),
                    orgenhet.getGyldigFom(), requireNonNullElse(orgenhet.getGyldigTom(), ""), HtmlUtils.htmlEscape(gyldigTom.toString())));
        }

        orgEnhetCommandService.avvikleOrgEnhet(
            orgenhet.getFid(),
            new AvvikleOrgEnhet(
                gyldigTom,
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
                HtmlUtils.htmlEscape(orgenhetId), HtmlUtils.htmlEscape(orgenhet.getNavn()),
                orgenhet.getGyldigFom(), requireNonNullElse(orgenhet.getGyldigTom(), ""), HtmlUtils.htmlEscape(gyldigTom.toString())));
    }

    @Operation(
        description = "Lag en korrigerOrgEnhet EvtSrc melding",
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
    @PostMapping("/korrigerOrgEnhet")
    public ResponseEntity<String> korrigerOrgEnhet(
        @Parameter(description = "OrgEnhet Fid") @RequestParam String orgEnhetFid,
        @Parameter(description = "OrgEnhet navn") @RequestParam(required = false) String orgEnhetNavn,
        @Parameter(description = "OrgEnhet gyldigFom") @RequestParam(required = false) LocalDate orgEnhetGyldigFom,
        @Parameter(description = "OrgEnhet gyldigTom") @RequestParam(required = false) LocalDate orgEnhetGyldigTom,
        @Parameter(description = "OrgEnhet OverordnetOrgEnhetFid") @RequestParam(required = false) String orgEnhetOverordnetOrgEnhetFid,
        @Parameter(description = "OrgEnhet LederRessursFid") @RequestParam(required = false) String lederRessursFid,
        @Parameter(description = "Skriv CONFIRM for å bekrefte!") @RequestParam String confirm
    ) {
        var orgEnhet = orgenhetService.getByFid(orgEnhetFid).orElseThrow(() -> OrgEnhetNotFoundException.fid(orgEnhetFid));

        var ressursState = ressursStateRepository.findByFid(lederRessursFid)
            .filter(r -> isActive(r.startdato(), r.sluttdato()))
            .orElseThrow(() -> RessursNotFoundException.fid(lederRessursFid));

        orgenhetService.getByFid(orgEnhetOverordnetOrgEnhetFid)
            .orElseThrow(() -> OrgEnhetNotFoundException.fid(orgEnhetOverordnetOrgEnhetFid));

        if (!"CONFIRM".equals(confirm)) {
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body("Orgenhet %s - %s kan korrigeres, men vil ikke utføres da handling ikke er bekreftet".formatted(
                    HtmlUtils.htmlEscape(orgEnhetFid), HtmlUtils.htmlEscape(orgEnhetNavn)));
        }


        orgEnhetCommandService.korrigerOrgEnhet(
            orgEnhetFid,
            new KorrigerOrgEnhet(
                orgEnhet.getId(),
                isNull(orgEnhetNavn) ? orgEnhet.getNavn() : orgEnhetNavn,
                isNull(orgEnhetGyldigFom) ? orgEnhet.getGyldigFom() : orgEnhetGyldigFom,
                isNull(orgEnhetGyldigTom) ? orgEnhet.getGyldigTom() :orgEnhetGyldigTom,
                isNull(orgEnhetOverordnetOrgEnhetFid) ? orgEnhet.getOverordnetOrgEnhetFid() : orgEnhetOverordnetOrgEnhetFid,
                isNull(lederRessursFid) ? ressursState.fid() : lederRessursFid,
                new InputMetadata(
                    "/api/admin/manuelleEvtsrcMeldinger",
                    "korrigerOrgEnhet",
                    orgEnhet.getId(),
                    new Origination(
                        SecurityUtil.getAuthenticatedNavIdent(),
                        "Admin swagger/rest"
                    )
                )
            )
        );

        return ResponseEntity.ok().body("OrgEnhet %s - %s har blitt korrigert".formatted(
            HtmlUtils.htmlEscape(orgEnhet.getId()), HtmlUtils.htmlEscape(orgEnhetNavn)));
    }

}
