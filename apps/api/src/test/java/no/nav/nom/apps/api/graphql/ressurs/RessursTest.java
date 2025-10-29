package no.nav.nom.apps.api.graphql.ressurs;

import com.fasterxml.jackson.databind.JsonNode;
import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.dvh.dto.DvhTelefon;
import no.nav.nom.apps.api.graphql.GraphQLMatchers;
import no.nav.nom.apps.api.graphql.GraphQLTestUtil;
import no.nav.nom.apps.api.orgenhet.OrgenhetTestUtils;
import no.nav.nom.apps.api.orgenhetleder.LederForRessurs;
import no.nav.nom.apps.api.orgenhetleder.OrgEnhetLederService;
import no.nav.nom.contract.nom.api.graphql.Ressurs;
import no.nav.nom.contract.nom.api.graphql.RessursResult;
import no.nav.nom.contract.nom.api.graphql.ResultCode;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;
import no.nav.nom.contract.nom.state.OrgEnhetLederState;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepository;
import no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateRepository;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import no.nav.nom.libs.test.MockOAuth2ServerInitializer;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static no.nav.nom.apps.api.TestUtil.createRessursState;
import static no.nav.nom.apps.api.common.TestData.orgtilknytningState;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;


@ActiveProfiles({"GraphQLTest", "test"})
@GraphQlTest
@ContextConfiguration(classes = {GraphqlTestConfig.class}, initializers = MockOAuth2ServerInitializer.class)
@AutoConfigureWireMock(port = 0) // For AD wiremock resultat (epost oppslag)
@DisplayName("GraphQL - Ressurs spørringer")
class RessursTest {
    @Autowired
    protected GraphQLTestUtil gqlUtil;

    @Autowired
    private DvhRepository dvhRepository;

    @Autowired
    protected OrgEnhetStateRepository orgEnhetStateRepository;

    @Autowired
    private RessursStateRepository ressursStateRepository;

    @Autowired
    OrgtilknytningStateRepository orgtilknytningStateRepository;

    @MockitoBean
    protected OrgEnhetLederService orgEnhetLederService;

    @BeforeEach
    protected void setup() {
        orgEnhetStateRepository.clear();
//        organiseringStateRepository.clear();
        ressursStateRepository.clear();
        orgtilknytningStateRepository.clear();
    }

    @Test
    @DisplayName("Leser inn med graphql navIdent parameter")
    void scenario11() {
        var ressurs = createRessursState("1").toBuilder().navident("testNavIdent").build();
        ressursStateRepository.update(ressurs);

        final var response = gqlUtil.postForResource("ressurs/scenario1.graphql");
        final var outputRessurs = response.path("ressurs").entity(Ressurs.class).get();

        assertEquals(ressurs.navident(), outputRessurs.getNavIdent());
        assertEquals(sladdetPersonident(ressurs.personident()), outputRessurs.getPersonIdent());
    }

    @Test
    @DisplayName("Leser inn med graphql navIdent og personIdent parameter bulk")
    void scenario112() {
        var ressurs1 = createRessursState("1").toBuilder().navident("testNavIdent1").personident("testPersonIdent1").build();
        var ressurs2 = createRessursState("2").toBuilder().navident("testNavIdent2").personident("testPersonIdent2").build();
        ressursStateRepository.update(ressurs1);
        ressursStateRepository.update(ressurs2);

        final var response = gqlUtil.postForResource("ressurs/scenario12.graphql")
            .path("ressurser")
            .entityList(RessursResult.class)
            .get();

        assertEquals(4, response.size());
        RessursResult navIdent = get(response, "testNavIdent1");
        assertEquals("testNavIdent1", navIdent.getId());
        assertEquals(ResultCode.OK, navIdent.getCode());

        RessursResult navIdentNotFound = get(response, "notFoundNavIdent");
        assertEquals("notFoundNavIdent", navIdentNotFound.getId());
        assertEquals(ResultCode.NOT_FOUND, navIdentNotFound.getCode());

        RessursResult personIdent = get(response, "testPersonIdent2");
        assertEquals("testPersonIdent2", personIdent.getId());
        assertEquals(ResultCode.OK, personIdent.getCode());

        RessursResult personIdentNotFound = get(response, "notFoundPersonIdent");
        assertEquals("notFoundPersonIdent", personIdentNotFound.getId());
        assertEquals(ResultCode.NOT_FOUND, personIdentNotFound.getCode());
    }

