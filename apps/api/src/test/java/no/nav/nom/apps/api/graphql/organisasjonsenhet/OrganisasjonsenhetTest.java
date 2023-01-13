package no.nav.nom.apps.api.graphql.organisasjonsenhet;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.nom.apps.api.DvhOrgEnhetMock;
import no.nav.nom.apps.api.dvh.DvhRestConsumer;
import no.nav.nom.apps.api.orgenhet.eventsourcing.state.OrgEnhetStateRepository;
import no.nav.nom.contract.nom.api.graphql.OrgEnhet;
import no.nav.nom.contract.nom.api.graphql.OrgEnhetResult;
import no.nav.nom.contract.nom.api.graphql.OrgEnhetsKobling;
import no.nav.nom.contract.nom.api.graphql.OrgEnhetsLeder;
import no.nav.nom.contract.nom.api.graphql.ResultCode;
import no.nav.nom.contract.nom.state.RessursState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("GraphQL.HentEnhet")
class OrganisasjonsenhetTest extends OrganisasjonsenhetTestBase {

    // TODO: Wait for answer on https://github.com/spring-projects/spring-graphql/issues/506 to add support for optional arguments using ProjectedPayload

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DvhRestConsumer dvhRestConsumer;

    @Autowired
    private OrgEnhetStateRepository orgEnhetStateRepository;

    @Test
    @DisplayName("Laster organisasjonsenhet inn fra dvh og leser fra graphql med id parameter")
    void scenario11() {
        final var mockedEnhet = DvhOrgEnhetMock.getBuilderWithDefault().ORGENHET_KODE("testId1").ORGENHET_NAVN("testNavn").ORGNODE_NIV("ORGENHET")
                .ENHETTYPE_NAVN("TYPEEE").build();
        dvhOrgenhetMockUtils.addToResponse(mockedEnhet);

        dvhRestConsumer.loadRessurserOgOrgEnheter();

        // Gammel Search - FeatureToggle of
        enableOrgEnhetEventSource(false);

        final var outputEnhetOld = postForResource("organisasjonsenhet/scenario1.graphql")
                .path("organisasjonsenhet")
                .entity(OrgEnhet.class)
                .get();

        assertEquals(mockedEnhet.getORGENHET_KODE(), outputEnhetOld.getAgressoId());
        assertEquals(mockedEnhet.getENHETTYPE_NAVN(), outputEnhetOld.getAgressoOrgenhetType());

        // Ny Search - FeatureToggle on

        // Since stream infrastructure is not up, the streams pipeline has not created state.
        // Lets do it manually, to test getmethods below.
        orgEnhetStateRepository.putEventAggregate(dvhOrgenhetMockUtils.dvhMockToState(mockedEnhet, 1, String.valueOf(mockedEnhet.getORGENHET_FRA_PERIODE()), Optional.of(String.valueOf(mockedEnhet.getORGENHET_TIL_PERIODE()))));

        enableOrgEnhetEventSource(true);

        final var outputEnhetNew = postForResource("organisasjonsenhet/scenario1.graphql")
            .path("organisasjonsenhet")
            .entity(OrgEnhet.class)
            .get();

        assertEquals(mockedEnhet.getORGENHET_KODE(), outputEnhetNew.getAgressoId());
        assertEquals(mockedEnhet.getENHETTYPE_NAVN(), outputEnhetNew.getAgressoOrgenhetType());
    }

