package no.nav.nom.apps.api.graphql.common.exception;

import no.nav.nom.apps.api.graphql.common.ErrorCode;
import org.springframework.graphql.ResponseError;
import org.springframework.graphql.client.ClientGraphQlResponse;

import java.util.List;

public class PDLBolkException extends RuntimeException{

    private final ClientGraphQlResponse graphQlResponse;
    private final ErrorCode errorCode;
    public PDLBolkException(String message, ClientGraphQlResponse graphQlResponse, ErrorCode errorCode) {
        super(message);
        this.graphQlResponse = graphQlResponse;
        this.errorCode = errorCode;
    }

    public ClientGraphQlResponse getSource() {
        return graphQlResponse;
    }
    public List<ResponseError> getErrors() {
        return graphQlResponse.getErrors();
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
