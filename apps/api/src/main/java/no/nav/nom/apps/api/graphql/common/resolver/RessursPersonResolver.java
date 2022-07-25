package no.nav.nom.apps.api.graphql.common.resolver;

import graphql.execution.DataFetcherResult;
import graphql.kickstart.tools.GraphQLResolver;
import graphql.schema.DataFetchingEnvironment;
import no.nav.nom.apps.api.graphql.common.LoaderUtils;
import no.nav.nom.apps.api.graphql.common.loader.PersonBatchLoader;
import no.nav.nom.contract.nom.api.graphql.FolkeregisterPerson;
import no.nav.nom.contract.nom.api.graphql.Ressurs;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;


@Component
public class RessursPersonResolver implements GraphQLResolver<Ressurs> {

    public CompletableFuture<DataFetcherResult<FolkeregisterPerson>> folkeregisterPerson(Ressurs ressurs, DataFetchingEnvironment env) {
        return LoaderUtils.get(env, PersonBatchLoader.PERSON_DATALOADER, ressurs.getPersonIdent());
    }

    public CompletableFuture<DataFetcherResult<FolkeregisterPerson>> person(Ressurs ressurs, DataFetchingEnvironment env) {
        return folkeregisterPerson(ressurs, env);
    }
}
