package no.nav.nom.apps.admin.sanitycheck.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.nom.apps.admin.SwaggerConfig;
import no.nav.nom.apps.admin.dto.ressurs.RessursHendelseKafkaDTO;
import no.nav.nom.apps.admin.sanitycheck.service.RessursHendelseSanityCheckService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.http.ResponseEntity.ok;

@Tag(name = "Sanity checks for Ressurs-hendelser")
@RestController
@RequestMapping("SanityCheck/ressurshendelser")
@PreAuthorize("hasAuthority('ADMIN')")
public class RessursHendelseSanityCheckController {

    private final RessursHendelseSanityCheckService service;

    public RessursHendelseSanityCheckController(RessursHendelseSanityCheckService service) {
        this.service = service;
    }

    @Operation(
        description = "Finn duplikate hendelser",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Success")
        }
    )
    @GetMapping(value = "/duplikater")
    public ResponseEntity<List<RessursHendelseKafkaDTO>> dataSanityCheckRessurser() {
        return ok(service.finnDuplikateHendelser());
    }

}
