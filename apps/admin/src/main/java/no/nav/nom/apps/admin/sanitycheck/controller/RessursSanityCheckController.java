package no.nav.nom.apps.admin.sanitycheck.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.admin.SanityCheckMappingUtils;
import no.nav.nom.apps.admin.SwaggerConfig;
import no.nav.nom.apps.admin.sanitycheck.StandardResponse;
import no.nav.nom.apps.admin.sanitycheck.dto.ressurs.RessursSomHarSluttetOgErLederForEnhetSanityCheckResponse;
import no.nav.nom.apps.admin.sanitycheck.dto.ressurs.RessursMedEnhet;
import no.nav.nom.apps.admin.sanitycheck.dto.ressurs.RessursMedFlereDOL;
import no.nav.nom.apps.admin.sanitycheck.dto.ressurs.RessursOrgtilknytningKoblingInfo;
import no.nav.nom.apps.admin.sanitycheck.dto.ressurs.RessursOrgtilknytningUlikSluttdato;
import no.nav.nom.apps.admin.sanitycheck.dto.ressurs.RobotRessurs;
import no.nav.nom.apps.admin.sanitycheck.service.RessursSanityCheckService;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.utils.common.excel.ExcelGenerator;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.springframework.http.ResponseEntity.ok;

@Tag(name = "Admin for å kjøre sanity check på Ressurs")
@Slf4j
@RestController
@RequestMapping("SanityCheck/ressurs")
@PreAuthorize("hasAuthority('ADMIN')")
public class RessursSanityCheckController {

    private final RessursSanityCheckService ressursSanityCheckService;
    private final ExcelGenerator excelGenerator;

    public RessursSanityCheckController(RessursSanityCheckService ressursSanityCheckService) {
        this.ressursSanityCheckService = ressursSanityCheckService;
        this.excelGenerator = new ExcelGenerator();
    }

