package no.nav.nom.apps.api.graphql.common.resolver;

//import no.nav.nom.apps.api.graphql.common.loader.PersonBatchLoader;
import no.nav.nom.apps.api.graphql.common.loader.PersonBatchLoader;
import no.nav.nom.contract.nom.api.graphql.FolkeregisterPerson;
import no.nav.nom.contract.nom.api.graphql.PersonResult;
import no.nav.nom.contract.nom.api.graphql.Ressurs;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;


@Component
public class FolkeregisterPersonResolver {

    private final PersonBatchLoader batchLoader;

    public FolkeregisterPersonResolver(PersonBatchLoader batchLoader) {
        this.batchLoader = batchLoader;
    }

    public FolkeregisterPerson folkeregisterPerson(PersonResult person) {
        return null;
//        return LoaderUtils.get(env, PersonBatchLoader.PERSON_DATALOADER, ressurs.getPersonIdent());
    }

    public List<FolkeregisterPerson> folkeregisterPerson(Set<PersonResult> personer) {
        return null;
    }

    public FolkeregisterPerson folkeregisterPerson(Ressurs ressurs) {
        return null;
//        return LoaderUtils.get(env, PersonBatchLoader.PERSON_DATALOADER, ressurs.getPersonIdent());
    }

    public Map<Ressurs, FolkeregisterPerson> folkeregisterPerson(Set<Ressurs> ressurser) {
        return null;
    }


    public Map<String, FolkeregisterPerson> folkeregisterPerson(Set<String> personidenter) {

        return null;
    }
}
