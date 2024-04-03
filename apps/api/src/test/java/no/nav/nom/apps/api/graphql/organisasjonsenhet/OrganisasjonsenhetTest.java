package no.nav.nom.apps.api.graphql.organisasjonsenhet;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import no.nav.nom.apps.api.DvhOrgEnhetMock;
import no.nav.nom.apps.api.DvhOrgEnhetMockUtils;
import no.nav.nom.apps.api.dvh.DvhRestConsumer;
import no.nav.nom.apps.api.graphql.GraphQLTestBase;
import no.nav.nom.apps.api.organisering.OrganiseringConfig;
import no.nav.nom.apps.api.organisering.eventsourcing.state.OrganiseringStateRepository;
import no.nav.nom.apps.api.orgenhet.eventsourcing.state.OrgEnhetStateRepository;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.state.OrgtilknytningStateRepository;
import no.nav.nom.contract.nom.api.graphql.OrgEnhet;
import no.nav.nom.contract.nom.api.graphql.OrgEnhetResult;
import no.nav.nom.contract.nom.api.graphql.OrgEnhetsKobling;
import no.nav.nom.contract.nom.api.graphql.OrgEnhetsLeder;
import no.nav.nom.contract.nom.api.graphql.ResultCode;
import no.nav.nom.contract.nom.state.RessursState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.toMap;
import static no.nav.nom.apps.api.common.TestData.orgtilknytningState;
import static no.nav.nom.apps.api.orgenhet.OrgenhetTestUtils.orgEnhetStateToOrganiseringState;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

// TODO: Integrasjonstest - plasser og navngi riktig

@DisplayName("GraphQL.HentEnhet")
class OrganisasjonsenhetTest extends GraphQLTestBase {

    @Autowired
    private DvhRestConsumer dvhRestConsumer;

    @Autowired
    public DvhOrgEnhetMockUtils dvhOrgenhetMockUtils;

    @Autowired
    protected OrgEnhetStateRepository orgEnhetStateRepository;

    @Autowired
    protected OrganiseringStateRepository organiseringStateRepository;

    @Autowired
    OrgtilknytningStateRepository orgtilknytningStateRepository;

    @Test
    @DisplayName("Laster organisasjonsenhet inn fra dvh og leser fra graphql med id parameter")
    void scenario11() {
        final var mockedEnhet = DvhOrgEnhetMock.getBuilderWithDefault().ORGENHET_KODE("testId1").ORGENHET_NAVN("testNavn").ORGNODE_NIV("ORGENHET")
            .ENHETTYPE_NAVN("TYPEEE").build();
        dvhOrgenhetMockUtils.addToResponse(mockedEnhet);

        dvhRestConsumer.loadRessurserOgOrgEnheter();

        // Since stream infrastructure is not up, the streams pipeline has not created state.
        // Lets do it manually, to test getmethods below.
        final var evAgg = dvhOrgenhetMockUtils.dvhMockToState(mockedEnhet, 1, String.valueOf(mockedEnhet.getORGENHET_FRA_PERIODE()), Optional.of(String.valueOf(mockedEnhet.getORGENHET_TIL_PERIODE())));
        orgEnhetStateRepository.putEventAggregate(evAgg);

        dvhRestConsumer.loadLedereAndOrganiseringerFromDvh();

        final var outputEnhet = postForResource("organisasjonsenhet/scenario1.graphql")
            .path("organisasjonsenhet")
            .entity(OrgEnhet.class)
            .get();

        assertEquals(mockedEnhet.getORGENHET_KODE(), outputEnhet.getAgressoId());
        assertEquals(mockedEnhet.getENHETTYPE_NAVN(), outputEnhet.getAgressoOrgenhetType());

        final var outputEnhet2 = postForResource("organisasjonsenhet/scenario13.graphql")
            .path("orgEnhet")
            .entity(OrgEnhet.class)
            .get();

        assertEquals(evAgg.id(),outputEnhet2.getId());
    }