    @Operation(
        description = "Sjekk aktive ressurser",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Success")
        }
    )
    @GetMapping(value = "/ressurser")
    public ResponseEntity<RessursOrgtilknytningKoblingInfo> dataSanityCheckRessurser() {
        return ok(ressursSanityCheckService.aktiveRessurser());
    }

    @Operation(
        description = "List ressurser med aktive orgtilknytninger for enhet om er inaktiv eller uten organisering",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Success")
        }
    )
    @GetMapping(value = "/listRessurserTilIkkeAktivEnhet")
    public ResponseEntity<List<RessursMedEnhet>> listRessurserTilIkkeAktivEnhet(
        @RequestParam(defaultValue = "false") boolean inkluderEksterne,
        @RequestParam(defaultValue = "false") boolean inkluderHelfo
    ) {
        return ok(ressursSanityCheckService.ressurserMedAktivOrgtilknytningTilIkkeAktivEnhet(inkluderEksterne, inkluderHelfo));
    }

    @Operation(
        description = "List tilknytninger med gyldighet utover ressurens gyldighetsperiode",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Success")
        }
    )
    @GetMapping(value = "/listTilkoblingerUtoverRessursGyldighetPeriode")
    public ResponseEntity<List<RessursMedEnhet>> listTilkoblingerUtoverRessursGyldighetPeriode() {
        return ok(ressursSanityCheckService.ressurserMedEnhetUtoverRessursSluttdato());
    }

    @Operation(
        description = "List ressurser med blank mellomnavn",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Success")
        }
    )
    @GetMapping(value = "/ressursMedBlankMellomNavn")
    public ResponseEntity<List<RessursState>> listRessurserMedBlankMellomNavn() {
        return ok(ressursSanityCheckService.ressurserMedBlankMellomnavn());
    }

    @Operation(
        description = "List duplikate ressurser på samme navident",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Success")
        }
    )
    @GetMapping(value = "/duplikateRessurserPaaNavident")
    public ResponseEntity<List<RessursState>> duplikateRessurserPaaNavident() {
        return ok(ressursSanityCheckService.finnDuplikateRessurserPaaSammeNavident());
    }

    @Operation(
        description = "Valider ressurser som har en ulik sluttdato på organisasjonstilknytning og ressurs sluttdato",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        @ApiResponse(responseCode = "200", description = "Success")
    )
    @GetMapping(value = "/ressursSluttdatoUlikOrgTilknytningSluttdato")
    public ResponseEntity<List<RessursOrgtilknytningUlikSluttdato>> ressurserMedUlikOrgTilknytningSluttDato() {
        return ok(ressursSanityCheckService.ressurserMedUlikOrgTilknytningSluttDato());
    }

    @Operation(
        description = "Valider ressurser som har flere enn en DOL",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        @ApiResponse(responseCode = "200", description = "Success")
    )
    @GetMapping(value = "ressursMedFlereEnnEnDOL")
    public ResponseEntity<List<RessursMedFlereDOL>> ressursMedFlereEnnEnDOL() {
        return ok(ressursSanityCheckService.ressursMedFlereEnnEnDOL());
    }

    @Operation(
        description = "Henter aktive ressurser uten orgtilknytning eller med orgtilknytning som ikke er aktiv",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        @ApiResponse(responseCode = "200", description = "Success")
    )
    @GetMapping("/ressursUtenOrgtilknytning")
    public ResponseEntity<List<RessursState>> ressursUtenOrgtilknytning() {
        return ok(ressursSanityCheckService.aktiveRessurserUtenOrgtilknytning());
    }

    @Operation(
        description = "List robot ressurser",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        @ApiResponse(responseCode = "200", description = "Success")
    )
    @GetMapping("/robotRessurser")
    public ResponseEntity<List<RobotRessurs>> listRobotRessurser() {
        return ok(ressursSanityCheckService.robotRessurser());
    }

    @Operation(
        description = "Sammenligner fremtidige endringer i ressurser med forsinkede hendelser",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Success")
        }
    )
    @GetMapping(value = "/delayedEventsComparison")
    public ResponseEntity<List<RessursState>> delayedEventsComparison() {
        return ok(ressursSanityCheckService.compareFutureChangedWithDelayEvents());
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
            case "listRessurserTilIkkeAktivEnhet" -> exportRessurserTilIkkeAktivEnhet(response);
            case "listTilkoblingerUtoverRessursGyldighetPeriode" -> exportTilkoblingerUtoverRessursGyldighetPeriode(response);
            case "ressursMedBlankMellomnavn" -> exportRessursMedBlankMellomNavn(response);
            case "ressursSluttdatoUlikOrgTilknytningSluttdato" -> exportRessursSluttdatoUlikOrgTilknytningSluttdato(response);
            case "ressursUtenOrgtilknytning" -> exportRessursUtenOrgtilknytning(response);
            default -> response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid export type");
        }
    }

    @Operation(
        description = "Valider ressurs som har sluttet, men står som leder på orgEnhet",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Success")
        }
    )
    @GetMapping(value = "/sluttetRessursMedLederRollePaaOrgEnhet")
    public ResponseEntity<List<RessursSomHarSluttetOgErLederForEnhetSanityCheckResponse>> validerRessursMedLederRollePaaOrgEnhet() {
        return ok(ressursSanityCheckService.validerRessursMedLederRollePaaOrgEnhet());
    }

    private void exportRessurserTilIkkeAktivEnhet(HttpServletResponse response) throws IOException {
        try {
            var ressurserMedIkkeAktivEnhet = ressursSanityCheckService.ressurserMedAktivOrgtilknytningTilIkkeAktivEnhet(true, true);

            List<Map<String, Object>> ressurserMedIkkeAktivEnhetMap = SanityCheckMappingUtils.ressursMedEnhetToMapList(ressurserMedIkkeAktivEnhet);
            var headers = ressurserMedIkkeAktivEnhetMap.getFirst().keySet().stream().toList();
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=ressurserMedIkkeAktivEnhet.xlsx");
            excelGenerator.generateWorkbook(response, "ressurserMedIkkeAktivEnhet", headers, ressurserMedIkkeAktivEnhetMap);
        } catch (Exception e) {
            log.error("Error generating excel", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error generating excel");
        }
    }

    private void exportTilkoblingerUtoverRessursGyldighetPeriode(HttpServletResponse response) throws IOException {
        try {
            var tilkoblingerUtoverRessursGyldighetPeriode = ressursSanityCheckService.ressurserMedEnhetUtoverRessursSluttdato();

            List<Map<String, Object>> tilkoblingerUtoverRessursGyldighetPeriodeMap = SanityCheckMappingUtils.ressursMedEnhetToMapList(
                tilkoblingerUtoverRessursGyldighetPeriode);
            var headers = tilkoblingerUtoverRessursGyldighetPeriodeMap.getFirst().keySet().stream().toList();
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=tilkoblingerUtoverRessursGyldighetPeriode.xlsx");
            excelGenerator.generateWorkbook(response, "tilkoblingerUtoverRessursGyldighetPeriode", headers, tilkoblingerUtoverRessursGyldighetPeriodeMap);
        } catch (Exception e) {
            log.error("Error generating excel", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error generating excel");
        }
    }

    private void exportRessursMedBlankMellomNavn(HttpServletResponse response) throws IOException {
        try {
            var ressursMedBlankMellomNavn = ressursSanityCheckService.ressurserMedBlankMellomnavn();

            List<Map<String, Object>> ressursMedBlankMellomNavnMap = SanityCheckMappingUtils.ressursStateToMapList(ressursMedBlankMellomNavn);
            var headers = ressursMedBlankMellomNavnMap.getFirst().keySet().stream().toList();
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=ressursMedBlankMellomnavn.xlsx");
            excelGenerator.generateWorkbook(response, "ressursMedBlankMellomnavn", headers, ressursMedBlankMellomNavnMap);
        } catch (Exception e) {
            log.error("Error generating excel", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error generating excel");
        }
    }

    private void exportRessursSluttdatoUlikOrgTilknytningSluttdato(HttpServletResponse response) throws IOException {
        try {
            var ressursSluttdatoUlikOrgTilknytningSluttdato = ressursSanityCheckService.ressurserMedUlikOrgTilknytningSluttDato();

            List<Map<String, Object>> ressursSluttdatoUlikOrgTilknytningSluttdatoMap = SanityCheckMappingUtils.ressursOrgtilknytningUlikSluttdatoToMapList(
                ressursSluttdatoUlikOrgTilknytningSluttdato);
            var headers = ressursSluttdatoUlikOrgTilknytningSluttdatoMap.getFirst().keySet().stream().toList();
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=ressursSluttdatoUlikOrgTilknytningSluttdato.xlsx");
            excelGenerator.generateWorkbook(
                response,
                "ressursSluttdatoUlikOrgTilknytningSluttdato",
                headers,
                ressursSluttdatoUlikOrgTilknytningSluttdatoMap
            );
        } catch (Exception e) {
            log.error("Error generating excel", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error generating excel");
        }
    }

    private void exportRessursUtenOrgtilknytning(HttpServletResponse response) throws IOException {
        try {
            var ressursUtenOrgtilknytning = ressursSanityCheckService.aktiveRessurserUtenOrgtilknytning();

            List<Map<String, Object>> ressursUtenOrgtilknytningMap = SanityCheckMappingUtils.ressursStateToMapList(ressursUtenOrgtilknytning);
            var headers = ressursUtenOrgtilknytningMap.getFirst().keySet().stream().toList();
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=ressursUtenOrgtilknytning.xlsx");
            excelGenerator.generateWorkbook(response, "ressursUtenOrgtilknytning", headers, ressursUtenOrgtilknytningMap);
        } catch (Exception e) {
            log.error("Error generating excel", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error generating excel");
        }
    }

    @ExceptionHandler({
        Exception.class
    })
    public ResponseEntity<StandardResponse> handleException(Exception e) {
        log.error("Ukjent feil", e);
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(StandardResponse.builder()
                .message("Ukjent feil")
                .exceptionMessage(e.getMessage())
                .build());
    }
}
