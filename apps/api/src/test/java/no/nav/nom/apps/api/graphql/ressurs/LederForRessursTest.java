package no.nav.nom.apps.api.graphql.ressurs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import no.nav.nom.apps.api.DvhOrgEnhetMock;
import no.nav.nom.apps.api.DvhOrgEnhetMockUtils;
import no.nav.nom.apps.api.dvh.DvhRestConsumer;
import no.nav.nom.apps.api.graphql.GraphQLExtendedIntegrationTestBase;
import no.nav.nom.apps.api.organisering.DvhOrganiseringMock;
import no.nav.nom.apps.api.organisering.DvhOrganiseringMockUtils;
import no.nav.nom.apps.api.organisering.OrganiseringConfig;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

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
    @Qualifier("oldOrgtilknytningStateRepository")
    OrgtilknytningStateRepository orgtilknytningStateRepository;

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
        addToOrgEnhetStateRepository(1, ressursSinNedersteEnhet);
        addToOrgEnhetStateRepository(2, ressursSinOvreEnhet);
        addToOrgEnhetStateRepository(3, ledersEnhet);


        dvhOrganiseringMockUtils.clearResponse(); // Ikke bruke responser i JSON-filer
        dvhOrganiseringMockUtils.addToResponse(
            getDvhOrganiseringMock(ressursSinNedersteEnhet, ressursSinOvreEnhet, ressurs.navident()),
            getDvhOrganiseringMock(ressursSinOvreEnhet, ledersEnhet, ressurs.navident()),
            getDvhOrganiseringMock(ledersEnhet, enhetPaHoyereNiva, leder.navident())
        );

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
        when(unleash.isEnabled(OrganiseringConfig.UNLEASH_USE_NEW_ORGANISASJON_SOURCE)).thenReturn(true);

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


        dvhOrganiseringMockUtils.clearResponse(); // Ikke bruke responser i JSON-filer
        dvhOrganiseringMockUtils.addToResponse(
            getDvhOrganiseringMock(ressursSinNedersteEnhet, ressursSinOvreEnhet, ressurs.navident()),
            getDvhOrganiseringMock(ressursSinOvreEnhet, ledersEnhet, ressurs.navident()),
            getDvhOrganiseringMock(ledersEnhet, enhetPaHoyereNiva, leder.navident())
        );

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
        organiseringStateRepository.putEventAggregate(orgEnhetStateToOrganiseringState(1, ressursSinNedersteEnhetState, ressursSinOvreEnhetState));
        organiseringStateRepository.putEventAggregate(orgEnhetStateToOrganiseringState(2, ressursSinOvreEnhetState, ledersEnhetState));


        final var response = postForResource("ressurs/leder_over_rekursivt.graphql");

        final var ledere = response.path("ressurs.ledere").entity(JsonNode.class).get();
        assertThat(ledere.size()).isEqualTo(1);

        // Navident til leder på første nivå der ressurs ikke selv er leder skal returneres
        assertThat(ledere.get(0).get("ressurs").get("navident").textValue()).isEqualTo(leder.navident());
    }

    private void addToOrgEnhetStateRepository(int idSuffix, DvhOrgEnhetMock dvhOrgEnhetMock) {
        orgEnhetStateRepository.update(dvhOrgenhetMockUtils.dvhMockToState(
            dvhOrgEnhetMock,
            idSuffix,
            String.valueOf(dvhOrgEnhetMock.getORGENHET_FRA_PERIODE()),
            Optional.of(String.valueOf(dvhOrgEnhetMock.getORGENHET_TIL_PERIODE()))
        ));
    }

    private OrgEnhetState toOrgEnhetStateWithId(int idSuffix, DvhOrgEnhetMock dvhOrgEnhetMock) {
        return dvhOrgenhetMockUtils.dvhMockToState(
            dvhOrgEnhetMock,
            idSuffix,
            String.valueOf(dvhOrgEnhetMock.getORGENHET_FRA_PERIODE()),
            Optional.of(String.valueOf(dvhOrgEnhetMock.getORGENHET_TIL_PERIODE()))
        );
    }

    private DvhOrganiseringMock getDvhOrganiseringMock(DvhOrgEnhetMock orgEnhet, DvhOrgEnhetMock orgEnhetOver, String leder) {
        return DvhOrganiseringMock.getBuilderWithDefault()
            .ORGNODE_KODE(orgEnhet.getORGENHET_KODE())
            .ORGNODE_NAVN(orgEnhet.getORGENHET_NAVN())
            .ORGNODE_NIVA(orgEnhet.getORGNODE_NIV())
            .LEDER_NAVIDENT(leder)
            .ORGNODE_KODE_OVER(orgEnhetOver.getORGENHET_KODE())
            .ORGNODE_NAVN_OVER(orgEnhetOver.getORGENHET_NAVN())
            .ORGNODE_NIVA_OVER(orgEnhetOver.getORGNODE_NIV())
            .build();
    }

}
