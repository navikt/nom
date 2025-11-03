package no.nav.nom.apps.api.graphql.ressurs;

import com.fasterxml.jackson.databind.JsonNode;
import no.nav.nom.apps.api.DvhOrgEnhetMock;
import no.nav.nom.apps.api.DvhOrgEnhetMockUtils;
import no.nav.nom.apps.api.common.PrefixedUlidCreator;
import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.dvh.DvhRestConsumer;
import no.nav.nom.apps.api.dvh.dto.DvhTelefon;
import no.nav.nom.apps.api.graphql.GraphQLExtendedIntegrationTestBase;
import no.nav.nom.apps.api.graphql.GraphQLMatchers;
import no.nav.nom.contract.nom.IdentType;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.TelefonType;
import no.nav.nom.contract.nom.api.graphql.Ressurs;
import no.nav.nom.contract.nom.api.graphql.RessursResult;
import no.nav.nom.contract.nom.api.graphql.ResultCode;
import no.nav.nom.contract.nom.eventsource.Jobbtelefon;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateRepository;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static no.nav.nom.apps.api.common.TestData.orgtilknytningState;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


@DisplayName("GraphQL.HentRessursEventsource")
class RessursTest extends GraphQLExtendedIntegrationTestBase {

    @Autowired
    private DvhRestConsumer dvhRestConsumer;

    @Autowired
    private DvhRepository dvhRepository;

    @Autowired
    public DvhOrgEnhetMockUtils dvhOrgenhetMockUtils;

    @Autowired
    OrgtilknytningStateRepository orgtilknytningStateRepository;

    @Test
    @DisplayName("Leser inn med graphql navIdent parameter")
    void scenario11() {
        RessursState ressursState = kafkaUtils.createRessursHendelseEventSource("");

        final var response = postForResource("ressurs/scenario1.graphql");

        final var outputRessurs = response.path("ressurs").entity(Ressurs.class).get();
        assertEquals(ressursState.navident(), outputRessurs.getNavIdent());
        assertEquals(sladdetPersonident(ressursState.personident()), outputRessurs.getPersonIdent());
    }

    @Test
    @DisplayName("Leser inn med graphql navIdent og personIdent parameter bulk")
    void scenario112() {
        kafkaUtils.createRessursHendelseEventSource("1");
        kafkaUtils.createRessursHendelseEventSource("2");

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
        RessursState ressurs = kafkaUtils.createRessursHendelseEventSource("");

        final var response = postForResource("ressurs/scenario2.graphql");

        final var outputRessurs = response.path("ressurs").entity(Ressurs.class).get();
        assertEquals(ressurs.navident(), outputRessurs.getNavIdent());
        assertEquals(sladdetPersonident(ressurs.personident()), outputRessurs.getPersonIdent());
    }

    @Test
    @DisplayName("Leser inn med graphql uten parameter bulk")
    void scenario13() {
        kafkaUtils.createRessursHendelseEventSource("1");
        kafkaUtils.createRessursHendelseEventSource("2");
        kafkaUtils.createRessursHendelseEventSource("3");

        final var response = postForResource("ressurs/scenario3.graphql");

        final var outputRessurs = response.path("ressurser").entityList(Ressurs.class).get();
        assertEquals(3, outputRessurs.size());
    }

    @Test
    @DisplayName("Kan hente ut kobling for ressurs")
    void scenario21() {
        final var ressursState = kafkaUtils.createRessursHendelseEventSource("2");
        final var enhet = DvhOrgEnhetMock.getBuilderWithDefault().ORGENHET_KODE("testId2").build();
        dvhOrgenhetMockUtils.addToResponse(enhet);
        dvhRestConsumer.loadRessurserOgOrgEnheter();

        // Since stream infrastructure is not up, the streams pipeline has not created state.
        // Lets do it manually, to test get methods below.
        orgEnhetStateRepository.update(dvhOrgenhetMockUtils.dvhMockToState(
            enhet,
            1,
            String.valueOf(enhet.getORGENHET_FRA_PERIODE()),
            String.valueOf(enhet.getORGENHET_TIL_PERIODE())
        ));
        orgtilknytningStateRepository.update(
            "FID_1",
            orgtilknytningState().toBuilder()
                .fid("FID_1")
                .navident(ressursState.navident())
                .orgEnhetId("ID_1").build()
        );

        //        dvhRestConsumer.loadKoblingerLedereOgTlfFromDvh();

        final var response = postForResource("ressurs/scenario42.graphql");

        final var ressurs = response.path("ressurs").entity(JsonNode.class).get();
        final var actualOrganisasjonsEnhetId = ressurs.get("koblinger").get(0).get("organisasjonsenhet").get("agressoId").asText();
        assertEquals("testId2", actualOrganisasjonsEnhetId);
    }

