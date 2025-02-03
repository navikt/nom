package no.nav.nom.apps.api.u4diff.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.common.SwaggerConfig;
import no.nav.nom.apps.api.u4diff.U4Diff;
import no.nav.nom.apps.api.u4diff.U4DiffService;
import no.nav.nom.apps.api.rest.common.StandardResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

import static no.nav.nom.apps.api.u4diff.api.U4DiffExcelGenerator.ExportType.ALLE_DIFFERANSER;
import static no.nav.nom.apps.api.u4diff.api.U4DiffExcelGenerator.ExportType.IDENTER_IKKE_I_U4;
import static no.nav.nom.apps.api.u4diff.api.U4DiffExcelGenerator.ExportType.IKKE_KLASSIFISERTE_DIFFERANSER;
import static no.nav.nom.apps.api.u4diff.api.U4DiffExcelGenerator.ExportType.SEKTOR_DIFF_PAA_RESSURS;
import static no.nav.nom.apps.api.u4diff.api.U4DiffExcelGenerator.ExportType.ULIK_ENHET;

@RestController
@Tag(name = "Admin for U4 diff mot NOM")
@PreAuthorize("hasAuthority('ADMIN')")
@RequestMapping("/api/admin/u4diff")
@Slf4j
public class U4DiffController {
    private final U4DiffService u4DiffService;
    private final U4DiffExcelGenerator u4DiffExcelGenerator;

    public U4DiffController(U4DiffService u4DiffService, U4DiffExcelGenerator u4DiffExcelGenerator) {
        this.u4DiffService = u4DiffService;
        this.u4DiffExcelGenerator = u4DiffExcelGenerator;
    }

    // IDENTER_IKKE_I_U4

    @Operation(
        description = "List differanser for navidenter uten stillingperioder i U4",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @GetMapping("/identerUtenStillingIU4")
    public ResponseEntity<List<U4Diff>> identerUtenStillingIU4() {
        return ResponseEntity.ok(u4DiffService.getStatligeIkkeIU4());
    }

    @Operation(
        description = "Generer Excel for differanser for navidenter uten stillingperioder i U4",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Excel generert"),
    })
    @GetMapping("/identerUtenStillingIU4/excel")
    public void identerUtenStillingIU4Excel(HttpServletResponse response) {
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=identerUtenStillingIU4.xlsx");
        u4DiffExcelGenerator.generate(response, u4DiffService.getStatligeIkkeIU4(), IDENTER_IKKE_I_U4);
    }


    // SEKTOR_DIFF_PAA_RESSURS

    @Operation(
        description = "List differanser i sektor på Ressurs mellom NOM og U4",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @GetMapping("/sektorDifferansePaaRessurs")
    public ResponseEntity<List<U4Diff>> sektorDifferansePaaRessurs() {
        return ResponseEntity.ok(u4DiffService.getSektorDifferanse());
    }

    @Operation(
        description = "Generer Excel for differanser i sektor på Ressurs mellom NOM og U4",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Excel generert"),
    })
    @GetMapping("/sektorDifferansePaaRessurs/excel")
    public void sektorDifferansePaaRessursExcel(HttpServletResponse response) {
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=sektorDifferanse.xlsx");
        u4DiffExcelGenerator.generate(response, u4DiffService.getSektorDifferanse(), SEKTOR_DIFF_PAA_RESSURS);
    }

    // Ulik enhet i NOM og U4

    @Operation(
        description = "List differanser i enhet mellom NOM og U4",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @GetMapping("/ulikEnhet")
    public ResponseEntity<List<U4Diff>> ulikEnhet() {
        return ResponseEntity.ok(u4DiffService.getUlikEnhet());
    }

    @Operation(
        description = "Generer Excel for differanser i enhet mellom NOM og U4",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Excel generert"),
    })
    @GetMapping("/ulikEnhet/excel")
    public void ulikEnhetExcel(HttpServletResponse response) {
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=ulikEnhet.xlsx");
        u4DiffExcelGenerator.generate(response, u4DiffService.getUlikEnhet(), ULIK_ENHET);
    }

    // Ikke-klassifiserte differanser

    @Operation(
        description = "List alle ikke-klasssifiserte differanser mellom NOM og U4",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @GetMapping("/ikkeKlassifiserteDifferanser")
    public ResponseEntity<List<U4Diff>> ikkeKlassifiserteDifferanser() {
        return ResponseEntity.ok(u4DiffService.getIkkeKlassifiserteDifferanser());
    }

    @Operation(
        description = "Generer Excel for alle  ikke-klasssifiserte differanser mellom NOM og U4",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Excel generert"),
    })
    @GetMapping("/ikkeKlassifiserteDifferanser/excel")
    public void ikkeKlassifiserteDifferanserExcel(HttpServletResponse response) {
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=ikkeKlassifiserteDifferanser.xlsx");
        u4DiffExcelGenerator.generate(response, u4DiffService.getIkkeKlassifiserteDifferanser(), IKKE_KLASSIFISERTE_DIFFERANSER);
    }

    // Alle differanser

    @Operation(
        description = "List alle differanser mellom NOM og U4",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @GetMapping("/alleDifferanser")
    public ResponseEntity<List<U4Diff>> alleDifferanser() {
        return ResponseEntity.ok(u4DiffService.getAlleDiffRader());
    }

    @Operation(
        description = "Generer Excel for alle differanser mellom NOM og U4",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Excel generert"),
    })
    @GetMapping("/alleDifferanser/excel")
    public void alleDifferanserExcel(HttpServletResponse response) {
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=alleDifferanser.xlsx");
        u4DiffExcelGenerator.generate(response, u4DiffService.getAlleDiffRader(), ALLE_DIFFERANSER);
    }

    @ExceptionHandler({
        IOException.class
    })
    public ResponseEntity<StandardResponse> handleIOException(Exception e) {
        log.error("Feil ved generering av Excel", e);
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(StandardResponse.builder()
                .message("Feil ved generering av Excel")
                .build());
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
                .build());
    }
}
