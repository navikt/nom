package no.nav.nom.apps.admin.sanitycheck.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.admin.SanityCheckMappingUtils;
import no.nav.nom.apps.admin.SwaggerConfig;
import no.nav.nom.apps.admin.sanitycheck.dto.orgtilknytning.DuplikatOrgtilknytning;
import no.nav.nom.apps.admin.sanitycheck.dto.orgtilknytning.OrgtilknytningMedOrgEnhet;
import no.nav.nom.apps.admin.sanitycheck.dto.orgtilknytning.OverlappendeOrgtilknytning;
import no.nav.nom.apps.admin.sanitycheck.service.OrgtilknytningSanityCheckService;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.libs.utils.common.excel.ExcelGenerator;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Tag(name = "Admin for å kjøre sanity check på Orgtilknytninger")
@Slf4j
@RestController
@RequestMapping("SanityCheck/orgtilknytning")
@PreAuthorize("hasAuthority('ADMIN')")
public class OrgtilknytningSanityCheckController {

    private final OrgtilknytningSanityCheckService orgtilknytningSanityCheckService;
    private final ExcelGenerator excelGenerator;

    public OrgtilknytningSanityCheckController(
        OrgtilknytningSanityCheckService orgtilknytningSanityCheckService
    ) {
        this.orgtilknytningSanityCheckService = orgtilknytningSanityCheckService;
        this.excelGenerator = new ExcelGenerator();
    }

