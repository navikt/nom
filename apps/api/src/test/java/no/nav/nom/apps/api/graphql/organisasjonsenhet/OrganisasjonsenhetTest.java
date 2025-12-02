package no.nav.nom.apps.api.graphql.organisasjonsenhet;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.nom.apps.api.graphql.GraphQLTestUtil;
import no.nav.nom.apps.api.graphql.GraphQlControllerForTest;
import no.nav.nom.apps.api.graphql.GraphqlTestConfig;
import no.nav.nom.apps.api.orgenhet.OrgenhetTestUtils;
import no.nav.nom.apps.gql.NomGraphQlAutoConfiguration;
import no.nav.nom.contract.nom.api.graphql.OrgEnhet;
import no.nav.nom.contract.nom.api.graphql.OrgEnhetResult;
import no.nav.nom.contract.nom.api.graphql.OrgEnhetsKobling;
import no.nav.nom.contract.nom.api.graphql.OrgEnhetsLeder;
import no.nav.nom.contract.nom.api.graphql.ResultCode;
import no.nav.nom.contract.nom.state.OrgEnhetLederState;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.contract.nom.state.OrganiseringState;
import no.nav.nom.libs.state.organisering.OrganiseringStateRepositoryImpl;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepositoryImpl;
import no.nav.nom.libs.state.orgenhetleder.OrgEnhetLederStateRepository;
import no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateRepositoryImpl;
import no.nav.nom.libs.state.ressurs.RessursStateRepositoryImpl;
import no.nav.nom.libs.test.MockOAuth2ServerInitializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.toMap;
import static no.nav.nom.apps.api.TestUtil.createRessursState;
import static no.nav.nom.apps.api.common.TestData.orgtilknytningState;
import static no.nav.nom.apps.api.orgenhet.OrgenhetTestUtils.orgEnhetStateToOrganiseringState;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("GraphQLTest")
@GraphQlTest
@ContextConfiguration(classes = {GraphqlTestConfig.class, NomGraphQlAutoConfiguration.class, GraphQlControllerForTest.class}, initializers = MockOAuth2ServerInitializer.class )
@DisplayName("GraphQL - Organisasjonsenhet spørringer")
class OrganisasjonsenhetTest {

    @Autowired protected GraphQLTestUtil gqlUtil;

    @Autowired protected OrgEnhetStateRepositoryImpl orgEnhetStateRepository;

    @Autowired protected OrganiseringStateRepositoryImpl organiseringStateRepository;

    @Autowired protected OrgtilknytningStateRepositoryImpl orgtilknytningStateRepository;

    @Autowired protected RessursStateRepositoryImpl ressursStateRepository;

    @Autowired
    public ObjectMapper objectMapper;

    @Autowired
    private OrgEnhetLederStateRepository orgEnhetLederStateRepository;

    @BeforeEach
    protected void setup() {
        orgEnhetStateRepository.clear();
        organiseringStateRepository.clear();
        ressursStateRepository.clear();
        orgtilknytningStateRepository.clear();
    }

    @Test
    @DisplayName("Laster organisasjonsenhet inn fra dvh og leser fra graphql med id parameter")
    void scenario11() {
        var orgenhet = OrgenhetTestUtils.createDefault().toBuilder().fid("FID_1").id("ID_1").agressoId("testId1").agressoOrgNivaa("ORGENHET").build();
        orgEnhetStateRepository.update(orgenhet);

        var outputEnhet = gqlUtil.postForResource("organisasjonsenhet/scenario1.graphql")
            .path("organisasjonsenhet")
            .entity(OrgEnhet.class)
            .get();

        assertEquals(orgenhet.navn(), outputEnhet.getNavn());
        assertEquals(orgenhet.agressoId(), outputEnhet.getAgressoId());
        assertEquals(orgenhet.agressoOrgenhetType(), outputEnhet.getAgressoOrgenhetType());

        outputEnhet = gqlUtil.postForResource("organisasjonsenhet/scenario13.graphql")
            .path("orgEnhet")
            .entity(OrgEnhet.class)
            .get();

        assertEquals(orgenhet.id(),outputEnhet.getId());
        assertEquals(orgenhet.navn(), outputEnhet.getNavn());
    }

