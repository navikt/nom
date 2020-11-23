package no.nav.nom.apps.api.graphql;

import com.graphql.spring.boot.test.GraphQLTestTemplate;
import no.nav.nom.apps.api.TestBase;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class GraphQLTestBase extends TestBase {

    @Autowired
    protected GraphQLTestTemplate graphQLTestTemplate;
}
