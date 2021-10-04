package no.nav.nom.apps.api.graphql.common;

import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
import lombok.experimental.UtilityClass;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;
import java.util.Optional;

@UtilityClass
public class SecurityUtil {

    public static <T> Optional<DataFetcherResult<T>> checkAuth(DataFetchingEnvironment env, List<String> preAuth) {
        CustomGraphQLContext context = env.getContext();

        if(context.isAuthenticated()) {
            if (preAuth == null || preAuth.contains(((JwtAuthenticationToken) context.getAuthentication()).getTokenAttributes().get("azp"))) {
                return Optional.empty();
            }
        }

        return Optional.of(DataFetcherResult.<T>newResult()
                .error(ErrorCode.UNAUTHENTICATED.construct(env, "Missing or invalid token"))
                .build());
    }
}
