package no.nav.nom.apps.api.graphql.ressurs;

import com.fasterxml.jackson.databind.JsonNode;
import com.thomaskasene.wiremock.junit.WireMockStubs;
import lombok.SneakyThrows;
import lombok.val;
import no.nav.nom.apps.api.DvhOrgenhetMock;
import no.nav.nom.apps.api.dvh.DvhRestConsumer;
import no.nav.nom.apps.api.ressurs.contract.IRessurs;
import no.nav.nom.contract.nom.api.graphql.Ressurs;
import no.nav.nom.contract.nom.api.graphql.RessursResult;
import no.nav.nom.contract.nom.api.graphql.ResultCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


@DisplayName("GraphQL.HentRessurs")
class RessursTest extends RessursTestBase {

    @Autowired
    private DvhRestConsumer dvhRestConsumer;

    @Test
    @SneakyThrows
    @DisplayName("Leser inn med graphql navIdent parameter")
    void scenario11() {
        IRessurs ressurs = createRessursHendelse("");

        val response = postForResource("ressurs/scenario1.graphql", false);

        val outputRessurs = response.get("$.data.ressurs", Ressurs.class);
        assertEquals(ressurs.getNavIdent(), outputRessurs.getNavIdent());
        assertEquals(ressurs.getPersonIdent(), outputRessurs.getPersonIdent());
    }

    @Test
    @SneakyThrows
    @DisplayName("Leser inn med graphql navIdent og personIdent parameter bulk")
    void scenario112() {
        createRessursHendelse("1");
        createRessursHendelse("2");

        val response = postForResource("ressurs/scenario12.graphql", false);

        val outputRessurs = response.getList("$.data.ressurser", RessursResult.class);
        assertThat(outputRessurs).hasSize(4);
        RessursResult navIdent = get(outputRessurs, "testNavIdent1");
        assertEquals(navIdent.getId(), "testNavIdent1");
        assertEquals(navIdent.getCode(), ResultCode.OK);
        RessursResult navIdentNotFound = get(outputRessurs, "notFoundNavIdent");
        assertEquals(navIdentNotFound.getId(), "notFoundNavIdent");
        assertEquals(navIdentNotFound.getCode(), ResultCode.NOT_FOUND);

        RessursResult personIdent = get(outputRessurs, "testPersonIdent2");
        assertEquals(personIdent.getId(), "testPersonIdent2");
        assertEquals(personIdent.getCode(), ResultCode.OK);
        RessursResult personIdentNotFound = get(outputRessurs, "notFoundPersonIdent");
        assertEquals(personIdentNotFound.getId(), "notFoundPersonIdent");
        assertEquals(personIdentNotFound.getCode(), ResultCode.NOT_FOUND);
    }

    private RessursResult get(List<RessursResult> list, String ident) {
        return list.stream().filter(o -> ident.equals(o.getId())).findFirst().orElseThrow();
    }

    @Test
    @SneakyThrows
    @DisplayName("Leser inn med graphql personIdent parameter")
    void scenario12() {
        IRessurs ressurs = createRessursHendelse("");

        val response = postForResource("ressurs/scenario2.graphql", false);

        val outputRessurs = response.get("$.data.ressurs", Ressurs.class);
        assertEquals(ressurs.getNavIdent(), outputRessurs.getNavIdent());
        assertEquals(ressurs.getPersonIdent(), outputRessurs.getPersonIdent());
    }

    @Test
    @SneakyThrows
    @DisplayName("Leser inn med graphql uten parameter")
    void scenario13() {
        createRessursHendelse("1");
        createRessursHendelse("2");
        createRessursHendelse("3");


        val response = postForResource("ressurs/scenario3.graphql", false);

        val outputRessurs = response.getList("$.data.ressurser", Ressurs.class);
        assertEquals(3, outputRessurs.size());
    }

    @Test
    @SneakyThrows
    @DisplayName("Kan hente ut kobling for ressurs")
    void scenario21() {
        createRessursHendelse("2");
        val enhet = DvhOrgenhetMock.getBuilderWithDefault().ORGENHET_KODE("testId2").build();
        dvhOrgenhetMockUtils.addToResponse(enhet);
        dvhRestConsumer.loadFromDvh();

        val response = postForResource("ressurs/scenario42.graphql", false);

        val ressurs = response.get("$.data.ressurs", JsonNode.class);
        val actualOrganisasjonsEnhetId = ressurs.get("koblinger").get(0).get("organisasjonsenhet").get("agressoId").asText();
        assertEquals("testId2", actualOrganisasjonsEnhetId);
    }

