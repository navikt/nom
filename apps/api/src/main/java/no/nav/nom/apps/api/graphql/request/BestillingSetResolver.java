package no.nav.nom.apps.api.graphql.request;

import graphql.execution.DataFetcherResult;
import graphql.kickstart.tools.GraphQLQueryResolver;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import no.nav.nom.apps.api.graphql.common.SecurityUtil;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

import static org.springframework.util.CollectionUtils.isEmpty;

@Component
@RequiredArgsConstructor
public class BestillingSetResolver implements GraphQLQueryResolver {

    private final BestillingSetInMemoryRepository repository;

    public DataFetcherResult<BestillingSet> bestillingSet(UUID requstSetId, DataFetchingEnvironment env) {
        return SecurityUtil.<BestillingSet>checkAuth(env).orElseGet(() -> {
            var set = repository.get(requstSetId);
            return build(set);
        });
    }

    public DataFetcherResult<List<BestillingSet>> bestillingSets(List<UUID> requstSetIds, DataFetchingEnvironment env) {
        return SecurityUtil.<List<BestillingSet>>checkAuth(env).orElseGet(() -> {
            var sets = isEmpty(requstSetIds) ? repository.all() : repository.get(requstSetIds);
            return build(sets);
        });
    }

    private <T> DataFetcherResult<T> build(T data) {
        return DataFetcherResult.<T>newResult()
                .data(data)
                .build();
    }
}