    @Test
    @DisplayName("Laster organisasjonsenhet inn fra dvh og leser fra graphql med id parameter bulk")
    void scenario112() {
        final var mockedEnhet = DvhOrgEnhetMock.getBuilderWithDefault().ORGENHET_KODE("testId1").build();
        dvhOrgenhetMockUtils.addToResponse(mockedEnhet);
        dvhRestConsumer.loadRessurserOgOrgEnheter();

        // Gammel Search - FeatureToggle of
        enableOrgEnhetEventSource(false);

        final var outputEnhetOld = postForResource("organisasjonsenhet/scenario12.graphql")
                .path("organisasjonsenheter")
                .entityList(OrgEnhetResult.class)
                .get();

        assertTrue(getEnhetInList(mockedEnhet.getORGENHET_KODE(), outputEnhetOld).isPresent());
        assertEquals(mockedEnhet.getORGENHET_KODE(), getEnhetInList(mockedEnhet.getORGENHET_KODE(), outputEnhetOld).get().getId());
        assertEquals(ResultCode.OK, getEnhetInList(mockedEnhet.getORGENHET_KODE(), outputEnhetOld).get().getCode());

        assertTrue(getEnhetInList("notFoundIdent", outputEnhetOld).isPresent());
        assertEquals(mockedEnhet.getORGENHET_KODE(), getEnhetInList(mockedEnhet.getORGENHET_KODE(), outputEnhetOld).get().getId());
        assertEquals(ResultCode.NOT_FOUND, getEnhetInList("notFoundIdent", outputEnhetOld).get().getCode());

        // Ny Search - FeatureToggle on
        enableOrgEnhetEventSource(true);

        // Since stream infrastructure is not up, the streams pipeline has not created state.
        // Lets do it manually, to test getmethods below.
        orgEnhetStateRepository.putEventAggregate(dvhOrgenhetMockUtils.dvhMockToState(mockedEnhet, 1, String.valueOf(mockedEnhet.getORGENHET_FRA_PERIODE()), Optional.of(String.valueOf(mockedEnhet.getORGENHET_TIL_PERIODE()))));

        final var outputEnhetNew = postForResource("organisasjonsenhet/scenario12.graphql")
            .path("organisasjonsenheter")
            .entityList(OrgEnhetResult.class)
            .get();

        assertTrue(getEnhetInList(mockedEnhet.getORGENHET_KODE(), outputEnhetNew).isPresent());
        assertEquals(mockedEnhet.getORGENHET_KODE(), getEnhetInList(mockedEnhet.getORGENHET_KODE(), outputEnhetNew).get().getId());
        assertEquals(ResultCode.OK, getEnhetInList(mockedEnhet.getORGENHET_KODE(), outputEnhetNew).get().getCode());

        assertTrue(getEnhetInList("notFoundIdent", outputEnhetNew).isPresent());
        assertEquals(mockedEnhet.getORGENHET_KODE(), getEnhetInList(mockedEnhet.getORGENHET_KODE(), outputEnhetNew).get().getId());
        assertEquals(ResultCode.NOT_FOUND, getEnhetInList("notFoundIdent", outputEnhetNew).get().getCode());

    }

    @Test
    @DisplayName("Laster organisasjonsenhet inn fra dvh og leser fra graphql bulk filtrert på nivå")
    void scenario113() {
        final var mockedEnhet = DvhOrgEnhetMock.getBuilderWithDefault().ORGENHET_KODE("testId4").ORGNODE_NIV("ORGNIV2").build();
        dvhOrgenhetMockUtils.addToResponse(mockedEnhet);
        dvhRestConsumer.loadRessurserOgOrgEnheter();

        // Gammel Search - FeatureToggle of
        enableOrgEnhetEventSource(false);

        final var outputEnhetOld = postForResource("organisasjonsenhet/scenario113.graphql")
                .path("organisasjonsenheter")
                .entityList(OrgEnhetResult.class)
                .get();

        assertEquals(mockedEnhet.getORGENHET_KODE(), outputEnhetOld.get(0).getId());
        assertEquals(ResultCode.OK, outputEnhetOld.get(0).getCode());
        assertEquals(mockedEnhet.getORGNODE_NIV(), outputEnhetOld.get(0).getOrgNiv());

        // Ny Search - FeatureToggle on
        enableOrgEnhetEventSource(true);

        // Since stream infrastructure is not up, the streams pipeline has not created state.
        // Lets do it manually, to test getmethods below.
        orgEnhetStateRepository.putEventAggregate(dvhOrgenhetMockUtils.dvhMockToState(mockedEnhet, 1, String.valueOf(mockedEnhet.getORGENHET_FRA_PERIODE()), Optional.of(String.valueOf(mockedEnhet.getORGENHET_TIL_PERIODE()))));

        final var outputEnhetNew = postForResource("organisasjonsenhet/scenario113.graphql")
            .path("organisasjonsenheter")
            .entityList(OrgEnhetResult.class)
            .get();

        assertEquals(mockedEnhet.getORGENHET_KODE(), outputEnhetNew.get(0).getId());
        assertEquals(ResultCode.OK, outputEnhetNew.get(0).getCode());
        assertEquals(mockedEnhet.getORGNODE_NIV(), outputEnhetNew.get(0).getOrgNiv());
    }