    @Test
    @DisplayName("Laster organisasjonsenhet inn fra dvh og leser fra graphql med id parameter bulk")
    void scenario112() {
        var orgenhet = OrgenhetTestUtils.createDefault().toBuilder().agressoId("testId1").build();
        orgEnhetStateRepository.update(orgenhet);

        final var outputEnhet = gqlUtil.postForResource("organisasjonsenhet/scenario12.graphql")
            .path("organisasjonsenheter")
            .entityList(OrgEnhetResult.class)
            .get();

        assertTrue(getEnhetInList(orgenhet.agressoId(), outputEnhet).isPresent());
        assertEquals(orgenhet.agressoId(), getEnhetInList(orgenhet.agressoId(), outputEnhet).get().getId());
        assertEquals(ResultCode.OK, getEnhetInList(orgenhet.agressoId(), outputEnhet).get().getCode());

        assertTrue(getEnhetInList("notFoundIdent", outputEnhet).isPresent());
        assertEquals(orgenhet.agressoId(), getEnhetInList(orgenhet.agressoId(), outputEnhet).get().getId());
        assertEquals(ResultCode.NOT_FOUND, getEnhetInList("notFoundIdent", outputEnhet).get().getCode());
    }

    @Test
    @DisplayName("Laster organisasjonsenhet inn fra dvh og leser fra graphql bulk filtrert på nivå")
    void scenario113() {
        var orgenhet = OrgenhetTestUtils.createDefault().toBuilder().agressoId("testId4").agressoOrgNivaa("ORGNIV2").build();
        orgEnhetStateRepository.update(orgenhet);

        final var outputEnhet = gqlUtil.postForResource("organisasjonsenhet/scenario113.graphql")
            .path("organisasjonsenheter")
            .entityList(OrgEnhetResult.class)
            .get();

        assertEquals(orgenhet.agressoId(), outputEnhet.getFirst().getId());
        assertEquals(ResultCode.OK, outputEnhet.getFirst().getCode());
        assertEquals(orgenhet.agressoOrgNivaa(), outputEnhet.getFirst().getOrgNiv());
    }

    @Test
    @DisplayName("Laster organisasjonsenhet inn fra dvh og leser fra graphql bulk filtrert på nivå og agressoId")
    void scenario114() {
        var orgenhet = OrgenhetTestUtils.createDefault().toBuilder().agressoId("testId4").agressoOrgNivaa("ORGNIV2").build();
        orgEnhetStateRepository.update(orgenhet);

        final var outputEnhet = gqlUtil.postForResource("organisasjonsenhet/scenario114.graphql")
            .path("organisasjonsenheter")
            .entityList(OrgEnhetResult.class)
            .get();

        final var actualTestId4 = getEnhetInList(orgenhet.agressoId(), outputEnhet);
        final var actualNotFound = getEnhetInList("notFoundId", outputEnhet);

        assertTrue(actualTestId4.isPresent());
        assertEquals(orgenhet.agressoId(), actualTestId4.get().getId());
        assertEquals(orgenhet.agressoOrgNivaa(), actualTestId4.get().getOrgNiv());
        assertEquals(ResultCode.OK, actualTestId4.get().getCode());

        assertTrue(actualNotFound.isPresent());
        assertEquals("notFoundId", actualNotFound.get().getId());
        assertEquals("ORGNIV2", actualNotFound.get().getOrgNiv());
        assertEquals(ResultCode.NOT_FOUND, actualNotFound.get().getCode());
    }

