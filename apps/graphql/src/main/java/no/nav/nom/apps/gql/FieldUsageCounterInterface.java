package no.nav.nom.apps.gql;

import graphql.schema.DataFetchingEnvironment;

public interface FieldUsageCounterInterface {
    void count(DataFetchingEnvironment dataFetchingEnvironment);
}
