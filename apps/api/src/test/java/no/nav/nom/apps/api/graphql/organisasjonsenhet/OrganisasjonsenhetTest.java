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
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

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
        kafkaUtils.sendFromKafkaMock();
        dvhRestConsumer.loadFromDvh();

        val response = graphQLTestTemplate.postForResource("organisasjonsenhet/scenario1.graphql");
        assertThat(response, hasNoErrors());

        val outputEnhet = response.getList("$.data.organisasjonsenhet", Organisasjonsenhet.class);
        assertEquals(1, outputEnhet.size());
    }

    @Test
    @SneakyThrows
    @DisplayName("Sender organisasjonsenhet inn fra kafka og leser fra graphql uten id parameter")
    void scenario12() {
        kafkaUtils.sendFromKafkaMock();
        dvhRestConsumer.loadFromDvh();

        val response = graphQLTestTemplate.postForResource("organisasjonsenhet/scenario2.graphql");
        assertThat(response, hasNoErrors());
        assertEquals(3, response.getList("$.data.organisasjonsenhet", Organisasjonsenhet.class).size());
    }

    @Test
    @SneakyThrows
    @DisplayName("Sender inn organisasjonsenheter organisert i hierarki fra kafka, leser og validerer organiseringene")
    void scenario21(){
        kafkaUtils.sendFromKafkaMock();
        dvhRestConsumer.loadFromDvh();

        val response = graphQLTestTemplate.postForResource("organisasjonsenhet/scenario3.graphql");
        assertThat(response, hasNoErrors());
        val enhetResponse = response.getList("$.data.organisasjonsenhet", JsonNode.class).get(0);

        val organiserteEnheter = StreamSupport.stream(enhetResponse.get("organiseringer").spliterator(), false)
                .collect(Collectors.toMap(
                        o -> o.get("retning").asText(),
                        o -> objectMapper.convertValue(o.get("organisasjonsenhet"), Organisasjonsenhet.class)));

        assertEquals("testId1", organiserteEnheter.get("over").getId());
        assertEquals("testId3", organiserteEnheter.get("under").getId());
    }

    @Test
    @SneakyThrows
    @DisplayName("Tester at graphQL spørring på leder hvor enhet har ledig gir leder")
    void scenario31(){
        val inputLeder = ressursTestTemplate("");

        kafkaUtils.sendFromKafkaMock(inputLeder);
        dvhRestConsumer.loadFromDvh();

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
        kafkaUtils.sendFromKafkaMock();
        dvhRestConsumer.loadFromDvh();

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
        val inputRessurs = ressursTestTemplate("");
        kafkaUtils.sendFromKafkaMock(inputRessurs);
        dvhRestConsumer.loadFromDvh();

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
        kafkaUtils.sendFromKafkaMock();
        dvhRestConsumer.loadFromDvh();

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