    @Test
    @DisplayName("Kobling er tom liste om kobling for person ikke finnes")
    void scenario22() {
        kafkaUtils.createRessursHendelseEventSource("3");

        final var response = postForResource("ressurs/scenario22.graphql");

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
            new Jobbtelefon("12345678", TelefonType.NAV_TJENESTE_TELEFON),
            null,
            LocalDate.now().plusDays(10),
            null,
            null,
            Collections.emptyList(),
            IdentType.STANDARD,
            "S123456",
            null
        );

        kafkaUtils.createRessursHendelseEventSource(ressursState);

        final var response = postForResource("ressurs/scenario1.graphql");

        final var outputRessurs = response.path("ressurs").entity(Ressurs.class).get();
        assertEquals(ressursState.navident(), outputRessurs.getNavIdent());
        assertEquals(sladdetPersonident(ressursState.personident()), outputRessurs.getPersonIdent());
        assertEquals(ressursState.sluttdato(), outputRessurs.getSluttdato());
    }

    @Test
    @DisplayName("Kan hente startdato når satt")
    void scenario32() {
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
            new Jobbtelefon("12345678", TelefonType.NAV_TJENESTE_TELEFON),
            null,
            LocalDate.now().plusDays(10),
            LocalDate.now().minusDays(10),
            null,
            Collections.emptyList(),
            IdentType.STANDARD,
            "S123456",
            null
        );

        kafkaUtils.createRessursHendelseEventSource(ressursState);

        final var response = postForResource("ressurs/scenario1.graphql");

        final var outputRessurs = response.path("ressurs").entity(Ressurs.class).get();
        assertEquals(ressursState.navident(), outputRessurs.getNavIdent());
        assertEquals(sladdetPersonident(ressursState.personident()), outputRessurs.getPersonIdent());
        assertEquals(ressursState.startdato(), outputRessurs.getStartdato());
    }


    @Test
    @DisplayName("Kan hente fulle navn fra NOM")
    void scenarioNavn1() {
        var r1 = kafkaUtils.createRessursHendelseEventSource("3");
        var r2 = kafkaUtils.createRessursHendelseEventSource("4");

        pdlUtils.addPersonToPdlResponse(true, "testPersonIdent3", "testPersonIdent4");

        final var response = postForResource("ressurs/scenario5.graphql");

        final var ressurser = response.path("ressurser").entityList(JsonNode.class).get();
        assertEquals(2, ressurser.size());

        // assertions krever samme rekkefølge i respons som i event-source
        assertEquals(r1.visningsnavn().getFornavnMellomnavn(),  ressurser.get(0).get("ressurs").get("fornavn").asText());
        assertEquals(r1.visningsnavn().etternavn(),             ressurser.get(0).get("ressurs").get("etternavn").asText());

        assertEquals(r2.visningsnavn().getFornavnMellomnavn(),  ressurser.get(1).get("ressurs").get("fornavn").asText());
        assertEquals(r2.visningsnavn().etternavn(),             ressurser.get(1).get("ressurs").get("etternavn").asText());
    }

    @Test
    @DisplayName("Kan hente navn uten mellomnavn fra NOM")
    void scenarioNavn2() {
        var r3 = kafkaUtils.createRessursHendelseEventSource("3");
        var r4 = kafkaUtils.createRessursHendelseEventSource("4");

        pdlUtils.addPersonToPdlResponse(false, "testPersonIdent3", "testPersonIdent4");

        final var response = postForResource("ressurs/scenario5.graphql");

        final var ressurser = response.path("ressurser").entityList(JsonNode.class).get();
        assertEquals(2, ressurser.size());

        // assertions krever samme rekkefølge i respons som i event-source
        assertEquals(r3.visningsnavn().getFornavnMellomnavn(),  ressurser.get(0).get("ressurs").get("fornavn").asText());
        assertEquals(r3.visningsnavn().etternavn(),             ressurser.get(0).get("ressurs").get("etternavn").asText());

        assertEquals(r4.visningsnavn().getFornavnMellomnavn(),  ressurser.get(1).get("ressurs").get("fornavn").asText());
        assertEquals(r4.visningsnavn().etternavn(),             ressurser.get(1).get("ressurs").get("etternavn").asText());
    }

    @Test
    @DisplayName("Kan hente email fra AD og visningsnavn")
    void scenario745() {
        final var ressurs1 = kafkaUtils.createRessursHendelseEventSource("1");
        final var ressurs2 = kafkaUtils.createRessursHendelseEventSource("2");
        final var ressurs3 = kafkaUtils.createRessursHendelseEventSource("3");

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
        kafkaUtils.createRessursHendelseEventSource("");
        var enhet = DvhOrgEnhetMock.getBuilderWithDefault()
            .ORGENHET_KODE("testId1")
            .ORGNODE_NIV("ORGENHET")
            .ORGENHET_NAVN("testNavn")
            .build();
        dvhOrgenhetMockUtils.addToResponse(enhet);
        dvhRestConsumer.loadRessurserOgOrgEnheter();

        // Since stream infrastructure is not up, the streams pipeline has not created state.
        // Lets do it manually, to test getmethods below.
        orgEnhetStateRepository.update(dvhOrgenhetMockUtils.dvhMockToState(
            enhet,
            1,
            String.valueOf(enhet.getORGENHET_FRA_PERIODE()),
            String.valueOf(enhet.getORGENHET_TIL_PERIODE())
        ));

        dvhRestConsumer.loadLedereAndOrganiseringerFromDvh();

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
        final var ressursState1 = kafkaUtils.createRessursHendelseEventSource("1");
        final var ressursState2 = kafkaUtils.createRessursHendelseEventSource("2");

        dvhRestConsumer.loadRessurserOgOrgEnheter();

        // Since stream infrastructure is not up, the streams pipeline has not created state.
        // Lets do it manually, to test getmethods below.
        orgEnhetStateRepository.update(dvhOrgenhetMockUtils.dvhMockToState(
            mockedEnhet,
            1,
            String.valueOf(mockedEnhet.getORGENHET_FRA_PERIODE()),
            String.valueOf(mockedEnhet.getORGENHET_TIL_PERIODE())
        ));
        orgtilknytningStateRepository.update(
            "FID_1",
            orgtilknytningState().toBuilder()
                .fid("FID_1")
                .navident(ressursState2.navident())
                .orgEnhetId("ID_1").build()
        );

        dvhRestConsumer.loadLedereAndOrganiseringerFromDvh();

        final var response = postForResource("ressurs/scenario7.graphql");

        final var ledere = response.path("ressurs.ledere").entity(JsonNode.class).get();
        assertThat(ledere.size()).isEqualTo(1);
        assertThat(ledere.get(0).get("ressurs").get("navIdent").textValue()).isEqualTo("testNavIdent1");
    }

    @Test
    @DisplayName("Hente leder for ressurs som er leder av sin enhet")
    void scenario37() {
        kafkaUtils.createRessursHendelseEventSource("1");   // Oppretter ressursen "testNavIdent1"
        kafkaUtils.createRessursHendelseEventSource("2");
        final var ressursState4 = kafkaUtils.createRessursHendelseEventSource("4");   // Oppretter ressursen "testNavIdent4"
        final var ressursLederForDenneEnheten = DvhOrgEnhetMock.getBuilderWithDefault()
            .ORGENHET_KODE("testId5").ORGENHET_NAVN("testNavn").ORGNODE_NIV("ORGENHET").build(); // Oppretter orgEnhet 5
        final var lederenErPaDenneEnheten = DvhOrgEnhetMock.getBuilderWithDefault()
            .ORGENHET_KODE("testId4").ORGENHET_NAVN("testNavn4").ORGNODE_NIV("ORGNIV2").build();  // Oppretter orgEnhet 4

        dvhOrgenhetMockUtils.addToResponse(ressursLederForDenneEnheten, lederenErPaDenneEnheten);   //  Returnere orgEnheter fra kall til DVH

        // Since stream infrastructure is not up, the streams pipeline has not created state.
        // Lets do it manually, to test getmethods below.
        orgEnhetStateRepository.update(dvhOrgenhetMockUtils.dvhMockToState(
            ressursLederForDenneEnheten,
            1,
            String.valueOf(ressursLederForDenneEnheten.getORGENHET_FRA_PERIODE()),
            String.valueOf(ressursLederForDenneEnheten.getORGENHET_TIL_PERIODE())
        ));   // orgEnhet 5 får ID 1
        orgEnhetStateRepository.update(dvhOrgenhetMockUtils.dvhMockToState(
            lederenErPaDenneEnheten,
            2,// orgEnhet 4 får ID 2
            String.valueOf(lederenErPaDenneEnheten.getORGENHET_FRA_PERIODE()),
            String.valueOf(lederenErPaDenneEnheten.getORGENHET_TIL_PERIODE())
        ));

        // Lagrer orgTilknytning for testNavIdent4 til orgEnhet ID_2 (testId4)
        orgtilknytningStateRepository.update(
            "FID_1",
            orgtilknytningState().toBuilder()
                .fid("FID_1")
                .navident(ressursState4.navident())
                .orgEnhetId("ID_2").build()
        );

        dvhRestConsumer.loadLedereAndOrganiseringerFromDvh();

        final var response = postForResource("ressurs/scenario71.graphql");

        final var ledere = response.path("ressurs.ledere").entity(JsonNode.class).get();
        assertThat(ledere.size()).isEqualTo(1);

        // Sjekke at ledere for testNavIdent4 er testNavIdent1
        assertThat(ledere.get(0).get("ressurs").get("navIdent").textValue()).isEqualTo("testNavIdent1");
    }

    @Test
    @DisplayName("Kan slå opp ressurs ved eksakt telefonnummer")
    void scenario52() {
        RessursState ressursState = kafkaUtils.createRessursHendelseEventSource("1");

        String telefonNummer = "4712345678";
        DvhTelefon mockDvhTelefon = DvhTelefon.builder()
            .nummer(telefonNummer)
            .navIdent(ressursState.navident())
            .type(DvhTelefon.DvhTelefonType.TJENESTETELEFON)
            .build();

        dvhRepository.saveTlf(List.of(mockDvhTelefon));

        final var response = postForResource("ressurs/scenario52.graphql");

        final var outputRessurs = response.path("ressurs").entity(JsonNode.class).get();
        assertEquals(ressursState.navident(), outputRessurs.get("navident").textValue());
        assertEquals(outputRessurs.get("telefon").get(0).get("nummer").textValue(), telefonNummer);
    }

    @Test
    @DisplayName("Kan slå opp ressurs ved telefonnummer uten landskode")
    void scenario53() {
        RessursState ressursState = kafkaUtils.createRessursHendelseEventSource("1");

        String telefonNummer = "4712345678";
        DvhTelefon mockDvhTelefon = DvhTelefon.builder()
            .nummer(telefonNummer)
            .navIdent(ressursState.navident())
            .type(DvhTelefon.DvhTelefonType.TJENESTETELEFON)
            .build();

        dvhRepository.saveTlf(List.of(mockDvhTelefon));

        final var response = postForResource("ressurs/scenario53.graphql");

        final var outputRessurs = response.path("ressurs").entity(JsonNode.class).get();
        assertEquals(ressursState.navident(), outputRessurs.get("navident").textValue());
        assertEquals(outputRessurs.get("telefon").get(0).get("nummer").textValue(), telefonNummer);
    }

    private String sladdetPersonident(String personident){
        return personident.replaceFirst(".{5}$","*****");
    }
}
