package no.nav.nom.apps.api.graphql.ressurs;

import com.fasterxml.jackson.databind.JsonNode;
import graphql.GraphQLError;
import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
import no.nav.nom.apps.api.graphql.GraphQLMatchers;
import no.nav.nom.apps.api.graphql.GraphQLTestUtil;
import no.nav.nom.apps.api.graphql.GraphQlControllerForTest;
import no.nav.nom.apps.api.graphql.GraphqlTestConfig2;
import no.nav.nom.apps.api.orgenhet.OrgenhetTestUtils;
import no.nav.nom.apps.gql.ErrorCode;
import no.nav.nom.apps.gql.GraphQlAutoConfiguration;
import no.nav.nom.apps.gql.GraphqlSupportInterface;
import no.nav.nom.contract.nom.IdentType;
import no.nav.nom.contract.nom.TelefonType;
import no.nav.nom.contract.nom.api.graphql.Ressurs;
import no.nav.nom.contract.nom.api.graphql.RessursResult;
import no.nav.nom.contract.nom.api.graphql.ResultCode;
import no.nav.nom.contract.nom.eventsource.Jobbtelefon;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;
import no.nav.nom.contract.nom.state.OrgEnhetLederState;
import no.nav.nom.contract.nom.state.OrganiseringState;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.libs.state.organisering.OrganiseringStateRepositoryImpl;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepositoryImpl;
import no.nav.nom.libs.state.orgenhetleder.OrgEnhetLederStateRepositoryImpl;
import no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateRepositoryImpl;
import no.nav.nom.libs.state.ressurs.RessursStateRepositoryImpl;
import no.nav.nom.libs.test.MockOAuth2ServerInitializer;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static no.nav.nom.apps.api.TestUtil.createRessursState;
import static no.nav.nom.apps.api.TestUtil.createRessursStateBuilder;
import static no.nav.nom.apps.api.common.TestData.orgtilknytningState;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


@ActiveProfiles({"GraphQLTest"})
@GraphQlTest
@ContextConfiguration(classes = {GraphqlTestConfig2.class, GraphQlAutoConfiguration.class, GraphQlControllerForTest.class}, initializers = MockOAuth2ServerInitializer.class)
@DisplayName("GraphQL - Ressurs spørringer")
class RessursTest {
    @Autowired
    protected GraphQLTestUtil gqlUtil;

    @Autowired
    protected OrgEnhetStateRepositoryImpl orgEnhetStateRepository;

    @Autowired
    private RessursStateRepositoryImpl ressursStateRepository;

    @Autowired
    OrgtilknytningStateRepositoryImpl orgtilknytningStateRepository;

    @Autowired
    protected OrgEnhetLederStateRepositoryImpl orgEnhetLederStateRepository;

    @Autowired
    OrganiseringStateRepositoryImpl organiseringStateRepository;

    @MockitoSpyBean
    GraphqlSupportInterface graphqlSupportInterface = new GraphqlSupportInterface(){

        private final GraphqlSupportInterface delegate = GraphqlSupportInterface.dummyImplementation();

        @Override
        public String remedyEnhetId(String nomId) {
            return delegate.remedyEnhetId(nomId);
        }

        @Override
        public Map<Ressurs, DataFetcherResult<String>> epost(List<Ressurs> ressurser) {
            var out = delegate.epost(ressurser);
            var maybeNavIdent3Ressurs = out.keySet().stream().filter(it -> it.getNavident().equals("testNavIdent3")).findFirst();
            maybeNavIdent3Ressurs.ifPresent(r -> {
                var prev = out.get(r);
                var n = prev.transform(x -> x.error(GraphQLError.newError().build()));
                out.put(r, n);});

            return out;
        }

        @Override
        public void count(DataFetchingEnvironment dataFetchingEnvironment) {
            delegate.count(dataFetchingEnvironment);
        }

        @Override
        public boolean isWaitingToIndexLucene() {
            return delegate.isWaitingToIndexLucene();
        }
    };

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

        final var response = gqlUtil.postForResource("ressurs/scenario42.graphql");

        final var outputRessurs = response.path("ressurs").entity(JsonNode.class).get();
        final var outputOrganisasjonsEnhetId = outputRessurs.get("orgTilknytninger").get(0).get("orgEnhet").get("agressoId").asText();

