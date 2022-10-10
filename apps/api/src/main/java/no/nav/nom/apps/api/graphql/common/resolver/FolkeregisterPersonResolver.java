package no.nav.nom.apps.api.graphql.common.resolver;

import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
import no.nav.nom.apps.api.graphql.common.loader.PersonBatchLoader;
import no.nav.nom.contract.nom.api.graphql.FolkeregisterPerson;
import org.dataloader.BatchLoaderEnvironment;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.util.Collections.singletonList;
import static org.dataloader.BatchLoaderEnvironment.newBatchLoaderEnvironment;


@Component
public class FolkeregisterPersonResolver {

    private final PersonBatchLoader personBatchLoader;

    public FolkeregisterPersonResolver(PersonBatchLoader personBatchLoader) {
        this.personBatchLoader = personBatchLoader;
    }

    public <T> DataFetcherResult<FolkeregisterPerson> folkeregisterPerson(T ressurs, Function<T, String> identResolver, DataFetchingEnvironment environment) {
        var env = newBatchLoaderEnvironment().context(environment.getGraphQlContext()).build(); // Hack to get around the PersonBatchLoaders need for a BatchLoaderEnvironment
        return folkeregisterPerson(singletonList(ressurs), identResolver, env).get(ressurs);
    }

    public <T> Map<T, DataFetcherResult<FolkeregisterPerson>> folkeregisterPerson(List<T> ressurser, Function<T, String> identResolver, BatchLoaderEnvironment environment) {
        return personBatchLoader.load(ressurser, identResolver, environment);
    }
}
