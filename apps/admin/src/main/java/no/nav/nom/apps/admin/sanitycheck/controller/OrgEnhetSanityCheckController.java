package no.nav.nom.apps.admin.sanitycheck.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.admin.SanityCheckMappingUtils;
import no.nav.nom.apps.admin.SwaggerConfig;
import no.nav.nom.apps.admin.sanitycheck.dto.orgenhet.EnheterOgOrganisering;
import no.nav.nom.apps.admin.sanitycheck.dto.orgenhet.OrgEnhetArbeidsomraadeInfo;
import no.nav.nom.apps.admin.sanitycheck.dto.orgenhet.OrgEnhetOrganisering;
import no.nav.nom.apps.admin.sanitycheck.dto.orgenhet.OrgEnhetStartOgSluttdato;
import no.nav.nom.apps.admin.sanitycheck.service.OrgEnhetSanityCheckService;
import no.nav.nom.contract.nom.state.OrgEnhetState;
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

@Tag(name = "Admin for å kjøre sanity check på OrgEnhet")
@Slf4j
@RestController
@RequestMapping("SanityCheck/orgenhet")
@PreAuthorize("hasAuthority('ADMIN')")
public class OrgEnhetSanityCheckController {

    private final OrgEnhetSanityCheckService orgEnhetSanityCheckService;
    private final ExcelGenerator excelGenerator;

    public OrgEnhetSanityCheckController(OrgEnhetSanityCheckService orgEnhetSanityCheckService) {
        this.orgEnhetSanityCheckService = orgEnhetSanityCheckService;
        this.excelGenerator = new  ExcelGenerator();
    }


