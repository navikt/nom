package no.nav.nom.apps.api.graphql;

import graphql.execution.DataFetcherResult;
import no.nav.nom.apps.gql.GraphQlController;
import no.nav.nom.apps.gql.resolvers.LederResolver;
import no.nav.nom.apps.gql.resolvers.OrgEnhetResolver;
import no.nav.nom.apps.gql.resolvers.OrganiseringResolver;
import no.nav.nom.apps.gql.resolvers.OrgtilknytningResolver;
import no.nav.nom.apps.gql.resolvers.RessursResolver;
import no.nav.nom.apps.gql.resolvers.SearchResolver;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;

import java.util.stream.Collectors;

@Profile("GraphQLTest")
@Controller
public class GraphQlControllerForTest extends GraphQlController {
    public GraphQlControllerForTest(
        OrgEnhetResolver orgEnhetResolver,
        OrgtilknytningResolver orgtilknytningResolver,
        OrganiseringResolver organiseringResolver,
        LederResolver lederResolver,
        RessursResolver ressursResolver,
        SearchResolver searchResolver
    ) {
        super(
            orgEnhetResolver,
            orgtilknytningResolver,
            organiseringResolver,
            lederResolver,
            // dummy remedy id resolver
            s -> "remedy-"+s,
            ressursResolver,
            // dummy email property batch resolver
            ressurser -> ressurser.stream().collect(
                Collectors.toMap(
                    (ressurs) -> ressurs,
                    (ressurs) -> DataFetcherResult.<String>newResult().data(ressurs.getNavident()+"@dummy.mail").build())
            ),
            searchResolver,
            // dummy field usage counter
            (dataFetchingEnvironment) -> {}
        );
    }
}
