package no.nav.nom.apps.api.graphql.common;

import graphql.ErrorClassification;
import graphql.ErrorType;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static java.util.Collections.singletonMap;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    UNAUTHENTICATED(ErrorType.ExecutionAborted, "unauthenticated"),
    UNAUTHORIZED(ErrorType.ExecutionAborted, "unauthorized"),
    NOT_FOUND(ErrorType.ExecutionAborted, "not_found"),
    BAD_REQUEST(ErrorType.ValidationError, "bad_request"),
    SERVER_ERROR(ErrorType.DataFetchingException, "server_error");

    private final ErrorClassification type;
    private final String text;

    public GraphQLError construct(DataFetchingEnvironment env, String message) {
        return GraphqlErrorBuilder.newError(env)
                .message(message)
                .errorType(type)
                .extensions(singletonMap("code", text))
                .build();
    }
}
