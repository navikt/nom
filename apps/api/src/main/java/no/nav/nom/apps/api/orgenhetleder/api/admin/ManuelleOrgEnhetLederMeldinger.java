package no.nav.nom.apps.api.orgenhetleder.api.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.common.SwaggerConfig;
import no.nav.nom.apps.api.graphql.common.SecurityUtil;
import no.nav.nom.apps.api.orgenhetleder.eventsourcing.producer.OrgEnhetLederCommandService;
import no.nav.nom.apps.api.orgenhetleder.eventsourcing.producer.commands.OppdaterOrgEnhetLederSluttdato;
import no.nav.nom.apps.api.orgenhetleder.eventsourcing.producer.commands.OppdaterOrgEnhetLederStartdato;
import no.nav.nom.apps.api.orgenhetleder.eventsourcing.producer.commands.OpprettOrgEnhetLeder;
import no.nav.nom.apps.api.orgenhetleder.eventsourcing.producer.commands.SlettOrgEnhetLeder;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.Origination;
import no.nav.nom.libs.state.orgenhetleder.OrgEnhetLederStateRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.isNull;

@Slf4j
@Tag(name = "Admin for å lage manuelle leder Evtsrc meldinger")
@RestController
@RequestMapping("/api/admin/manuelleEvtsrcMeldinger")
@PreAuthorize("hasAuthority('ADMIN')")
public class ManuelleOrgEnhetLederMeldinger {

    private final OrgEnhetLederCommandService orgEnhetLederCommandService;
    private final OrgEnhetLederStateRepository orgEnhetLederStateRepository;

    public ManuelleOrgEnhetLederMeldinger(OrgEnhetLederCommandService orgEnhetLederCommandService,
                                          OrgEnhetLederStateRepository orgEnhetLederStateRepository) {
        this.orgEnhetLederCommandService = orgEnhetLederCommandService;
        this.orgEnhetLederStateRepository = orgEnhetLederStateRepository;
    }

