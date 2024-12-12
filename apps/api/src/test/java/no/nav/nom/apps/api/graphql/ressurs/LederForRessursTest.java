package no.nav.nom.apps.api.graphql.ressurs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import no.nav.nom.apps.api.DvhOrgEnhetMock;
import no.nav.nom.apps.api.DvhOrgEnhetMockUtils;
import no.nav.nom.apps.api.dvh.DvhOldTdvService;
import no.nav.nom.apps.api.dvh.DvhRestConsumer;
import no.nav.nom.apps.api.dvh.dto.DvhLeder;
import no.nav.nom.apps.api.dvh.dto.DvhOrganisering;
import no.nav.nom.apps.api.graphql.GraphQLExtendedIntegrationTestBase;
import no.nav.nom.apps.api.organisering.DvhOrganiseringMockUtils;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static no.nav.nom.apps.api.common.TestData.orgtilknytningState;
import static no.nav.nom.apps.api.orgenhet.OrgenhetTestUtils.orgEnhetStateToOrganiseringState;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@DisplayName("GraphQL.HentLederForRessursFlereNivaer")
public class LederForRessursTest extends GraphQLExtendedIntegrationTestBase {

    @Autowired
    private DvhRestConsumer dvhRestConsumer;

    @Autowired
    public DvhOrgEnhetMockUtils dvhOrgenhetMockUtils;

    @Autowired
    public DvhOrganiseringMockUtils dvhOrganiseringMockUtils;

    @Autowired
    OrgtilknytningStateRepository orgtilknytningStateRepository;

    @MockBean
    DvhOldTdvService dvhOldTdvService;