        assertEquals(orgenhet.agressoId(), outputOrganisasjonsEnhetId);
    }

    @Test
    @DisplayName("Kobling er tom liste om kobling for person ikke finnes")
    void scenario22() {
        var ressurs = createRessursState("3").toBuilder().navident("testNavIdent3").build();
        ressursStateRepository.update(ressurs);

        final var response = gqlUtil.postForResource("ressurs/scenario22.graphql");

        final var outputRessurs = response.path("ressurs").entity(JsonNode.class).get();
        assertTrue(outputRessurs.get("orgTilknytninger").isEmpty());
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
//        when(graphqlSupportInterface.epost(Mockito.any()));

        final var response = gqlUtil.postForResource("ressurs/scenario8.graphql");
//        MatcherAssert.assertThat(response, GraphQLMatchers.hasErrors());

        final var ressurser = response.path("ressurser")
            .entityList(JsonNode.class)
            .hasSize(3)
            .get();


        ressurser.sort(Comparator.comparing(j -> j.get("id").asText()));

        assertEquals("testNavIdent1@dummy.mail", ressurser.get(0).get("ressurs").get("epost").asText());
        assertEquals(ressurser.get(0).get("ressurs").get("visningsNavn").asText(), ressurs1.visningsnavn().getFormattedVisningsnavn());
        assertEquals("testNavIdent2@dummy.mail", ressurser.get(1).get("ressurs").get("epost").asText());
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


        var lederState = OrgEnhetLederState.builder()
            .fid("FID_1")
            .navident(ressurs.navident())
            .orgEnhetId(orgenhet.id())
            .startdato(LocalDate.now().minusYears(1))
            .build();
        orgEnhetLederStateRepository.update(lederState.fid(),lederState);

        final var response = gqlUtil.postForResource("ressurs/scenario6.graphql");

        final var lederFor = response.path("ressurs.lederFor").entity(JsonNode.class).get();
        assertEquals(1, lederFor.size());
        assertEquals(orgenhet.agressoId(), lederFor.get(0).get("orgEnhet").get("agressoId").textValue());
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
            .orgEnhetId(orgenhet.id()).build();

        orgtilknytningStateRepository.update("FID_1", ot);

        var lederState = OrgEnhetLederState.builder().orgEnhetId(orgenhet.id()).navident(leder.navident()).startdato(LocalDate.now().minusYears(1)).fid("?").build();
        orgEnhetLederStateRepository.update(lederState.fid(),lederState);
//
//        doReturn(
//            List.of(new LederForRessurs(leder.navident(), orgenhet.id(), ot.gyldigFom(), ot.gyldigTom(), ot.erDagligOppfolging()))
//        ).when(orgEnhetLederStateRepository).getLedereForRessurs(ansatt.navident(), false);

        final var response = gqlUtil.postForResource("ressurs/scenario7.graphql");

        final var ledere = response.path("ressurs.ledere").entity(JsonNode.class).get();
        assertEquals(1, ledere.size());
        assertEquals(leder.navident(), ledere.get(0).get("ressurs").get("navIdent").textValue());
    }

    @Test
    @DisplayName("Hente leder for ressurs som er leder av sin enhet - Note tester ikke orgEnhetLederService.getLedereForRessurs sin logikk")
    void scenario37() {
        var overLederRessurs = createRessursState("1");
        var underLederRessurs = createRessursState("4").toBuilder().navident("testNavIdent4").build();
        ressursStateRepository.update(overLederRessurs);
        ressursStateRepository.update(underLederRessurs);

        var orgenhet = OrgenhetTestUtils.createDefault();
        var overEnhet = OrgenhetTestUtils.createDefault();
        var organisering = OrganiseringState.builder().fid("FIDO").idOver(overEnhet.id()).id(orgenhet.id()).gyldigFom(LocalDate.now().minusYears(1)).build();
        orgEnhetStateRepository.update(orgenhet);
        orgEnhetStateRepository.update(overEnhet);
        organiseringStateRepository.update(organisering);

        OrgtilknytningState ot = orgtilknytningState().toBuilder()
            .fid("FID_1")
            .navident(underLederRessurs.navident())
            .orgEnhetId(orgenhet.id())
            .erDagligOppfolging(true)
            .gyldigFom(LocalDate.now().minusYears(1))
            .build();

        orgtilknytningStateRepository.update("FID_1",ot);

        var underLederState = OrgEnhetLederState.builder()
            .fid("FID_2")
            .navident(underLederRessurs.navident())
            .orgEnhetId(orgenhet.id())
            .startdato(LocalDate.now().minusYears(1))
            .build();

        var overLederState = OrgEnhetLederState.builder()
            .fid("FID_3")
            .navident(overLederRessurs.navident())
            .orgEnhetId(overEnhet.id())
            .startdato(LocalDate.now().minusYears(1))
            .build();


        orgEnhetLederStateRepository.update(underLederState.fid(), underLederState);
        orgEnhetLederStateRepository.update(overLederState.fid(), overLederState);

//
//        doReturn(
//            List.of(new LederForRessurs(overLederRessurs.navident(), orgenhet.id(), ot.gyldigFom(), ot.gyldigTom(), ot.erDagligOppfolging()))
//        ).when(orgEnhetLederStateRepository).getLedereForRessurs(ansattRessurs.navident(), false);

        final var response = gqlUtil.postForResource("ressurs/scenario71.graphql");
        final var ledere = response.path("ressurs.ledere").entity(JsonNode.class).get();
        assertEquals(1, ledere.size());
        assertEquals(overLederRessurs.navident(), ledere.get(0).get("ressurs").get("navIdent").textValue());
    }

    @Test
    @DisplayName("Kan slå opp ressurs ved eksakt telefonnummer")
    void scenario52() {
        var telefonNummer = "4712345678";
        var ressurs = createRessursStateBuilder("1").jobbtelefon(new Jobbtelefon(telefonNummer,TelefonType.NAV_TJENESTE_TELEFON)).build();
        ressursStateRepository.update(ressurs);

//        String telefonNummer = "4712345678";
//        DvhTelefon mockDvhTelefon = DvhTelefon.builder()
//            .nummer(telefonNummer)
//            .navIdent(ressurs.navident())
//            .type(DvhTelefon.DvhTelefonType.TJENESTETELEFON)
//            .build();
//
//        dvhRepository.saveTlf(List.of(mockDvhTelefon));

        final var response = gqlUtil.postForResource("ressurs/scenario52.graphql");
        response.errors();

        final var outputRessurs = response.path("ressurs").entity(JsonNode.class).get();
        assertEquals(ressurs.navident(), outputRessurs.get("navident").textValue());
        assertEquals(telefonNummer.substring(2), outputRessurs.get("telefon").get(0).get("nummer").textValue());
    }

    @Test
    @DisplayName("Kan slå opp ressurs ved telefonnummer uten landskode")
    void scenario53() {
        var ressurs = createRessursState("1");
        ressursStateRepository.update(ressurs);

        String telefonNummer = "4712345678";

        final var response = gqlUtil.postForResource("ressurs/scenario53.graphql");

        final var outputRessurs = response.path("ressurs").entity(JsonNode.class).get();
        assertEquals(ressurs.navident(), outputRessurs.get("navident").textValue());
        assertEquals(telefonNummer.substring(2), outputRessurs.get("telefon").get(0).get("nummer").textValue());
    }


    @Test
    @DisplayName("Hente telefonnummer på ressurs")
    void scenario9_1() {
        var nomTelefon = "12345678"; //nr registrert i NOM
        var jobbtelefon = "87654321"; // nummmer fra Telenor/DVH
        var ressurs = createRessursState("1").toBuilder()
            .primaryTelefonnummer(nomTelefon)
            .jobbtelefon(new Jobbtelefon(jobbtelefon, TelefonType.NAV_TJENESTE_TELEFON))
            .build();
        ressursStateRepository.update(ressurs);

        final var response = gqlUtil.postForResource("ressurs/scenario9.graphql");

        final var outputRessurs = response.path("ressurs").entity(JsonNode.class).get();
        assertEquals(jobbtelefon, outputRessurs.get("primaryTelefon").textValue());
        assertEquals(2, outputRessurs.get("telefon").size());
        assertEquals(jobbtelefon, outputRessurs.get("telefon").get(0).get("nummer").textValue());
        assertEquals(nomTelefon, outputRessurs.get("telefon").get(1).get("nummer").textValue());
    }

    @Test
    @DisplayName("Hente telefonnummer på ressurs noe registrert med landskode og mellomrom")
    void scenario9_2() {
        var nomTelefon = "12345678"; //nr registrert i NOM
        var jobbtelefon = "87654321"; // nummmer fra Telenor/DVH
        var ressurs = createRessursState("1").toBuilder()
            .primaryTelefonnummer("+47 123 45 678")
            .jobbtelefon(new Jobbtelefon("87 65 43 21", TelefonType.NAV_TJENESTE_TELEFON))
            .build();
        ressursStateRepository.update(ressurs);

        final var response = gqlUtil.postForResource("ressurs/scenario9.graphql");

        final var outputRessurs = response.path("ressurs").entity(JsonNode.class).get();
        assertEquals(jobbtelefon, outputRessurs.get("primaryTelefon").textValue());
        assertEquals(2, outputRessurs.get("telefon").size());
        assertEquals(jobbtelefon, outputRessurs.get("telefon").get(0).get("nummer").textValue());
        assertEquals(nomTelefon, outputRessurs.get("telefon").get(1).get("nummer").textValue());
    }

    @Test
    @DisplayName("Hente telefonnummer på ressurs uten jobbtelefon")
    void scenario9_3() {
        var nomTelefon = "1234567890"; //nr registrert i NOM
        var ressurs = createRessursState("1").toBuilder()
            .primaryTelefonnummer(nomTelefon)
            .jobbtelefon(null)
            .build();
        ressursStateRepository.update(ressurs);

        final var response = gqlUtil.postForResource("ressurs/scenario9.graphql");

        final var outputRessurs = response.path("ressurs").entity(JsonNode.class).get();
        assertEquals(nomTelefon, outputRessurs.get("primaryTelefon").textValue());
        assertEquals(1, outputRessurs.get("telefon").size());
        assertEquals(nomTelefon, outputRessurs.get("telefon").get(0).get("nummer").textValue());
    }

    @Test
    @DisplayName("Hente telefonnummer på ressurs uten primaryTelefonnummer")
    void scenario9_4() {
        var ressurs = createRessursState("1").toBuilder()
            .jobbtelefon(null)
            .primaryTelefonnummer(null)
            .build();
        ressursStateRepository.update(ressurs);

        final var response = gqlUtil.postForResource("ressurs/scenario9.graphql");

        final var outputRessurs = response.path("ressurs").entity(JsonNode.class).get();
        assertNull(outputRessurs.get("primaryTelefon").textValue());
        assertEquals(0, outputRessurs.get("telefon").size());
    }

    @Test
    @DisplayName("Hente identtype på ressurs - STANDARD")
    void scenario10_1() {
        var ressurs = createRessursState("1").toBuilder()
            .identType(IdentType.STANDARD)
            .build();
        ressursStateRepository.update(ressurs);

        final var response = gqlUtil.postForResource("ressurs/scenario10.graphql");

        final var outputRessurs = response.path("ressurs").entity(JsonNode.class).get();
        assertEquals(IdentType.STANDARD.name(), outputRessurs.get("identType").textValue());
    }

    @Test
    @DisplayName("Hente identtype på ressurs - ROBOT")
    void scenario10_2() {
        var ressurs = createRessursState("1").toBuilder()
            .identType(IdentType.ROBOT)
            .build();
        ressursStateRepository.update(ressurs);

        final var response = gqlUtil.postForResource("ressurs/scenario10.graphql");

        final var outputRessurs = response.path("ressurs").entity(JsonNode.class).get();
        assertEquals(IdentType.ROBOT.name(), outputRessurs.get("identType").textValue());
    }

    @Test
    @DisplayName("Hente identtype på ressurs - null/UKJENT")
    void scenario10_3() {
        var ressurs = createRessursState("1").toBuilder()
            .identType(null)
            .build();
        ressursStateRepository.update(ressurs);

        final var response = gqlUtil.postForResource("ressurs/scenario10.graphql");

        final var outputRessurs = response.path("ressurs").entity(JsonNode.class).get();
        assertEquals(IdentType.UKJENT.name(), outputRessurs.get("identType").textValue());
    }

    private RessursResult get(List<RessursResult> list, String ident) {
        return list.stream().filter(o -> ident.equals(o.getId())).findFirst().orElseThrow();
    }

    private String sladdetPersonident(String personident){
        return personident.replaceFirst(".{5}$","*****");
    }
}
