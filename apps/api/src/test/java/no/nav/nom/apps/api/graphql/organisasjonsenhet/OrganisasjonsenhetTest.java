package no.nav.nom.apps.api.graphql.organisasjonsenhet;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.val;
import no.nav.nom.apps.api.dvh.DvhRestConsumer;
import no.nav.nom.contract.nom.api.graphql.Organisasjonsenhet;
import no.nav.nom.contract.nom.api.graphql.OrganisasjonsenhetsKobling;
import no.nav.nom.contract.nom.api.graphql.OrganisasjonsenhetsLeder;
import no.nav.nom.contract.nom.hendelse.HendelseTo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("GraphQL.HentEnhet")
class OrganisasjonsenhetTest extends OrganisasjonsenhetTestBase {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DvhRestConsumer dvhRestConsumer;

    @Test
    @SneakyThrows
    @DisplayName("Sender organisasjonsenhet inn fra kafka og leser fra graphql med id parameter")
    void scenario11() {
        kafkaUtils.sendHendelseFromKafkaMock();
        dvhRestConsumer.loadFromDvh();

        val outputEnhet = postForResource("organisasjonsenhet/scenario1.graphql", false).getList("$.data.organisasjonsenhet", Organisasjonsenhet.class);
        assertEquals(1, outputEnhet.size());
    }

    @Test
    @SneakyThrows
    @DisplayName("Sender organisasjonsenhet inn fra kafka og leser fra graphql uten id parameter")
    void scenario12() {
        kafkaUtils.sendHendelseFromKafkaMock();
        dvhRestConsumer.loadFromDvh();

        val response = postForResource("organisasjonsenhet/scenario2.graphql", false);
        assertEquals(3, response.getList("$.data.organisasjonsenhet", Organisasjonsenhet.class).size());
    }

    @Test
    @SneakyThrows
    @DisplayName("Sender inn organisasjonsenheter organisert i hierarki fra kafka, leser og validerer organiseringene")
    void scenario21() {
        kafkaUtils.sendHendelseFromKafkaMock();
        dvhRestConsumer.loadFromDvh();

        val response = postForResource("organisasjonsenhet/scenario3.graphql", false);
        val enhetResponse = response.getList("$.data.organisasjonsenhet", JsonNode.class).get(0);

        val organiserteEnheter = StreamSupport.stream(enhetResponse.get("organiseringer").spliterator(), false)
                .collect(Collectors.toMap(
                        o -> o.get("retning").asText(),
                        o -> objectMapper.convertValue(o.get("organisasjonsenhet"), Organisasjonsenhet.class)));

        assertEquals("testId1", organiserteEnheter.get("over").getAgressoId());
        assertEquals("testId3", organiserteEnheter.get("under").getAgressoId());
    }

    @Test
    @SneakyThrows
    @DisplayName("Sender inn organisasjonsenheter organisert i hierarki fra kafka, filtrerer på retning")
    void scenario33() {
        kafkaUtils.sendHendelseFromKafkaMock();
        dvhRestConsumer.loadFromDvh();

        val response = postForResource("organisasjonsenhet/scenario33.graphql", false);
        val enhetResponse = response.getList("$.data.organisasjonsenhet", JsonNode.class).get(0);

        val organiserteEnheter = StreamSupport.stream(enhetResponse.get("organiseringer").spliterator(), false)
                .collect(Collectors.toMap(
                        o -> o.get("retning").asText(),
                        o -> objectMapper.convertValue(o.get("organisasjonsenhet"), Organisasjonsenhet.class)));

        assertThat(organiserteEnheter).hasSize(1);
        assertEquals("testId1", organiserteEnheter.get("over").getAgressoId());
    }

    @Test
    @SneakyThrows
    @DisplayName("Tester at graphQL spørring på leder hvor enhet har ledig gir leder")
    void scenario31() {
        HendelseTo hendelse = createRessursHendelse("");
        kafkaUtils.sendHendelseFromKafkaMock(hendelse);

        dvhRestConsumer.loadFromDvh();

        val response = postForResource("organisasjonsenhet/scenario4.graphql", false);

        val outputEnhetJson = response.getList("$.data.organisasjonsenhet", JsonNode.class).get(0);
        val outputLeder = objectMapper.convertValue(outputEnhetJson.get("leder"), new TypeReference<List<OrganisasjonsenhetsLeder>>() {
        }).get(0);

        assertEquals(getNavIdent(hendelse), outputLeder.getRessurs().getNavIdent());
        assertEquals(getPersonIdent(hendelse), outputLeder.getRessurs().getPersonIdent());
    }

