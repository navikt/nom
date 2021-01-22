package no.nav.nom.apps.api.graphql.ressurs;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.SneakyThrows;
import lombok.val;
import no.nav.nom.apps.api.dvh.DvhRestConsumer;
import no.nav.nom.contract.nom.api.graphql.Ressurs;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static no.nav.nom.apps.api.graphql.GraphQLMatchers.hasNoErrors;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


@DisplayName("GraphQL.HentRessurs")
public class RessursTest extends RessursTestBase {

    @Autowired
    private DvhRestConsumer dvhRestConsumer;

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

    @Test
    @SneakyThrows
    @DisplayName("Kan hente ut kobling for ressurs")
    void scenario21() {

        val inputEnhet = enhetTestTemplate("2", null, 0, null);
        val inputRessurs = ressursTestTemplate("2");

        kafkaUtils.sendFromKafkaMock(inputEnhet, inputRessurs);
        dvhRestConsumer.loadKoblingerFromDvh();

        val response = graphQLTestTemplate.postForResource("ressurs/scenario42.graphql");
        assertThat(response, hasNoErrors());

        val ressurs = response.getList("$.data.ressurs", JsonNode.class).get(0);
        val actualOrganisasjonsEnhetId = ressurs.get("koblinger").get(0).get("organisasjonsenhet").get("id");
        assertEquals(inputEnhet.getEnhetKode(), actualOrganisasjonsEnhetId.asText());
    }

    @Test
    @SneakyThrows
    @DisplayName("Kobling er tom liste om kobling for person ikke finnes")
    void scenario22() {
        val inputRessurs = ressursTestTemplate("2");
        kafkaUtils.sendFromKafkaMock(inputRessurs);

        val response = graphQLTestTemplate.postForResource("ressurs/scenario42.graphql");
        assertThat(response, hasNoErrors());


        val ressurs = response.getList("$.data.ressurs", JsonNode.class).get(0);
        assertTrue(ressurs.get("koblinger").isEmpty());
    }

}