    @Test
    @DisplayName("Laster organisasjonsenhet inn fra dvh og leser fra graphql bulk filtrert på nivå og agressoId")
    void scenario114() {
        final var mockedEnhet = DvhOrgEnhetMock.getBuilderWithDefault().ORGENHET_KODE("testId4").ORGNODE_NIV("ORGNIV2").build();
        dvhOrgenhetMockUtils.addToResponse(mockedEnhet);
        dvhRestConsumer.loadRessurserOgOrgEnheter();

        // Gammel Search - FeatureToggle of
        enableOrgEnhetEventSource(false);

        final var outputEnhetOld = postForResource("organisasjonsenhet/scenario114.graphql")
                .path("organisasjonsenheter")
                .entityList(OrgEnhetResult.class)
                .get();

        final var actualTestId4Old = getEnhetInList(mockedEnhet.getORGENHET_KODE(), outputEnhetOld);
        final var actualNotFoundOld = getEnhetInList("notFoundId", outputEnhetOld);

        assertTrue(actualTestId4Old.isPresent());
        assertEquals(mockedEnhet.getORGENHET_KODE(), actualTestId4Old.get().getId());
        assertEquals(mockedEnhet.getORGNODE_NIV(), actualTestId4Old.get().getOrgNiv());
        assertEquals(ResultCode.OK, actualTestId4Old.get().getCode());

        assertTrue(actualNotFoundOld.isPresent());
        assertEquals("notFoundId", actualNotFoundOld.get().getId());
        assertEquals("ORGNIV2", actualNotFoundOld.get().getOrgNiv());
        assertEquals(ResultCode.NOT_FOUND, actualNotFoundOld.get().getCode());

        // Ny Search - FeatureToggle on
        enableOrgEnhetEventSource(true);

        // Since stream infrastructure is not up, the streams pipeline has not created state.
        // Lets do it manually, to test getmethods below.
        orgEnhetStateRepository.putEventAggregate(dvhOrgenhetMockUtils.dvhMockToState(mockedEnhet, 1, String.valueOf(mockedEnhet.getORGENHET_FRA_PERIODE()), Optional.of(String.valueOf(mockedEnhet.getORGENHET_TIL_PERIODE()))));

        final var outputEnhetNew = postForResource("organisasjonsenhet/scenario114.graphql")
            .path("organisasjonsenheter")
            .entityList(OrgEnhetResult.class)
            .get();

        final var actualTestId4New = getEnhetInList(mockedEnhet.getORGENHET_KODE(), outputEnhetNew);
        final var actualNotFoundNew = getEnhetInList("notFoundId", outputEnhetNew);

        assertTrue(actualTestId4New.isPresent());
        assertEquals(mockedEnhet.getORGENHET_KODE(), actualTestId4New.get().getId());
        assertEquals(mockedEnhet.getORGNODE_NIV(), actualTestId4New.get().getOrgNiv());
        assertEquals(ResultCode.OK, actualTestId4New.get().getCode());

        assertTrue(actualNotFoundNew.isPresent());
        assertEquals("notFoundId", actualNotFoundNew.get().getId());
        assertEquals("ORGNIV2", actualNotFoundNew.get().getOrgNiv());
        assertEquals(ResultCode.NOT_FOUND, actualNotFoundNew.get().getCode());
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

        // Gammel Search - FeatureToggle of
        enableOrgEnhetEventSource(false);

        final var responseOld = postForResource("organisasjonsenhet/scenario2.graphql");
        assertEquals(5, responseOld.path("organisasjonsenheter").entityList(OrgEnhetResult.class).get().size());

        // Ny Search - FeatureToggle on
        enableOrgEnhetEventSource(true);

        // Since stream infrastructure is not up, the streams pipeline has not created state.
        // Lets do it manually, to test getmethods below.
        orgEnhetStateRepository.putEventAggregate(dvhOrgenhetMockUtils.dvhMockToState(mockedEnhet1, 1, String.valueOf(mockedEnhet1.getORGENHET_FRA_PERIODE()), Optional.of(String.valueOf(mockedEnhet1.getORGENHET_TIL_PERIODE()))));
        orgEnhetStateRepository.putEventAggregate(dvhOrgenhetMockUtils.dvhMockToState(mockedEnhet2, 2, String.valueOf(mockedEnhet2.getORGENHET_FRA_PERIODE()), Optional.of(String.valueOf(mockedEnhet2.getORGENHET_TIL_PERIODE()))));
        orgEnhetStateRepository.putEventAggregate(dvhOrgenhetMockUtils.dvhMockToState(mockedEnhet3, 3, String.valueOf(mockedEnhet3.getORGENHET_FRA_PERIODE()), Optional.of(String.valueOf(mockedEnhet3.getORGENHET_TIL_PERIODE()))));
        orgEnhetStateRepository.putEventAggregate(dvhOrgenhetMockUtils.dvhMockToState(mockedEnhet4, 4, String.valueOf(mockedEnhet4.getORGENHET_FRA_PERIODE()), Optional.of(String.valueOf(mockedEnhet4.getORGENHET_TIL_PERIODE()))));
        orgEnhetStateRepository.putEventAggregate(dvhOrgenhetMockUtils.dvhMockToState(mockedEnhet5, 5, String.valueOf(mockedEnhet5.getORGENHET_FRA_PERIODE()), Optional.of(String.valueOf(mockedEnhet5.getORGENHET_TIL_PERIODE()))));

        final var responseNew = postForResource("organisasjonsenhet/scenario2.graphql");
        assertEquals(5, responseNew.path("organisasjonsenheter").entityList(OrgEnhetResult.class).get().size());
    }