    @Test
    @DisplayName("Laster organisasjonsenhet inn fra dvh og leser fra graphql uten id parameter")
    void scenario12() {
        var orgenhet1 = OrgenhetTestUtils.createDefault().toBuilder().navn("test1").agressoId("test1").agressoOrgNivaa("ORGENHET").build();
        var orgenhet2 = OrgenhetTestUtils.createDefault().toBuilder().navn("test2").agressoId("test2").agressoOrgNivaa("ORGENHET").build();
        var orgenhet3 = OrgenhetTestUtils.createDefault().toBuilder().navn("test3").agressoId("test3").agressoOrgNivaa("ORGENHET").build();
        var orgenhet4 = OrgenhetTestUtils.createDefault().toBuilder().navn("test4").agressoId("test4").agressoOrgNivaa("ORGENHET").build();
        var orgenhet5 = OrgenhetTestUtils.createDefault().toBuilder().navn("test5").agressoId("test5").agressoOrgNivaa("ORGENHET").build();

        orgEnhetStateRepository.update(orgenhet1);
        orgEnhetStateRepository.update(orgenhet2);
        orgEnhetStateRepository.update(orgenhet3);
        orgEnhetStateRepository.update(orgenhet4);
        orgEnhetStateRepository.update(orgenhet5);

        final var response = gqlUtil.postForResource("organisasjonsenhet/scenario2.graphql");
        assertEquals(5, response.path("organisasjonsenheter").entityList(OrgEnhetResult.class).get().size());
    }

    @Test
    @DisplayName("Laster inn organisasjonsenheter organisert i hierarki fra dvh, leser og validerer organiseringene")
    void scenario21() {
        var orgenhet1 = OrgenhetTestUtils.createDefault().toBuilder().navn("testNavn4").agressoId("testId4").agressoOrgNivaa("ORGNIV2").build();
        var orgenhet2 = OrgenhetTestUtils.createDefault().toBuilder().navn("testNavn").agressoId("testId2").agressoOrgNivaa("ORGENHET").build();
        var orgenhet3 = OrgenhetTestUtils.createDefault().toBuilder().navn("testNavn").agressoId("testId3").agressoOrgNivaa("ORGNIV3").build();

        orgEnhetStateRepository.update(orgenhet1);
        orgEnhetStateRepository.update(orgenhet2);
        orgEnhetStateRepository.update(orgenhet3);

        OrganiseringState organiseringState1 = getOrganiseringState("organisering_1", orgenhet2, orgenhet3, LocalDate.now(), null);
        OrganiseringState organiseringState2 = getOrganiseringState("organisering_2", orgenhet3, orgenhet1, LocalDate.now(), null);
        organiseringStateRepository.update(organiseringState1);
        organiseringStateRepository.update(organiseringState2);

        final var response = gqlUtil.postForResource("organisasjonsenhet/scenario3.graphql");
        final var enhetResponse = response.path("organisasjonsenhet").entity(JsonNode.class).get();

        final var organiserteEnheter = StreamSupport.stream(enhetResponse.get("organiseringer").spliterator(), false)
            .collect(
                toMap(
                    o -> o.get("retning").asText(),
                    o -> objectMapper.convertValue(o.get("orgEnhet"), OrgEnhet.class)
                )
            );

        assertEquals(orgenhet1.agressoId(), organiserteEnheter.get("over").getAgressoId());
        assertEquals(orgenhet2.agressoId(), organiserteEnheter.get("under").getAgressoId());
    }

    @Test
    @DisplayName("Ny organisering: Laster inn organisasjonsenheter organisert i hierarki fra dvh, leser og validerer organiseringene")
    void scenario21_new_organisering() {
        var orgenhetOver =  OrgenhetTestUtils.createDefault().toBuilder().id("ID_1").navn("testNavnOver").agressoId("testIdOver").agressoOrgNivaa("ORGNIV2").build();
        var orgenhetUnder = OrgenhetTestUtils.createDefault().toBuilder().id("ID_2").navn("testNavnUnder").agressoId("testIdUnder").agressoOrgNivaa("ORGENHET").build();
        var orgenhet =      OrgenhetTestUtils.createDefault().toBuilder().id("ID_3").navn("testNavn").agressoId("testId3").agressoOrgNivaa("ORGNIV3").build();

        orgEnhetStateRepository.update(orgenhetOver);
        orgEnhetStateRepository.update(orgenhetUnder);
        orgEnhetStateRepository.update(orgenhet);

        organiseringStateRepository.update(orgEnhetStateToOrganiseringState(1, orgenhet, orgenhetOver));
        organiseringStateRepository.update(orgEnhetStateToOrganiseringState(2, orgenhetUnder, orgenhet));

        final var response = gqlUtil.postForResource("organisasjonsenhet/scenario3.graphql");
        final var enhetResponse = response.path("organisasjonsenhet").entity(JsonNode.class).get();

        final var organiserteEnheter = StreamSupport.stream(enhetResponse.get("organiseringer").spliterator(), false)
            .collect(
                toMap(
                    o -> o.get("retning").asText(),
                    o -> objectMapper.convertValue(o.get("orgEnhet"), OrgEnhet.class)
                )
            );

        assertEquals("ID_1", organiserteEnheter.get("over").getId());
        assertEquals(orgenhetOver.agressoId(), organiserteEnheter.get("over").getAgressoId());

        assertEquals("ID_2", organiserteEnheter.get("under").getId());
        assertEquals(orgenhetUnder.agressoId(), organiserteEnheter.get("under").getAgressoId());
    }


