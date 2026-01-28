package no.nav.nom.apps.api.graphql.ressurs;

import com.fasterxml.jackson.databind.JsonNode;
import no.nav.nom.apps.api.common.OrgEnhetStateBuilder;
import no.nav.nom.apps.api.common.RessursStateBuilder;
import no.nav.nom.apps.api.graphql.GraphQlTestBase;
import no.nav.nom.contract.nom.state.OrgEnhetLederState;
import no.nav.nom.contract.nom.state.OrganiseringState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static no.nav.nom.apps.api.common.TestData.orgtilknytningState;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles({"GraphQLTest","localgql"})
@GraphQlTest
@DisplayName("GraphQL.HentLederForRessursFlereNivaer")
public class LederForRessursTest extends GraphQlTestBase {


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
        resetState(); // workaround da testene per nå kan påvirke hverandre (pga samme navident i bruk?)
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


    @Test
    @DisplayName("Hente leder gjennom flere nivåer for ressurs som er leder av sin enhet, stye ressursLeder dato innenfor OT dato ")
    void hente_leder_gjennom_flere_nivaaer_for_ressurs_som_leder_enhet_styr_dato_i_ressursLeder_info_innenfor_ot_dato() {
        resetState(); // workaround da testene per nå kan påvirke hverandre (pga samme navident i bruk?)

        var ressurs = new RessursStateBuilder(1).navident("testNavIdent1").build();
        var leder1 = new RessursStateBuilder(1).navident("leder1").personident("01011000000").build();
        var leder2 = new RessursStateBuilder(1).navident("leder2").personident("01011011111").build();

        ressursStateRepository.update(ressurs);
        ressursStateRepository.update(leder1);
        ressursStateRepository.update(leder2);

        var orgenhet = new OrgEnhetStateBuilder(1).id("ID_1").agressoId("testId2").agressoOrgNivaa("ORGENHET").build();
        var ledersLederEnhet = new OrgEnhetStateBuilder(1).id("idLederEnhet").navn("Leder sin enhet").agressoOrgNivaa("ORGNIV2").lederFid(leder2.fid()).build();

        orgEnhetStateRepository.update(orgenhet);
        orgEnhetStateRepository.update(ledersLederEnhet);

        organiseringStateRepository.update(new OrganiseringState("FID_1", orgenhet.id(), orgenhet.navn(), ledersLederEnhet.id(), ledersLederEnhet.navn(), LocalDate.now().minusYears(1), null));

        var ot = orgtilknytningState().toBuilder()
            .fid("FID_1")
            .navident(ressurs.navident())
            .orgEnhetId(orgenhet.id())
            .gyldigFom(LocalDate.now().minusMonths(1))
            .gyldigTom(LocalDate.now().plusMonths(1))
            .build();
        orgtilknytningStateRepository.update(ot.fid(), ot);

        var lederStateSegSelv = OrgEnhetLederState.builder().fid("LEDER_FID_1").orgEnhetId(orgenhet.id()).navident(ressurs.navident()).startdato(LocalDate.now().minusYears(1)).sluttdato(LocalDate.now().minusDays(16)).build();
        var lederlederState =   OrgEnhetLederState.builder().fid("LEDER_FID_2").orgEnhetId(ledersLederEnhet.id()).navident(leder1.navident()).startdato(LocalDate.now().minusYears(1)).sluttdato(null).build();
        var lederState2 =       OrgEnhetLederState.builder().fid("LEDER_FID_3").orgEnhetId(orgenhet.id()).navident(leder2.navident()).startdato(LocalDate.now().minusDays(15)).sluttdato(null).build();
        orgEnhetLederStateRepository.update(lederStateSegSelv.fid(),lederStateSegSelv);
        orgEnhetLederStateRepository.update(lederlederState.fid(),lederlederState);
        orgEnhetLederStateRepository.update(lederState2.fid(),lederState2);

        final var response = gqlUtil.postForResource("ressurs/ledere_over_rekursivt.graphql");

        final var ledere = response.path("ressurs.ledere").entity(JsonNode.class).get();
        assertEquals(2, ledere.size());

        // Navident til leder på første nivå der ressurs ikke selv er leder skal returneres
        assertEquals(leder1.navident(),             ledere.get(0).get("ressurs").get("navident").textValue());
        assertEquals(ot.gyldigFom(),                LocalDate.parse(ledere.get(0).get("gyldigFom").textValue()));
        assertEquals(lederStateSegSelv.sluttdato(), LocalDate.parse(ledere.get(0).get("gyldigTom").textValue()));
        assertEquals(ledersLederEnhet.id(),         ledere.get(0).get("orgEnhet").get("id").textValue());

        assertEquals(leder2.navident(),             ledere.get(1).get("ressurs").get("navident").textValue());
        assertEquals(lederState2.startdato(),       LocalDate.parse(ledere.get(1).get("gyldigFom").textValue()));
        assertEquals(ot.gyldigTom(),                LocalDate.parse(ledere.get(1).get("gyldigTom").textValue()));
        assertEquals(orgenhet.id(),                 ledere.get(1).get("orgEnhet").get("id").textValue());
    }
}