    @Test
    @DisplayName("Laster inn organisasjonsenheter organisert i hierarki fra dvh, leser og validerer organiseringene")
    void scenario21() {
        final var mockEnhet1 = DvhOrgEnhetMock.getBuilderWithDefault().ORGENHET_KODE("testId4").ORGNODE_NIV("ORGNIV2").ORGENHET_NAVN("testNavn4").build();
        final var mockEnhet2 = DvhOrgEnhetMock.getBuilderWithDefault().ORGENHET_KODE("testId2").ORGNODE_NIV("ORGENHET").ORGENHET_NAVN("testNavn").build();
        final var mockEnhet3 = DvhOrgEnhetMock.getBuilderWithDefault().ORGENHET_KODE("testId3").ORGNODE_NIV("ORGNIV3").ORGENHET_NAVN("testNavn").build();
        dvhOrgenhetMockUtils.addToResponse(mockEnhet1, mockEnhet2, mockEnhet3);
        dvhRestConsumer.loadRessurserOgOrgEnheter();

        // Gammel Search - FeatureToggle of
        enableOrgEnhetEventSource(false);

        dvhRestConsumer.loadKoblingerLedereOgTlfFromDvh();


        final var responseOld = postForResource("organisasjonsenhet/scenario3.graphql");
        final var enhetResponseOld = responseOld.path("organisasjonsenhet").entity(JsonNode.class).get();

        final var organiserteEnheterOld = StreamSupport.stream(enhetResponseOld.get("organiseringer").spliterator(), false)
                .collect(
                        toMap(
                                o -> o.get("retning").asText(),
                                o -> objectMapper.convertValue(o.get("organisasjonsenhet"), OrgEnhet.class)
                        )
                );

        assertEquals(mockEnhet1.getORGENHET_KODE(), organiserteEnheterOld.get("over").getAgressoId());
        assertEquals(mockEnhet2.getORGENHET_KODE(), organiserteEnheterOld.get("under").getAgressoId());

        // Ny Search - FeatureToggle on
        enableOrgEnhetEventSource(true);

        // Since stream infrastructure is not up, the streams pipeline has not created state.
        // Lets do it manually, to test getmethods below.
        orgEnhetStateRepository.putEventAggregate(dvhOrgenhetMockUtils.dvhMockToState(mockEnhet1, 1, String.valueOf(mockEnhet1.getORGENHET_FRA_PERIODE()), Optional.of(String.valueOf(mockEnhet1.getORGENHET_TIL_PERIODE()))));
        orgEnhetStateRepository.putEventAggregate(dvhOrgenhetMockUtils.dvhMockToState(mockEnhet2, 2, String.valueOf(mockEnhet2.getORGENHET_FRA_PERIODE()), Optional.of(String.valueOf(mockEnhet2.getORGENHET_TIL_PERIODE()))));
        orgEnhetStateRepository.putEventAggregate(dvhOrgenhetMockUtils.dvhMockToState(mockEnhet3, 3, String.valueOf(mockEnhet3.getORGENHET_FRA_PERIODE()), Optional.of(String.valueOf(mockEnhet3.getORGENHET_TIL_PERIODE()))));

        dvhRestConsumer.loadKoblingerLedereOgTlfFromDvh();

        final var responseNew = postForResource("organisasjonsenhet/scenario3.graphql");
        final var enhetResponseNew = responseNew.path("organisasjonsenhet").entity(JsonNode.class).get();

        final var organiserteEnheterNew = StreamSupport.stream(enhetResponseNew.get("organiseringer").spliterator(), false)
            .collect(
                toMap(
                    o -> o.get("retning").asText(),
                    o -> objectMapper.convertValue(o.get("organisasjonsenhet"), OrgEnhet.class)
                )
            );

        assertEquals(mockEnhet1.getORGENHET_KODE(), organiserteEnheterNew.get("over").getAgressoId());
        assertEquals(mockEnhet2.getORGENHET_KODE(), organiserteEnheterNew.get("under").getAgressoId());
    }