    @Operation(
        description = "Opprett ny leder for orgenhet manuelt",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "success"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Not found")
        }
    )
    @PostMapping(value = "/opprettManuellOrgEnhetLeder")
    public ResponseEntity<String> opprettManuellOrgEnhetLeder(
        @Parameter(description = "Navident til leder") @RequestParam String navident,
        @Parameter(description = "OrgEnhet id") @RequestParam String orgEnhetId,
        @Parameter(description = "Startdato for leder i orgEnhet") @RequestParam(required = false) LocalDate startdato,
        @Parameter(description = "Sluttdato for leder i orgEnhet") @RequestParam(required = false) LocalDate sluttdato,
        @Parameter(description = "Skriv CONFIRM for å bekrefte!") @RequestParam String confirm
    ) {
        if (orgEnhetLederStateRepository.hasActiveInPeriodByOrgEnhetId(orgEnhetId, startdato, sluttdato)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        if (!"CONFIRM".equals(confirm)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        Map<String, Object> arguments = new HashMap<>();
        arguments.put("navident", navident);
        arguments.put("orgEnhetId", orgEnhetId);
        arguments.put("startdato", startdato);
        arguments.put("sluttdato", sluttdato);

        var inputMetadata = new InputMetadata(
            "/api/admin/manuelleEvtsrcMeldinger",
            "opprettOrgEnhetLeder",
            arguments,
            new Origination(
                SecurityUtil.getAuthenticatedNavIdent(),
                "Admin swagger/rest"
            )
        );

        OpprettOrgEnhetLeder opprettOrgEnhetLeder = OpprettOrgEnhetLeder.builder()
            .navident(navident)
            .orgEnhetId(orgEnhetId)
            .inputMetadata(inputMetadata)
            .build();

        var fid = orgEnhetLederCommandService.opprettOrgEnhetLeder(opprettOrgEnhetLeder);

        if (startdato != null) {
            orgEnhetLederCommandService.oppdaterOrgEnhetLederStartdato(fid, OppdaterOrgEnhetLederStartdato.builder().startdato(startdato).inputMetadata(inputMetadata).build());
        }

        if (sluttdato != null) {
            orgEnhetLederCommandService.oppdaterOrgEnhetLederSluttdato(fid, OppdaterOrgEnhetLederSluttdato.builder().sluttdato(sluttdato).inputMetadata(inputMetadata).build());
        }

        return ResponseEntity.ok()
            .body("Leder %s er opprettet med FID %s i orgenhet %s".formatted(
                HtmlUtils.htmlEscape(navident),
                HtmlUtils.htmlEscape(fid),
                HtmlUtils.htmlEscape(orgEnhetId)));
    }

    @Operation(
        description = "Oppdater startdato for en OrgEnhetLeder manuelt",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "success"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Not found")
        }
    )
    @PutMapping("/oppdaterManuellOrgEnhetLederStartdato")
    public ResponseEntity<String> oppdaterManuellOrgEnhetLederStartdato(
        @Parameter(description = "FID for leder i OrgEnhet hvor startdato skal oppdateres") @RequestParam String fid,
        @Parameter(description = "Startdato for leder i OrgEnhet") @RequestParam LocalDate startdato,
        @Parameter(description = "Skriv CONFIRM for å bekrefte!") @RequestParam String confirm
    ) {
        if (!validFid(fid)) return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();

        if (isNull(startdato)) return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();

        if (!"CONFIRM".equals(confirm)) return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();

        return orgEnhetLederStateRepository.findByFid(fid).map(
            orgEnhetLederState -> {
                var inputMetadata = InputMetadata.builder()
                    .inputInterfaceName("/api/admin/manuelleEvtsrcMeldinger")
                    .inputInterfaceMethodName("oppdaterManuellOrgEnhetLederStartdato")
                    .inputInterfaceMethodArguments(fid)
                    .origination(new Origination(
                        SecurityUtil.getAuthenticatedNavIdent(),
                        "Admin swagger/rest"
                    )).build();

                orgEnhetLederCommandService.oppdaterOrgEnhetLederStartdato(
                    fid,
                    OppdaterOrgEnhetLederStartdato.builder()
                        .startdato(startdato)
                        .inputMetadata(inputMetadata)
                        .build()
                );
                return ResponseEntity.ok().body("Startdato til FID %s ble oppdatert til %s".formatted(fid, startdato));
            }).orElse(ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body("Fant ikke OrgEnhetLederState med FID %s".formatted(HtmlUtils.htmlEscape(fid))));
    }

    @Operation(
        description = "Oppdater sluttdato for en OrgEnhetLeder manuelt",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "success"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Not found")
        }
    )
    @PutMapping("/oppdaterManuellOrgEnhetLederSluttdato")
    public ResponseEntity<String> oppdaterManuellOrgEnhetLederSluttdato(
        @Parameter(description = "FID for leder i OrgEnhet hvor sluttdato skal oppdateres") @RequestParam String fid,
        @Parameter(description = "Sluttdato for leder i OrgEnhet") @RequestParam(required = false) LocalDate sluttdato,
        @Parameter(description = "Skriv CONFIRM for å bekrefte!") @RequestParam String confirm
    ) {
        if (!validFid(fid)) return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();

        if (!"CONFIRM".equals(confirm)) return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();

        return orgEnhetLederStateRepository.findByFid(fid).map(
            orgEnhetLederState -> {
                var inputMetadata = InputMetadata.builder()
                    .inputInterfaceName("/api/admin/manuelleEvtsrcMeldinger")
                    .inputInterfaceMethodName("oppdaterManuellOrgEnhetLederSluttdato")
                    .inputInterfaceMethodArguments(fid)
                    .origination(new Origination(
                        SecurityUtil.getAuthenticatedNavIdent(),
                        "Admin swagger/rest"
                    )).build();

                orgEnhetLederCommandService.oppdaterOrgEnhetLederSluttdato(
                    fid,
                    OppdaterOrgEnhetLederSluttdato.builder()
                        .sluttdato(sluttdato)
                        .inputMetadata(inputMetadata)
                        .build()
                );
                return ResponseEntity.ok().body("Sluttdato til FID %s ble oppdatert til %s".formatted(fid, sluttdato));
            }).orElse(ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body("Fant ikke OrgEnhetLederState med FID %s".formatted(HtmlUtils.htmlEscape(fid))));
    }

    @Operation(
        description = "Slett OrgEnhetLeder manuelt",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "success"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden"),
            @ApiResponse(responseCode = "404", description = "Not found")
        }
    )
    @DeleteMapping("/slettManuelleOrgEnhetLederState")
    public ResponseEntity<String> slettManuelleOrgEnhetLederState(
        @Parameter(description = "FID for leder som skal slettes") @RequestParam String fid,
        @Parameter(description = "Skriv CONFIRM for å bekrefte!") @RequestParam String confirm
    ) {
        if (!validFid(fid)) return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();

        if (!"CONFIRM".equals(confirm)) return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();

        return orgEnhetLederStateRepository.findByFid(fid).map(
            orgEnhetLederState -> {
                var inputMetadata = InputMetadata.builder()
                    .inputInterfaceName("/api/admin/manuelleEvtsrcMeldinger")
                    .inputInterfaceMethodName("slettManuelleOrgEnhetLederState")
                    .inputInterfaceMethodArguments(fid)
                    .origination(new Origination(
                        SecurityUtil.getAuthenticatedNavIdent(),
                        "Admin swagger/rest"
                    )).build();

                orgEnhetLederCommandService.slettOrgEnhetLederState(
                    SlettOrgEnhetLeder.builder()
                        .fid(fid)
                        .inputMetadata(inputMetadata)
                        .build()
                );
                return ResponseEntity.ok().body("OrgEnhetLederState for orgenhet %s og navident %s for perioden %s til %s er slettet"
                    .formatted(fid, orgEnhetLederState.navident(), orgEnhetLederState.startdato(), orgEnhetLederState.sluttdato()));
            }
        ).orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body("Fant ikke OrgEnhetLederState med FID %s".formatted(HtmlUtils.htmlEscape(fid))));
    }

    private boolean validFid(String fid) {
        return fid.matches("[a-zA-Z0-9\\-]+");
    }
}