    @Test
    @SneakyThrows
    @DisplayName("Kobling er tom liste om kobling for person ikke finnes")
    void scenario22() {
        createRessursHendelse("2");

        val response = postForResource("ressurs/scenario42.graphql", false);

        val ressurs = response.get("$.data.ressurs", JsonNode.class);
        assertTrue(ressurs.get("koblinger").isEmpty());
    }

    @Test
    @SneakyThrows
    @DisplayName("Kan hente sluttdato når satt")
    void scenario31() {
        IRessurs ressurs = createRessursHendelse("");
        val sluttdato = LocalDate.parse("2020-12-31");

        val response = postForResource("ressurs/scenario1.graphql", false);

        val outputRessurs = response.get("$.data.ressurs", Ressurs.class);
        assertEquals(ressurs.getNavIdent(), outputRessurs.getNavIdent());
        assertEquals(ressurs.getPersonIdent(), outputRessurs.getPersonIdent());
        assertEquals(sluttdato, outputRessurs.getSluttdato());
    }

    @Nested
    @WireMockStubs
    class Navn {

        @Test
        @SneakyThrows
        @DisplayName("Kan hente fulle navn fra PDL")
        void scenario31() {
            createRessursHendelse("3");
            createRessursHendelse("4");

            pdlUtils.addPersonToPdlResponse(true, "testPersonIdent3", "testPersonIdent4");

            val response = postForResource("ressurs/scenario5.graphql", false);

            val ressurser = response.getList("$.data.ressurser", JsonNode.class);
            assertEquals(2, ressurser.size());

            pdlUtils.assertNavn(ressurser.get(0).get("ressurs").get("personIdent").textValue(), ressurser.get(0).get("ressurs").get("person").get("navn"));
            pdlUtils.assertNavn(ressurser.get(1).get("ressurs").get("personIdent").textValue(), ressurser.get(1).get("ressurs").get("person").get("navn"));
        }

        @Test
        @SneakyThrows
        @DisplayName("Kan hente navn uten mellomnavn fra PDL")
        void scenario32() {
            createRessursHendelse("3");
            createRessursHendelse("4");

            pdlUtils.addPersonToPdlResponse(false, "testPersonIdent3", "testPersonIdent4");

            val response = postForResource("ressurs/scenario5.graphql", false);

            val ressurser = response.getList("$.data.ressurser", JsonNode.class);
            assertEquals(2, ressurser.size());

            pdlUtils.assertNavn(ressurser.get(0).get("ressurs").get("personIdent").textValue(), ressurser.get(0).get("ressurs").get("person").get("navn"));
            pdlUtils.assertNavn(ressurser.get(1).get("ressurs").get("personIdent").textValue(), ressurser.get(1).get("ressurs").get("person").get("navn"));
        }

        @Test
        @SneakyThrows
        @DisplayName("Hente navn hvor PDL er utilgjengelig")
        void scenario33() {
            createRessursHendelse("3");
            createRessursHendelse("4");

            pdlUtils.addPersonToPdlResponse(false, "testPersonIdent3", "testPersonIdent4");
            stubFor(post("/graphql").willReturn(aResponse().withStatus(HttpStatus.NOT_FOUND.value())));

            val response = postForResource("ressurs/scenario5.graphql", true);

            val errors = response.getList("$.errors", JsonNode.class);
            assertEquals(2, errors.size());

            val ressurs = response.getList("$.data.ressurser", JsonNode.class).get(0);
            assertTrue(ressurs.hasNonNull("id"));
            assertFalse(ressurs.hasNonNull("person"));
        }

        @Test
        @SneakyThrows
        @DisplayName("Hente navn hvor PDL er gir feil og manglende respons")
        void scenario34() {
            createRessursHendelse("3");
            createRessursHendelse("4");

            pdlUtils.addErrorToPdlResponse("testPersonIdent3", "bad_request");
            pdlUtils.addErrorToPdlResponse("testPersonIdent4", "not_found");

            val response = postForResource("ressurs/scenario5.graphql", true);

            val errors = response.getList("$.errors", JsonNode.class);
            assertEquals(2, errors.size());

            val ressurs = response.getList("$.data.ressurser", JsonNode.class).get(0);
            assertTrue(ressurs.hasNonNull("id"));
            assertFalse(ressurs.hasNonNull("person"));
        }
    }