    @Test
    @DisplayName("Laster inn organisasjonsenheter organisert i hierarki fra dvh, filtrerer på retning")
    void scenario33() {
        final var mockEnhet1 = DvhOrgEnhetMock.getBuilderWithDefault().ORGENHET_KODE("testId2").ORGENHET_NAVN("testNavn").ORGNODE_NIV("ORGENHET").build();
        final var mockEnhet2 = DvhOrgEnhetMock.getBuilderWithDefault().ORGENHET_KODE("testId3").ORGENHET_NAVN("testNavn").ORGNODE_NIV("ORGNIV3").build();

        dvhOrgenhetMockUtils.addToResponse(mockEnhet1, mockEnhet2);
        dvhRestConsumer.loadRessurserOgOrgEnheter();

        // Gammel Search - FeatureToggle of
        enableOrgEnhetEventSource(false);

        dvhRestConsumer.loadKoblingerLedereOgTlfFromDvh();

        final var responseOld = postForResource("organisasjonsenhet/scenario33.graphql");
        final var enhetResponseOld = responseOld.path("organisasjonsenhet").entity(JsonNode.class).get();

        final var organiserteEnheterOld = StreamSupport.stream(enhetResponseOld.get("organiseringer").spliterator(), false)
                .collect(toMap(
                        o -> o.get("retning").asText(),
                        o -> objectMapper.convertValue(o.get("organisasjonsenhet"), OrgEnhet.class)
                ));

        assertThat(organiserteEnheterOld).hasSize(1);
        assertEquals(mockEnhet2.getORGENHET_KODE(), organiserteEnheterOld.get("over").getAgressoId());

        // Ny Search - FeatureToggle on
        enableOrgEnhetEventSource(true);

        // Since stream infrastructure is not up, the streams pipeline has not created state.
        // Lets do it manually, to test getmethods below.
        orgEnhetStateRepository.putEventAggregate(dvhOrgenhetMockUtils.dvhMockToState(mockEnhet1, 1, String.valueOf(mockEnhet1.getORGENHET_FRA_PERIODE()), Optional.of(String.valueOf(mockEnhet1.getORGENHET_TIL_PERIODE()))));
        orgEnhetStateRepository.putEventAggregate(dvhOrgenhetMockUtils.dvhMockToState(mockEnhet2, 2, String.valueOf(mockEnhet2.getORGENHET_FRA_PERIODE()), Optional.of(String.valueOf(mockEnhet2.getORGENHET_TIL_PERIODE()))));

        dvhRestConsumer.loadKoblingerLedereOgTlfFromDvh();

        final var responseNew = postForResource("organisasjonsenhet/scenario33.graphql");
        final var enhetResponseNew = responseNew.path("organisasjonsenhet").entity(JsonNode.class).get();

        final var organiserteEnheterNew = StreamSupport.stream(enhetResponseNew.get("organiseringer").spliterator(), false)
            .collect(toMap(
                o -> o.get("retning").asText(),
                o -> objectMapper.convertValue(o.get("organisasjonsenhet"), OrgEnhet.class)
            ));

        assertThat(organiserteEnheterNew).hasSize(1);
        assertEquals(mockEnhet2.getORGENHET_KODE(), organiserteEnheterNew.get("over").getAgressoId());
    }

