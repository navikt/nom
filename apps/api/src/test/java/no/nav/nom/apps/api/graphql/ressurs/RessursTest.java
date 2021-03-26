package no.nav.nom.apps.api.graphql.ressurs;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.SneakyThrows;
import lombok.val;
import no.nav.nom.apps.api.dvh.DvhRestConsumer;
import no.nav.nom.contract.nom.api.graphql.Ressurs;
import no.nav.nom.contract.nom.hendelse.HendelseTo;
import no.nav.nom.contract.nom.hendelse.HendelseTo.Endringstype;
import no.nav.nom.contract.nom.hendelse.Hendelsestyper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


@DisplayName("GraphQL.HentRessurs")
class RessursTest extends RessursTestBase {

    @Autowired
    private DvhRestConsumer dvhRestConsumer;

    @Test
    @SneakyThrows
    @DisplayName("Sender inn ressurs fra kafka og leser inn med graphql navIdent parameter")
    void scenario11() {
        HendelseTo hendelse = createRessursHendelse("");
        kafkaUtils.sendHendelseFromKafkaMock(hendelse);

        val response = postForResource("ressurs/scenario1.graphql", false);

        val outputRessurs = response.getList("$.data.ressurs", Ressurs.class).get(0);
        assertEquals(getNavIdent(hendelse), outputRessurs.getNavIdent());
        assertEquals(getPersonIdent(hendelse), outputRessurs.getPersonIdent());
    }

    @Test
    @SneakyThrows
    @DisplayName("Sender inn ressurs fra kafka og leser inn med graphql personIdent parameter")
    void scenario12() {
        HendelseTo hendelse = createRessursHendelse("");
        kafkaUtils.sendHendelseFromKafkaMock(hendelse);

        val response = postForResource("ressurs/scenario2.graphql", false);

        val outputRessurs = response.getList("$.data.ressurs", Ressurs.class).get(0);
        assertEquals(getNavIdent(hendelse), outputRessurs.getNavIdent());
        assertEquals(getPersonIdent(hendelse), outputRessurs.getPersonIdent());
    }

    @Test
    @SneakyThrows
    @DisplayName("Sender inn ressurs fra kafka og leser inn med graphql uten parameter")
    void scenario13() {
        kafkaUtils.sendHendelseFromKafkaMock(createRessursHendelse("1"), createRessursHendelse("2"), createRessursHendelse("3"));

        val response = postForResource("ressurs/scenario3.graphql", false);

        val outputRessurs = response.getList("$.data.ressurs", Ressurs.class);
        assertEquals(3, outputRessurs.size());
    }

    @Test
    @SneakyThrows
    @DisplayName("Kan hente ut kobling for ressurs")
    void scenario21() {
        kafkaUtils.sendHendelseFromKafkaMock(createRessursHendelse("2"));
        dvhRestConsumer.loadFromDvh();

        val response = postForResource("ressurs/scenario42.graphql", false);

        val ressurs = response.getList("$.data.ressurs", JsonNode.class).get(0);
        val actualOrganisasjonsEnhetId = ressurs.get("koblinger").get(0).get("organisasjonsenhet").get("agressoId").asText();
        assertEquals("testId2", actualOrganisasjonsEnhetId);
    }

    @Test
    @SneakyThrows
    @DisplayName("Kobling er tom liste om kobling for person ikke finnes")
    void scenario22() {
        kafkaUtils.sendHendelseFromKafkaMock(createRessursHendelse("2"));

        val response = postForResource("ressurs/scenario42.graphql", false);

        val ressurs = response.getList("$.data.ressurs", JsonNode.class).get(0);
        assertTrue(ressurs.get("koblinger").isEmpty());
    }

    @Test
    @SneakyThrows
    @DisplayName("Kan hente fulle navn fra PDL")
    void scenario31() {
        kafkaUtils.sendHendelseFromKafkaMock(createRessursHendelse("3"), createRessursHendelse("4"));
        pdlUtils.addPersonToPdlResponse(true, "testPersonIdent3", "testPersonIdent4");

        val response = postForResource("ressurs/scenario5.graphql", false);

        val ressurser = response.getList("$.data.ressurs", JsonNode.class);
        assertEquals(2, ressurser.size());

        pdlUtils.assertNavn(ressurser.get(0).get("personIdent").asText(), ressurser.get(0).get("person").get("navn"));
        pdlUtils.assertNavn(ressurser.get(1).get("personIdent").asText(), ressurser.get(1).get("person").get("navn"));
    }

