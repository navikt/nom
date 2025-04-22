package no.nav.nom.apps.admin.sanitycheck.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.nom.apps.admin.SwaggerConfig;
import no.nav.nom.apps.admin.sanitycheck.dto.ressurs.*;
import no.nav.nom.apps.admin.sanitycheck.service.RessursSanityCheckService;
import no.nav.nom.contract.nom.state.RessursState;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.http.ResponseEntity.ok;

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
        @RequestParam(defaultValue = "false") boolean inkluderHelfo) {
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
    public ResponseEntity<List<RobotRessursSanityCheckResponse>> listRobotRessurser() {
        return ok(ressursSanityCheckService.robotRessurser());
    }
    
}
