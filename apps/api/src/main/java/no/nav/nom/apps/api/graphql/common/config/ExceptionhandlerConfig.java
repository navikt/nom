package no.nav.nom.apps.api.graphql.common.config;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import no.nav.nom.apps.api.graphql.common.exception.PDLBolkException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.DataFetcherExceptionResolver;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;

import java.util.List;
import java.util.function.BiFunction;

import static java.util.Collections.singletonMap;

@Configuration(proxyBeanMethods = false)
public class ExceptionhandlerConfig {

//    @Bean
//    public DataFetcherExceptionResolver NIKResponseExceptionResolver() {
//        return DataFetcherExceptionResolver.forSingleError(RessursResolver::handleNIKException);
//    }

    @Bean
    public DataFetcherExceptionResolver PDLResponseExceptionResolver() {
        return forMultipleErrors((throwable, environment) -> {
            if (throwable instanceof PDLBolkException exception) {
                return exception.getErrors().stream()
                        .map(error -> exception.getErrorCode().construct(environment, exception.getMessage()))
                        .toList();
            }
            return null;
        });
    }

    /**
     * Factory method to create a {@link DataFetcherExceptionResolver} to resolve
     * an exception to a list of GraphQL errors. Effectively, a shortcut
     * for creating {@link DataFetcherExceptionResolverAdapter} and overriding
     * its {@code resolveToMultipleErrors} method.
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

