package no.nav.nom.apps.api.graphql;

import no.nav.nom.apps.api.common.AppProperties;
import no.nav.nom.apps.api.graphql.request.RequestSetInMemoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class BestillingTestBase extends GraphQLTestBase {

    @Autowired
    private RequestSetInMemoryRepository requestSetRepository;
    @Autowired
    private AppProperties appProperties;

    @BeforeEach
    void setUpBestilling() {
        // set as sandbox for tests
        appProperties.setAppName("nom-sandbox-api");
    }
}
