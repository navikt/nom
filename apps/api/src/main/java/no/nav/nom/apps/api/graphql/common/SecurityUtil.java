package no.nav.nom.apps.api.graphql.common;

import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
import lombok.experimental.UtilityClass;

import java.util.Optional;

@UtilityClass
public class SecurityUtil {

    public static <T> Optional<DataFetcherResult<T>> checkAuth(DataFetchingEnvironment env) {
        if (!((CustomGraphQLContext) env.getContext()).isAuthenticated()) {
            return Optional.of(DataFetcherResult.<T>newResult()
                    .error(ErrorCode.UNAUTHENTICATED.construct(env, "Missing or invalid token"))
                    .build());
        }
        return Optional.empty();
    }
}
