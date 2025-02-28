package no.nav.nom.apps.api.person.api.app;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.nom.apps.api.common.SwaggerConfig;
import no.nav.nom.apps.api.pdl.PdlClient;
import no.nav.nom.apps.api.person.api.dto.FolkeregisterPerson;
import no.nav.nom.apps.api.person.api.dto.Person;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

import static java.util.Objects.isNull;

@Tag(name = "Person")
@RestController
@RequestMapping(value = {"/app/person"})
public class PersonController {

    private final RessursStateRepository ressursStateRepository;
    private final PdlClient pdlClient;

    public PersonController(
        RessursStateRepository ressursStateRepository,
        PdlClient pdlClient
    ) {
        this.ressursStateRepository = ressursStateRepository;
        this.pdlClient = pdlClient;
    }

    @PostMapping
    @Operation(
        description = "Henter person med gitt personident",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses(
        {
            @ApiResponse(responseCode = "200", description = "Returnerer en instanse av Person"),
        }
    )
    public ResponseEntity<Person> findPerson(@RequestBody GetPersonRequest getPersonRequest) {
        Person person = null;
        var personident = getPersonRequest.getPersonident();
        if (isNull(getPersonRequest.getIdentType()) || getPersonRequest.getIdentType().isStandard()) {
            var folkeregisterPerson = getPerson(personident);
            if (folkeregisterPerson.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            var ressursState = ressursStateRepository.findByPersonident(personident);
            person = ressursState
                .map(rs -> new Person(rs.personident(), rs.navident(), folkeregisterPerson.get()))
                .orElse(new Person(personident, null, folkeregisterPerson.get()));
        } else {
            var ressursState = ressursStateRepository.findByPersonident(personident);
            if (ressursState.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            person = ressursState
                .map(rs -> new Person(rs.personident(), rs.navident(), null))
                .orElse(new Person(personident, null, null));
        }
        return ResponseEntity.ok(person);

    }

    private Optional<FolkeregisterPerson> getPerson(String personident) {
        var person = pdlClient.hentPerson(personident);
        return person.map(value -> new FolkeregisterPerson(
            new FolkeregisterPerson.RessursNavn(
                value.getFornavn(),
                value.getMellomnavn(),
                value.getEtternavn()
            )
        ));
    }

}