    @Test
    @DisplayName("Laster inn organisasjonsenheter organisert i hierarki fra dvh, filtrerer på retning")
    void scenario33() {
        var orgenhet1 = OrgenhetTestUtils.createDefault().toBuilder().navn("testNavn").agressoId("testId2").agressoOrgNivaa("ORGENHET").build();
        var orgenhet2 = OrgenhetTestUtils.createDefault().toBuilder().navn("testNavn").agressoId("testId3").agressoOrgNivaa("ORGNIV3").build();

        orgEnhetStateRepository.update(orgenhet1);
        orgEnhetStateRepository.update(orgenhet2);

        OrganiseringState organiseringState = getOrganiseringState("organisering_1", orgenhet1, orgenhet2, LocalDate.now(), null);
        organiseringStateRepository.update(organiseringState);

        final var response = gqlUtil.postForResource("organisasjonsenhet/scenario33.graphql");
        final var enhetResponse = response.path("organisasjonsenhet").entity(JsonNode.class).get();

        final var organiserteEnheter = StreamSupport.stream(enhetResponse.get("organiseringer").spliterator(), false)
            .collect(toMap(
                o -> o.get("retning").asText(),
                o -> objectMapper.convertValue(o.get("orgEnhet"), OrgEnhet.class)
            ));

        assertThat(organiserteEnheter).hasSize(1);
        assertEquals(orgenhet2.agressoId(), organiserteEnheter.get("over").getAgressoId());
    }

    @Test
    @DisplayName("Ny organisering: Laster inn organisasjonsenheter organisert i hierarki fra dvh, filtrerer på retning")
    void scenario33_new_organisering() {
        var orgenhet = OrgenhetTestUtils.createDefault().toBuilder().navn("testNavn").agressoId("testId2").agressoOrgNivaa("ORGENHET").build();
        var orgenhetOver = OrgenhetTestUtils.createDefault().toBuilder().id("ID_2").navn("testNavn").agressoId("testId3").agressoOrgNivaa("ORGNIV3").build();
        var orgenhetUnder = OrgenhetTestUtils.createDefault().toBuilder().navn("testNavn").agressoId("testId3").agressoOrgNivaa("ORGNIV3").build();

        orgEnhetStateRepository.update(orgenhet);
        orgEnhetStateRepository.update(orgenhetOver);
        orgEnhetStateRepository.update(orgenhetUnder);

        organiseringStateRepository.update(orgEnhetStateToOrganiseringState(1, orgenhet, orgenhetOver));
        organiseringStateRepository.update(orgEnhetStateToOrganiseringState(2, orgenhetUnder, orgenhet));

        final var response = gqlUtil.postForResource("organisasjonsenhet/scenario33.graphql");
        final var enhetResponse = response.path("organisasjonsenhet").entity(JsonNode.class).get();

        final var organiserteEnheter = StreamSupport.stream(enhetResponse.get("organiseringer").spliterator(), false)
            .collect(toMap(
                o -> o.get("retning").asText(),
                o -> objectMapper.convertValue(o.get("orgEnhet"), OrgEnhet.class)
            ));

        assertThat(organiserteEnheter).hasSize(1);

        assertEquals("ID_2", organiserteEnheter.get("over").getId());
        assertEquals(orgenhetOver.agressoId(), organiserteEnheter.get("over").getAgressoId());
    }