    @Test
    @DisplayName("Tester at graphQL spørring på leder hvor enhet har ledig gir leder")
    void scenario31() {
        final var mockEnhet = DvhOrgEnhetMock.getBuilderWithDefault().ORGENHET_KODE("testId1").ORGENHET_NAVN("testNavn").ORGNODE_NIV("ORGENHET").build();
        dvhOrgenhetMockUtils.addToResponse(mockEnhet);

        RessursState ressurs = createRessursHendelseEventSource("");

        dvhRestConsumer.loadRessurserOgOrgEnheter();

        // Gammel Search - FeatureToggle of
        enableOrgEnhetEventSource(false);

        dvhRestConsumer.loadKoblingerLedereOgTlfFromDvh();

        final var responseOld = postForResource("organisasjonsenhet/scenario4.graphql");
        final var outputEnhetJsonOld = responseOld.path("organisasjonsenhet").entity(JsonNode.class).get();
        final var outputLederOld = objectMapper.convertValue(outputEnhetJsonOld.get("leder"), new TypeReference<List<OrgEnhetsLeder>>() {}).get(0);

        assertEquals(ressurs.navident(), outputLederOld.getRessurs().getNavIdent());
        assertEquals(ressurs.personident(), outputLederOld.getRessurs().getPersonIdent());

        // Ny Search - FeatureToggle on
        enableOrgEnhetEventSource(true);

        // Since stream infrastructure is not up, the streams pipeline has not created state.
        // Lets do it manually, to test getmethods below.
        orgEnhetStateRepository.putEventAggregate(dvhOrgenhetMockUtils.dvhMockToState(mockEnhet, 1, String.valueOf(mockEnhet.getORGENHET_FRA_PERIODE()), Optional.of(String.valueOf(mockEnhet.getORGENHET_TIL_PERIODE()))));

        dvhRestConsumer.loadKoblingerLedereOgTlfFromDvh();

        final var responseNew = postForResource("organisasjonsenhet/scenario4.graphql");
        final var outputEnhetJsonNew = responseNew.path("organisasjonsenhet").entity(JsonNode.class).get();
        final var outputLederNew = objectMapper.convertValue(outputEnhetJsonNew.get("leder"), new TypeReference<List<OrgEnhetsLeder>>() {}).get(0);

        assertEquals(ressurs.navident(), outputLederNew.getRessurs().getNavIdent());
        assertEquals(ressurs.personident(), outputLederNew.getRessurs().getPersonIdent());
    }

    @Test
    @DisplayName("Tester at graphQL spørring på leder for enhet, hvor enhet ikke har leder")
    void scenario32() {
        final var mockEnhet = DvhOrgEnhetMock.getBuilderWithDefault().ORGENHET_KODE("testId3").ORGENHET_NAVN("testNavn").ORGNODE_NIV("ORGNIV3").build();
        dvhOrgenhetMockUtils.addToResponse(mockEnhet);
        dvhRestConsumer.loadRessurserOgOrgEnheter();

        // Gammel Search - FeatureToggle of
        enableOrgEnhetEventSource(false);

        dvhRestConsumer.loadKoblingerLedereOgTlfFromDvh();

        final var responseOld = postForResource("organisasjonsenhet/scenario32.graphql");
        final var outputEnhetJsonOld = responseOld.path("organisasjonsenhet").entity(JsonNode.class).get();
        final var outputLedereOld = objectMapper.convertValue(outputEnhetJsonOld.get("leder"), new TypeReference<List<OrgEnhetsLeder>>() {});

        assertEquals(0, outputLedereOld.size());

        // Ny Search - FeatureToggle on
        enableOrgEnhetEventSource(true);

        // Since stream infrastructure is not up, the streams pipeline has not created state.
        // Lets do it manually, to test getmethods below.
        orgEnhetStateRepository.putEventAggregate(dvhOrgenhetMockUtils.dvhMockToState(mockEnhet, 1, String.valueOf(mockEnhet.getORGENHET_FRA_PERIODE()), Optional.of(String.valueOf(mockEnhet.getORGENHET_TIL_PERIODE()))));

        dvhRestConsumer.loadKoblingerLedereOgTlfFromDvh();

        final var responseNew = postForResource("organisasjonsenhet/scenario32.graphql");
        final var outputEnhetJsonNew = responseNew.path("organisasjonsenhet").entity(JsonNode.class).get();
        final var outputLedereNew = objectMapper.convertValue(outputEnhetJsonNew.get("leder"), new TypeReference<List<OrgEnhetsLeder>>() {});

        assertEquals(0, outputLedereNew.size());

    }

