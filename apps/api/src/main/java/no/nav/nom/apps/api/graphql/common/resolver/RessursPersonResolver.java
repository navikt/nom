package no.nav.nom.apps.api.graphql.common.resolver;

import graphql.execution.DataFetcherResult;
import graphql.kickstart.execution.context.GraphQLContext;
import graphql.kickstart.tools.GraphQLResolver;
import graphql.schema.DataFetchingEnvironment;
import lombok.val;
import no.nav.nom.apps.api.graphql.common.loader.PersonBatchLoader;
import no.nav.nom.contract.nom.api.graphql.Person;
import no.nav.nom.contract.nom.api.graphql.Ressurs;
import org.dataloader.DataLoader;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;


@Component
public class RessursPersonResolver implements GraphQLResolver<Ressurs> {

    public CompletableFuture<DataFetcherResult<Person>> person(Ressurs ressurs, DataFetchingEnvironment env) {
        val registry = ((GraphQLContext) env.getContext()).getDataLoaderRegistry();
        DataLoader<String, DataFetcherResult<Person>> dataLoader = registry.getDataLoader(PersonBatchLoader.PERSON_DATALOADER);

        return dataLoader.load(ressurs.getPersonIdent(), env);
    }
}
