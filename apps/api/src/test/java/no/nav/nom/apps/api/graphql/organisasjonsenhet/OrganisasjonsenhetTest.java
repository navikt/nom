package no.nav.nom.apps.api.graphql.organisasjonsenhet;

import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("GraphQL.HentEnhet")
class OrganisasjonsenhetTest extends OrganisasjonsenhetTestBase {

    @Test
    void scenario1() throws Exception {
        val response = graphQLTestTemplate.postForResource("organisasjonsenhet/scenario1.graphql");

        System.out.println("");
    }
}