    @Test
    @DisplayName("Henter ut person navn i kontekst av enhet leder spørring")
    void scenario34() {
        final var mockEnhet = DvhOrgEnhetMock.getBuilderWithDefault().ORGENHET_KODE("testId1").ORGENHET_NAVN("testNavn").ORGNODE_NIV("ORGENHET").build();
        dvhOrgenhetMockUtils.addToResponse(mockEnhet);
        RessursState ressurs = createRessursHendelseEventSource("");

        pdlUtils.addPersonToPdlResponse(true, ressurs.personident());
        dvhRestConsumer.loadRessurserOgOrgEnheter();

        // Gammel Search - FeatureToggle of
        enableOrgEnhetEventSource(false);

        dvhRestConsumer.loadKoblingerLedereOgTlfFromDvh();

        final var responseOld = postForResource("organisasjonsenhet/scenario34.graphql");
        final var outputEnhetJsonOld = responseOld.path("organisasjonsenhet").entity(JsonNode.class).get();

        final var lederNavnOld = outputEnhetJsonOld.get("leder")
                .get(0)
                .get("ressurs")
                .get("person")
                .get("navn");

        pdlUtils.assertNavn(ressurs.personident(), lederNavnOld);

        // Ny Search - FeatureToggle on
        enableOrgEnhetEventSource(true);

        // Since stream infrastructure is not up, the streams pipeline has not created state.
        // Lets do it manually, to test getmethods below.
        orgEnhetStateRepository.putEventAggregate(dvhOrgenhetMockUtils.dvhMockToState(mockEnhet, 1, String.valueOf(mockEnhet.getORGENHET_FRA_PERIODE()), Optional.of(String.valueOf(mockEnhet.getORGENHET_TIL_PERIODE()))));

        dvhRestConsumer.loadKoblingerLedereOgTlfFromDvh();

        final var responseNew = postForResource("organisasjonsenhet/scenario34.graphql");
        final var outputEnhetJsonNew = responseNew.path("organisasjonsenhet").entity(JsonNode.class).get();

        final var lederNavnNew = outputEnhetJsonNew.get("leder")
            .get(0)
            .get("ressurs")
            .get("person")
            .get("navn");

        pdlUtils.assertNavn(ressurs.personident(), lederNavnOld);
    }

    @Test
    @DisplayName("Leser kobling fra DVH og leser fra graphQl")
    void scenario41() {
        final var mockEnhet = DvhOrgEnhetMock.getBuilderWithDefault().ORGENHET_KODE("testId1").ORGENHET_NAVN("testNavn").ORGNODE_NIV("ORGENHET").build();
        dvhOrgenhetMockUtils.addToResponse(mockEnhet);

        RessursState ressurs = createRessursHendelseEventSource("");
        dvhRestConsumer.loadRessurserOgOrgEnheter();

        // Gammel Search - FeatureToggle of
        enableOrgEnhetEventSource(false);

        dvhRestConsumer.loadKoblingerLedereOgTlfFromDvh();

        final var responseOld = postForResource("organisasjonsenhet/scenario41.graphql");

        final var outputEnhetJsonOld = responseOld.path("organisasjonsenhet").entity(JsonNode.class).get();
        final var outputKoblingerOld = objectMapper.convertValue(outputEnhetJsonOld.get("koblinger"), new TypeReference<List<OrgEnhetsKobling>>() {});

        assertEquals(1, outputKoblingerOld.size());

        final var outputKoblingOld = outputKoblingerOld.get(0);
        assertEquals("2021-01-01", outputKoblingOld.getGyldigFom().toString());
        assertEquals("2099-12-31", outputKoblingOld.getGyldigTom().toString());

        final var outputRessursOld = outputKoblingerOld.get(0).getRessurs();
        assertEquals(ressurs.navident(), outputRessursOld.getNavIdent());
        assertEquals(ressurs.personident(), outputRessursOld.getPersonIdent());

        // Ny Search - FeatureToggle on
        enableOrgEnhetEventSource(true);

        // Since stream infrastructure is not up, the streams pipeline has not created state.
        // Lets do it manually, to test getmethods below.
        orgEnhetStateRepository.putEventAggregate(dvhOrgenhetMockUtils.dvhMockToState(mockEnhet, 1, String.valueOf(mockEnhet.getORGENHET_FRA_PERIODE()), Optional.of(String.valueOf(mockEnhet.getORGENHET_TIL_PERIODE()))));

        dvhRestConsumer.loadKoblingerLedereOgTlfFromDvh();

        final var responseNew = postForResource("organisasjonsenhet/scenario41.graphql");

        final var outputEnhetJsonNew = responseNew.path("organisasjonsenhet").entity(JsonNode.class).get();
        final var outputKoblingerNew = objectMapper.convertValue(outputEnhetJsonNew.get("koblinger"), new TypeReference<List<OrgEnhetsKobling>>() {});

        assertEquals(1, outputKoblingerNew.size());

        final var outputKoblingNew = outputKoblingerNew.get(0);
        assertEquals("2021-01-01", outputKoblingNew.getGyldigFom().toString());
        assertEquals("2099-12-31", outputKoblingNew.getGyldigTom().toString());

        final var outputRessursNew = outputKoblingerNew.get(0).getRessurs();
        assertEquals(ressurs.navident(), outputRessursNew.getNavIdent());
        assertEquals(ressurs.personident(), outputRessursNew.getPersonIdent());
    }