    @Operation(
        description = "Sjekk enheter antall og dato",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Success")
        }
    )
    @GetMapping(value = "/enheter")
    public ResponseEntity<OrgEnhetStartOgSluttdato> dataSanityCheckEnheter() {
        return ResponseEntity.ok(orgEnhetSanityCheckService.sjekkOrgEnhetStartSluttDato());
    }

    @Operation(
        description = "Sjekk aktive enheter og organiseringer",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Success")
        }
    )
    @GetMapping(value = "/enheterOgOrganisering")
    public ResponseEntity<EnheterOgOrganisering> dataSanityCheckEnheterOgOrganisering() {
        return ResponseEntity.ok(orgEnhetSanityCheckService.sjekkAktiveEnheterOgOrganisering());
    }

    @Operation(
        description = "List aktive enheter Uten over-enhet",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Success")
        }
    )
    @GetMapping(value = "/listEnheterUtenOverEnheter")
    public ResponseEntity<List<OrgEnhetOrganisering>> listEnheterUtenOverEnheter() {
        return ResponseEntity.ok(orgEnhetSanityCheckService.enhetUtenOverEnhet());
    }

    @Operation(
        description = "List aktive enheter Uten over-enhet men med under-enheter",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Success")
        }
    )
    @GetMapping(value = "/listEnheterUtenOverEnheterMedUnderEnheter")
    public ResponseEntity<List<OrgEnhetOrganisering>> listEnheterUtenOverEnheterMedUnderEnheter() {
        return ResponseEntity.ok(orgEnhetSanityCheckService.enhetUtenOverEnhetMedUnderEnhet());
    }

    @Operation(
        description = "List aktive enheter som er duplikat på samme unique agresso id ('agressoId+Navn+orgNiv')",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Success")
        }
    )
    @GetMapping(value = "/listDuplikateEnheter")
    public ResponseEntity<List<OrgEnhetState>> listDuplikateEnheter() {
        return ResponseEntity.ok(orgEnhetSanityCheckService.finnDuplikateEnheter());
    }

    @Operation(
        description = "List bunn enheter uten eksakt 1 arbeidsområde i hierarkiet",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Success")
        }
    )
    @GetMapping(value = "/bunnEnheterSomIkkeHarEksakt1ArbeidsomradeIOrghierarkietStier")
    public ResponseEntity<List<OrgEnhetOrganisering>> bunnEnheterSomIkkeHarEksakt1ArbeidsomradeIOrghierarkietStier() {
        return ResponseEntity.ok(orgEnhetSanityCheckService.bunnEnheterSomIkkeHarEksakt1ArbeidsomradeIOrghierarkietStier());
    }

    @Operation(
        description = "Valider angivelse av ARBEIDSOMRÅDE på enheter",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Success")
        }
    )
    @GetMapping(value = "/validerArbeidsomraade")
    public ResponseEntity<OrgEnhetArbeidsomraadeInfo> validerArbeidsomraade() {
        return ResponseEntity.ok(orgEnhetSanityCheckService.validerArbeidsomraade());
    }

    @Operation(
        description = "List orgenheter med aktive deliveringer som ligger utenfor aktiv organisasjonshierarki",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Success")
        }
    )
    @GetMapping("/orgenheterMedAktiveDeliveringerSomLiggerUtenforHierarki")
    public ResponseEntity<List<OrgEnhetOrganisering>> orgenheterMedAktiveDeliveringerSomLiggerUtenforHierarki() {
        return ResponseEntity.ok(orgEnhetSanityCheckService.getOrgenheterMedAktiveDeliveringerSomLiggerUtenforHierarki());
    }


    @Operation(
        description = "Exporter valgt Sanity Check til Excel",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Exported file")
        }
    )
    @GetMapping("/exportToExcel/{exportType}")
    public void exportToExcel(HttpServletResponse response, @PathVariable String exportType) throws IOException {
        switch (exportType) {
            case "enheterUtenOverEnheter" -> exportEnheterUtenOverEnheter(response);
            case "enheterUtenOverEnheterMedUnderEnheter" -> exportEnheterUtenOverEnheterMedUnderEnheter(response);
            case "duplikateEnheter" -> exportDuplikateEnheter(response);
            case "bunnEnheterSomIkkeHarEksakt1ArbeidsomradeIOrghierarkietStier" -> exportBunnEnheterSomIkkeHarEksakt1ArbeidsomradeIOrghierarkietStier(response);
            case "validerArbeidsomraade" -> exportValiderArbeidsomraade(response);
            default -> response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid export type");
        }
    }

    @Operation(
        description = "List orgenher i NAV orghierarkiet som ikke har leder",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Success")
        }
    )
    @GetMapping(value = "/listOrgenheterUtenLeder")
    public ResponseEntity<List<OrgEnhetOrganisering>> listOrgenheterUtenLeder() {
        return ResponseEntity.ok(orgEnhetSanityCheckService.listOrgenheterUtenLeder());
    }

    private void exportEnheterUtenOverEnheter(HttpServletResponse response) throws IOException {
        try {
            var orgEnheterUtenOverEnhet = orgEnhetSanityCheckService.enhetUtenOverEnhet();

            List<Map<String, Object>> enheterUtenOverEnheterMap = SanityCheckMappingUtils.orgEnhetOrganiseringToMapList(orgEnheterUtenOverEnhet);
            var headers = enheterUtenOverEnheterMap.getFirst().keySet().stream().toList();
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=orgenhetUtenOverEnhet.xlsx");
            excelGenerator.generateWorkbook(response, "orgenhetUtenOverEnhet", headers, enheterUtenOverEnheterMap);
        } catch (Exception e) {
            log.error("Failed to generate Excel file", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to generate Excel file");
        }
    }

    private void exportEnheterUtenOverEnheterMedUnderEnheter(HttpServletResponse response) throws IOException {
        try {
            var orgEnheterUtenOverEnhetMedUnderEnhet = orgEnhetSanityCheckService.enhetUtenOverEnhetMedUnderEnhet();
            List<Map<String, Object>> enheterUtenOverEnheterMedUnderEnheterMap = SanityCheckMappingUtils.orgEnhetOrganiseringToMapList(orgEnheterUtenOverEnhetMedUnderEnhet);
            var headers = enheterUtenOverEnheterMedUnderEnheterMap.getFirst().keySet().stream().toList();
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=orgenhetUtenOverEnhetMedUnderEnhet.xlsx");
            excelGenerator.generateWorkbook(response, "orgenhetUtenOverEnhetMedUnderEnhet", headers, enheterUtenOverEnheterMedUnderEnheterMap);
        } catch (Exception e) {
            log.error("Failed to generate Excel file", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to generate Excel file");
        }
    }

    private void exportDuplikateEnheter(HttpServletResponse response) throws IOException {
        try {
            var duplikateEnheter = orgEnhetSanityCheckService.finnDuplikateEnheter();
            List<Map<String, Object>> duplikateEnheterMap = SanityCheckMappingUtils.orgEnhetStateToMapList(duplikateEnheter);
            var headers = duplikateEnheterMap.getFirst().keySet().stream().toList();
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=duplikateEnheter.xlsx");
            excelGenerator.generateWorkbook(response, "duplikateEnheter", headers, duplikateEnheterMap);
        } catch (Exception e) {
            log.error("Failed to generate Excel file", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to generate Excel file");
        }
    }

    private void exportBunnEnheterSomIkkeHarEksakt1ArbeidsomradeIOrghierarkietStier(HttpServletResponse response) throws IOException {
        try {
            var bunnEnheterSomIkkeHarEksakt1ArbeidsomradeIOrghierarkietStier = orgEnhetSanityCheckService.bunnEnheterSomIkkeHarEksakt1ArbeidsomradeIOrghierarkietStier();
            List<Map<String, Object>> bunnEnheterSomIkkeHarEksakt1ArbeidsomradeIOrghierarkietStierMap = SanityCheckMappingUtils.orgEnhetOrganiseringToMapList(bunnEnheterSomIkkeHarEksakt1ArbeidsomradeIOrghierarkietStier);
            var headers = bunnEnheterSomIkkeHarEksakt1ArbeidsomradeIOrghierarkietStierMap.getFirst().keySet().stream().toList();
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=bunnEnheterSomIkkeHarEksakt1ArbeidsomradeIOrghierarkietStier.xlsx");
            excelGenerator.generateWorkbook(response, "bunnEnheterSomIkkeHarEksakt1ArbeidsomradeIOrghierarkietStier", headers, bunnEnheterSomIkkeHarEksakt1ArbeidsomradeIOrghierarkietStierMap);
        } catch (Exception e) {
            log.error("Failed to generate Excel file", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to generate Excel file");
        }
    }

    private void exportValiderArbeidsomraade(HttpServletResponse response) throws IOException {
        try {
            var validerArbeidsomraade = orgEnhetSanityCheckService.validerArbeidsomraade();
            List<Map<String, Object>> validerArbeidsomraadeMap = SanityCheckMappingUtils.orgEnhetArbeidsomraadeInfoToMapList(validerArbeidsomraade.loevnoderMedFeil());
            var headers = validerArbeidsomraadeMap.getFirst().keySet().stream().toList();
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=validerArbeidsomraade.xlsx");
            excelGenerator.generateWorkbook(response, "validerArbeidsomraade", headers, validerArbeidsomraadeMap);
        } catch (Exception e) {
            log.error("Failed to generate Excel file", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to generate Excel file");
        }
    }
}
