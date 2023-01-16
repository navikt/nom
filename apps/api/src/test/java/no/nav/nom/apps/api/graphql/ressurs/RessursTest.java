package no.nav.nom.apps.api.graphql.ressurs;

import com.fasterxml.jackson.databind.JsonNode;
import com.thomaskasene.wiremock.junit.WireMockStubs;
import no.nav.nom.apps.api.DvhOrgEnhetMock;
import no.nav.nom.apps.api.common.PrefixedUlidCreator;
import no.nav.nom.apps.api.dvh.DvhRestConsumer;
import no.nav.nom.apps.api.graphql.GraphQLMatchers;
import no.nav.nom.apps.api.orgenhet.eventsourcing.state.OrgEnhetStateRepository;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.api.graphql.Ressurs;
import no.nav.nom.contract.nom.api.graphql.RessursResult;
import no.nav.nom.contract.nom.api.graphql.ResultCode;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.test.RsaStub;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.test.context.EmbeddedKafka;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


@DisplayName("GraphQL.HentRessursEventsource")
class RessursTest extends RessursTestBase {

    @Autowired
    private DvhRestConsumer dvhRestConsumer;

    @Autowired
    private OrgEnhetStateRepository orgEnhetStateRepository;

    @Test
    @DisplayName("Leser inn med graphql navIdent parameter")
    void scenario11() {
        RessursState ressursState = createRessursHendelseEventSource("");

        final var response = postForResource("ressurs/scenario1.graphql");

        final var outputRessurs = response.path("ressurs").entity(Ressurs.class).get();
        assertEquals(ressursState.navident(), outputRessurs.getNavIdent());
        assertEquals(ressursState.personident(), outputRessurs.getPersonIdent());
    }

    @Test
    @DisplayName("Leser inn med graphql navIdent og personIdent parameter bulk")
    void scenario112() {
        createRessursHendelseEventSource("1");
        createRessursHendelseEventSource("2");

        final var response = postForResource("ressurs/scenario12.graphql")
                .path("ressurser")
                .entityList(RessursResult.class)
                .get();

        assertThat(response).hasSize(4);
        RessursResult navIdent = get(response, "testNavIdent1");
        assertEquals(navIdent.getId(), "testNavIdent1");
        assertEquals(navIdent.getCode(), ResultCode.OK);
        RessursResult navIdentNotFound = get(response, "notFoundNavIdent");
        assertEquals(navIdentNotFound.getId(), "notFoundNavIdent");
        assertEquals(navIdentNotFound.getCode(), ResultCode.NOT_FOUND);

        RessursResult personIdent = get(response, "testPersonIdent2");
        assertEquals(personIdent.getId(), "testPersonIdent2");
        assertEquals(personIdent.getCode(), ResultCode.OK);
        RessursResult personIdentNotFound = get(response, "notFoundPersonIdent");
        assertEquals(personIdentNotFound.getId(), "notFoundPersonIdent");
        assertEquals(personIdentNotFound.getCode(), ResultCode.NOT_FOUND);
    }

    private RessursResult get(List<RessursResult> list, String ident) {
        return list.stream().filter(o -> ident.equals(o.getId())).findFirst().orElseThrow();
    }

    @Test
    @DisplayName("Leser inn med graphql personIdent parameter")
    void scenario12() {
        RessursState ressurs = createRessursHendelseEventSource("");

        final var response = postForResource("ressurs/scenario2.graphql");

        final var outputRessurs = response.path("ressurs").entity(Ressurs.class).get();
        assertEquals(ressurs.navident(), outputRessurs.getNavIdent());
        assertEquals(ressurs.personident(), outputRessurs.getPersonIdent());
    }

    @Test
    @DisplayName("Leser inn med graphql uten parameter bulk")
    void scenario13() {
        createRessursHendelseEventSource("1");
        createRessursHendelseEventSource("2");
        createRessursHendelseEventSource("3");

        final var response = postForResource("ressurs/scenario3.graphql");

        final var outputRessurs = response.path("ressurser").entityList(Ressurs.class).get();
        assertEquals(3, outputRessurs.size());
    }