    @Test
    @DisplayName("Leser inn med graphql personIdent parameter")
    void scenario12() {
        var ressurs = createRessursState("1").toBuilder().personident("testPersonIdent").build();
        ressursStateRepository.update(ressurs);

        final var outputRessurs = gqlUtil.postForResource("ressurs/scenario2.graphql").path("ressurs").entity(Ressurs.class).get();

        assertEquals(ressurs.navident(), outputRessurs.getNavIdent());
        assertEquals(sladdetPersonident(ressurs.personident()), outputRessurs.getPersonIdent());
    }

    @Test
    @DisplayName("Leser inn med graphql uten parameter bulk")
    void scenario13() {
        var ressurs1 = createRessursState("1");
        var ressurs2 = createRessursState("2");
        var ressurs3 = createRessursState("3");
        ressursStateRepository.update(ressurs1);
        ressursStateRepository.update(ressurs2);
        ressursStateRepository.update(ressurs3);

        final var outputRessurs = gqlUtil.postForResource("ressurs/scenario3.graphql").path("ressurser").entityList(Ressurs.class).get();

        assertEquals(3, outputRessurs.size());
    }

    @Test
    @DisplayName("Kan hente ut kobling for ressurs")
    void scenario21() {
        var ressurs = createRessursState("2").toBuilder().navident("testNavIdent2").build();
        ressursStateRepository.update(ressurs);

        var orgenhet = OrgenhetTestUtils.createDefault().toBuilder().id("ID_1").agressoId("testId2").agressoOrgNivaa("ORGENHET").build();
        orgEnhetStateRepository.update(orgenhet);

        orgtilknytningStateRepository.update(
            "FID_1",
            orgtilknytningState().toBuilder()
                .fid("FID_1")
                .navident(ressurs.navident())
                .orgEnhetId("ID_1").build()
        );

        //        dvhRestConsumer.loadKoblingerLedereOgTlfFromDvh();

        final var response = gqlUtil.postForResource("ressurs/scenario42.graphql");

        final var outputRessurs = response.path("ressurs").entity(JsonNode.class).get();
        final var outputOrganisasjonsEnhetId = outputRessurs.get("koblinger").get(0).get("organisasjonsenhet").get("agressoId").asText();

        assertEquals(orgenhet.agressoId(), outputOrganisasjonsEnhetId);
    }

    @Test
    @DisplayName("Kobling er tom liste om kobling for person ikke finnes")
    void scenario22() {
        var ressurs = createRessursState("3").toBuilder().navident("testNavIdent3").build();
        ressursStateRepository.update(ressurs);

        final var response = gqlUtil.postForResource("ressurs/scenario22.graphql");

        final var outputRessurs = response.path("ressurs").entity(JsonNode.class).get();
        assertTrue(outputRessurs.get("koblinger").isEmpty());
    }

    @Test
    @DisplayName("Kan hente sluttdato når satt")
    void scenario31() {
        var ressurs = createRessursState("1").toBuilder().navident("testNavIdent").sluttdato(LocalDate.now().plusDays(10)).build();
        ressursStateRepository.update(ressurs);

        final var response = gqlUtil.postForResource("ressurs/scenario1.graphql");

        final var outputRessurs = response.path("ressurs").entity(Ressurs.class).get();
        assertEquals(ressurs.navident(), outputRessurs.getNavIdent());
        assertEquals(sladdetPersonident(ressurs.personident()), outputRessurs.getPersonIdent());
        assertEquals(ressurs.sluttdato(), outputRessurs.getSluttdato());
    }

    @Test
    @DisplayName("Kan hente startdato når satt")
    void scenario32() {
        var ressurs = createRessursState("1").toBuilder().navident("testNavIdent").personident("01011000001").build();
        ressursStateRepository.update(ressurs);

        final var response = gqlUtil.postForResource("ressurs/scenario1.graphql");

        final var outputRessurs = response.path("ressurs").entity(Ressurs.class).get();
        assertEquals(ressurs.navident(), outputRessurs.getNavIdent());
        assertEquals(sladdetPersonident(ressurs.personident()), outputRessurs.getPersonIdent());
        assertEquals(ressurs.startdato(), outputRessurs.getStartdato());
    }


    @Test
    @DisplayName("Kan hente fulle navn fra NOM")
    void scenarioNavn1() {
        var ressurs1 = createRessursState("1");
        var ressurs2 = createRessursState("2");
        ressursStateRepository.update(ressurs1);
        ressursStateRepository.update(ressurs2);

        final var response = gqlUtil.postForResource("ressurs/scenario5.graphql");

        final var ressurser = response.path("ressurser").entityList(JsonNode.class).get();
        assertEquals(2, ressurser.size());

        // assertions krever samme rekkefølge i respons som i event-source
        assertEquals(ressurs1.visningsnavn().getFornavnMellomnavn(),  ressurser.get(0).get("ressurs").get("fornavn").asText());
        assertEquals(ressurs1.visningsnavn().etternavn(),             ressurser.get(0).get("ressurs").get("etternavn").asText());

        assertEquals(ressurs2.visningsnavn().getFornavnMellomnavn(),  ressurser.get(1).get("ressurs").get("fornavn").asText());
        assertEquals(ressurs2.visningsnavn().etternavn(),             ressurser.get(1).get("ressurs").get("etternavn").asText());
    }

