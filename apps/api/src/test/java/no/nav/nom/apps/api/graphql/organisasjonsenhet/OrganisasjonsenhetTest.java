package no.nav.nom.apps.api.graphql.organisasjonsenhet;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.val;
import no.nav.nom.apps.api.DvhOrgenhetMock;
import no.nav.nom.apps.api.dvh.DvhRestConsumer;
import no.nav.nom.apps.api.ressurs.contract.RessursI;
import no.nav.nom.contract.nom.api.graphql.OrgEnhet;
import no.nav.nom.contract.nom.api.graphql.OrgEnhetResult;
import no.nav.nom.contract.nom.api.graphql.OrgEnhetsKobling;
import no.nav.nom.contract.nom.api.graphql.OrgEnhetsLeder;
import no.nav.nom.contract.nom.api.graphql.ResultCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
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
    @DisplayName("Laster organisasjonsenhet inn fra dvh og leser fra graphql med id parameter")
    void scenario11() {
        val mockedEnhet = DvhOrgenhetMock.getBuilderWithDefault().ORGENHET_KODE("testId1").ORGENHET_NAVN("testNavn").ORGNODE_NIV("ORGENHET")
                .ENHETTYPE_NAVN("TYPEEE").build();
        dvhOrgenhetMockUtils.addToResponse(mockedEnhet);

        dvhRestConsumer.loadFromDvh();

        val outputEnhet = postForResource("organisasjonsenhet/scenario1.graphql", false).get("$.data.organisasjonsenhet", OrgEnhet.class);
        assertEquals(mockedEnhet.getORGENHET_KODE(), outputEnhet.getAgressoId());
        assertEquals(mockedEnhet.getENHETTYPE_NAVN(), outputEnhet.getAgressoOrgenhetType());
    }

    @Test
    @SneakyThrows
    @DisplayName("Laster organisasjonsenhet inn fra dvh og leser fra graphql med id parameter bulk")
    void scenario112() {
        val mockedEnhet = DvhOrgenhetMock.getBuilderWithDefault().ORGENHET_KODE("testId1").build();
        dvhOrgenhetMockUtils.addToResponse(mockedEnhet);
        dvhRestConsumer.loadFromDvh();

        val outputEnhet = postForResource("organisasjonsenhet/scenario12.graphql", false).getList("$.data.organisasjonsenheter", OrgEnhetResult.class);

        assertTrue(getEnhetInList(mockedEnhet.getORGENHET_KODE(), outputEnhet).isPresent());
        assertEquals(mockedEnhet.getORGENHET_KODE(), getEnhetInList(mockedEnhet.getORGENHET_KODE(), outputEnhet).get().getId());
        assertEquals(ResultCode.OK, getEnhetInList(mockedEnhet.getORGENHET_KODE(), outputEnhet).get().getCode());

        assertTrue(getEnhetInList("notFoundIdent", outputEnhet).isPresent());
        assertEquals(mockedEnhet.getORGENHET_KODE(), getEnhetInList(mockedEnhet.getORGENHET_KODE(), outputEnhet).get().getId());
        assertEquals(ResultCode.NOT_FOUND, getEnhetInList("notFoundIdent", outputEnhet).get().getCode());
    }

    @Test
    @SneakyThrows
    @DisplayName("Laster organisasjonsenhet inn fra dvh og leser fra graphql bulk filtrert på nivå")
    void scenario113() {
        val mockedEnhet = DvhOrgenhetMock.getBuilderWithDefault().ORGENHET_KODE("testId4").ORGNODE_NIV("ORGNIV2").build();
        dvhOrgenhetMockUtils.addToResponse(mockedEnhet);
        dvhRestConsumer.loadFromDvh();

        val outputEnhet = postForResource("organisasjonsenhet/scenario113.graphql", false).getList("$.data.organisasjonsenheter", OrgEnhetResult.class);
        assertEquals(mockedEnhet.getORGENHET_KODE(), outputEnhet.get(0).getId());
        assertEquals(ResultCode.OK, outputEnhet.get(0).getCode());
        assertEquals(mockedEnhet.getORGNODE_NIV(), outputEnhet.get(0).getOrgNiv());
    }

    @Test
    @SneakyThrows
    @DisplayName("Laster organisasjonsenhet inn fra dvh og leser fra graphql bulk filtrert på nivå og agressoId")
    void scenario114() {
        val mockedEnhet = DvhOrgenhetMock.getBuilderWithDefault().ORGENHET_KODE("testId4").ORGNODE_NIV("ORGNIV2").build();
        dvhOrgenhetMockUtils.addToResponse(mockedEnhet);
        dvhRestConsumer.loadFromDvh();

        val outputEnhet = postForResource("organisasjonsenhet/scenario114.graphql", false).getList("$.data.organisasjonsenheter", OrgEnhetResult.class);
        val actualTestId4 = getEnhetInList(mockedEnhet.getORGENHET_KODE(), outputEnhet);
        val actualNotFound = getEnhetInList("notFoundId", outputEnhet);

        assertTrue(actualTestId4.isPresent());
        assertEquals(mockedEnhet.getORGENHET_KODE(), actualTestId4.get().getId());
        assertEquals(mockedEnhet.getORGNODE_NIV(), actualTestId4.get().getOrgNiv());
        assertEquals(ResultCode.OK, actualTestId4.get().getCode());

        assertTrue(actualNotFound.isPresent());
        assertEquals("notFoundId", actualNotFound.get().getId());
        assertEquals("ORGNIV2", actualNotFound.get().getOrgNiv());
        assertEquals(ResultCode.NOT_FOUND, actualNotFound.get().getCode());
    }

    @Test
    @SneakyThrows
    @DisplayName("Laster organisasjonsenhet inn fra dvh og leser fra graphql uten id parameter")
    void scenario12() {
        dvhOrgenhetMockUtils.addToResponse(
                DvhOrgenhetMock.getBuilderWithDefault().ORGENHET_NAVN("test1").ORGENHET_KODE("test1").ORGNODE_NIV("ORGENHET").build(),
                DvhOrgenhetMock.getBuilderWithDefault().ORGENHET_NAVN("test2").ORGENHET_KODE("test2").ORGNODE_NIV("ORGENHET").build(),
                DvhOrgenhetMock.getBuilderWithDefault().ORGENHET_NAVN("test3").ORGENHET_KODE("test3").ORGNODE_NIV("ORGENHET").build(),
                DvhOrgenhetMock.getBuilderWithDefault().ORGENHET_NAVN("test4").ORGENHET_KODE("test4").ORGNODE_NIV("ORGENHET").build(),
                DvhOrgenhetMock.getBuilderWithDefault().ORGENHET_NAVN("test5").ORGENHET_KODE("test5").ORGNODE_NIV("ORGENHET").build()
        );

        dvhRestConsumer.loadFromDvh();

        val response = postForResource("organisasjonsenhet/scenario2.graphql", false);
        assertEquals(5, response.getList("$.data.organisasjonsenheter", OrgEnhetResult.class).size());
    }

    @Test
    @SneakyThrows
    @DisplayName("Laster inn organisasjonsenheter organisert i hierarki fra dvh, leser og validerer organiseringene")
    void scenario21() {
        val mockEnhet1 = DvhOrgenhetMock.getBuilderWithDefault().ORGENHET_KODE("testId4").ORGNODE_NIV("ORGNIV2").ORGENHET_NAVN("testNavn4").build();
        val mockEnhet2 = DvhOrgenhetMock.getBuilderWithDefault().ORGENHET_KODE("testId2").ORGNODE_NIV("ORGENHET").ORGENHET_NAVN("testNavn").build();
        val mockEnhet3 = DvhOrgenhetMock.getBuilderWithDefault().ORGENHET_KODE("testId3").ORGNODE_NIV("ORGNIV3").ORGENHET_NAVN("testNavn").build();
        dvhOrgenhetMockUtils.addToResponse(mockEnhet1, mockEnhet2, mockEnhet3);
        dvhRestConsumer.loadFromDvh();

        val response = postForResource("organisasjonsenhet/scenario3.graphql", false);
        val enhetResponse = response.get("$.data.organisasjonsenhet", JsonNode.class);

        val organiserteEnheter = StreamSupport.stream(enhetResponse.get("organiseringer").spliterator(), false)
                .collect(Collectors.toMap(
                        o -> o.get("retning").asText(),
                        o -> objectMapper.convertValue(o.get("organisasjonsenhet"), OrgEnhet.class)));

        assertEquals(mockEnhet1.getORGENHET_KODE(), organiserteEnheter.get("over").getAgressoId());
        assertEquals(mockEnhet2.getORGENHET_KODE(), organiserteEnheter.get("under").getAgressoId());
    }

    @Test
    @SneakyThrows
    @DisplayName("Laster inn organisasjonsenheter organisert i hierarki fra dvh, filtrerer på retning")
    void scenario33() {
        val mockEnhet1 = DvhOrgenhetMock.getBuilderWithDefault().ORGENHET_KODE("testId2").ORGENHET_NAVN("testNavn").ORGNODE_NIV("ORGENHET").build();
        val mockEnhet2 = DvhOrgenhetMock.getBuilderWithDefault().ORGENHET_KODE("testId3").ORGENHET_NAVN("testNavn").ORGNODE_NIV("ORGNIV3").build();

        dvhOrgenhetMockUtils.addToResponse(mockEnhet1, mockEnhet2);
        dvhRestConsumer.loadFromDvh();

        val response = postForResource("organisasjonsenhet/scenario33.graphql", false);
        val enhetResponse = response.get("$.data.organisasjonsenhet", JsonNode.class);

        val organiserteEnheter = StreamSupport.stream(enhetResponse.get("organiseringer").spliterator(), false)
                .collect(Collectors.toMap(
                        o -> o.get("retning").asText(),
                        o -> objectMapper.convertValue(o.get("organisasjonsenhet"), OrgEnhet.class)));

        assertThat(organiserteEnheter).hasSize(1);
        assertEquals(mockEnhet2.getORGENHET_KODE(), organiserteEnheter.get("over").getAgressoId());
    }

    @Test
    @SneakyThrows
    @DisplayName("Tester at graphQL spørring på leder hvor enhet har ledig gir leder")
    void scenario31() {
        val mockEnhet = DvhOrgenhetMock.getBuilderWithDefault().ORGENHET_KODE("testId1").ORGENHET_NAVN("testNavn").ORGNODE_NIV("ORGENHET").build();
        dvhOrgenhetMockUtils.addToResponse(mockEnhet);

        RessursI ressurs = createRessursHendelse("");


        dvhRestConsumer.loadFromDvh();

        val response = postForResource("organisasjonsenhet/scenario4.graphql", false);

        val outputEnhetJson = response.get("$.data.organisasjonsenhet", JsonNode.class);
        val outputLeder = objectMapper.convertValue(outputEnhetJson.get("leder"), new TypeReference<List<OrgEnhetsLeder>>() {
        }).get(0);

        assertEquals(ressurs.getNavIdent(), outputLeder.getRessurs().getNavIdent());
        assertEquals(ressurs.getPersonIdent(), outputLeder.getRessurs().getPersonIdent());
    }

    @Test
    @SneakyThrows
    @DisplayName("Tester at graphQL spørring på leder for enhet, hvor enhet ikke har leder")
    void scenario32() {
        val mockEnhet = DvhOrgenhetMock.getBuilderWithDefault().ORGENHET_KODE("testId3").ORGENHET_NAVN("testNavn").ORGNODE_NIV("ORGNIV3").build();
        dvhOrgenhetMockUtils.addToResponse(mockEnhet);
        dvhRestConsumer.loadFromDvh();

        val response = postForResource("organisasjonsenhet/scenario32.graphql", false);

        val outputEnhetJson = response.get("$.data.organisasjonsenhet", JsonNode.class);
        val outputLedere = objectMapper.convertValue(outputEnhetJson.get("leder"), new TypeReference<List<OrgEnhetsLeder>>() {
        });

        assertEquals(0, outputLedere.size());
    }

    @Test
    @SneakyThrows
    @DisplayName("Henter ut person navn i kontekst av enhet leder spørring")
    void scenario34() {
        val mockEnhet = DvhOrgenhetMock.getBuilderWithDefault().ORGENHET_KODE("testId1").ORGENHET_NAVN("testNavn").ORGNODE_NIV("ORGENHET").build();
        dvhOrgenhetMockUtils.addToResponse(mockEnhet);
        RessursI ressurs = createRessursHendelse("");

        pdlUtils.addPersonToPdlResponse(true, ressurs.getPersonIdent());
        dvhRestConsumer.loadFromDvh();

        val response = postForResource("organisasjonsenhet/scenario34.graphql", false);

        val outputEnhetJson = response.get("$.data.organisasjonsenhet", JsonNode.class);
        val lederNavn = outputEnhetJson.get("leder")
                .get(0).get("ressurs").get("person").get("navn");
        pdlUtils.assertNavn(ressurs.getPersonIdent(), lederNavn);
    }


    @Test
    @SneakyThrows
    @DisplayName("Leser kobling fra DVH og leser fra graphQl")
    void scenario41() {
        val mockEnhet = DvhOrgenhetMock.getBuilderWithDefault().ORGENHET_KODE("testId1").ORGENHET_NAVN("testNavn").ORGNODE_NIV("ORGENHET").build();
        dvhOrgenhetMockUtils.addToResponse(mockEnhet);

        RessursI ressurs = createRessursHendelse("");
        dvhRestConsumer.loadFromDvh();

        val response = postForResource("organisasjonsenhet/scenario41.graphql", false);

        val outputEnhetJson = response.get("$.data.organisasjonsenhet", JsonNode.class);
        val outputKoblinger = objectMapper.convertValue(outputEnhetJson.get("koblinger"), new TypeReference<List<OrgEnhetsKobling>>() {
        });

        assertEquals(1, outputKoblinger.size());
        val outputKobling = outputKoblinger.get(0);
        assertEquals("2021-01-01", outputKobling.getGyldigFom().toString());
        assertEquals("2099-12-31", outputKobling.getGyldigTom().toString());

        val outputRessurs = outputKoblinger.get(0).getRessurs();
        assertEquals(ressurs.getNavIdent(), outputRessurs.getNavIdent());
        assertEquals(ressurs.getPersonIdent(), outputRessurs.getPersonIdent());
    }

    @Test
    @SneakyThrows
    @DisplayName("Kobling er en tom liste om ingen kobling som refererer til organisasjonsenheten eksisterer.")
    void scenario42() {
        val mockEnhet = DvhOrgenhetMock.getBuilderWithDefault().ORGENHET_KODE("testId1").ORGENHET_NAVN("testNavn").ORGNODE_NIV("ORGENHET").build();
        dvhOrgenhetMockUtils.addToResponse(mockEnhet);

        dvhRestConsumer.loadFromDvh();

        val response = postForResource("organisasjonsenhet/scenario41.graphql", false);

        val outputEnhetJson = response.get("$.data.organisasjonsenhet", JsonNode.class);
        assertThat(outputEnhetJson.get("koblinger")).isEmpty();
    }

    @Test
    @SneakyThrows
    @DisplayName("Henter ut person navn i kontekst av enhet kobling spørring")
    void scenario43() {
        val mockEnhet = DvhOrgenhetMock.getBuilderWithDefault().ORGENHET_KODE("testId1").ORGENHET_NAVN("testNavn").ORGNODE_NIV("ORGENHET").build();
        dvhOrgenhetMockUtils.addToResponse(mockEnhet);

        RessursI ressurs = createRessursHendelse("");
        pdlUtils.addPersonToPdlResponse(true, ressurs.getPersonIdent());
        dvhRestConsumer.loadFromDvh();

        val response = postForResource("organisasjonsenhet/scenario34.graphql", false);

        val outputEnhetJson = response.get("$.data.organisasjonsenhet", JsonNode.class);
        val lederNavn = outputEnhetJson.get("koblinger")
                .get(0).get("ressurs").get("person").get("navn");
        pdlUtils.assertNavn(ressurs.getPersonIdent(), lederNavn);
    }

    private Optional<OrgEnhetResult> getEnhetInList(String id, List<OrgEnhetResult> organisasjonsenhetResult){
        return organisasjonsenhetResult.stream()
                .filter(e -> e.getId().equals(id))
                .findFirst();
    }
}

