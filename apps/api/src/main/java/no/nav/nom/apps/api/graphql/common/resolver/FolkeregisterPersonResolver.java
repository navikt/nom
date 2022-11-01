package no.nav.nom.apps.api.graphql.common.resolver;

import graphql.execution.DataFetcherResult;
import no.nav.nom.apps.api.graphql.common.loader.PersonBatchLoader;
import no.nav.nom.contract.nom.api.graphql.FolkeregisterPerson;
import org.dataloader.BatchLoaderEnvironment;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.function.Function;


@Component
public class FolkeregisterPersonResolver {

    private final PersonBatchLoader personBatchLoader;

    public FolkeregisterPersonResolver(PersonBatchLoader personBatchLoader) {
        this.personBatchLoader = personBatchLoader;
    }

    public <T> Mono<DataFetcherResult<FolkeregisterPerson>> folkeregisterPerson(T ressurs, Function<T, String> identResolver) {
        return personBatchLoader.loadSingle(ressurs, identResolver);
    }

    public <T> Mono<Map<T, DataFetcherResult<FolkeregisterPerson>>> folkeregisterPerson(List<T> ressurser, Function<T, String> identResolver) {
        return personBatchLoader.loadMany(ressurser, identResolver);
    }
}