    @Test
    @DisplayName("Hente leder gjennom flere nivåer for ressurs som er leder av sin enhet ")
    void hente_leder_gjennom_flere_nivaaer_for_ressurs_som_leder_enhet() throws JsonProcessingException {

        // Ressursen vi skal hente leder for er leder på to nivåer
        final String ressursNedersteEnhetId = "idRessursEnhet";
        final String ressursNedersteEnhetNavn = "Ressurs sin nederste enhet";
        final String ressursNedersteEnhetNiva = "ORGENHET";

        final String ressursOvreEnhetId = "idMellomEnhet";
        final String ressursOvreEnhetNavn = "Ressurs sin øverste enhet";
        final String ressursOvreEnhetNiva = "ORGNIV3";

        final String lederEnhetId = "idLederEnhet";
        final String lederEnhetNavn = "Leder sin enhet";
        final String lederEnhetNiva = "ORGNIV2";


        dvhOrgenhetMockUtils.createEmptyResponse(); // Ikke bruke Wiremock-responser fra JSON-konfig

        var ressurs = kafkaUtils.createRessursHendelseEventSource("1");   // Oppretter ressursen med ID "testNavIdent1"
        var leder = kafkaUtils.createRessursHendelseEventSource("2");     // Oppretter leder med ID "testNavIdent2"


        var ressursSinNedersteEnhet = DvhOrgEnhetMock.getBuilderWithDefault()
            .ORGENHET_KODE(ressursNedersteEnhetId)
            .ORGNODE_NIV(ressursNedersteEnhetNiva)
            .ORGENHET_NAVN(ressursNedersteEnhetNavn)
            .LEDER_RESSURSNR("testNavIdent1")
            .build();

        var ressursSinOvreEnhet = DvhOrgEnhetMock.getBuilderWithDefault()
            .ORGENHET_KODE(ressursOvreEnhetId)
            .ORGNODE_NIV(ressursOvreEnhetNiva)
            .ORGENHET_NAVN(ressursOvreEnhetNavn)
            .LEDER_RESSURSNR("testNavIdent1")
            .build();

        var ledersEnhet = DvhOrgEnhetMock.getBuilderWithDefault()
            .ORGENHET_KODE(lederEnhetId)
            .ORGNODE_NIV(lederEnhetNiva)
            .ORGENHET_NAVN(lederEnhetNavn)
            .LEDER_RESSURSNR("testNavIdent2")
            .build();

        // Brukes bare for å få en gyldig organisering for ledersEnhet
        var enhetPaHoyereNiva = DvhOrgEnhetMock.getBuilderWithDefault()
            .ORGENHET_KODE("id")
            .ORGNODE_NIV("ORGNIV1")
            .ORGENHET_NAVN("navn")
            .LEDER_RESSURSNR("leder")
            .build();

        dvhOrgenhetMockUtils.addToResponse(ressursSinNedersteEnhet, ressursSinOvreEnhet, ledersEnhet);

        // Streams-app er ikke aktiv, så må legge manuelt inn i repository
        var ressursSinNedersteEnhetState = addToOrgEnhetStateRepository(1, ressursSinNedersteEnhet);
        var ressursSinOvreEnhetState = addToOrgEnhetStateRepository(2, ressursSinOvreEnhet);
        var ledersEnhetState = addToOrgEnhetStateRepository(3, ledersEnhet);
        var enhetPaHoyereNivaState = addToOrgEnhetStateRepository(4, enhetPaHoyereNiva);

        var dvhOrganisering1 = mockOrganisering(ressursSinNedersteEnhet, ressursSinOvreEnhet);
        var dvhOrganisering2 = mockOrganisering(ressursSinOvreEnhet, ledersEnhet);
        var dvhOrganisering3 = mockOrganisering(ledersEnhet, enhetPaHoyereNiva);

        var dvhLeder1 = mockLeder(ressursSinNedersteEnhet);
        var dvhLeder2 = mockLeder(ressursSinOvreEnhet);
        var dvhLeder3 = mockLeder(ledersEnhet);

        // Streams-app er ikke aktiv, så må legge manuelt inn i repository
        addToOrganiseringStateRepository(1, ressursSinNedersteEnhetState, ressursSinOvreEnhetState);
        addToOrganiseringStateRepository(2, ressursSinOvreEnhetState, ledersEnhetState);
        addToOrganiseringStateRepository(3, ledersEnhetState, enhetPaHoyereNivaState);

        when(dvhOldTdvService.getTdvOrganiseringer()).thenReturn(List.of(dvhOrganisering1, dvhOrganisering2, dvhOrganisering3));
        when(dvhOldTdvService.getTdvLedere()).thenReturn(List.of(dvhLeder1, dvhLeder2, dvhLeder3));

        when(dvhOldTdvService.getTdvOrganiseringer()).thenReturn(List.of(dvhOrganisering1, dvhOrganisering2, dvhOrganisering3));
        when(dvhOldTdvService.getTdvLedere()).thenReturn(List.of(dvhLeder1, dvhLeder2, dvhLeder3));
        when(dvhOldTdvService.getTdvOrganiseringer()).thenReturn(List.of(dvhOrganisering1, dvhOrganisering2, dvhOrganisering3));
        when(dvhOldTdvService.getTdvLedere()).thenReturn(List.of(dvhLeder1, dvhLeder2, dvhLeder3));

        // Knytte ressurs til org. enhet på laveste nivå
        orgtilknytningStateRepository.update(
            "FID_1",
            orgtilknytningState().toBuilder()
                .fid("FID_1")
                .navident(ressurs.navident())
                .orgEnhetId("ID_1")
                .erDagligOppfolging(true)
                .build()
        );

        dvhRestConsumer.loadLedereAndOrganiseringerFromDvh();

        final var response = postForResource("ressurs/leder_over_rekursivt.graphql");

        final var ledere = response.path("ressurs.ledere").entity(JsonNode.class).get();
        assertThat(ledere.size()).isEqualTo(1);


        // Navident til leder på første nivå der ressurs ikke selv er leder skal returneres
        assertThat(ledere.get(0).get("ressurs").get("navident").textValue()).isEqualTo(leder.navident());

        // Sjekk at personnummer er sladdet ut.
        assertThat(ledere.get(0).get("ressurs").get("personident").textValue()).endsWith("*****");
    }