    @Test
    @SneakyThrows
    @DisplayName("Tester at graphQL spørring på leder for enhet, hvor enhet ikke har leder")
    void scenario32() {
        kafkaUtils.sendHendelseFromKafkaMock();
        dvhRestConsumer.loadFromDvh();

        val response = postForResource("organisasjonsenhet/scenario32.graphql", false);

        val outputEnhetJson = response.getList("$.data.organisasjonsenhet", JsonNode.class).get(0);
        val outputLedere = objectMapper.convertValue(outputEnhetJson.get("leder"), new TypeReference<List<OrganisasjonsenhetsLeder>>() {
        });

        assertEquals(0, outputLedere.size());
    }

    @Test
    @SneakyThrows
    @DisplayName("Henter ut person navn i kontekst av enhet leder spørring")
    void scenario34() {
        HendelseTo hendelse = createRessursHendelse("");

        pdlUtils.addPersonToPdlResponse(true, getPersonIdent(hendelse));
        kafkaUtils.sendHendelseFromKafkaMock(hendelse);
        dvhRestConsumer.loadFromDvh();

        val response = postForResource("organisasjonsenhet/scenario34.graphql", false);

        val outputEnhetJson = response.getList("$.data.organisasjonsenhet", JsonNode.class).get(0);
        val lederNavn = outputEnhetJson.get("leder")
                .get(0).get("ressurs").get("person").get("navn");
        pdlUtils.assertNavn(getPersonIdent(hendelse), lederNavn);
    }


    @Test
    @SneakyThrows
    @DisplayName("Leser kobling fra DVH og leser fra graphQl")
    void scenario41() {
        HendelseTo hendelse = createRessursHendelse("");
        kafkaUtils.sendHendelseFromKafkaMock(hendelse);
        dvhRestConsumer.loadFromDvh();

        val response = postForResource("organisasjonsenhet/scenario41.graphql", false);

        val outputEnhetJson = response.getList("$.data.organisasjonsenhet", JsonNode.class).get(0);
        val outputKoblinger = objectMapper.convertValue(outputEnhetJson.get("koblinger"), new TypeReference<List<OrganisasjonsenhetsKobling>>() {
        });

        assertEquals(1, outputKoblinger.size());
        val outputKobling = outputKoblinger.get(0);
        assertEquals("2021-01-01", outputKobling.getGyldigFom().toString());
        assertEquals("2099-12-31", outputKobling.getGyldigTom().toString());

        val outputRessurs = outputKoblinger.get(0).getRessurs();
        assertEquals(getNavIdent(hendelse), outputRessurs.getNavIdent());
        assertEquals(getPersonIdent(hendelse), outputRessurs.getPersonIdent());
    }

    @Test
    @SneakyThrows
    @DisplayName("Kobling er en tom liste om ingen kobling som refererer til organisasjonsenheten eksisterer.")
    void scenario42() {
        kafkaUtils.sendHendelseFromKafkaMock();
        dvhRestConsumer.loadFromDvh();

        val response = postForResource("organisasjonsenhet/scenario41.graphql", false);

        val outputEnhetJson = response.getList("$.data.organisasjonsenhet", JsonNode.class).get(0);
        assertThat(outputEnhetJson.get("koblinger")).isEmpty();
    }

    @Test
    @SneakyThrows
    @DisplayName("Henter ut person navn i kontekst av enhet kobling spørring")
    void scenario43() {
        HendelseTo hendelse = createRessursHendelse("");
        pdlUtils.addPersonToPdlResponse(true, getPersonIdent(hendelse));
        kafkaUtils.sendHendelseFromKafkaMock(hendelse);
        dvhRestConsumer.loadFromDvh();

        val response = postForResource("organisasjonsenhet/scenario34.graphql", false);

        val outputEnhetJson = response.getList("$.data.organisasjonsenhet", JsonNode.class).get(0);
        val lederNavn = outputEnhetJson.get("koblinger")
                .get(0).get("ressurs").get("person").get("navn");
        pdlUtils.assertNavn(getPersonIdent(hendelse), lederNavn);
    }
}