    @Test
    @SneakyThrows
    @DisplayName("Kan hente email og visningsnavn fra AD")
    void scenario745() {
        createRessursHendelse("1");
        createRessursHendelse("2");
        createRessursHendelse("3");


        val response = postForResource("ressurs/scenario8.graphql", true);

        val ressurser = response.getList("$.data.ressurser", JsonNode.class);
        assertEquals(3, ressurser.size());
        ressurser.sort(Comparator.comparing(j -> j.get("id").asText()));

        assertThat(ressurser.get(0).get("ressurs").get("epost").asText()).isEqualTo("testNavIdent1@domain.no");
        assertThat(ressurser.get(0).get("ressurs").get("visningsNavn").asText()).isEqualTo("Visning 1");
        assertThat(ressurser.get(1).get("ressurs").get("epost").asText()).isEqualTo("testNavIdent2@domain.no");
        assertThat(ressurser.get(1).get("ressurs").get("visningsNavn").asText()).isEqualTo("Visning 2");
        assertTrue(ressurser.get(2).get("ressurs").get("epost").isNull());

        val errors = response.getList("$.errors", JsonNode.class);
        assertEquals(2, errors.size());
    }

    @Test
    @SneakyThrows
    @DisplayName("Hente enheter ressurs er leder for")
    void scenario35() {
        createRessursHendelse("");
        dvhOrgenhetMockUtils.addToResponse(
                DvhOrgenhetMock.getBuilderWithDefault()
                        .ORGENHET_KODE("testId1")
                        .ORGNODE_NIV("ORGENHET")
                        .ORGENHET_NAVN("testNavn")
                        .build()
        );
        dvhRestConsumer.loadFromDvh();

        val response = postForResource("ressurs/scenario6.graphql", false);

        val lederFor = response.get("$.data.ressurs.lederFor", JsonNode.class);
        assertThat(lederFor.size()).isEqualTo(1);
        assertThat(lederFor.get(0).get("organisasjonsenhet").get("agressoId").textValue()).isEqualTo("testId1");
    }

    @Test
    @SneakyThrows
    @DisplayName("Hente leder for ressurs")
    void scenario36() {
        val mockedEnhet = DvhOrgenhetMock.getBuilderWithDefault().ORGENHET_KODE("testId2")
                .ORGENHET_NAVN("testNavn")
                .ORGNODE_NIV("ORGENHET")
                .build();
        dvhOrgenhetMockUtils.addToResponse(mockedEnhet);
        createRessursHendelse("1");
        createRessursHendelse("2");

        dvhRestConsumer.loadFromDvh();

        val response = postForResource("ressurs/scenario7.graphql", false);

        val ledere = response.get("$.data.ressurs.ledere", JsonNode.class);
        assertThat(ledere.size()).isEqualTo(1);
        assertThat(ledere.get(0).get("ressurs").get("navIdent").textValue()).isEqualTo("testNavIdent1");
    }

    @Test
    @SneakyThrows
    @DisplayName("Hente leder for ressurs som er leder av sin enhet")
    void scenario37() {
        createRessursHendelse("1");
        createRessursHendelse("2");
        createRessursHendelse("4");
        val ressursLederForDenneEnheten = DvhOrgenhetMock.getBuilderWithDefault()
                        .ORGENHET_KODE("testId5").ORGENHET_NAVN("testNavn").ORGNODE_NIV("ORGENHET").build();
        val lederenErPaDenneEnheten = DvhOrgenhetMock.getBuilderWithDefault()
                .ORGENHET_KODE("testId4").ORGENHET_NAVN("testNavn4").ORGNODE_NIV("ORGNIV2").build();

        dvhOrgenhetMockUtils.addToResponse(ressursLederForDenneEnheten, lederenErPaDenneEnheten);

        dvhRestConsumer.loadFromDvh();

        val response = postForResource("ressurs/scenario71.graphql", false);

        val ledere = response.get("$.data.ressurs.ledere", JsonNode.class);
        assertThat(ledere.size()).isEqualTo(1);
        assertThat(ledere.get(0).get("ressurs").get("navIdent").textValue()).isEqualTo("testNavIdent1");
    }

    @Test
    @SneakyThrows
    @DisplayName("Ikke tillate henting av personNr uten riktig gruppemedlemskap")
    void scenario51(){
        createRessursHendelse("1");
        createRessursHendelse("2");
        createRessursHendelse("4");
        val ressursLederForDenneEnheten = DvhOrgenhetMock.getBuilderWithDefault()
                .ORGENHET_KODE("testId5").ORGENHET_NAVN("testNavn").ORGNODE_NIV("ORGENHET").build();
        val lederenErPaDenneEnheten = DvhOrgenhetMock.getBuilderWithDefault()
                .ORGENHET_KODE("testId4").ORGENHET_NAVN("testNavn4").ORGNODE_NIV("ORGNIV2").build();

        dvhOrgenhetMockUtils.addToResponse(ressursLederForDenneEnheten, lederenErPaDenneEnheten);

        dvhRestConsumer.loadFromDvh();
        val res = postForResourceWithoutAzp("ressurs/scenario51.graphql", true);
        assertThat(res.isOk());
    }
}
