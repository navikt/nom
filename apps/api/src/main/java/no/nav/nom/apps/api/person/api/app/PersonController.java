package no.nav.nom.apps.api.person.api.app;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.nom.apps.api.common.SwaggerConfig;
import no.nav.nom.apps.api.nik.LookupNavidentRequest;
import no.nav.nom.apps.api.nik.NIKClient;
import no.nav.nom.apps.api.pdl.PdlClient;
import no.nav.nom.apps.api.person.api.dto.FolkeregisterPerson;
import no.nav.nom.apps.api.person.api.dto.Person;
import no.nav.nom.contract.nom.IdentType;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@Tag(name = "Person")
@RestController
@RequestMapping(value = {"/app/person"})
public class PersonController {

    private final RessursStateRepository ressursStateRepository;
    private final PdlClient pdlClient;
    private final NIKClient nIKClient;

    public PersonController(
        RessursStateRepository ressursStateRepository,
        PdlClient pdlClient,
        NIKClient nIKClient
    ) {
        this.ressursStateRepository = ressursStateRepository;
        this.pdlClient = pdlClient;
        this.nIKClient = nIKClient;
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
        enum IdentTypeBucket {
            AssumeStandardIdent,
            NonStandardIdent;

            static IdentTypeBucket fromIdentType(IdentType identType) {
                return identType == null || identType == IdentType.STANDARD ? AssumeStandardIdent : NonStandardIdent;
            }
        }

        var personident = getPersonRequest.getPersonident();
        var currentIdentBucket = IdentTypeBucket.fromIdentType(getPersonRequest.getIdentType());

        return switch(currentIdentBucket) {
            case AssumeStandardIdent -> {
                var folkeregisterPerson = getPerson(personident);
                if (folkeregisterPerson.isEmpty()) {
                    yield ResponseEntity.notFound().build();
                }
                var maybeRessursState = ressursStateRepository.findByPersonident(personident);
                var maybePersonFromRessursState = maybeRessursState.map(rs -> new Person(rs.personident(), rs.navident(), folkeregisterPerson.get()));
                var person = maybePersonFromRessursState.orElseGet(() -> new Person(personident, lookupMaybeNikNavIdent(personident), folkeregisterPerson.get()));
                yield ResponseEntity.ok(person);
            }
            case NonStandardIdent -> {
                var ressursState = ressursStateRepository.findByPersonident(personident);
                if (ressursState.isEmpty()) {
                    yield ResponseEntity.notFound().build();
                }
                var person = ressursState
                    .map(rs -> new Person(rs.personident(), rs.navident(), null))
                    .orElse(new Person(personident, null, null));
                yield ResponseEntity.ok(person);
            }
        };
    }

    private String lookupMaybeNikNavIdent(String personident) {
        var navIdentResponse = nIKClient.lookupNavident(new LookupNavidentRequest(personident));
        var okStatus = navIdentResponse.getStatusCode().is2xxSuccessful();
        if (okStatus) {
            var responseBody = navIdentResponse.getBody();
            return responseBody != null ? navIdentResponse.getBody().getNavident() : null;
        }
        return null;
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