    @Test
    @DisplayName("Kan hente ut kobling for ressurs")
    void scenario21() {
        createRessursHendelseEventSource("2");
        final var enhet = DvhOrgEnhetMock.getBuilderWithDefault().ORGENHET_KODE("testId2").build();
        dvhOrgenhetMockUtils.addToResponse(enhet);
        dvhRestConsumer.loadRessurserOgOrgEnheter();

        // Since stream infrastructure is not up, the streams pipeline has not created state.
        // Lets do it manually, to test get methods below.
        orgEnhetStateRepository.putEventAggregate(dvhOrgenhetMockUtils.dvhMockToState(enhet, 1, String.valueOf(enhet.getORGENHET_FRA_PERIODE()), Optional.of(String.valueOf(enhet.getORGENHET_TIL_PERIODE()))));

        dvhRestConsumer.loadKoblingerLedereOgTlfFromDvh();

        final var response = postForResource("ressurs/scenario42.graphql");

        final var ressurs = response.path("ressurs").entity(JsonNode.class).get();
        final var actualOrganisasjonsEnhetId = ressurs.get("koblinger").get(0).get("organisasjonsenhet").get("agressoId").asText();
        assertEquals("testId2", actualOrganisasjonsEnhetId);
    }

    @Test
    @DisplayName("Kobling er tom liste om kobling for person ikke finnes")
    void scenario22() {
        createRessursHendelseEventSource("2");

        final var response = postForResource("ressurs/scenario42.graphql");

        final var ressurs = response.path("ressurs").entity(JsonNode.class).get();
        assertTrue(ressurs.get("koblinger").isEmpty());
    }

    @Test
    @DisplayName("Kan hente sluttdato når satt")
    void scenario31() {
        RessursState ressursState = new RessursState(
                PrefixedUlidCreator.createRessursUlid(),
                "testPersonIdent",
                "testNavIdent",
                Sektor.NAV_STATLIG,
                new Visningsnavn(
                        "fornavn",
                        Optional.of("mellomnavn"),
                        "etternavn"
                ),
                Optional.of(LocalDate.now().plusDays(10))
        );

        createRessursHendelseEventSource(ressursState);

        final var response = postForResource("ressurs/scenario1.graphql");

        final var outputRessurs = response.path("ressurs").entity(Ressurs.class).get();
        assertEquals(ressursState.navident(), outputRessurs.getNavIdent());
        assertEquals(ressursState.personident(), outputRessurs.getPersonIdent());
        assertEquals(ressursState.sluttdato().orElseThrow(), outputRessurs.getSluttdato());
    }

    @Nested
    @WireMockStubs(RsaStub.class)
    @EmbeddedKafka(partitions = 1, topics = {"nom-state-v1"}, controlledShutdown = true)
    class Navn {

        @Test
        @DisplayName("Kan hente fulle navn fra PDL")
        void scenario31() {
            createRessursHendelseEventSource("3");
            createRessursHendelseEventSource("4");

            pdlUtils.addPersonToPdlResponse(true, "testPersonIdent3", "testPersonIdent4");

            final var response = postForResource("ressurs/scenario5.graphql");

            final var ressurser = response.path("ressurser").entityList(JsonNode.class).get();
            assertEquals(2, ressurser.size());

            pdlUtils.assertNavn(ressurser.get(0).get("ressurs").get("personIdent").textValue(), ressurser.get(0).get("ressurs").get("person").get("navn"));
            pdlUtils.assertNavn(ressurser.get(1).get("ressurs").get("personIdent").textValue(), ressurser.get(1).get("ressurs").get("person").get("navn"));
        }

        @Test
        @DisplayName("Kan hente navn uten mellomnavn fra PDL")
        void scenario32() {
            createRessursHendelseEventSource("3");
            createRessursHendelseEventSource("4");

            pdlUtils.addPersonToPdlResponse(false, "testPersonIdent3", "testPersonIdent4");

            final var response = postForResource("ressurs/scenario5.graphql");

            final var ressurser = response.path("ressurser").entityList(JsonNode.class).get();
            assertEquals(2, ressurser.size());

            pdlUtils.assertNavn(ressurser.get(0).get("ressurs").get("personIdent").textValue(), ressurser.get(0).get("ressurs").get("person").get("navn"));
            pdlUtils.assertNavn(ressurser.get(1).get("ressurs").get("personIdent").textValue(), ressurser.get(1).get("ressurs").get("person").get("navn"));
        }

        @Test
        @DisplayName("Hente navn hvor PDL er utilgjengelig")
        void scenario33() {
            createRessursHendelseEventSource("3");
            createRessursHendelseEventSource("4");

            pdlUtils.addPersonToPdlResponse(false, "testPersonIdent3", "testPersonIdent4");
            stubFor(post("/graphql").willReturn(aResponse().withStatus(HttpStatus.NOT_FOUND.value())));

            final var response = postForResource("ressurs/scenario5.graphql");

            response.errors().satisfy(errors -> assertEquals(2, errors.size()));

            final var ressurs = response.path("ressurser").entityList(JsonNode.class).get().get(0);
            assertTrue(ressurs.hasNonNull("id"));
            assertFalse(ressurs.hasNonNull("person"));
        }

