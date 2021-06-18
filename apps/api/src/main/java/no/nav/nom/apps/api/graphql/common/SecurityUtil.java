package no.nav.nom.apps.api.graphql.common;

import graphql.execution.DataFetcherResult;
import graphql.language.OperationDefinition.Operation;
import graphql.schema.DataFetchingEnvironment;
import lombok.experimental.UtilityClass;

import java.util.Optional;

@UtilityClass
public class SecurityUtil {

    public static <T> Optional<DataFetcherResult<T>> checkAuth(DataFetchingEnvironment env) {
        CustomGraphQLContext context = env.getContext();
        if (!context.isAuthenticated()) {
            return Optional.of(DataFetcherResult.<T>newResult()
                    .error(ErrorCode.UNAUTHENTICATED.construct(env, "Missing or invalid token"))
                    .build());
        }
        if (env.getOperationDefinition().getOperation() == Operation.MUTATION && !context.isSandbox()) {
            return Optional.of(DataFetcherResult.<T>newResult()
                    .error(ErrorCode.DISABLED_FEATURE.construct(env, "All mutations are only supported in sandbox for now"))
                    .build());
        }
        return Optional.empty();
    }
}
