package no.nav.nom.apps.gql;

import no.nav.nom.apps.gql.resolvers.LederResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.DataFetcherExceptionResolver;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;

@Configuration(proxyBeanMethods = false)
public class GraphQlExceptionHandler extends DataFetcherExceptionResolverAdapter {
//
//    @Override
//    public GraphQLError resolveToSingleError(Throwable e, DataFetchingEnvironment dataFetchingEnvironment) {
//        return GraphqlErrorBuilder.newError()
//            .message(e.getMessage())
//            .errorType(ErrorType.DataFetchingException)
//            .location(dataFetchingEnvironment.getSource())
//            .build();
//    }

    @Bean
    public DataFetcherExceptionResolver LederNotFoundExceptionResolver() {
        return DataFetcherExceptionResolver.forSingleError(LederResolver::handleLederNotFoundException);
    }

    @Bean
    public DataFetcherExceptionResolver notRelevantQueryHandler() {
        return DataFetcherExceptionResolver.forSingleError(AbstractGraphQlController::handleNotRelevantQueryException);
    }


}

