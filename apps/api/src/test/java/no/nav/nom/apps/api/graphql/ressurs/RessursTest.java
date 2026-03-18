package no.nav.nom.apps.api.graphql.ressurs;

import no.nav.nom.apps.api.graphql.GraphQLMatchers;
import no.nav.nom.apps.api.graphql.GraphQlTestBase;
import no.nav.nom.apps.api.orgenhet.OrgenhetTestUtils;
import no.nav.nom.contract.ad.AdRessurs;
import no.nav.nom.contract.ad.AdRessurser;
import no.nav.nom.contract.nom.IdentType;
import no.nav.nom.contract.nom.TelefonType;
import no.nav.nom.contract.nom.api.graphql.Ressurs;
import no.nav.nom.contract.nom.api.graphql.RessursResult;
import no.nav.nom.contract.nom.api.graphql.ResultCode;
import no.nav.nom.contract.nom.eventsource.Jobbtelefon;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;
import no.nav.nom.contract.nom.state.OrgEnhetLederState;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.contract.nom.state.OrganiseringState;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.JsonNode;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static no.nav.nom.apps.api.TestUtil.createRessursState;
import static no.nav.nom.apps.api.TestUtil.createRessursStateBuilder;
import static no.nav.nom.apps.api.common.TestData.orgtilknytningState;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles({"GraphQLTest","localgql"})
@GraphQlTest
public class RessursTest extends GraphQlTestBase {


    @BeforeEach
    protected void setup() {
        orgEnhetStateRepository.clear();
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

        var resultMap = response.stream()
            .collect(Collectors.toMap(
                RessursResult::getId,      // Use the 'id' field as the Map key
                r -> r
            ));

        assertEquals(4, response.size());
        RessursResult navIdent = resultMap.get("testNavIdent1");
        assertEquals("testNavIdent1", navIdent.getId());
        assertEquals(ResultCode.OK, navIdent.getCode());

        RessursResult navIdentNotFound = resultMap.get("notFoundNavIdent");
        assertEquals("notFoundNavIdent", navIdentNotFound.getId());
        assertEquals(ResultCode.NOT_FOUND, navIdentNotFound.getCode());

        RessursResult personIdent = resultMap.get("testPersonIdent2");
        assertEquals("testPersonIdent2", personIdent.getId());
        assertEquals(ResultCode.OK, personIdent.getCode());

        RessursResult personIdentNotFound = resultMap.get("notFoundPersonIdent");
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
        final var outputOrganisasjonsEnhetId = outputRessurs.get("orgTilknytninger").get(0).get("orgEnhet").get("agressoId").asString();

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

        var resultMap = ressurser.stream()
            .collect(Collectors.toMap(
                r -> r.get("id").asString(),      // Use the 'id' field as the Map key
                r -> r
            ));

        var result1 = resultMap.get(ressurs1.navident());
        var result2 = resultMap.get(ressurs2.navident());

        assertEquals(ressurs1.visningsnavn().getFornavnMellomnavn(),  result1.get("ressurs").get("fornavn").asString());
        assertEquals(ressurs1.visningsnavn().etternavn(),             result1.get("ressurs").get("etternavn").asString());

        assertEquals(ressurs2.visningsnavn().getFornavnMellomnavn(),  result2.get("ressurs").get("fornavn").asString());
        assertEquals(ressurs2.visningsnavn().etternavn(),             result2.get("ressurs").get("etternavn").asString());
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

        var resultMap = ressurser.stream()
            .collect(Collectors.toMap(
                r -> r.get("id").asString(),      // Use the 'id' field as the Map key
                r -> r
            ));

        var result1 = resultMap.get(ressurs1.navident());
        var result2 = resultMap.get(ressurs2.navident());

        // assertions krever samme rekkefølge i respons som i event-source
        assertEquals(ressurs1.visningsnavn().getFornavnMellomnavn(),  result1.get("ressurs").get("fornavn").asString());
        assertEquals(ressurs1.visningsnavn().etternavn(),             result1.get("ressurs").get("etternavn").asString());

        assertEquals(ressurs2.visningsnavn().getFornavnMellomnavn(),  result2.get("ressurs").get("fornavn").asString());
        assertEquals(ressurs2.visningsnavn().etternavn(),             result2.get("ressurs").get("etternavn").asString());
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

        var fakeRespons = Stream.of(ressurs1, ressurs2, ressurs3).map(ressursState -> {
            if (ressursState.equals(ressurs3)) {
                return AdRessurs.ikkeFunnet(ressursState.navident());
            }
            return AdRessurs.builder().navIdent(ressursState.navident()).email(ressursState.navident() + "@dummy.mail").build();
        }).toList();

        Mockito.when(this.adClient.getAdRessurser(Mockito.anySet()))
            .thenReturn(new AdRessurser(fakeRespons));

        final var response = gqlUtil.postForResource("ressurs/scenario8.graphql");
        MatcherAssert.assertThat(response, GraphQLMatchers.hasErrors());

        final var ressurser = response.path("ressurser")
            .entityList(JsonNode.class)
            .hasSize(3)
            .get();

        ressurser.sort(Comparator.comparing(j -> j.get("id").asString()));

        assertEquals("testNavIdent1@dummy.mail", ressurser.get(0).get("ressurs").get("epost").asString());
        assertEquals(ressurser.get(0).get("ressurs").get("visningsNavn").asString(), ressurs1.visningsnavn().getFormattedVisningsnavn());
        assertEquals("testNavIdent2@dummy.mail", ressurser.get(1).get("ressurs").get("epost").asString());
        assertEquals(ressurser.get(1).get("ressurs").get("visningsNavn").asString(), ressurs2.visningsnavn().getFormattedVisningsnavn());
        assertEquals(ressurser.get(2).get("ressurs").get("visningsNavn").asString(), ressurs3.visningsnavn().getFormattedVisningsnavn());
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
        assertEquals(orgenhet.agressoId(), lederFor.get(0).get("orgEnhet").get("agressoId").stringValue());
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

        final var response = gqlUtil.postForResource("ressurs/scenario7.graphql");

        final var ledere = response.path("ressurs.ledere").entity(JsonNode.class).get();
        assertEquals(1, ledere.size());
        assertEquals(leder.navident(), ledere.get(0).get("ressurs").get("navIdent").stringValue());
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
        assertEquals(overLederRessurs.navident(), ledere.get(0).get("ressurs").get("navIdent").stringValue());
    }

