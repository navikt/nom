package no.nav.nom.apps.gql;

import graphql.ErrorType;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import org.springframework.context.annotation.Configuration;
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



}

