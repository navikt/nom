package no.nav.nom.apps.api.graphql.person;

import no.nav.nom.apps.api.ressurs.eventsourcing.state.RessursStateRepository;
import no.nav.nom.contract.nom.api.graphql.PersonResult;
import no.nav.nom.contract.nom.state.RessursState;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class PersonResolver {

    private final RessursStateRepository ressursStateRepository;

    public PersonResolver(RessursStateRepository ressursStateRepository) {
        this.ressursStateRepository = ressursStateRepository;
    }

    public Optional<PersonResult> person(String personident) {
        if (personident == null) {
            return Optional.empty();
        }
        return ressursStateRepository.findByPersonident(personident).map(PersonResolver::fromRessurs).or(() -> Optional.of(baseRessurs(personident)));

    }

    private static PersonResult baseRessurs(String personIdent) {
        return PersonResult.builder().personIdent(personIdent).personident(personIdent).build();
    }

    public static PersonResult fromRessurs(RessursState ressurs) {
        return PersonResult.builder()
                .personident(ressurs.personident())
                .personIdent(ressurs.personident())
                .nomId(ressurs.fid())
                .navident(ressurs.navident())
                .navIdent(ressurs.navident())
                .build();
    }
}
