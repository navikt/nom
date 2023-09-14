package no.nav.nom.apps.api.rest.person;

import io.getunleash.Unleash;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.nom.apps.api.common.SwaggerConfig;
import no.nav.nom.apps.api.pdl.PdlClient;
import no.nav.nom.apps.api.pdl.Person;
import no.nav.nom.apps.api.rest.dto.NavnDTO;
import no.nav.nom.apps.api.rest.remedy.RemedyController;
import no.nav.nom.apps.api.rest.remedy.RessursDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Person")
@RestController
@RequestMapping("/api/v1/person")
public class PersonContoller {

    public static final String UNLEASH_REDIRECT_TIL_REMEDY_CONTROLLER = "nom.remedy_redirect_til_remedycontroller";
    private final Unleash unleash;
    private final RemedyController remedyController;
    private final PdlClient pdlClient;

    public PersonContoller(Unleash unleash, RemedyController remedyController, PdlClient pdlClient) {
        this.unleash = unleash;
        this.remedyController = remedyController;
        this.pdlClient = pdlClient;
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
        if (unleash.isEnabled(UNLEASH_REDIRECT_TIL_REMEDY_CONTROLLER)) {
            return  ResponseEntity.ok(convert(remedyController.finnRessurs(request.getPersonident())));

        }
        return ResponseEntity.ok(pdlClient.hentPerson(request.getPersonident())
                                         .map(this::convert)
                                         .orElse(null));
    }

    private PdlPersonDTO convert(Person person) {
        return PdlPersonDTO.builder()
            .navn(NavnDTO.builder()
                .fornavn(person.getFornavn())
                .mellomnavn(person.getMellomnavn())
                .etternavn(person.getEtternavn())
                .build())
            .build();
    }

    private PdlPersonDTO convert(RessursDTO ressursDTO) {
        return PdlPersonDTO.builder()
                .navn(NavnDTO.builder()
                        .fornavn(ressursDTO.getNavn().getFornavn())
                        .mellomnavn(ressursDTO.getNavn().getMellomnavn())
                        .etternavn(ressursDTO.getNavn().getEtternavn())
                        .build())
                .build();
    }

}