    @Test
    @DisplayName("Kan hente navn uten mellomnavn fra NOM")
    void scenarioNavn2() {
        var ressurs1 = createRessursState("1").toBuilder().visningsnavn(new Visningsnavn("F", Optional.empty(), "E")).build();
        var ressurs2 = createRessursState("2").toBuilder().visningsnavn(new Visningsnavn("F", Optional.empty(), "E")).build();
        ressursStateRepository.update(ressurs1);
        ressursStateRepository.update(ressurs2);

        final var response = gqlUtil.postForResource("ressurs/scenario5.graphql");

        final var ressurser = response.path("ressurser").entityList(JsonNode.class).get();
        assertEquals(2, ressurser.size());

        // assertions krever samme rekkefølge i respons som i event-source
        assertEquals(ressurs1.visningsnavn().getFornavnMellomnavn(),  ressurser.get(0).get("ressurs").get("fornavn").asText());
        assertEquals(ressurs1.visningsnavn().etternavn(),             ressurser.get(0).get("ressurs").get("etternavn").asText());

        assertEquals(ressurs2.visningsnavn().getFornavnMellomnavn(),  ressurser.get(1).get("ressurs").get("fornavn").asText());
        assertEquals(ressurs2.visningsnavn().etternavn(),             ressurser.get(1).get("ressurs").get("etternavn").asText());
    }

    @Test
    @DisplayName("Kan hente email fra AD og visningsnavn")
    void scenario745() {
        var ressurs1 = createRessursState("1");
        var ressurs2 = createRessursState("2");
        var ressurs3 = createRessursState("3");
        ressursStateRepository.update(ressurs1);
        ressursStateRepository.update(ressurs2);
        ressursStateRepository.update(ressurs3);

        final var response = gqlUtil.postForResource("ressurs/scenario8.graphql");
        MatcherAssert.assertThat(response, GraphQLMatchers.hasErrors());

        final var ressurser = response.path("ressurser")
            .entityList(JsonNode.class)
            .hasSize(3)
            .get();

        ressurser.sort(Comparator.comparing(j -> j.get("id").asText()));

        assertEquals("testNavIdent1@domain.no", ressurser.get(0).get("ressurs").get("epost").asText());
        assertEquals(ressurser.get(0).get("ressurs").get("visningsNavn").asText(), ressurs1.visningsnavn().getFormattedVisningsnavn());
        assertEquals("testNavIdent2@domain.no", ressurser.get(1).get("ressurs").get("epost").asText());
        assertEquals(ressurser.get(1).get("ressurs").get("visningsNavn").asText(), ressurs2.visningsnavn().getFormattedVisningsnavn());
        assertEquals(ressurser.get(2).get("ressurs").get("visningsNavn").asText(), ressurs3.visningsnavn().getFormattedVisningsnavn());
        assertTrue(ressurser.get(2).get("ressurs").get("epost").isNull());

        response.errors().satisfy(errors -> assertEquals(1, errors.size()));
    }

    @Test
    @DisplayName("Hente enheter ressurs er leder for")
    void scenario35() {
        var ressurs = createRessursState("1").toBuilder().navident("testNavIdent").build();
        ressursStateRepository.update(ressurs);

        var orgenhet = OrgenhetTestUtils.createDefault();
        orgEnhetStateRepository.update(orgenhet);

        doReturn(
            List.of(OrgEnhetLederState.builder()
                .fid("FID_1")
                .navident(ressurs.navident())
                .orgEnhetId(orgenhet.id())
                .startdato(LocalDate.now().minusYears(1))
                .build()
            )
        ).when(orgEnhetLederService).getOrgEnheterNavidentErLederFor(ressurs.navident());

        final var response = gqlUtil.postForResource("ressurs/scenario6.graphql");

        final var lederFor = response.path("ressurs.lederFor").entity(JsonNode.class).get();
        assertEquals(1, lederFor.size());
        assertEquals(orgenhet.agressoId(), lederFor.get(0).get("organisasjonsenhet").get("agressoId").textValue());
    }