    @Test
    @DisplayName("Ny organisering: Hente leder gjennom flere nivåer for ressurs som er leder av sin enhet ")
    void hente_leder_gjennom_flere_nivaer_for_ressurs_som_leder_enhet_med_ny_organisering() throws JsonProcessingException {
        // Ressursen vi skal hente leder for er leder på to nivåer
        final String ressursNedersteEnhetId = "idRessursEnhet";
        final String ressursNedersteEnhetNavn = "Ressurs sin nederste enhet";
        final String ressursNedersteEnhetNiva = "ORGENHET";

        final String ressursOvreEnhetId = "idMellomEnhet";
        final String ressursOvreEnhetNavn = "Ressurs sin øverste enhet";
        final String ressursOvreEnhetNiva = "ORGNIV3";

        final String lederEnhetId = "idLederEnhet";
        final String lederEnhetNavn = "Leder sin enhet";
        final String lederEnhetNiva = "ORGNIV2";


        dvhOrgenhetMockUtils.createEmptyResponse(); // Ikke bruke Wiremock-responser fra JSON-konfig

        RessursState ressurs = kafkaUtils.createRessursHendelseEventSource("1");   // Oppretter ressursen med ID "testNavIdent1"
        RessursState leder = kafkaUtils.createRessursHendelseEventSource("2");     // Oppretter leder med ID "testNavIdent2"


        DvhOrgEnhetMock ressursSinNedersteEnhet = DvhOrgEnhetMock.getBuilderWithDefault()
            .ORGENHET_KODE(ressursNedersteEnhetId)
            .ORGNODE_NIV(ressursNedersteEnhetNiva)
            .ORGENHET_NAVN(ressursNedersteEnhetNavn)
            .LEDER_RESSURSNR("testNavIdent1")
            .build();

        DvhOrgEnhetMock ressursSinOvreEnhet = DvhOrgEnhetMock.getBuilderWithDefault()
            .ORGENHET_KODE(ressursOvreEnhetId)
            .ORGNODE_NIV(ressursOvreEnhetNiva)
            .ORGENHET_NAVN(ressursOvreEnhetNavn)
            .LEDER_RESSURSNR("testNavIdent1")
            .build();

        DvhOrgEnhetMock ledersEnhet = DvhOrgEnhetMock.getBuilderWithDefault()
            .ORGENHET_KODE(lederEnhetId)
            .ORGNODE_NIV(lederEnhetNiva)
            .ORGENHET_NAVN(lederEnhetNavn)
            .LEDER_RESSURSNR("testNavIdent2")
            .build();

        // Brukes bare for å få en gyldig organisering for ledersEnhet
        DvhOrgEnhetMock enhetPaHoyereNiva = DvhOrgEnhetMock.getBuilderWithDefault()
            .ORGENHET_KODE("idHoyereNiva")
            .ORGNODE_NIV("ORGNIV1")
            .ORGENHET_NAVN("Enhet høyere nivå")
            .LEDER_RESSURSNR("lederHoyereNiva")
            .build();

        dvhOrgenhetMockUtils.addToResponse(ressursSinNedersteEnhet, ressursSinOvreEnhet, ledersEnhet, enhetPaHoyereNiva);


        OrgEnhetState ressursSinNedersteEnhetState = toOrgEnhetStateWithId(1, ressursSinNedersteEnhet);
        OrgEnhetState ressursSinOvreEnhetState = toOrgEnhetStateWithId(2, ressursSinOvreEnhet);
        OrgEnhetState ledersEnhetState = toOrgEnhetStateWithId(3, ledersEnhet);

        // Streams-app er ikke aktiv, så må legge manuelt inn i repository
        orgEnhetStateRepository.update(ressursSinNedersteEnhetState);
        orgEnhetStateRepository.update(ressursSinOvreEnhetState);
        orgEnhetStateRepository.update(ledersEnhetState);

        var dvhOrganisering1 = mockOrganisering(ressursSinNedersteEnhet, ressursSinOvreEnhet);
        var dvhOrganisering2 = mockOrganisering(ressursSinOvreEnhet, ledersEnhet);
        var dvhOrganisering3 = mockOrganisering(ledersEnhet, enhetPaHoyereNiva);

        var dvhLeder1 = mockLeder(ressursSinNedersteEnhet);
        var dvhLeder2 = mockLeder(ressursSinOvreEnhet);
        var dvhLeder3 = mockLeder(ledersEnhet);

        when(dvhOldTdvService.getTdvOrganiseringer()).thenReturn(List.of(dvhOrganisering1, dvhOrganisering2, dvhOrganisering3));
        when(dvhOldTdvService.getTdvLedere()).thenReturn(List.of(dvhLeder1, dvhLeder2, dvhLeder3));

        // Knytte ressurs til org. enhet på laveste nivå
        orgtilknytningStateRepository.update(
            "FID_1",
            orgtilknytningState().toBuilder()
                .fid("FID_1")
                .navident(ressurs.navident())
                .orgEnhetId("ID_1")
                .erDagligOppfolging(true)
                .build()
        );

        dvhRestConsumer.loadLedereAndOrganiseringerFromDvh();


        // Samme som over - må lagre organisering state manuelt
        organiseringStateRepository.update(orgEnhetStateToOrganiseringState(1, ressursSinNedersteEnhetState, ressursSinOvreEnhetState));
        organiseringStateRepository.update(orgEnhetStateToOrganiseringState(2, ressursSinOvreEnhetState, ledersEnhetState));


        final var response = postForResource("ressurs/leder_over_rekursivt.graphql");

        final var ledere = response.path("ressurs.ledere").entity(JsonNode.class).get();
        assertThat(ledere.size()).isEqualTo(1);

        // Navident til leder på første nivå der ressurs ikke selv er leder skal returneres
        assertThat(ledere.get(0).get("ressurs").get("navident").textValue()).isEqualTo(leder.navident());
    }

