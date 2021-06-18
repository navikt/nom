package no.nav.nom.apps.api.graphql.request;

import graphql.execution.DataFetcherResult;
import graphql.kickstart.tools.GraphQLQueryResolver;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import no.nav.nom.apps.api.graphql.common.SecurityUtil;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RequestSetResolver implements GraphQLQueryResolver {

    private final RequestSetInMemoryRepository repository;

    public DataFetcherResult<RequestSet> requestSet(UUID requstSetId, DataFetchingEnvironment env) {
        return SecurityUtil.<RequestSet>checkAuth(env).orElseGet(() -> {
            var set = repository.get(requstSetId);
            return build(set);
        });
    }

    public DataFetcherResult<List<RequestSet>> requestSets(List<UUID> requstSetIds, DataFetchingEnvironment env) {
        return SecurityUtil.<List<RequestSet>>checkAuth(env).orElseGet(() -> {
            var sets = requstSetIds.isEmpty() ? repository.all() : repository.get(requstSetIds);
            return build(sets);
        });
    }

    private <T> DataFetcherResult<T> build(T data) {
        return DataFetcherResult.<T>newResult()
                .data(data)
                .build();
    }
}
