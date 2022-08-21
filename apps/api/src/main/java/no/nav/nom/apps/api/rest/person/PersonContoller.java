package no.nav.nom.apps.api.rest.person;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.val;
import no.nav.nom.apps.api.common.SwaggerConfig;
import no.nav.nom.apps.api.graphql.PdlConsumer;
import no.nav.nom.contract.nom.api.graphql.FolkeregisterPerson;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Person")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/person")
public class PersonContoller {

    private final PdlConsumer pdlConsumer;

    @Value("${app.graphql.preauth.query.person}")
    private List<String> preAuth;

    @PostMapping("/getPerson")
    @Operation(
            description = "Personoppslag",
            security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN))
    @ApiResponses(value = @ApiResponse(responseCode = "200", description = "Success"))
    public ResponseEntity<PdlPersonDTO> getPerson(@RequestBody PdlPersonRequest request){
        return checkAuth() ? ResponseEntity.status(200).body(pdlConsumer.hentPerson(request.getPersonident()).map(this::convert).orElse(null))
        : ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }


    private PdlPersonDTO convert(FolkeregisterPerson person){
        return PdlPersonDTO.builder()
                .navn(
                        PdlPersonDTO.Navn.builder()
                                .fornavn(person.getNavn().getFornavn())
                                .mellomnavn(person.getNavn().getMellomnavn())
                                .etternavn(person.getNavn().getEtternavn())
                                .build()
                )
                .build();
    }

    public boolean checkAuth(){
        val token = (JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();

        if(token != null){
            return preAuth.contains(token.getTokenAttributes().get("azp"));
        }
        return false;
    }
}
