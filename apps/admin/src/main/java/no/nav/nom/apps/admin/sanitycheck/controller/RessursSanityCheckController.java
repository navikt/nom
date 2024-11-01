package no.nav.nom.apps.admin.sanitycheck.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.nom.apps.admin.SwaggerConfig;
import no.nav.nom.apps.admin.sanitycheck.dto.ressurs.RessursMedEnhet;
import no.nav.nom.apps.admin.sanitycheck.dto.ressurs.RessursOrgtilknytningKoblingInfo;
import no.nav.nom.apps.admin.sanitycheck.service.RessursSanityCheckService;
import no.nav.nom.contract.nom.state.RessursState;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Admin for å kjøre sanity check på Ressurs")
@RestController
@RequestMapping("SanityCheck/ressurs")
@PreAuthorize("hasAuthority('ADMIN')")
public class RessursSanityCheckController {

    private final RessursSanityCheckService ressursSanityCheckService;

    public RessursSanityCheckController(RessursSanityCheckService ressursSanityCheckService) {
        this.ressursSanityCheckService = ressursSanityCheckService;
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
        return ResponseEntity.ok(ressursSanityCheckService.aktiveRessurser());
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
        @RequestParam(defaultValue = "false") boolean inkluderHelfo) {
        return ResponseEntity.ok(ressursSanityCheckService.ressurserMedAktivOrgtilknytningTilIkkeAktivEnhet(inkluderEksterne, inkluderHelfo));
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
        return ResponseEntity.ok(ressursSanityCheckService.ressurserMedEnhetUtoverRessursSluttdato());
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
        return ResponseEntity.ok(ressursSanityCheckService.ressurserMedBlankMellomnavn());
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
        return ResponseEntity.ok(ressursSanityCheckService.finnDuplikateRessurserPaaSammeNavident());
    }

}
