package no.nav.nom.apps.api.graphql.ressurs;

import lombok.SneakyThrows;
import lombok.val;
import no.nav.nom.contract.nom.api.graphql.Ressurs;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static no.nav.nom.apps.api.graphql.GraphQLMatchers.hasNoErrors;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;


@DisplayName("GraphQL.HentRessurs")
public class RessursTest extends RessursTestBase {

    @Test
    @SneakyThrows
    @DisplayName("Sender inn ressurs fra kafka og leser inn med graphql navIdent parameter")
    void scenario11(){
        val inputRessurs = ressursTestTemplate("");
        kafkaUtils.sendFromKafkaMock(inputRessurs);

        val response = graphQLTestTemplate.postForResource("ressurs/scenario1.graphql");
        assertThat(response, hasNoErrors());

        val outputRessurs = response.getList("$.data.ressurs", Ressurs.class).get(0);
        assertEquals(inputRessurs.getNavident(), outputRessurs.getNavIdent());
        assertEquals(inputRessurs.getPersonIdent(), outputRessurs.getPersonIdent());
    }

    @Test
    @SneakyThrows
    @DisplayName("Sender inn ressurs fra kafka og leser inn med graphql personIdent parameter")
    void scenario12(){
        val inputRessurs = ressursTestTemplate("");
        kafkaUtils.sendFromKafkaMock(inputRessurs);

        val response = graphQLTestTemplate.postForResource("ressurs/scenario2.graphql");
        assertThat(response, hasNoErrors());

        val outputRessurs = response.getList("$.data.ressurs", Ressurs.class).get(0);
        assertEquals(inputRessurs.getNavident(), outputRessurs.getNavIdent());
        assertEquals(inputRessurs.getPersonIdent(), outputRessurs.getPersonIdent());
    }

    @Test
    @SneakyThrows
    @DisplayName("Sender inn ressurs fra kafka og leser inn med graphql uten parameter")
    void scenario13(){
        val inputRessurs1 = ressursTestTemplate("1");
        val inputRessurs2 = ressursTestTemplate("2");
        val inputRessurs3 = ressursTestTemplate("3");

        kafkaUtils.sendFromKafkaMock(inputRessurs1, inputRessurs2, inputRessurs3);

        val response = graphQLTestTemplate.postForResource("ressurs/scenario3.graphql");
        assertThat(response, hasNoErrors());

        val outputRessurs = response.getList("$.data.ressurs", Ressurs.class);
        assertEquals(3, outputRessurs.size());
    }
}