    @Operation(
        description = "List tilknytninger med sluttdato på først dag i måneden",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Success")
        }
    )
    @GetMapping(value = "/listTilkoblingerSomharTomDatoForstDagImaneden")
    public ResponseEntity<List<OrgtilknytningState>> listTilkoblingerSomharTomDatoFoorstDagImaneden() {
        return ResponseEntity.ok(orgtilknytningSanityCheckService.listTilkoblingerSomharTomDatoFoorstDagImaneden());
    }


    @Operation(
        description = "List tilknytninger med gyldighet utover enhetens gyldighetsperiode",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Success")
        }
    )
    @GetMapping(value = "/listTilkoblingerUtoverOrgEnhetGyldighetPeriode")
    public ResponseEntity<List<OrgtilknytningMedOrgEnhet>> listTilkoblingerUtoverOgEnhetGyldighetPeriode() {
        return ResponseEntity.ok(orgtilknytningSanityCheckService.orgtilknytningerUtoverEnhetGyldighetsPeriode());
    }

    @Operation(
        description = "List orgtilknytninger med negativ periode (gyldigTom < gyldigFom)",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Success")
        }
    )
    @GetMapping(value = "/orgtilknytningNegativPeriode")
    public ResponseEntity<List<OrgtilknytningState>> orgtilknytningNegativPeriode() {
        return ResponseEntity.ok(orgtilknytningSanityCheckService.orgtilknytningNegativPeriode());
    }

    @Operation(
        description = "List orgtilknytninger med flere dagligOppfolging i current periode (gyldigFom < now < gyldigTom)",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Success")
        }
    )
    @GetMapping(value = "/orgtilknytningMedFlereAktivDagligOppfolging")
    public ResponseEntity<List<OrgtilknytningState>> orgtilknytningMedFlereAktivDagligOppfolging() {
        // Group by navident for current period and dagligOppfolging
        return ResponseEntity.ok(orgtilknytningSanityCheckService.orgtilknytningerMedFlereDOLSammePeriode());
    }

    @Operation(
        description = "List orgtilknytninger med feil kombinasjon av sektor og tilknytningstype",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Success")
        }
    )
    @GetMapping(value = "/orgtilknytningStatesMedUgyldigeSektorTilknytninger")
    public ResponseEntity<List<OrgtilknytningState>> orgtilknytningStatesMedUgyldigeSektorTilknytninger() {
        return ResponseEntity.ok(orgtilknytningSanityCheckService.orgtilknytningStatesMedUgyldigeSektorTilknytninger());
    }

    @GetMapping(value = "/orgtilknytningStatesMedTilknytningtypeUtenSektor")
    public ResponseEntity<List<OrgtilknytningState>> orgtilknytningStatesMedTilknytningtypeUtenSektor() {
        return ResponseEntity.ok(orgtilknytningSanityCheckService.orgtilknytningStatesMedTilknytningtypeUtenSektor());
    }

    @Operation(
        description = "List orgtilknytninger med flere overlappende dagligOppfolging i aktiv eller fremtidig periode (gyldigTom > now)",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Success")
        }
    )
    @GetMapping(value = "/orgtilknytningMedFlereOverlappendeDagligOppfolgingCurrentOgFuture")
    public ResponseEntity<List<OverlappendeOrgtilknytning>> orgtilknytningMedFlereOverlappendeDagligOppfolgingCurrentOgFuture() {
        return ResponseEntity.ok(orgtilknytningSanityCheckService.orgtilknytningMedFlereOverlappendeDagligOppfolgingCurrentOgFuture());
    }

    @Operation(
        description = "List duplikate orgtilknytninger på samme orgenhet og navident",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Success")
        }
    )
    @GetMapping(value = "/duplikateOrgtilknytninger")
    public ResponseEntity<List<DuplikatOrgtilknytning>> duplikateOrgtilknytninger() {
        return ResponseEntity.ok(orgtilknytningSanityCheckService.duplikateOrgtilknytninger());
    }

    @Operation(
        description = "Sammenligner fremtidige endringer i orgtilknytning med forsinkede hendelser",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Success")
        }
    )
    @GetMapping(value = "/delayedEventsComparison")
    public ResponseEntity<List<OrgtilknytningState>> delayedEventsComparison() {
        return ResponseEntity.ok(orgtilknytningSanityCheckService.compareFutureChangesWithDelayedEvents());
    }

    @Operation
        (
            description = "List orgtilknytninger med gyldigFom i fremtiden",
            security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
        )
    @ApiResponses
        (
            {
                @ApiResponse(responseCode = "200", description = "Success")
            }
        )
    @GetMapping(value = "/overlappendeHistoriskeDolOrgtilknytninger")
    public ResponseEntity<List<OverlappendeOrgtilknytning>> overlappendeHistoriskeDolOrgtilknytninger() {
        return ResponseEntity.ok(orgtilknytningSanityCheckService.orgtilknytningerMedOverlappendeHistoriskeDol());
    }

    @Operation(
        description = "Exporter tabell til Excel",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Success")
        }
    )
    @GetMapping(value = "/exportToExcel/{exportType}")
    public void exportToExcel(HttpServletResponse response, @PathVariable("exportType") String exportType) throws IOException {
        switch (exportType) {
            case "listTilkoblingerSomharTomDatoForstDagImaneden" -> exportTilkoblingerSomharTomDatoForstDagImaneden(response);
            case "orgtilknytningStatesMedUgyldigeSektorTilknytninger" -> exportOrgtilknytningStatesMedUgyldigeSektorTilknytninger(response);
            case "orgtilknytningStatesMedTilknytningtypeUtenSektor" -> exportOrgtilknytningStatesMedTilknytningtypeUtenSektor(response);
            case "overlappendeHistoriskeDolOrgtilknytninger" -> exportOverlappendeHistoriskeDolOrgtilknytninger(response);
            default -> response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid export type");
        }
    }

    private void exportTilkoblingerSomharTomDatoForstDagImaneden(HttpServletResponse response) throws IOException {
        try {
            var tilkoblingerSomharTomDatoForstDagImaneden = orgtilknytningSanityCheckService.listTilkoblingerSomharTomDatoFoorstDagImaneden();

            List<Map<String, Object>> tilkoblingerSomharTomDatoForstDagImanedenMapList = SanityCheckMappingUtils.orgtilknytningStateToMapList(
                tilkoblingerSomharTomDatoForstDagImaneden);
            var headers = tilkoblingerSomharTomDatoForstDagImanedenMapList.getFirst().keySet().stream().toList();
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=tilkoblingerMedTomDatoForsteDag.xlsx");
            excelGenerator.generateWorkbook(
                response,
                "tilkoblingerMedTomDatoForsteDag",
                headers,
                tilkoblingerSomharTomDatoForstDagImanedenMapList
            );
        } catch (Exception e) {
            log.error("Error generating Excel for tilkoblingerSomharTomDatoForstDagImaneden", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error generating Excel");
        }
    }

    private void exportOrgtilknytningStatesMedUgyldigeSektorTilknytninger(HttpServletResponse response) throws IOException {
        try {
            var orgtilknytningStatesMedUgyldigeSektorTilknytninger = orgtilknytningSanityCheckService.orgtilknytningStatesMedUgyldigeSektorTilknytninger();

            List<Map<String, Object>> orgtilknytningStatesMedUgyldigeSektorTilknytningerMapList = SanityCheckMappingUtils.orgtilknytningStateToMapList(
                orgtilknytningStatesMedUgyldigeSektorTilknytninger);
            var headers = orgtilknytningStatesMedUgyldigeSektorTilknytningerMapList.getFirst().keySet().stream().toList();
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=orgtilknytningerMedUgyldigeSektorTilknytninger.xlsx");
            excelGenerator.generateWorkbook(
                response,
                "orgtilknytningerMedUgyldigeSektorTilknytninger",
                headers,
                orgtilknytningStatesMedUgyldigeSektorTilknytningerMapList
            );
        } catch (Exception e) {
            log.error("Error generating Excel for orgtilknytningStatesMedUgyldigeSektorTilknytninger", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error generating Excel");
        }
    }

    private void exportOrgtilknytningStatesMedTilknytningtypeUtenSektor(HttpServletResponse response) throws IOException {
        try {
            var orgtilknytningStatesMedTilknytningtypeUtenSektor = orgtilknytningSanityCheckService.orgtilknytningStatesMedTilknytningtypeUtenSektor();

            List<Map<String, Object>> orgtilknytningStatesMedTilknytningtypeUtenSektorMapList = SanityCheckMappingUtils.orgtilknytningStateToMapList(
                orgtilknytningStatesMedTilknytningtypeUtenSektor);
            var headers = orgtilknytningStatesMedTilknytningtypeUtenSektorMapList.getFirst().keySet().stream().toList();
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=orgtilknytningerMedTilknytningtypeUtenSektor.xlsx");
            excelGenerator.generateWorkbook(
                response,
                "orgtilknytningerMedTilknytningtypeUtenSektor",
                headers,
                orgtilknytningStatesMedTilknytningtypeUtenSektorMapList
            );
        } catch (Exception e) {
            log.error("Error generating Excel for orgtilknytningStatesMedTilknytningtypeUtenSektor", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error generating Excel");
        }
    }

    private void exportOverlappendeHistoriskeDolOrgtilknytninger(HttpServletResponse response) throws IOException {
        try {
            var overlappendeHistoriskeDolOrgtilknytninger = orgtilknytningSanityCheckService.orgtilknytningerMedOverlappendeHistoriskeDol();

            List<Map<String, Object>> overlappendeHistoriskeDolOrgtilknytningerMapList = SanityCheckMappingUtils.overlappendeOrgtilknytningToMapList(
                overlappendeHistoriskeDolOrgtilknytninger);
            var headers = overlappendeHistoriskeDolOrgtilknytningerMapList.getFirst().keySet().stream().toList();
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=overlappendeHistoriskeDolOrgtilknytninger.xlsx");
            excelGenerator.generateWorkbook(
                response,
                "overlappendeHistoriskeDolOrgtilknytninger",
                headers,
                overlappendeHistoriskeDolOrgtilknytningerMapList
            );
        } catch (Exception e) {
            log.error("Error generating Excel for overlappendeHistoriskeDolOrgtilknytninger", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error generating Excel");
        }
    }
}
