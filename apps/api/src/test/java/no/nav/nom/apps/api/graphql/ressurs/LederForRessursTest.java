package no.nav.nom.apps.api.graphql.ressurs;

import com.fasterxml.jackson.databind.JsonNode;
import no.nav.nom.apps.api.common.OrgEnhetStateBuilder;
import no.nav.nom.apps.api.common.RessursStateBuilder;
import no.nav.nom.apps.api.graphql.GraphQLTestUtil;
import no.nav.nom.apps.api.graphql.NomGraphQlControllerForTest;
import no.nav.nom.apps.api.graphql.GraphqlTestConfig;
import no.nav.nom.apps.gql.NomGraphQlAutoConfiguration;
import no.nav.nom.contract.nom.state.OrgEnhetLederState;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepositoryImpl;
import no.nav.nom.libs.state.orgenhetleder.OrgEnhetLederStateRepositoryImpl;
import no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateRepositoryImpl;
import no.nav.nom.libs.state.ressurs.RessursStateRepositoryImpl;
import no.nav.nom.libs.test.MockOAuth2ServerInitializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.time.LocalDate;

import static no.nav.nom.apps.api.common.TestData.orgtilknytningState;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("GraphQLTest")
@GraphQlTest
@ContextConfiguration(classes = {GraphqlTestConfig.class, NomGraphQlAutoConfiguration.class, NomGraphQlControllerForTest.class}, initializers = MockOAuth2ServerInitializer.class)
@DisplayName("GraphQL.HentLederForRessursFlereNivaer")
public class LederForRessursTest {
    @Autowired
    protected GraphQLTestUtil gqlUtil;

    @Autowired
    RessursStateRepositoryImpl ressursStateRepository;
    @Autowired
    OrgEnhetStateRepositoryImpl orgEnhetStateRepository;
    @Autowired
    OrgtilknytningStateRepositoryImpl orgtilknytningStateRepository;
    @Autowired
    OrgEnhetLederStateRepositoryImpl orgEnhetLederStateRepository;

    public LederForRessursTest() {

    }

    public void resetState(
    ) {
        orgEnhetLederStateRepository.clear();
        orgEnhetStateRepository.clear();
        orgtilknytningStateRepository.clear();
        ressursStateRepository.clear();
    }

    @Test
    @DisplayName("Hente leder gjennom flere nivåer for ressurs som er leder av sin enhet ")
    void hente_leder_gjennom_flere_nivaaer_for_ressurs_som_leder_enhet() {
        resetState(); // workaround da testene per nå kan påvirke hverandre
        final String lederEnhetId = "idLederEnhet";
        final String lederEnhetNavn = "Leder sin enhet";
        final String lederEnhetNiva = "ORGNIV2";

        var ressurs = new RessursStateBuilder(1).navident("testNavIdent1").build();
        var leder = new RessursStateBuilder(1).personident("01011012345").build();

        ressursStateRepository.update(ressurs);
        ressursStateRepository.update(leder);

        var orgenhet = new OrgEnhetStateBuilder(1).id("ID_1").agressoId("testId2").agressoOrgNivaa("ORGENHET").build();
        var ledersEnhet = new OrgEnhetStateBuilder(1).id(lederEnhetId).navn(lederEnhetNavn).agressoOrgNivaa(lederEnhetNiva).lederFid(leder.fid()).build();

        orgEnhetStateRepository.update(orgenhet);
        orgEnhetStateRepository.update(ledersEnhet);

        orgtilknytningStateRepository.update(
            "FID_1",
            orgtilknytningState().toBuilder()
                .fid("FID_1")
                .navident(ressurs.navident())
                .orgEnhetId(orgenhet.id()).build()
        );

        var lederState = OrgEnhetLederState.builder().fid("??").orgEnhetId(orgenhet.id()).navident(leder.navident()).startdato(LocalDate.now().minusYears(1)).build();
        orgEnhetLederStateRepository.update(lederState.fid(),lederState);

        final var response = gqlUtil.postForResource("ressurs/leder_over_rekursivt.graphql");

        final var ledere = response.path("ressurs.ledere").entity(JsonNode.class).get();
        assertEquals(1, ledere.size());

        // Navident til leder på første nivå der ressurs ikke selv er leder skal returneres
        assertEquals(leder.navident(), ledere.get(0).get("ressurs").get("navident").textValue());

        // Sjekk at personnummer er sladdet ut.
        assertTrue(ledere.get(0).get("ressurs").get("personident").textValue().endsWith("*****"));
    }
}