    @Test
    @DisplayName("Hente leder for ressurs")
    void scenario36() {
        var leder = createRessursState("1").toBuilder().navident("testNavIdent").build();
        var ansatt = createRessursState("2").toBuilder().navident("testNavIdent2").build();
        ressursStateRepository.update(leder);
        ressursStateRepository.update(ansatt);

        var orgenhet = OrgenhetTestUtils.createDefault();
        orgEnhetStateRepository.update(orgenhet);

        var ot = orgtilknytningState().toBuilder()
            .fid("FID_1")
            .navident(ansatt.navident())
            .orgEnhetId("ID_1").build();

        orgtilknytningStateRepository.update("FID_1", ot);

        doReturn(
            List.of(new LederForRessurs(leder.navident(), orgenhet.id(), ot.gyldigFom(), ot.gyldigTom(), ot.erDagligOppfolging()))
        ).when(orgEnhetLederService).getLedereForRessurs(ansatt.navident(), false);

        final var response = gqlUtil.postForResource("ressurs/scenario7.graphql");

        final var ledere = response.path("ressurs.ledere").entity(JsonNode.class).get();
        assertEquals(1, ledere.size());
        assertEquals(leder.navident(), ledere.get(0).get("ressurs").get("navIdent").textValue());
    }

    @Test
    @DisplayName("Hente leder for ressurs som er leder av sin enhet - Note tester ikke orgEnhetLederService.getLedereForRessurs sin logikk")
    void scenario37() {
        var overLederRessurs = createRessursState("1");
        var ansattRessurs = createRessursState("4").toBuilder().navident("testNavIdent4").build();
        ressursStateRepository.update(overLederRessurs);
        ressursStateRepository.update(ansattRessurs);

        var orgenhet = OrgenhetTestUtils.createDefault();
        var overEnhet = OrgenhetTestUtils.createDefault();
        orgEnhetStateRepository.update(orgenhet);
        orgEnhetStateRepository.update(overEnhet);

        OrgtilknytningState ot = orgtilknytningState().toBuilder()
            .fid("FID_1")
            .navident(ansattRessurs.navident())
            .orgEnhetId(orgenhet.id()).build();
        orgtilknytningStateRepository.update(
            "FID_1",
            ot
        );

        doReturn(
            List.of(new LederForRessurs(overLederRessurs.navident(), orgenhet.id(), ot.gyldigFom(), ot.gyldigTom(), ot.erDagligOppfolging()))
        ).when(orgEnhetLederService).getLedereForRessurs(ansattRessurs.navident(), false);

        final var response = gqlUtil.postForResource("ressurs/scenario71.graphql");
        final var ledere = response.path("ressurs.ledere").entity(JsonNode.class).get();
        assertEquals(1, ledere.size());
        assertEquals(overLederRessurs.navident(), ledere.get(0).get("ressurs").get("navIdent").textValue());
    }

    @Test
    @DisplayName("Kan slå opp ressurs ved eksakt telefonnummer")
    void scenario52() {
        var ressurs = createRessursState("1");
        ressursStateRepository.update(ressurs);

        String telefonNummer = "4712345678";
        DvhTelefon mockDvhTelefon = DvhTelefon.builder()
            .nummer(telefonNummer)
            .navIdent(ressurs.navident())
            .type(DvhTelefon.DvhTelefonType.TJENESTETELEFON)
            .build();

        dvhRepository.saveTlf(List.of(mockDvhTelefon));

        final var response = gqlUtil.postForResource("ressurs/scenario52.graphql");

        final var outputRessurs = response.path("ressurs").entity(JsonNode.class).get();
        assertEquals(ressurs.navident(), outputRessurs.get("navident").textValue());
        assertEquals(telefonNummer, outputRessurs.get("telefon").get(0).get("nummer").textValue());
    }

    @Test
    @DisplayName("Kan slå opp ressurs ved telefonnummer uten landskode")
    void scenario53() {
        var ressurs = createRessursState("1");
        ressursStateRepository.update(ressurs);

        String telefonNummer = "4712345678";
        DvhTelefon mockDvhTelefon = DvhTelefon.builder()
            .nummer(telefonNummer)
            .navIdent(ressurs.navident())
            .type(DvhTelefon.DvhTelefonType.TJENESTETELEFON)
            .build();

        dvhRepository.saveTlf(List.of(mockDvhTelefon));

        final var response = gqlUtil.postForResource("ressurs/scenario53.graphql");

        final var outputRessurs = response.path("ressurs").entity(JsonNode.class).get();
        assertEquals(ressurs.navident(), outputRessurs.get("navident").textValue());
        assertEquals(telefonNummer, outputRessurs.get("telefon").get(0).get("nummer").textValue());
    }

    private RessursResult get(List<RessursResult> list, String ident) {
        return list.stream().filter(o -> ident.equals(o.getId())).findFirst().orElseThrow();
    }

    private String sladdetPersonident(String personident){
        return personident.replaceFirst(".{5}$","*****");
    }
}
