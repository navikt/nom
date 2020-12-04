package no.nav.nom.apps.api.graphql.organisasjonsenhet;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.val;
import no.nav.nom.contract.dvh.Enhet;
import no.nav.nom.contract.nom.api.graphql.Organisasjonsenhet;
import no.nav.nom.contract.nom.api.graphql.Organisering;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static no.nav.nom.apps.api.graphql.GraphQLMatchers.hasNoErrors;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("GraphQL.HentEnhet")
class OrganisasjonsenhetTest extends OrganisasjonsenhetTestBase {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @SneakyThrows
    @DisplayName("Sender organisasjonsenhet inn fra kafka og leser fra graphql med id parameter")
    void scenario11(){
        val inputEnhet = enhetTestTemplate("", null,0);
        kafkaUtils.sendFromKafkaMock(inputEnhet);

        val response = graphQLTestTemplate.postForResource("organisasjonsenhet/scenario1.graphql");
        assertThat(response, hasNoErrors());

        val outputEnhet = response.getList("$.data.organisasjonsenhet", Organisasjonsenhet.class).get(0);
        orgEnhetAsserter(inputEnhet, outputEnhet);
    }

    @Test
    @SneakyThrows
    @DisplayName("Sender organisasjonsenhet inn fra kafka og leser fra graphql uten id parameter")
    void scenario12() {
        val inputEnhet = enhetTestTemplate("", null,0);
        val inputEnhet2 = enhetTestTemplate("2", inputEnhet.getEnhetKode(), 1);

        kafkaUtils.sendFromKafkaMock(inputEnhet, inputEnhet2);

        val response = graphQLTestTemplate.postForResource("organisasjonsenhet/scenario2.graphql");
        assertThat(response, hasNoErrors());
        assertEquals(2, response.getList("$.data.organisasjonsenhet", Organisasjonsenhet.class).size());
    }

    @Test
    @SneakyThrows
    @DisplayName("Sender inn organisasjonsenheter organisert i hierarki fra kafka, leser og validerer organiseringene")
    void scenario21(){
        val inputEnhetOver = enhetTestTemplate("1", null, 0);
        val inputEnhet = enhetTestTemplate("2", inputEnhetOver.getEnhetKode(), 1);
        val inputEnhetUnder = enhetTestTemplate("3", inputEnhet.getEnhetKode(), 2);

        kafkaUtils.sendFromKafkaMock(inputEnhetOver, inputEnhet, inputEnhetUnder);

        val response = graphQLTestTemplate.postForResource("organisasjonsenhet/scenario3.graphql");
        assertThat(response, hasNoErrors());
        val organiseringerResponse = response.getList("$.data.organisasjonsenhet", JsonNode.class).get(0);

        organiseringerResponse.get("organiseringer")
                .forEach(o -> {
                    val enhet = objectMapper.convertValue(o.get("organisasjonsenhet"), Organisasjonsenhet.class);
                    val organisering = objectMapper.convertValue(o, Organisering.class);

                    orgEnhetAsserter(organisering.getRetning().equals("over") ? inputEnhetOver : inputEnhetUnder, enhet);
                    organiseringAsserter(organisering.getRetning().equals("over") ? inputEnhet : inputEnhetUnder, organisering);
                });
    }

    private void orgEnhetAsserter(Enhet faktaEnhet, Organisasjonsenhet actualEnhet){
        assertEquals(faktaEnhet.getEnhetKode(), actualEnhet.getId());
        assertEquals(faktaEnhet.getEnhetNavn(), actualEnhet.getNavn());
        assertEquals(faktaEnhet.getEnhetGyldigFra(), actualEnhet.getGyldigFom());
        assertEquals(faktaEnhet.getEnhetGyldigTil(), actualEnhet.getGyldigTom());
    }

    private void organiseringAsserter(Enhet faktaEnhet, Organisering actualOrganisering){
        assertEquals(faktaEnhet.getOrgGyldigFra(), actualOrganisering.getGyldigFom());
        assertEquals(faktaEnhet.getOrgGyldigFra(), actualOrganisering.getGyldigTom());
    }


}

