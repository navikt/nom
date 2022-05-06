package no.nav.nom.apps.api.rest.person;

import lombok.RequiredArgsConstructor;
import lombok.val;
import no.nav.nom.apps.api.graphql.PdlConsumer;
import no.nav.nom.contract.nom.api.graphql.Person;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/person")
public class PersonContoller {

    private final PdlConsumer pdlConsumer;

    @Value("${app.graphql.preauth.query.person}")
    private List<String> preAuth;

    @PostMapping("/getPerson")
    public ResponseEntity<PdlPersonDTO> getPerson(@RequestBody PdlPersonRequest request){
        return checkAuth() ? ResponseEntity.status(200).body(pdlConsumer.hentPerson(request.getPersonident()).map(this::convert).orElse(null))
        : ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }


    private PdlPersonDTO convert(Person person){
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