    @Test
    @DisplayName("Laster organisasjonsenhet inn fra dvh og leser fra graphql med id parameter bulk")
    void scenario112() {
        final var mockedEnhet = DvhOrgEnhetMock.getBuilderWithDefault().ORGENHET_KODE("testId1").build();
        dvhOrgenhetMockUtils.addToResponse(mockedEnhet);
        dvhRestConsumer.loadRessurserOgOrgEnheter();

        // Since stream infrastructure is not up, the streams pipeline has not created state.
        // Lets do it manually, to test getmethods below.
        orgEnhetStateRepository.putEventAggregate(dvhOrgenhetMockUtils.dvhMockToState(mockedEnhet, 1, String.valueOf(mockedEnhet.getORGENHET_FRA_PERIODE()), Optional.of(String.valueOf(mockedEnhet.getORGENHET_TIL_PERIODE()))));

        dvhRestConsumer.loadLedereAndOrganiseringerFromDvh();

        final var outputEnhet = postForResource("organisasjonsenhet/scenario12.graphql")
            .path("organisasjonsenheter")
            .entityList(OrgEnhetResult.class)
            .get();

        assertTrue(getEnhetInList(mockedEnhet.getORGENHET_KODE(), outputEnhet).isPresent());
        assertEquals(mockedEnhet.getORGENHET_KODE(), getEnhetInList(mockedEnhet.getORGENHET_KODE(), outputEnhet).get().getId());
        assertEquals(ResultCode.OK, getEnhetInList(mockedEnhet.getORGENHET_KODE(), outputEnhet).get().getCode());

        assertTrue(getEnhetInList("notFoundIdent", outputEnhet).isPresent());
        assertEquals(mockedEnhet.getORGENHET_KODE(), getEnhetInList(mockedEnhet.getORGENHET_KODE(), outputEnhet).get().getId());
        assertEquals(ResultCode.NOT_FOUND, getEnhetInList("notFoundIdent", outputEnhet).get().getCode());

    }

    @Test
    @DisplayName("Laster organisasjonsenhet inn fra dvh og leser fra graphql bulk filtrert på nivå")
    void scenario113() {
        final var mockedEnhet = DvhOrgEnhetMock.getBuilderWithDefault().ORGENHET_KODE("testId4").ORGNODE_NIV("ORGNIV2").build();
        dvhOrgenhetMockUtils.addToResponse(mockedEnhet);
        dvhRestConsumer.loadRessurserOgOrgEnheter();

        // Since stream infrastructure is not up, the streams pipeline has not created state.
        // Lets do it manually, to test getmethods below.
        orgEnhetStateRepository.putEventAggregate(dvhOrgenhetMockUtils.dvhMockToState(mockedEnhet, 1, String.valueOf(mockedEnhet.getORGENHET_FRA_PERIODE()), Optional.of(String.valueOf(mockedEnhet.getORGENHET_TIL_PERIODE()))));

        dvhRestConsumer.loadLedereAndOrganiseringerFromDvh();

        final var outputEnhet = postForResource("organisasjonsenhet/scenario113.graphql")
            .path("organisasjonsenheter")
            .entityList(OrgEnhetResult.class)
            .get();

        assertEquals(mockedEnhet.getORGENHET_KODE(), outputEnhet.get(0).getId());
        assertEquals(ResultCode.OK, outputEnhet.get(0).getCode());
        assertEquals(mockedEnhet.getORGNODE_NIV(), outputEnhet.get(0).getOrgNiv());
    }

