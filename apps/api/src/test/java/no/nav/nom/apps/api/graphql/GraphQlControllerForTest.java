package no.nav.nom.apps.api.graphql;

import graphql.execution.DataFetcherResult;
import no.nav.nom.apps.gql.AbstractGraphQlController;
import no.nav.nom.apps.gql.GraphqlSupportInterface;
import no.nav.nom.apps.gql.resolvers.LederResolver;
import no.nav.nom.apps.gql.resolvers.OrgEnhetResolver;
import no.nav.nom.apps.gql.resolvers.OrganiseringResolver;
import no.nav.nom.apps.gql.resolvers.OrgtilknytningResolver;
import no.nav.nom.apps.gql.resolvers.RessursResolver;
import no.nav.nom.apps.gql.resolvers.SearchResolver;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;


@Profile("GraphQLTest")
@Controller
public class GraphQlControllerForTest extends AbstractGraphQlController {
    public GraphQlControllerForTest(
        OrgEnhetResolver orgEnhetResolver,
        OrgtilknytningResolver orgtilknytningResolver,
        OrganiseringResolver organiseringResolver,
        LederResolver lederResolver,
        RessursResolver ressursResolver,
        SearchResolver searchResolver,
        GraphqlSupportInterface graphqlSupportInterface
    ) {
        super(
            orgEnhetResolver,
            orgtilknytningResolver,
            organiseringResolver,
            lederResolver,
            ressursResolver,
            searchResolver,
            graphqlSupportInterface
        );
    }
}