    @Test
    @SneakyThrows
    @DisplayName("Kan hente navn uten mellomnavn fra PDL")
    void scenario32() {
        kafkaUtils.sendHendelseFromKafkaMock(createRessursHendelse("3"), createRessursHendelse("4"));
        pdlUtils.addPersonToPdlResponse(false, "testPersonIdent3", "testPersonIdent4");

        val response = postForResource("ressurs/scenario5.graphql", false);

        val ressurser = response.getList("$.data.ressurs", JsonNode.class);
        assertEquals(2, ressurser.size());

        pdlUtils.assertNavn(ressurser.get(0).get("personIdent").asText(), ressurser.get(0).get("person").get("navn"));
        pdlUtils.assertNavn(ressurser.get(1).get("personIdent").asText(), ressurser.get(1).get("person").get("navn"));
    }

    @Test
    @SneakyThrows
    @DisplayName("Hente navn hvor PDL er utilgjengelig")
    void scenario33() {
        kafkaUtils.sendHendelseFromKafkaMock(createRessursHendelse("3"), createRessursHendelse("4"));
        pdlUtils.addPersonToPdlResponse(false, "testPersonIdent3", "testPersonIdent4");
        stubFor(post("/proxy/pdl").willReturn(aResponse().withStatus(HttpStatus.NOT_FOUND.value())));

        val response = postForResource("ressurs/scenario5.graphql", true);

        val errors = response.getList("$.errors", JsonNode.class);
        assertEquals(2, errors.size());

        val ressurs = response.getList("$.data.ressurs", JsonNode.class).get(0);
        assertTrue(ressurs.hasNonNull("navIdent"));
        assertTrue(ressurs.hasNonNull("personIdent"));
        assertFalse(ressurs.hasNonNull("person"));
    }

    @Test
    @SneakyThrows
    @DisplayName("Hente navn hvor PDL er gir feil og manglende respons")
    void scenario34() {
        kafkaUtils.sendHendelseFromKafkaMock(createRessursHendelse("3"), createRessursHendelse("4"));
        pdlUtils.addErrorToPdlResponse("testPersonIdent3", "bad_request");
        pdlUtils.addErrorToPdlResponse("testPersonIdent4", "not_found");

        val response = postForResource("ressurs/scenario5.graphql", true);

        val errors = response.getList("$.errors", JsonNode.class);
        assertEquals(2, errors.size());

        val ressurs = response.getList("$.data.ressurs", JsonNode.class).get(0);
        assertTrue(ressurs.hasNonNull("navIdent"));
        assertTrue(ressurs.hasNonNull("personIdent"));
        assertFalse(ressurs.hasNonNull("person"));
    }

    @Test
    @SneakyThrows
    @DisplayName("Ressurs hendelser sendt på kafka")
    void scenario23() {
        kafkaUtils.sendHendelseFromKafkaMock();
        dvhRestConsumer.loadFromDvh();

        var records = getRecordsFromKafka();
        assertThat(records).hasSize(4);

        var hendelseOneJson = records.get(0).value();
        var hendelseOne = om.readValue(hendelseOneJson, HendelseTo.class);
        assertThat(hendelseOne.getId()).isNotNull();
        assertThat(hendelseOne.getReqId()).isNotNull();
        assertThat(hendelseOne.getType()).isEqualTo(Hendelsestyper.RESSURS_V1.getKey());
        assertThat(hendelseOne.getEndringstype()).isEqualTo(Endringstype.OPPRETTET);
        assertThat(hendelseOne.getOpprettet()).isCloseTo(LocalDateTime.now(), within(1, SECONDS));

        var ressurs = new no.nav.nom.apps.api.domain.Ressurs(List.of(hendelseOne));
        assertThat(ressurs.getId()).isNotNull();
        assertThat(ressurs.getPersonIdent()).isEqualTo("12345678901");
        assertThat(ressurs.getNavIdent()).isEqualTo("O123456");
    }

}