    @Test
    @DisplayName("Laster organisasjonsenhet inn fra dvh og leser fra graphql bulk filtrert på nivå og agressoId")
    void scenario114() {
        final var mockedEnhet = DvhOrgEnhetMock.getBuilderWithDefault().ORGENHET_KODE("testId4").ORGNODE_NIV("ORGNIV2").build();
        dvhOrgenhetMockUtils.addToResponse(mockedEnhet);
        dvhRestConsumer.loadRessurserOgOrgEnheter();

        // Since stream infrastructure is not up, the streams pipeline has not created state.
        // Lets do it manually, to test getmethods below.
        orgEnhetStateRepository.putEventAggregate(dvhOrgenhetMockUtils.dvhMockToState(mockedEnhet, 1, String.valueOf(mockedEnhet.getORGENHET_FRA_PERIODE()), Optional.of(String.valueOf(mockedEnhet.getORGENHET_TIL_PERIODE()))));

        dvhRestConsumer.loadLedereAndOrganiseringerFromDvh();

        final var outputEnhet = postForResource("organisasjonsenhet/scenario114.graphql")
            .path("organisasjonsenheter")
            .entityList(OrgEnhetResult.class)
            .get();

        final var actualTestId4 = getEnhetInList(mockedEnhet.getORGENHET_KODE(), outputEnhet);
        final var actualNotFound = getEnhetInList("notFoundId", outputEnhet);

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
    @DisplayName("Laster organisasjonsenhet inn fra dvh og leser fra graphql uten id parameter")
    void scenario12() {
        var mockedEnhet1 = DvhOrgEnhetMock.getBuilderWithDefault().ORGENHET_NAVN("test1").ORGENHET_KODE("test1").ORGNODE_NIV("ORGENHET").build();
        var mockedEnhet2 = DvhOrgEnhetMock.getBuilderWithDefault().ORGENHET_NAVN("test2").ORGENHET_KODE("test2").ORGNODE_NIV("ORGENHET").build();
        var mockedEnhet3 = DvhOrgEnhetMock.getBuilderWithDefault().ORGENHET_NAVN("test3").ORGENHET_KODE("test3").ORGNODE_NIV("ORGENHET").build();
        var mockedEnhet4 = DvhOrgEnhetMock.getBuilderWithDefault().ORGENHET_NAVN("test4").ORGENHET_KODE("test4").ORGNODE_NIV("ORGENHET").build();
        var mockedEnhet5 = DvhOrgEnhetMock.getBuilderWithDefault().ORGENHET_NAVN("test5").ORGENHET_KODE("test5").ORGNODE_NIV("ORGENHET").build();

        dvhOrgenhetMockUtils.addToResponse(mockedEnhet1, mockedEnhet2, mockedEnhet3, mockedEnhet4, mockedEnhet5);
        dvhRestConsumer.loadRessurserOgOrgEnheter();

        // Since stream infrastructure is not up, the streams pipeline has not created state.
        // Lets do it manually, to test getmethods below.
        orgEnhetStateRepository.putEventAggregate(dvhOrgenhetMockUtils.dvhMockToState(mockedEnhet1, 1, String.valueOf(mockedEnhet1.getORGENHET_FRA_PERIODE()), Optional.of(String.valueOf(mockedEnhet1.getORGENHET_TIL_PERIODE()))));
        orgEnhetStateRepository.putEventAggregate(dvhOrgenhetMockUtils.dvhMockToState(mockedEnhet2, 2, String.valueOf(mockedEnhet2.getORGENHET_FRA_PERIODE()), Optional.of(String.valueOf(mockedEnhet2.getORGENHET_TIL_PERIODE()))));
        orgEnhetStateRepository.putEventAggregate(dvhOrgenhetMockUtils.dvhMockToState(mockedEnhet3, 3, String.valueOf(mockedEnhet3.getORGENHET_FRA_PERIODE()), Optional.of(String.valueOf(mockedEnhet3.getORGENHET_TIL_PERIODE()))));
        orgEnhetStateRepository.putEventAggregate(dvhOrgenhetMockUtils.dvhMockToState(mockedEnhet4, 4, String.valueOf(mockedEnhet4.getORGENHET_FRA_PERIODE()), Optional.of(String.valueOf(mockedEnhet4.getORGENHET_TIL_PERIODE()))));
        orgEnhetStateRepository.putEventAggregate(dvhOrgenhetMockUtils.dvhMockToState(mockedEnhet5, 5, String.valueOf(mockedEnhet5.getORGENHET_FRA_PERIODE()), Optional.of(String.valueOf(mockedEnhet5.getORGENHET_TIL_PERIODE()))));

        dvhRestConsumer.loadLedereAndOrganiseringerFromDvh();

        final var response = postForResource("organisasjonsenhet/scenario2.graphql");
        assertEquals(5, response.path("organisasjonsenheter").entityList(OrgEnhetResult.class).get().size());
    }

    @Test
    @DisplayName("Laster inn organisasjonsenheter organisert i hierarki fra dvh, leser og validerer organiseringene")
    void scenario21() {
        final var mockEnhet1 = DvhOrgEnhetMock.getBuilderWithDefault().ORGENHET_KODE("testId4").ORGNODE_NIV("ORGNIV2").ORGENHET_NAVN("testNavn4").build();
        final var mockEnhet2 = DvhOrgEnhetMock.getBuilderWithDefault().ORGENHET_KODE("testId2").ORGNODE_NIV("ORGENHET").ORGENHET_NAVN("testNavn").build();
        final var mockEnhet3 = DvhOrgEnhetMock.getBuilderWithDefault().ORGENHET_KODE("testId3").ORGNODE_NIV("ORGNIV3").ORGENHET_NAVN("testNavn").build();
        dvhOrgenhetMockUtils.addToResponse(mockEnhet1, mockEnhet2, mockEnhet3);
        dvhRestConsumer.loadRessurserOgOrgEnheter();

        // Since stream infrastructure is not up, the streams pipeline has not created state.
        // Lets do it manually, to test getmethods below.
        orgEnhetStateRepository.putEventAggregate(dvhOrgenhetMockUtils.dvhMockToState(mockEnhet1, 1, String.valueOf(mockEnhet1.getORGENHET_FRA_PERIODE()), Optional.of(String.valueOf(mockEnhet1.getORGENHET_TIL_PERIODE()))));
        orgEnhetStateRepository.putEventAggregate(dvhOrgenhetMockUtils.dvhMockToState(mockEnhet2, 2, String.valueOf(mockEnhet2.getORGENHET_FRA_PERIODE()), Optional.of(String.valueOf(mockEnhet2.getORGENHET_TIL_PERIODE()))));
        orgEnhetStateRepository.putEventAggregate(dvhOrgenhetMockUtils.dvhMockToState(mockEnhet3, 3, String.valueOf(mockEnhet3.getORGENHET_FRA_PERIODE()), Optional.of(String.valueOf(mockEnhet3.getORGENHET_TIL_PERIODE()))));

        dvhRestConsumer.loadLedereAndOrganiseringerFromDvh();

        final var response = postForResource("organisasjonsenhet/scenario3.graphql");
        final var enhetResponse = response.path("organisasjonsenhet").entity(JsonNode.class).get();

        final var organiserteEnheter = StreamSupport.stream(enhetResponse.get("organiseringer").spliterator(), false)
            .collect(
                toMap(
                    o -> o.get("retning").asText(),
                    o -> objectMapper.convertValue(o.get("organisasjonsenhet"), OrgEnhet.class)
                )
            );

        assertEquals(mockEnhet1.getORGENHET_KODE(), organiserteEnheter.get("over").getAgressoId());
        assertEquals(mockEnhet2.getORGENHET_KODE(), organiserteEnheter.get("under").getAgressoId());
    }

    @Test
    @DisplayName("Ny organisering: Laster inn organisasjonsenheter organisert i hierarki fra dvh, leser og validerer organiseringene")
    void scenario21_new_organisering() {
        when(unleash.isEnabled(OrganiseringConfig.UNLEASH_USE_NEW_ORGANISASJON_SOURCE)).thenReturn(true);

        final var mockEnhetOver = DvhOrgEnhetMock.getBuilderWithDefault().ORGENHET_KODE("testIdOver").ORGNODE_NIV("ORGNIV2").ORGENHET_NAVN("testNavnOver").build();
        final var mockEnhetUnder = DvhOrgEnhetMock.getBuilderWithDefault().ORGENHET_KODE("testIdUnder").ORGNODE_NIV("ORGENHET").ORGENHET_NAVN("testNavnUnder").build();
        final var mockEnhet = DvhOrgEnhetMock.getBuilderWithDefault().ORGENHET_KODE("testId3").ORGNODE_NIV("ORGNIV3").ORGENHET_NAVN("testNavn").build();
        dvhOrgenhetMockUtils.addToResponse(mockEnhetOver, mockEnhetUnder, mockEnhet);
        dvhRestConsumer.loadRessurserOgOrgEnheter();

        // Since stream infrastructure is not up, the streams pipeline has not created state so it has to be done manually
        // dvhMockToState() sets IDs to "ID_" + nr
        var orgStateOver = dvhOrgenhetMockUtils.dvhMockToState(mockEnhetOver, 1, String.valueOf(mockEnhetOver.getORGENHET_FRA_PERIODE()), Optional.of(String.valueOf(mockEnhetOver.getORGENHET_TIL_PERIODE())));
        var orgStateUnder = dvhOrgenhetMockUtils.dvhMockToState(mockEnhetUnder, 2, String.valueOf(mockEnhetUnder.getORGENHET_FRA_PERIODE()), Optional.of(String.valueOf(mockEnhetUnder.getORGENHET_TIL_PERIODE())));
        var orgState = dvhOrgenhetMockUtils.dvhMockToState(mockEnhet, 3, String.valueOf(mockEnhet.getORGENHET_FRA_PERIODE()), Optional.of(String.valueOf(mockEnhet.getORGENHET_TIL_PERIODE())));

        orgEnhetStateRepository.putEventAggregate(orgStateOver);
        orgEnhetStateRepository.putEventAggregate(orgStateUnder);
        orgEnhetStateRepository.putEventAggregate(orgState);

        dvhRestConsumer.loadLedereAndOrganiseringerFromDvh();

        // Same as above - need to create state manually
        organiseringStateRepository.putEventAggregate(orgEnhetStateToOrganiseringState(1, orgState, orgStateOver));
        organiseringStateRepository.putEventAggregate(orgEnhetStateToOrganiseringState(2, orgStateUnder, orgState));


        final var response = postForResource("organisasjonsenhet/scenario3.graphql");
        final var enhetResponse = response.path("organisasjonsenhet").entity(JsonNode.class).get();

        final var organiserteEnheter = StreamSupport.stream(enhetResponse.get("organiseringer").spliterator(), false)
            .collect(
                toMap(
                    o -> o.get("retning").asText(),
                    o -> objectMapper.convertValue(o.get("organisasjonsenhet"), OrgEnhet.class)
                )
            );

        assertThat(organiserteEnheter.get("over").getId()).isEqualTo("ID_1");
        assertThat(organiserteEnheter.get("over").getAgressoId()).isEqualTo(mockEnhetOver.getORGENHET_KODE());

        assertThat(organiserteEnheter.get("under").getId()).isEqualTo("ID_2");
        assertThat(organiserteEnheter.get("under").getAgressoId()).isEqualTo(mockEnhetUnder.getORGENHET_KODE());
    }


    @Test
    @DisplayName("Laster inn organisasjonsenheter organisert i hierarki fra dvh, filtrerer på retning")
    void scenario33() {
        final var mockEnhet1 = DvhOrgEnhetMock.getBuilderWithDefault().ORGENHET_KODE("testId2").ORGENHET_NAVN("testNavn").ORGNODE_NIV("ORGENHET").build();
        final var mockEnhet2 = DvhOrgEnhetMock.getBuilderWithDefault().ORGENHET_KODE("testId3").ORGENHET_NAVN("testNavn").ORGNODE_NIV("ORGNIV3").build();

        dvhOrgenhetMockUtils.addToResponse(mockEnhet1, mockEnhet2);
        dvhRestConsumer.loadRessurserOgOrgEnheter();

        // Since stream infrastructure is not up, the streams pipeline has not created state.
        // Lets do it manually, to test getmethods below.
        orgEnhetStateRepository.putEventAggregate(dvhOrgenhetMockUtils.dvhMockToState(mockEnhet1, 1, String.valueOf(mockEnhet1.getORGENHET_FRA_PERIODE()), Optional.of(String.valueOf(mockEnhet1.getORGENHET_TIL_PERIODE()))));
        orgEnhetStateRepository.putEventAggregate(dvhOrgenhetMockUtils.dvhMockToState(mockEnhet2, 2, String.valueOf(mockEnhet2.getORGENHET_FRA_PERIODE()), Optional.of(String.valueOf(mockEnhet2.getORGENHET_TIL_PERIODE()))));

        dvhRestConsumer.loadLedereAndOrganiseringerFromDvh();

        final var response = postForResource("organisasjonsenhet/scenario33.graphql");
        final var enhetResponse = response.path("organisasjonsenhet").entity(JsonNode.class).get();

        final var organiserteEnheter = StreamSupport.stream(enhetResponse.get("organiseringer").spliterator(), false)
            .collect(toMap(
                o -> o.get("retning").asText(),
                o -> objectMapper.convertValue(o.get("organisasjonsenhet"), OrgEnhet.class)
            ));

        assertThat(organiserteEnheter).hasSize(1);
        assertEquals(mockEnhet2.getORGENHET_KODE(), organiserteEnheter.get("over").getAgressoId());
    }

    @Test
    @DisplayName("Ny organisering: Laster inn organisasjonsenheter organisert i hierarki fra dvh, filtrerer på retning")
    void scenario33_new_organisering() {
        when(unleash.isEnabled(OrganiseringConfig.UNLEASH_USE_NEW_ORGANISASJON_SOURCE)).thenReturn(true);

        final var mockEnhet = DvhOrgEnhetMock.getBuilderWithDefault().ORGENHET_KODE("testId2").ORGENHET_NAVN("testNavn").ORGNODE_NIV("ORGENHET").build();
        final var mockEnhetOver = DvhOrgEnhetMock.getBuilderWithDefault().ORGENHET_KODE("testId3").ORGENHET_NAVN("testNavn").ORGNODE_NIV("ORGNIV3").build();

        dvhOrgenhetMockUtils.addToResponse(mockEnhet, mockEnhetOver);
        dvhRestConsumer.loadRessurserOgOrgEnheter();

        // Since stream infrastructure is not up, the streams pipeline has not created state.
        // Lets do it manually, to test getmethods below.

        var orgState = dvhOrgenhetMockUtils.dvhMockToState(mockEnhet, 1, String.valueOf(mockEnhet.getORGENHET_FRA_PERIODE()), Optional.of(String.valueOf(mockEnhet.getORGENHET_TIL_PERIODE())));
        var orgStateOver = dvhOrgenhetMockUtils.dvhMockToState(mockEnhetOver, 2, String.valueOf(mockEnhetOver.getORGENHET_FRA_PERIODE()), Optional.of(String.valueOf(mockEnhetOver.getORGENHET_TIL_PERIODE())));
        var orgStateUnder = dvhOrgenhetMockUtils.dvhMockToState(DvhOrgEnhetMock.getBuilderWithDefault().build(), 3, String.valueOf(mockEnhetOver.getORGENHET_FRA_PERIODE()), Optional.of(String.valueOf(mockEnhetOver.getORGENHET_TIL_PERIODE())));

        orgEnhetStateRepository.putEventAggregate(orgState);
        orgEnhetStateRepository.putEventAggregate(orgStateOver);

        dvhRestConsumer.loadLedereAndOrganiseringerFromDvh();

        // Same as above - need to create state manually
        organiseringStateRepository.putEventAggregate(orgEnhetStateToOrganiseringState(1, orgState, orgStateOver));
        organiseringStateRepository.putEventAggregate(orgEnhetStateToOrganiseringState(2, orgStateUnder, orgState));


        final var response = postForResource("organisasjonsenhet/scenario33.graphql");
        final var enhetResponse = response.path("organisasjonsenhet").entity(JsonNode.class).get();

        final var organiserteEnheter = StreamSupport.stream(enhetResponse.get("organiseringer").spliterator(), false)
            .collect(toMap(
                o -> o.get("retning").asText(),
                o -> objectMapper.convertValue(o.get("organisasjonsenhet"), OrgEnhet.class)
            ));

        assertThat(organiserteEnheter).hasSize(1);

        assertThat(organiserteEnheter.get("over").getId()).isEqualTo("ID_2");
        assertThat(organiserteEnheter.get("over").getAgressoId()).isEqualTo(mockEnhetOver.getORGENHET_KODE());
    }

    @Test
    @DisplayName("Tester at graphQL spørring på leder hvor enhet har ledig gir leder")
    void scenario31() {
        final var mockEnhet = DvhOrgEnhetMock.getBuilderWithDefault().ORGENHET_KODE("testId1").ORGENHET_NAVN("testNavn").ORGNODE_NIV("ORGENHET").build();
        dvhOrgenhetMockUtils.addToResponse(mockEnhet);

        RessursState ressurs = kafkaUtils.createRessursHendelseEventSource("");

        dvhRestConsumer.loadRessurserOgOrgEnheter();

        // Since stream infrastructure is not up, the streams pipeline has not created state.
        // Lets do it manually, to test getmethods below.
        orgEnhetStateRepository.putEventAggregate(dvhOrgenhetMockUtils.dvhMockToState(mockEnhet, 1, String.valueOf(mockEnhet.getORGENHET_FRA_PERIODE()), Optional.of(String.valueOf(mockEnhet.getORGENHET_TIL_PERIODE()))));

        dvhRestConsumer.loadLedereAndOrganiseringerFromDvh();

        final var response = postForResource("organisasjonsenhet/scenario4.graphql");
        final var outputEnhetJson = response.path("organisasjonsenhet").entity(JsonNode.class).get();
        final var outputLeder = objectMapper.convertValue(outputEnhetJson.get("leder"), new TypeReference<List<OrgEnhetsLeder>>() {}).get(0);

        assertEquals(ressurs.navident(), outputLeder.getRessurs().getNavIdent());
        assertEquals(ressurs.personident().replaceFirst(".{5}$","*****"), outputLeder.getRessurs().getPersonIdent());
    }

    @Test
    @DisplayName("Tester at graphQL spørring på leder for enhet, hvor enhet ikke har leder")
    void scenario32() {
        final var mockEnhet = DvhOrgEnhetMock.getBuilderWithDefault().ORGENHET_KODE("testId3").ORGENHET_NAVN("testNavn").ORGNODE_NIV("ORGNIV3").build();
        dvhOrgenhetMockUtils.addToResponse(mockEnhet);
        dvhRestConsumer.loadRessurserOgOrgEnheter();

        // Since stream infrastructure is not up, the streams pipeline has not created state.
        // Lets do it manually, to test getmethods below.
        orgEnhetStateRepository.putEventAggregate(dvhOrgenhetMockUtils.dvhMockToState(mockEnhet, 1, String.valueOf(mockEnhet.getORGENHET_FRA_PERIODE()), Optional.of(String.valueOf(mockEnhet.getORGENHET_TIL_PERIODE()))));

        dvhRestConsumer.loadLedereAndOrganiseringerFromDvh();

        final var response = postForResource("organisasjonsenhet/scenario32.graphql");
        final var outputEnhetJson = response.path("organisasjonsenhet").entity(JsonNode.class).get();
        final var outputLedere = objectMapper.convertValue(outputEnhetJson.get("leder"), new TypeReference<List<OrgEnhetsLeder>>() {});

        assertEquals(0, outputLedere.size());

    }

    @Test
    @DisplayName("Henter ut person navn i kontekst av enhet leder spørring")
    void scenario34() {
        final var mockEnhet = DvhOrgEnhetMock.getBuilderWithDefault().ORGENHET_KODE("testId1").ORGENHET_NAVN("testNavn").ORGNODE_NIV("ORGENHET").build();
        dvhOrgenhetMockUtils.addToResponse(mockEnhet);
        RessursState ressurs = kafkaUtils.createRessursHendelseEventSource("");
        kafkaUtils.createRessursHendelseEventSource("1"); // Trenger ressurs med navIdent "testNavIdent1" pga. orgTilknytning som "lekker inn" fra annen test

        pdlUtils.addPersonToPdlResponse(true, ressurs.personident());
        dvhRestConsumer.loadRessurserOgOrgEnheter();

        // Since stream infrastructure is not up, the streams pipeline has not created state.
        // Lets do it manually, to test getmethods below.
        orgEnhetStateRepository.putEventAggregate(dvhOrgenhetMockUtils.dvhMockToState(mockEnhet, 1, String.valueOf(mockEnhet.getORGENHET_FRA_PERIODE()), Optional.of(String.valueOf(mockEnhet.getORGENHET_TIL_PERIODE()))));

        dvhRestConsumer.loadLedereAndOrganiseringerFromDvh();

        final var response = postForResource("organisasjonsenhet/scenario34.graphql");
        final var outputEnhetJson = response.path("organisasjonsenhet").entity(JsonNode.class).get();

        final var lederNavn = outputEnhetJson.get("leder")
            .get(0)
            .get("ressurs")
            .get("person")
            .get("navn");

        pdlUtils.assertNavn(ressurs.personident(), lederNavn);
    }

    @Test
    @DisplayName("Leser kobling fra DVH og leser fra graphQl")
    void scenario41() {
        final var mockEnhet = DvhOrgEnhetMock.getBuilderWithDefault().ORGENHET_KODE("testId1").ORGENHET_NAVN("testNavn").ORGNODE_NIV("ORGENHET").build();
        dvhOrgenhetMockUtils.addToResponse(mockEnhet);

        RessursState ressurs = kafkaUtils.createRessursHendelseEventSource("");
        dvhRestConsumer.loadRessurserOgOrgEnheter();

        // Since stream infrastructure is not up, the streams pipeline has not created state.
        // Lets do it manually, to test getmethods below.
        orgEnhetStateRepository.putEventAggregate(dvhOrgenhetMockUtils.dvhMockToState(mockEnhet, 1, String.valueOf(mockEnhet.getORGENHET_FRA_PERIODE()), Optional.of(String.valueOf(mockEnhet.getORGENHET_TIL_PERIODE()))));

        orgtilknytningStateRepository.putEventAggregate(
            "FID_1",
            orgtilknytningState().toBuilder()
                .fid("FID_1")
                .navident(ressurs.navident())
                .orgEnhetId("ID_1")
                .gyldigFom(LocalDate.of(2020, 1, 1))
                .build()
        );

        //        dvhRestConsumer.loadKoblingerLedereOgTlfFromDvh();

        final var response = postForResource("organisasjonsenhet/scenario41.graphql");

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
        final var mockEnhet = DvhOrgEnhetMock.getBuilderWithDefault().ORGENHET_KODE("testId10").ORGENHET_NAVN("testNavn10").ORGNODE_NIV("ORGENHET").build();
        dvhOrgenhetMockUtils.addToResponse(mockEnhet);

        dvhRestConsumer.loadRessurserOgOrgEnheter();

        // Since stream infrastructure is not up, the streams pipeline has not created state.
        // Lets do it manually, to test getmethods below.
        orgEnhetStateRepository.putEventAggregate(dvhOrgenhetMockUtils.dvhMockToState(mockEnhet, 42, String.valueOf(mockEnhet.getORGENHET_FRA_PERIODE()), Optional.of(String.valueOf(mockEnhet.getORGENHET_TIL_PERIODE()))));
        //        dvhRestConsumer.loadKoblingerLedereOgTlfFromDvh();

        final var response = postForResource("organisasjonsenhet/scenario42.graphql");
        final var outputEnhetJson = response.path("organisasjonsenhet").entity(JsonNode.class).get();

        assertThat(outputEnhetJson.get("koblinger")).isEmpty();
    }

    @Test
    @DisplayName("Henter ut person navn i kontekst av enhet kobling spørring")
    void scenario43() {
        final var mockEnhet = DvhOrgEnhetMock.getBuilderWithDefault().ORGENHET_KODE("testId1").ORGENHET_NAVN("testNavn").ORGNODE_NIV("ORGENHET").build();
        dvhOrgenhetMockUtils.addToResponse(mockEnhet);

        RessursState ressurs = kafkaUtils.createRessursHendelseEventSource("");
        pdlUtils.addPersonToPdlResponse(true, ressurs.personident());
        dvhRestConsumer.loadRessurserOgOrgEnheter();

        // Since stream infrastructure is not up, the streams pipeline has not created state.
        // Lets do it manually, to test getmethods below.
        orgEnhetStateRepository.putEventAggregate(dvhOrgenhetMockUtils.dvhMockToState(mockEnhet, 1, String.valueOf(mockEnhet.getORGENHET_FRA_PERIODE()), Optional.of(String.valueOf(mockEnhet.getORGENHET_TIL_PERIODE()))));

        //        dvhRestConsumer.loadKoblingerLedereOgTlfFromDvh();

        final var response = postForResource("organisasjonsenhet/scenario34.graphql");
        final var outputEnhetJson = response.path("organisasjonsenhet").entity(JsonNode.class).get();

        final var lederNavn = outputEnhetJson.get("koblinger")
            .get(0)
            .get("ressurs")
            .get("person")
            .get("navn");

        pdlUtils.assertNavn(ressurs.personident(), lederNavn);

    }

    private Optional<OrgEnhetResult> getEnhetInList(String id, List<OrgEnhetResult> organisasjonsenhetResult) {
        return organisasjonsenhetResult.stream()
            .filter(e -> e.getId().equals(id))
            .findFirst();
    }

    private String sladdetPersonident(String personident){
        return personident.replaceFirst(".{5}$","*****");
    }

}
