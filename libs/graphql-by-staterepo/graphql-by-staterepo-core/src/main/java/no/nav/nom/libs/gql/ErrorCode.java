package no.nav.nom.libs.gql;

import graphql.ErrorClassification;
import graphql.ErrorType;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;

import static java.util.Collections.singletonMap;


public enum ErrorCode {


    UNAUTHENTICATED(ErrorType.ExecutionAborted, "unauthenticated"),
    UNAUTHORIZED(ErrorType.ExecutionAborted, "unauthorized"),
    NOT_FOUND(ErrorType.ExecutionAborted, "not_found"),
    BAD_REQUEST(ErrorType.ValidationError, "bad_request"),
    SERVER_ERROR(ErrorType.DataFetchingException, "server_error"),
    DISABLED_FEATURE(ErrorType.OperationNotSupported, "disabled");

    private final ErrorClassification type;
    private final String text;


    ErrorCode(ErrorType type, String text) {
        this.type = type;
        this.text = text;
    };

    public GraphQLError construct(DataFetchingEnvironment env, String message) {
        return GraphqlErrorBuilder.newError(env)
                .message(message)
                .errorType(type)
                .extensions(singletonMap("code", text))
                .build();
    }

    public GraphQLError construct(String message) {
        return GraphqlErrorBuilder.newError()
                .message(message)
                .errorType(type)
                .extensions(singletonMap("code", text))
                .build();
    }
}
