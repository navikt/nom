package no.nav.nom.apps.api.person.api.app;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.nom.apps.api.common.SwaggerConfig;
import no.nav.nom.apps.api.graphql.common.loader.PersonBatchLoader;
import no.nav.nom.apps.api.ressurs.eventsourcing.state.RessursStateRepository;
import no.nav.nom.apps.api.person.api.dto.FolkeregisterPerson;
import no.nav.nom.apps.api.person.api.dto.Person;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@Tag(name = "Person")
@RestController
@RequestMapping(value = {"/app/person"})
public class PersonController {

    private final RessursStateRepository ressursStateRepository;
    private final PersonBatchLoader personBatchLoader;

    public PersonController(RessursStateRepository ressursStateRepository, PersonBatchLoader personBatchLoader) {
        this.ressursStateRepository = ressursStateRepository;
        this.personBatchLoader = personBatchLoader;
    }

    @GetMapping
    @Operation(
        description = "Henter person med gitt personident",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Returnerer en instanse av Person"),
    })
    public ResponseEntity<Person> findPerson(@RequestHeader("personident") String personident) {

        var folkeregisterPerson = resolveFrom(personident);
        if (folkeregisterPerson.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        var ressursState = ressursStateRepository.findByPersonident(personident);
        var person = ressursState
            .map(rs -> new Person(rs.personident(), rs.fid(), rs.navident(), folkeregisterPerson.get()))
            .orElse(new Person(personident, null, null, folkeregisterPerson.get()));

        return ResponseEntity.ok(person);
    }

    private Optional<FolkeregisterPerson> resolveFrom(String personident) {
        return fetchFolkeregisterPerson(personident)
            .map(no.nav.nom.contract.nom.api.graphql.FolkeregisterPerson::getNavn)
            .map(ressursNavn -> new FolkeregisterPerson.RessursNavn(ressursNavn.getFornavn(), ressursNavn.getMellomnavn(), ressursNavn.getEtternavn()))
            .map(FolkeregisterPerson::new);
    }

    private Optional<no.nav.nom.contract.nom.api.graphql.FolkeregisterPerson> fetchFolkeregisterPerson(String personident) {
        var dataFetcher = personBatchLoader.loadSingle(personident, String::valueOf).block();

        return (!dataFetcher.hasErrors() && dataFetcher.getData() != null) ? Optional.of(dataFetcher.getData()) : Optional.empty();
    }
}
