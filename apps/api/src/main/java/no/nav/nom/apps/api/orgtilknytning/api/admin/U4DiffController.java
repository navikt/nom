package no.nav.nom.apps.api.orgtilknytning.api.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.common.SwaggerConfig;
import no.nav.nom.apps.api.orgtilknytning.u4diff.U4Diff;
import no.nav.nom.apps.api.orgtilknytning.u4diff.U4DiffService;
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

    @Operation(
        description = "List differanser for navidenter uten stillingperioder i U4",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @GetMapping("/identerUtenStillingIU4")
    public ResponseEntity<List<U4Diff>> OrgtilknytningerUtenforHierarki() {
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
    public void OrgtilknytningerUtenforHierarkiExcel(HttpServletResponse response) {
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=identerUtenStillingIU4.xlsx");
        u4DiffExcelGenerator.generate(response, u4DiffService.getStatligeIkkeIU4());
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
