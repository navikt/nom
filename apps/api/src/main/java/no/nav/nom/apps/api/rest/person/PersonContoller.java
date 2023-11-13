package no.nav.nom.apps.api.rest.person;

import io.getunleash.Unleash;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.common.SwaggerConfig;
import no.nav.nom.apps.api.pdl.PdlClient;
import no.nav.nom.apps.api.pdl.Person;
import no.nav.nom.apps.api.rest.dto.NavnDTO;
import no.nav.nom.apps.api.ressurs.api.v1.remedy.RemedyController;
import no.nav.nom.apps.api.ressurs.api.v1.remedy.RessursDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static no.nav.nom.apps.api.common.AppConfig.SECURE_LOGS;

@Tag(name = "Person")
@RestController
@RequestMapping("/api/v1/person")
@Slf4j
public class PersonContoller {

    private final RemedyController remedyController;

    public PersonContoller(RemedyController remedyController) {
        this.remedyController = remedyController;
    }

    @PostMapping("/getPerson")
    @Operation(
            description = "Personoppslag",
            security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Success")
    })
    @PreAuthorize("hasAuthority('QUERY_PERSON')")
    public ResponseEntity<PdlPersonDTO> getPerson(@RequestBody PdlPersonRequest request) {
        log.info(SECURE_LOGS, "getPerson kalt fra Remedy:\n{}", request);
        return  ResponseEntity.ok(convert(remedyController.finnRessurs(request.getPersonident())));

    }

    private PdlPersonDTO convert(RessursDTO ressursDTO) {
        if (ressursDTO == null) return null;
        return PdlPersonDTO.builder()
                .navn(NavnDTO.builder()
                        .fornavn(ressursDTO.getNavn().getFornavn())
                        .mellomnavn(ressursDTO.getNavn().getMellomnavn())
                        .etternavn(ressursDTO.getNavn().getEtternavn())
                        .build())
                .build();
    }

}