        @Test
        @DisplayName("Hente navn hvor PDL er gir feil og manglende respons")
        void scenario34() {
            createRessursHendelseEventSource("3");
            createRessursHendelseEventSource("4");

            pdlUtils.addErrorToPdlResponse("testPersonIdent3", "bad_request");
            pdlUtils.addErrorToPdlResponse("testPersonIdent4", "not_found");

            final var response = postForResource("ressurs/scenario5.graphql");

            response.errors().satisfy(errors -> assertEquals(2, errors.size()));

            final var ressurs = response.path("ressurser").entityList(JsonNode.class).get().get(0);
            assertTrue(ressurs.hasNonNull("id"));
            assertFalse(ressurs.hasNonNull("person"));
        }
    }

    @Test
    @DisplayName("Kan hente email fra AD og visningsnavn")
    void scenario745() {
        final var ressurs1 = createRessursHendelseEventSource("1");
        final var ressurs2 = createRessursHendelseEventSource("2");
        final var ressurs3 = createRessursHendelseEventSource("3");

        final var response = postForResource("ressurs/scenario8.graphql");
        MatcherAssert.assertThat(response, GraphQLMatchers.hasErrors());

        final var ressurser = response.path("ressurser")
                .entityList(JsonNode.class)
                .hasSize(3)
                .get();

        ressurser.sort(Comparator.comparing(j -> j.get("id").asText()));

        assertThat(ressurser.get(0).get("ressurs").get("epost").asText()).isEqualTo("testNavIdent1@domain.no");
        assertThat(ressurser.get(0).get("ressurs").get("visningsNavn").asText()).isEqualTo(ressurs1.visningsnavn().getFormattedVisningsnavn());
        assertThat(ressurser.get(1).get("ressurs").get("epost").asText()).isEqualTo("testNavIdent2@domain.no");
        assertThat(ressurser.get(1).get("ressurs").get("visningsNavn").asText()).isEqualTo(ressurs2.visningsnavn().getFormattedVisningsnavn());
        assertThat(ressurser.get(2).get("ressurs").get("visningsNavn").asText()).isEqualTo(ressurs3.visningsnavn().getFormattedVisningsnavn());
        assertTrue(ressurser.get(2).get("ressurs").get("epost").isNull());

        response.errors().satisfy(errors -> assertEquals(1, errors.size()));
    }

    @Test
    @DisplayName("Hente enheter ressurs er leder for")
    void scenario35() {
        createRessursHendelseEventSource("");
        var enhet =  DvhOrgEnhetMock.getBuilderWithDefault()
            .ORGENHET_KODE("testId1")
            .ORGNODE_NIV("ORGENHET")
            .ORGENHET_NAVN("testNavn")
            .build();
        dvhOrgenhetMockUtils.addToResponse(enhet);
        dvhRestConsumer.loadRessurserOgOrgEnheter();

        // Since stream infrastructure is not up, the streams pipeline has not created state.
        // Lets do it manually, to test getmethods below.
        orgEnhetStateRepository.putEventAggregate(dvhOrgenhetMockUtils.dvhMockToState(enhet, 1, String.valueOf(enhet.getORGENHET_FRA_PERIODE()), Optional.of(String.valueOf(enhet.getORGENHET_TIL_PERIODE()))));

        dvhRestConsumer.loadKoblingerLedereOgTlfFromDvh();

        final var response = postForResource("ressurs/scenario6.graphql");

        final var lederFor = response.path("ressurs.lederFor").entity(JsonNode.class).get();
        assertThat(lederFor.size()).isEqualTo(1);
        assertThat(lederFor.get(0).get("organisasjonsenhet").get("agressoId").textValue()).isEqualTo("testId1");
    }