    @Test
    @DisplayName("Tester at graphQL spørring på leder hvor enhet har leder gir leder")
    void scenario31() {
        var orgenhet = OrgenhetTestUtils.createDefault().toBuilder().id("ID_1").navn("testNavn").agressoId("testId1").agressoOrgNivaa("ORGENHET").build();

        orgEnhetStateRepository.update(orgenhet);

        var ressurs = createRessursState("1");
        ressursStateRepository.update(ressurs);

        var leder = OrgEnhetLederState.builder()
            .fid("FID_1")
            .navident(ressurs.navident())
            .orgEnhetId(orgenhet.id())
            .startdato(LocalDate.now().minusYears(1))
            .build();
        orgEnhetLederStateRepository.update(leder.fid(),leder);

        final var response = gqlUtil.postForResource("organisasjonsenhet/scenario4.graphql");
        final var outputEnhetJson = response.path("organisasjonsenhet").entity(JsonNode.class).get();
        final var outputLeder = objectMapper.convertValue(outputEnhetJson.get("leder"), new TypeReference<List<OrgEnhetsLeder>>() {}).getFirst();

        assertEquals(ressurs.navident(), outputLeder.ressurs().getNavIdent());
        assertEquals(ressurs.personident().replaceFirst(".{5}$","*****"), outputLeder.ressurs().getPersonIdent());
    }

    @Test
    @DisplayName("Tester at graphQL spørring på leder for enhet, hvor enhet ikke har leder")
    void scenario32() {
        var orgenhet = OrgenhetTestUtils.createDefault().toBuilder().id("ID_1").navn("testNavn").agressoId("testId3").agressoOrgNivaa("ORGNIV3").build();

        orgEnhetStateRepository.update(orgenhet);

        final var response = gqlUtil.postForResource("organisasjonsenhet/scenario32.graphql");
        final var outputEnhetJson = response.path("organisasjonsenhet").entity(JsonNode.class).get();
        final var outputLedere = objectMapper.convertValue(outputEnhetJson.get("leder"), new TypeReference<List<OrgEnhetsLeder>>() {});

        assertEquals(0, outputLedere.size());
    }

    @Test
    @DisplayName("Henter ut person navn i kontekst av enhet leder spørring - ikke slå opp mot PDL mer")
    void scenario34() {
        var orgenhet = OrgenhetTestUtils.createDefault().toBuilder().id("ID_1").navn("testNavn").agressoId("testId1").agressoOrgNivaa("ORGENHET").build();

        orgEnhetStateRepository.update(orgenhet);

        var ressurs = createRessursState("1");
        ressursStateRepository.update(ressurs);

        var leder = OrgEnhetLederState.builder()
            .fid("FID_1")
            .navident(ressurs.navident())
            .orgEnhetId(orgenhet.id())
            .startdato(LocalDate.now().minusYears(1))
            .build();
        orgEnhetLederStateRepository.update(leder.fid(),leder);

        final var response = gqlUtil.postForResource("organisasjonsenhet/scenario34.graphql");
        final var outputEnhetJson = response.path("organisasjonsenhet").entity(JsonNode.class).get();
        final var outputLederJson = outputEnhetJson.get("leder").get(0).get("ressurs");

        assertEquals(ressurs.visningsnavn().getFornavnMellomnavn(), outputLederJson.get("fornavn").asText());
        assertEquals(ressurs.visningsnavn().etternavn(), outputLederJson.get("etternavn").asText());
    }

