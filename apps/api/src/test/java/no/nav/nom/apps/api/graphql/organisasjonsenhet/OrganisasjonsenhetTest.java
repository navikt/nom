package no.nav.nom.apps.api.graphql.organisasjonsenhet;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.val;
import no.nav.nom.apps.api.dvh.DvhRestConsumer;
import no.nav.nom.contract.dvh.Enhet;
import no.nav.nom.contract.nom.api.graphql.Organisasjonsenhet;
import no.nav.nom.contract.nom.api.graphql.OrganisasjonsenhetsKobling;
import no.nav.nom.contract.nom.api.graphql.OrganisasjonsenhetsLeder;
import no.nav.nom.contract.nom.api.graphql.Organisering;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static no.nav.nom.apps.api.graphql.GraphQLMatchers.hasNoErrors;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("GraphQL.HentEnhet")
class OrganisasjonsenhetTest extends OrganisasjonsenhetTestBase {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DvhRestConsumer dvhRestConsumer;

    @Test
    @SneakyThrows
    @DisplayName("Sender organisasjonsenhet inn fra kafka og leser fra graphql med id parameter")
    void scenario11(){
        val inputEnhet = enhetTestTemplate("", null,0, null);
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
        val inputEnhet = enhetTestTemplate("", null,0, null);
        val inputEnhet2 = enhetTestTemplate("2", inputEnhet.getEnhetKode(), 1, null);

        kafkaUtils.sendFromKafkaMock(inputEnhet, inputEnhet2);

        val response = graphQLTestTemplate.postForResource("organisasjonsenhet/scenario2.graphql");
        assertThat(response, hasNoErrors());
        assertEquals(2, response.getList("$.data.organisasjonsenhet", Organisasjonsenhet.class).size());
    }

    @Test
    @SneakyThrows
    @DisplayName("Sender inn organisasjonsenheter organisert i hierarki fra kafka, leser og validerer organiseringene")
    void scenario21(){
        val inputEnhetOver = enhetTestTemplate("1", null, 0, null);
        val inputEnhet = enhetTestTemplate("2", inputEnhetOver.getEnhetKode(), 1, null);
        val inputEnhetUnder = enhetTestTemplate("3", inputEnhet.getEnhetKode(), 2, null);

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

    @Test
    @SneakyThrows
    @DisplayName("Tester at graphQL spørring på leder hvor enhet har ledig gir leder")
    void scenario31(){
        val inputLeder = ressursTestTemplate("");
        val inputEnhet = enhetTestTemplate("", null, 0, inputLeder.getNavident().substring(1));

        kafkaUtils.sendFromKafkaMock(inputLeder, inputEnhet);

        val response = graphQLTestTemplate.postForResource("organisasjonsenhet/scenario4.graphql");
        assertThat(response, hasNoErrors());

        val outputEnhetJson = response.getList("$.data.organisasjonsenhet", JsonNode.class).get(0);
        val outputLeder = objectMapper.convertValue(outputEnhetJson.get("leder"), new TypeReference<List<OrganisasjonsenhetsLeder>>() {}).get(0);

        assertEquals(inputLeder.getNavident(), outputLeder.getRessurs().getNavIdent());
        assertEquals(inputLeder.getPersonIdent(), outputLeder.getRessurs().getPersonIdent());
    }

    @Test
    @SneakyThrows
    @DisplayName("Tester at graphQL spørring på leder for enhet, hvor enhet ikke har leder")
    void scenario32(){
        val inputEnhet = enhetTestTemplate("32", null, 0, null);
        kafkaUtils.sendFromKafkaMock(inputEnhet);

        val response = graphQLTestTemplate.postForResource("organisasjonsenhet/scenario32.graphql");
        assertThat(response, hasNoErrors());

        val outputEnhetJson = response.getList("$.data.organisasjonsenhet", JsonNode.class).get(0);
        val outputLedere = objectMapper.convertValue(outputEnhetJson.get("leder"), new TypeReference<List<OrganisasjonsenhetsLeder>>() {});

        assertEquals(0, outputLedere.size());
    }

    @Test
    @SneakyThrows
    @DisplayName("Leser kobling fra DVH og leser fra graphQl")
    void scenario41() {
        val inputEnhet = enhetTestTemplate("", null, 0, null);
        val inputRessurs = ressursTestTemplate("");
        kafkaUtils.sendFromKafkaMock(inputEnhet, inputRessurs);
        dvhRestConsumer.loadKoblingerFromDvh();

        val response = graphQLTestTemplate.postForResource("organisasjonsenhet/scenario41.graphql");
        assertThat(response, hasNoErrors());

        val outputEnhetJson = response.getList("$.data.organisasjonsenhet", JsonNode.class).get(0);
        val outputKoblinger = objectMapper.convertValue(outputEnhetJson.get("koblinger"), new TypeReference<List<OrganisasjonsenhetsKobling>>() {});

        assertEquals(1, outputKoblinger.size());
        val outputKobling = outputKoblinger.get(0);
        assertEquals("2021-01-01", outputKobling.getGyldigFom().toString());
        assertEquals("2099-12-31", outputKobling.getGyldigTom().toString());

        val outputRessurs = outputKoblinger.get(0).getRessurs();
        assertEquals(inputRessurs.getNavident(), outputRessurs.getNavIdent());
        assertEquals(inputRessurs.getPersonIdent(), outputRessurs.getPersonIdent());
    }

    @Test
    @SneakyThrows
    @DisplayName("Kobling er en tom liste om ingen kobling som refererer til organisasjonsenheten eksisterer.")
    void scenario42() {
        val inputEnhet = enhetTestTemplate("", null, 0, null);


        kafkaUtils.sendFromKafkaMock(inputEnhet);

        val response = graphQLTestTemplate.postForResource("organisasjonsenhet/scenario41.graphql");
        assertThat(response, hasNoErrors());

        val outputEnhetJson = response.getList("$.data.organisasjonsenhet", JsonNode.class).get(0);
        assertTrue(outputEnhetJson.get("koblinger").isEmpty());
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

