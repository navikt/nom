package no.nav.nom.apps.api.graphql.person;

import no.nav.nom.apps.api.ressurs.contract.IRessurs;
import no.nav.nom.apps.api.ressurs.contract.IRessursService;
import no.nav.nom.contract.nom.api.graphql.PersonResult;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class PersonResolver {

    private final IRessursService ressursService;

    public PersonResolver(IRessursService ressursService) {
        this.ressursService = ressursService;
    }

    public Optional<PersonResult> person(String personident) {
        if (personident == null) {
            return Optional.empty();
        }
        return ressursService.findByPersonIdent(personident)
                .map(PersonResolver::fromRessurs);

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
