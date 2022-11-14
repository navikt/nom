package no.nav.nom.apps.api.graphql.person;

import no.nav.nom.apps.api.ressurs.UnleashRessursServiceSelector;
import no.nav.nom.apps.api.ressurs.contract.IRessurs;
import no.nav.nom.contract.nom.api.graphql.PersonResult;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class PersonResolver {

    private final UnleashRessursServiceSelector ressursService;

    public PersonResolver(UnleashRessursServiceSelector ressursService) {
        this.ressursService = ressursService;
    }

    public Optional<PersonResult> person(String personident) {
        if (personident == null) {
            return Optional.empty();
        }
        return ressursService.findByPersonident(personident)
                .map(PersonResolver::fromRessurs)
                .or(() -> Optional.of(baseRessurs(personident)));

    }

    private static PersonResult baseRessurs(String personIdent) {
        return PersonResult.builder()
                .personIdent(personIdent)
                .personident(personIdent)
                .build();
    }

    public static PersonResult fromRessurs(IRessurs ressurs) {
        return PersonResult.builder()
                .personident(ressurs.getPersonIdent())
                .personIdent(ressurs.getPersonIdent())
                .nomId(ressurs.getId())
                .navident(ressurs.getNavIdent())
                .navIdent(ressurs.getNavIdent())
                .build();
    }
}