    private OrgEnhetState addToOrgEnhetStateRepository(int idSuffix, DvhOrgEnhetMock dvhOrgEnhetMock) {
        var state = dvhOrgenhetMockUtils.dvhMockToState(
            dvhOrgEnhetMock,
            idSuffix,
            String.valueOf(dvhOrgEnhetMock.getORGENHET_FRA_PERIODE()),
            Optional.of(String.valueOf(dvhOrgEnhetMock.getORGENHET_TIL_PERIODE()))
        );
        orgEnhetStateRepository.update(state);
        return state;
    }

    private void addToOrganiseringStateRepository(int idSuffix, OrgEnhetState enhetUnder, OrgEnhetState enhetOver) {
        organiseringStateRepository.update(
            orgEnhetStateToOrganiseringState(
                idSuffix,
                enhetUnder,
                enhetOver
            )
        );

    }

    private OrgEnhetState toOrgEnhetStateWithId(int idSuffix, DvhOrgEnhetMock dvhOrgEnhetMock) {
        return dvhOrgenhetMockUtils.dvhMockToState(
            dvhOrgEnhetMock,
            idSuffix,
            String.valueOf(dvhOrgEnhetMock.getORGENHET_FRA_PERIODE()),
            Optional.of(String.valueOf(dvhOrgEnhetMock.getORGENHET_TIL_PERIODE()))
        );
    }

    private DvhOrganisering mockOrganisering(DvhOrgEnhetMock underEnhet, DvhOrgEnhetMock overEnhet) {
        return new DvhOrganisering(
            overEnhet.getORGENHET_KODE(),
            overEnhet.getORGNODE_NIV(),
            overEnhet.getORGENHET_NAVN(),
            underEnhet.getORGENHET_KODE(),
            underEnhet.getORGNODE_NIV(),
            underEnhet.getORGENHET_NAVN(),
            LocalDate.now().minusDays(1),
            null
        );
    }

    private DvhLeder mockLeder(DvhOrgEnhetMock enhet) {
        return new DvhLeder(
            enhet.getLEDER_RESSURSNR(),
            enhet.getORGENHET_KODE(),
            enhet.getORGNODE_NIV(),
            enhet.getORGENHET_NAVN()
        );
    }
}
