package no.nav.nom.apps.api.rest.person;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.nom.apps.api.common.SwaggerConfig;
import no.nav.nom.apps.api.pdl.PdlConsumer;
import no.nav.nom.apps.api.pdl.Person;
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

    private final PdlConsumer pdlConsumer;

    public PersonContoller(PdlConsumer pdlConsumer) {
        this.pdlConsumer = pdlConsumer;
    }

    @PostMapping("/getPerson")
    @Operation(
            description = "Personoppslag",
            security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Success")
    })
    @PreAuthorize("hasAuthority(T(no.nav.nom.apps.api.common.security.AZPAuthoritiesConverter).AUTH_QUERY_PERSON)")
    public ResponseEntity<PdlPersonDTO> getPerson(@RequestBody PdlPersonRequest request) {
        return ResponseEntity.ok(pdlConsumer.hentPerson(request.getPersonident())
                                         .map(this::convert)
                                         .orElse(null));
    }


    private PdlPersonDTO convert(Person person) {
        return PdlPersonDTO.builder()
                .navn(PdlPersonDTO.Navn.builder()
                              .fornavn(person.getFornavn())
                              .mellomnavn(person.getMellomnavn())
                              .etternavn(person.getEtternavn())
                              .build())
                .build();
    }
}
