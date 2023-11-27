package no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.client;

import java.util.List;

public class NomApiGraphQLException extends RuntimeException{

    public NomApiGraphQLException(List<GraphQLError> graphQLErrors, String query) {
        super(String.format(
            "Got errors calling nom-api using query %s. Errors: %s",
            query,
            String.join("\n", graphQLErrors.stream().map(GraphQLError::message).toList())
        ));
    }
}