    @Test
    @DisplayName("Kan slå opp ressurs ved eksakt telefonnummer")
    void scenario52() {
        var telefonNummer = "4712345678";
        var ressurs = createRessursStateBuilder("1").jobbtelefon(new Jobbtelefon(telefonNummer, TelefonType.NAV_TJENESTE_TELEFON)).build();
        ressursStateRepository.update(ressurs);


        final var response = gqlUtil.postForResource("ressurs/scenario52.graphql");
        response.errors();

        final var outputRessurs = response.path("ressurs").entity(JsonNode.class).get();
        assertEquals(ressurs.navident(), outputRessurs.get("navident").stringValue());
        assertEquals(telefonNummer.substring(2), outputRessurs.get("telefon").get(0).get("nummer").stringValue());
    }

    @Test
    @DisplayName("Kan slå opp ressurs ved telefonnummer uten landskode")
    void scenario53() {
        var ressurs = createRessursState("1");
        ressursStateRepository.update(ressurs);

        String telefonNummer = "4712345678";

        final var response = gqlUtil.postForResource("ressurs/scenario53.graphql");

        final var outputRessurs = response.path("ressurs").entity(JsonNode.class).get();
        assertEquals(ressurs.navident(), outputRessurs.get("navident").stringValue());
        assertEquals(telefonNummer.substring(2), outputRessurs.get("telefon").get(0).get("nummer").stringValue());
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
        assertEquals(jobbtelefon, outputRessurs.get("primaryTelefon").stringValue());
        assertEquals(2, outputRessurs.get("telefon").size());
        assertEquals(jobbtelefon, outputRessurs.get("telefon").get(0).get("nummer").stringValue());
        assertEquals(nomTelefon, outputRessurs.get("telefon").get(1).get("nummer").stringValue());
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
        assertEquals(jobbtelefon, outputRessurs.get("primaryTelefon").stringValue());
        assertEquals(2, outputRessurs.get("telefon").size());
        assertEquals(jobbtelefon, outputRessurs.get("telefon").get(0).get("nummer").stringValue());
        assertEquals(nomTelefon, outputRessurs.get("telefon").get(1).get("nummer").stringValue());
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
        assertEquals(nomTelefon, outputRessurs.get("primaryTelefon").stringValue());
        assertEquals(1, outputRessurs.get("telefon").size());
        assertEquals(nomTelefon, outputRessurs.get("telefon").get(0).get("nummer").stringValue());
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
        assertNull(outputRessurs.get("primaryTelefon").stringValue());
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
        assertEquals(IdentType.STANDARD.name(), outputRessurs.get("identType").stringValue());
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
        assertEquals(IdentType.ROBOT.name(), outputRessurs.get("identType").stringValue());
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
        assertEquals(IdentType.UKJENT.name(), outputRessurs.get("identType").stringValue());
    }

    @Test
    @DisplayName("check deprecated ressurs.orgTilknytning")
    void scenario14(){
        var r = createRessursState("1");
        ressursStateRepository.update(r);

        var commonOrgtilknytningBuilder = OrgtilknytningState.builder().navident("testNavIdent1").gyldigFom(LocalDate.now().minusDays(1));

        orgtilknytningStateRepository.update("abc1", commonOrgtilknytningBuilder.fid("1-fid1").orgEnhetId("abc1").build());
        orgtilknytningStateRepository.update("abc2", commonOrgtilknytningBuilder.fid("1-fid2").orgEnhetId("abc2").build());
        orgEnhetStateRepository.update(OrgEnhetState.builder().fid("fid-abc1").id("abc1").build());
        orgEnhetStateRepository.update(OrgEnhetState.builder().fid("fid-abc2").id("abc2").build());

        // language=graphql
        var gqlQry = """
              query A{
                ressurs(where: {navident: "testNavIdent1"}){
                    navident
                    orgTilknytning{
                      orgEnhet{
                        id
                      }
                    }
                }
              }
              """;

        final var resp = gqlUtil.postForQuery(gqlQry);
        resp.errors().verify();
        resp.path("ressurs.navident").hasValue();
        resp.path("ressurs.orgTilknytning[0].orgEnhet.id").hasValue();
        resp.path("ressurs.orgTilknytning[1].orgEnhet.id").hasValue();


    }

    private String sladdetPersonident(String personident){
        return personident.replaceFirst(".{5}$","*****");
    }
}