    @Test
    @DisplayName("Leser kobling fra graphQl")
    void scenario41() {
        var orgenhet = OrgenhetTestUtils.createDefault().toBuilder().id("ID_1").navn("testNavn").agressoId("testId1").agressoOrgNivaa("ORGENHET").build();

        orgEnhetStateRepository.update(orgenhet);

        var ressurs = createRessursState("1");
        ressursStateRepository.update(ressurs);

        orgtilknytningStateRepository.update(
            "FID_1",
            orgtilknytningState().toBuilder()
                .fid("FID_1")
                .navident(ressurs.navident())
                .orgEnhetId("ID_1")
                .gyldigFom(LocalDate.of(2020, 1, 1))
                .build()
        );

        final var response = gqlUtil.postForResource("organisasjonsenhet/scenario41.graphql");

        final var outputEnhetJson = response.path("organisasjonsenhet").entity(JsonNode.class).get();
        final var outputKoblinger = objectMapper.convertValue(outputEnhetJson.get("koblinger"), new TypeReference<List<OrgEnhetsKobling>>() {});

        assertEquals(1, outputKoblinger.size());

        final var outputKobling = outputKoblinger.get(0);
        assertEquals("2020-01-01", outputKobling.getGyldigFom().toString());
        assertNull(outputKobling.getGyldigTom());

        final var outputRessurs = outputKoblinger.get(0).getRessurs();
        assertEquals(ressurs.navident(), outputRessurs.getNavIdent());
        assertEquals(sladdetPersonident(ressurs.personident()), outputRessurs.getPersonIdent());
    }

    @Test
    @DisplayName("Kobling er en tom liste om ingen kobling som refererer til organisasjonsenheten eksisterer.")
    void scenario42() {
        var orgenhet = OrgenhetTestUtils.createDefault().toBuilder().id("ID_1").navn("testNavn10").agressoId("testId10").agressoOrgNivaa("ORGENHET").build();

        orgEnhetStateRepository.update(orgenhet);

        final var response = gqlUtil.postForResource("organisasjonsenhet/scenario42.graphql");
        final var outputEnhetJson = response.path("organisasjonsenhet").entity(JsonNode.class).get();

        assertThat(outputEnhetJson.get("koblinger")).isEmpty();
    }

    @Test
    @DisplayName("Henter ut person navn i kontekst av enhet kobling spørring")
    void scenario43() {
        var orgenhet = OrgenhetTestUtils.createDefault().toBuilder().id("ID_1").navn("testNavn").agressoId("testId1").agressoOrgNivaa("ORGENHET").build();

        orgEnhetStateRepository.update(orgenhet);

        var ressurs = createRessursState("1");
        ressursStateRepository.update(ressurs);

        var leder = OrgEnhetLederState.builder()
            .fid("FID_1")
            .navident(ressurs.navident())
            .orgEnhetId(orgenhet.id())
            .startdato(LocalDate.now().minusYears(1))
            .build();
        orgEnhetLederStateRepository.update(leder.fid(),leder);

        final var response = gqlUtil.postForResource("organisasjonsenhet/scenario34.graphql");
        final var outputEnhetJson = response.path("organisasjonsenhet").entity(JsonNode.class).get();
        final var outputLederJson = outputEnhetJson.get("leder").get(0).get("ressurs");

        assertEquals(ressurs.visningsnavn().getFornavnMellomnavn(), outputLederJson.get("fornavn").asText());
        assertEquals(ressurs.visningsnavn().etternavn(), outputLederJson.get("etternavn").asText());
    }

    private Optional<OrgEnhetResult> getEnhetInList(String id, List<OrgEnhetResult> organisasjonsenhetResult) {
        return organisasjonsenhetResult.stream()
            .filter(e -> e.getId().equals(id))
            .findFirst();
    }

    private String sladdetPersonident(String personident){
        return personident.replaceFirst(".{5}$","*****");
    }

    private static OrganiseringState getOrganiseringState(String fid, OrgEnhetState orgeEnhetState, OrgEnhetState orgeEnhetStateOver, LocalDate gyldigFom, LocalDate gyldigTom) {
        return getOrganiseringState(fid, orgeEnhetState.id(), orgeEnhetState.navn(), orgeEnhetStateOver.id(), orgeEnhetStateOver.navn(), gyldigFom, gyldigTom);
    }

    private static OrganiseringState getOrganiseringState(String fid, String id, String navn, String idOver, String navnOver, LocalDate gyldigFom, LocalDate gyldigTom) {
        return OrganiseringState.builder()
            .fid(fid)
            .id(id)
            .navn(navn)
            .idOver(idOver)
            .navnOver(navnOver)
            .gyldigFom(gyldigFom)
            .gyldigTom(gyldigTom)
            .build();
    }
}