    @Test
    @DisplayName("Hente leder for ressurs")
    void scenario36() {
        final var mockedEnhet = DvhOrgEnhetMock.getBuilderWithDefault().ORGENHET_KODE("testId2")
                .ORGENHET_NAVN("testNavn")
                .ORGNODE_NIV("ORGENHET")
                .build();
        dvhOrgenhetMockUtils.addToResponse(mockedEnhet);
        createRessursHendelseEventSource("1");
        createRessursHendelseEventSource("2");

        dvhRestConsumer.loadRessurserOgOrgEnheter();

        // Since stream infrastructure is not up, the streams pipeline has not created state.
        // Lets do it manually, to test getmethods below.
        orgEnhetStateRepository.putEventAggregate(dvhOrgenhetMockUtils.dvhMockToState(mockedEnhet, 1, String.valueOf(mockedEnhet.getORGENHET_FRA_PERIODE()), Optional.of(String.valueOf(mockedEnhet.getORGENHET_TIL_PERIODE()))));

        dvhRestConsumer.loadKoblingerLedereOgTlfFromDvh();

        final var response = postForResource("ressurs/scenario7.graphql");

        final var ledere = response.path("ressurs.ledere").entity(JsonNode.class).get();
        assertThat(ledere.size()).isEqualTo(1);
        assertThat(ledere.get(0).get("ressurs").get("navIdent").textValue()).isEqualTo("testNavIdent1");
    }

    @Test
    @DisplayName("Hente leder for ressurs som er leder av sin enhet")
    void scenario37() {
        createRessursHendelseEventSource("1");
        createRessursHendelseEventSource("2");
        createRessursHendelseEventSource("4");
        final var ressursLederForDenneEnheten = DvhOrgEnhetMock.getBuilderWithDefault()
                .ORGENHET_KODE("testId5").ORGENHET_NAVN("testNavn").ORGNODE_NIV("ORGENHET").build();
        final var lederenErPaDenneEnheten = DvhOrgEnhetMock.getBuilderWithDefault()
                .ORGENHET_KODE("testId4").ORGENHET_NAVN("testNavn4").ORGNODE_NIV("ORGNIV2").build();

        dvhOrgenhetMockUtils.addToResponse(ressursLederForDenneEnheten, lederenErPaDenneEnheten);

        dvhRestConsumer.loadRessurserOgOrgEnheter();

        // Since stream infrastructure is not up, the streams pipeline has not created state.
        // Lets do it manually, to test getmethods below.
        orgEnhetStateRepository.putEventAggregate(dvhOrgenhetMockUtils.dvhMockToState(ressursLederForDenneEnheten, 1, String.valueOf(ressursLederForDenneEnheten.getORGENHET_FRA_PERIODE()), Optional.of(String.valueOf(ressursLederForDenneEnheten.getORGENHET_TIL_PERIODE()))));
        orgEnhetStateRepository.putEventAggregate(dvhOrgenhetMockUtils.dvhMockToState(lederenErPaDenneEnheten, 2, String.valueOf(lederenErPaDenneEnheten.getORGENHET_FRA_PERIODE()), Optional.of(String.valueOf(lederenErPaDenneEnheten.getORGENHET_TIL_PERIODE()))));

        dvhRestConsumer.loadKoblingerLedereOgTlfFromDvh();

        final var response = postForResource("ressurs/scenario71.graphql");

        final var ledere = response.path("ressurs.ledere").entity(JsonNode.class).get();
        assertThat(ledere.size()).isEqualTo(1);
        assertThat(ledere.get(0).get("ressurs").get("navIdent").textValue()).isEqualTo("testNavIdent1");
    }

    @Test
    @DisplayName("Ikke tillate henting av personNr uten riktig gruppemedlemskap")
    void scenario51() {
        createRessursHendelseEventSource("1");
        createRessursHendelseEventSource("2");
        createRessursHendelseEventSource("4");
        final var ressursLederForDenneEnheten = DvhOrgEnhetMock.getBuilderWithDefault()
                .ORGENHET_KODE("testId5").ORGENHET_NAVN("testNavn").ORGNODE_NIV("ORGENHET").build();
        final var lederenErPaDenneEnheten = DvhOrgEnhetMock.getBuilderWithDefault()
                .ORGENHET_KODE("testId4").ORGENHET_NAVN("testNavn4").ORGNODE_NIV("ORGNIV2").build();

        dvhOrgenhetMockUtils.addToResponse(ressursLederForDenneEnheten, lederenErPaDenneEnheten);

        dvhRestConsumer.loadRessurserOgOrgEnheter();
        dvhRestConsumer.loadKoblingerLedereOgTlfFromDvh();

        final var res = postForResourceWithoutAzp("ressurs/scenario51.graphql");
        res.errors().satisfy(errors -> {
            assertEquals(1, errors.size());
            assertTrue(errors.get(0).getMessage().contains("Forbidden"));
        });
    }
}
