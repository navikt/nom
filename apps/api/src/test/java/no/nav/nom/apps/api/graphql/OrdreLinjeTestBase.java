package no.nav.nom.apps.api.graphql;

import no.nav.nom.apps.api.common.AppProperties;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class OrdreLinjeTestBase extends GraphQLTestBase {

    @Autowired
    private AppProperties appProperties;

    @BeforeEach
    void setUpBestilling() {
        // set as sandbox for tests
        appProperties.setAppName("nom-sandbox-api");
    }
}
