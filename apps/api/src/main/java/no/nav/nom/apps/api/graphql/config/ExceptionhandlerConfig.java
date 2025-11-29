package no.nav.nom.apps.api.graphql.config;

import graphql.GraphQLError;
import graphql.schema.DataFetchingEnvironment;
import no.nav.nom.apps.api.graphql.common.SecurityUtil;
import no.nav.nom.apps.api.graphql.propertyresolvers.ActiveDirectoryRessursBatchLoader;
import no.nav.nom.apps.gql.resolvers.LederResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.DataFetcherExceptionResolver;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;

import java.util.List;
import java.util.function.BiFunction;

@Configuration(proxyBeanMethods = false)
public class ExceptionhandlerConfig {

    @Bean
    public DataFetcherExceptionResolver ADResponseExceptionResolver() {
        return DataFetcherExceptionResolver.forSingleError(ActiveDirectoryRessursBatchLoader::handleADResponseException);
    }

    @Bean
    public DataFetcherExceptionResolver unauthenticatedException() {
        return DataFetcherExceptionResolver.forSingleError(SecurityUtil::handleUnauthenticatedException);
    }

    /**
     * Factory method to create a {@link DataFetcherExceptionResolver} to resolve
     * an exception to a list of GraphQL errors. Effectively, a shortcut
     * for creating {@link DataFetcherExceptionResolverAdapter} and overriding
     * its {@code resolveToMultipleErrors} method.
     *
     * @param resolver the resolver function to use
     * @return the created instance
     */
    private static DataFetcherExceptionResolverAdapter forMultipleErrors(BiFunction<Throwable, DataFetchingEnvironment, List<GraphQLError>> resolver) {
        return new DataFetcherExceptionResolverAdapter() {
            @Override
            protected List<GraphQLError> resolveToMultipleErrors(Throwable ex, DataFetchingEnvironment env) {
                return resolver.apply(ex, env);
            }
        };
    }

}