    @Test
    @DisplayName("Kobling er en tom liste om ingen kobling som refererer til organisasjonsenheten eksisterer.")
    void scenario42() {
        final var mockEnhet = DvhOrgEnhetMock.getBuilderWithDefault().ORGENHET_KODE("testId10").ORGENHET_NAVN("testNavn10").ORGNODE_NIV("ORGENHET").build();
        dvhOrgenhetMockUtils.addToResponse(mockEnhet);

        dvhRestConsumer.loadRessurserOgOrgEnheter();

        // Gammel Search - FeatureToggle of
        enableOrgEnhetEventSource(false);

        dvhRestConsumer.loadKoblingerLedereOgTlfFromDvh();

        final var responseOld = postForResource("organisasjonsenhet/scenario42.graphql");
        final var outputEnhetJsonOld = responseOld.path("organisasjonsenhet").entity(JsonNode.class).get();

        assertThat(outputEnhetJsonOld.get("koblinger")).isEmpty();

        // Ny Search - FeatureToggle on
        enableOrgEnhetEventSource(true);

        // Since stream infrastructure is not up, the streams pipeline has not created state.
        // Lets do it manually, to test getmethods below.
        orgEnhetStateRepository.putEventAggregate(dvhOrgenhetMockUtils.dvhMockToState(mockEnhet, 1, String.valueOf(mockEnhet.getORGENHET_FRA_PERIODE()), Optional.of(String.valueOf(mockEnhet.getORGENHET_TIL_PERIODE()))));

        dvhRestConsumer.loadKoblingerLedereOgTlfFromDvh();

        final var responseNew = postForResource("organisasjonsenhet/scenario42.graphql");
        final var outputEnhetJsonNew = responseNew.path("organisasjonsenhet").entity(JsonNode.class).get();

        assertThat(outputEnhetJsonNew.get("koblinger")).isEmpty();
    }

    @Test
    @DisplayName("Henter ut person navn i kontekst av enhet kobling spørring")
    void scenario43() {
        final var mockEnhet = DvhOrgEnhetMock.getBuilderWithDefault().ORGENHET_KODE("testId1").ORGENHET_NAVN("testNavn").ORGNODE_NIV("ORGENHET").build();
        dvhOrgenhetMockUtils.addToResponse(mockEnhet);

        RessursState ressurs = createRessursHendelseEventSource("");
        pdlUtils.addPersonToPdlResponse(true, ressurs.personident());
        dvhRestConsumer.loadRessurserOgOrgEnheter();

        // Gammel Search - FeatureToggle of
        enableOrgEnhetEventSource(false);

        dvhRestConsumer.loadKoblingerLedereOgTlfFromDvh();

        final var responseOld = postForResource("organisasjonsenhet/scenario34.graphql");
        final var outputEnhetJsonOld = responseOld.path("organisasjonsenhet").entity(JsonNode.class).get();

        final var lederNavnOld = outputEnhetJsonOld.get("koblinger")
                .get(0)
                .get("ressurs")
                .get("person")
                .get("navn");

        pdlUtils.assertNavn(ressurs.personident(), lederNavnOld);

        // Ny Search - FeatureToggle on
        enableOrgEnhetEventSource(true);

        // Since stream infrastructure is not up, the streams pipeline has not created state.
        // Lets do it manually, to test getmethods below.
        orgEnhetStateRepository.putEventAggregate(dvhOrgenhetMockUtils.dvhMockToState(mockEnhet, 1, String.valueOf(mockEnhet.getORGENHET_FRA_PERIODE()), Optional.of(String.valueOf(mockEnhet.getORGENHET_TIL_PERIODE()))));

        dvhRestConsumer.loadKoblingerLedereOgTlfFromDvh();

        final var responseNew = postForResource("organisasjonsenhet/scenario34.graphql");
        final var outputEnhetJsonNew = responseNew.path("organisasjonsenhet").entity(JsonNode.class).get();

        final var lederNavnNew = outputEnhetJsonNew.get("koblinger")
            .get(0)
            .get("ressurs")
            .get("person")
            .get("navn");

        pdlUtils.assertNavn(ressurs.personident(), lederNavnNew);

    }

    private Optional<OrgEnhetResult> getEnhetInList(String id, List<OrgEnhetResult> organisasjonsenhetResult) {
        return organisasjonsenhetResult.stream()
                .filter(e -> e.getId().equals(id))
                .findFirst();
    }
}

