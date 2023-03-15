package no.nav.nom.apps.api.dvh.orgtilknytning;

import no.nav.nom.apps.api.DvhOrgEnhetMock;
import no.nav.nom.apps.api.DvhRessursUtils;
import no.nav.nom.apps.api.dvh.DvhTestBase;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;
import no.nav.nom.contract.nom.state.RessursState;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.junit.Assert.assertTrue;


public class OrgtilknytningTest extends DvhTestBase {

    @Test
    @DisplayName("Leser orgtilknytning fra DVH")
    public void test1() {
        final var mockedEnhet = DvhOrgEnhetMock.getBuilderWithDefault().ORGENHET_KODE("testId1").ORGENHET_NAVN("testNavn").ORGNODE_NIV("ORGENHET")
            .ENHETTYPE_NAVN("TYPEEE").build();

        final var ressursState = createRessursHendelseEventSource("");
        orgEnhetStateRepository.putEventAggregate(dvhOrgenhetMockUtils.dvhMockToState(mockedEnhet, 1, String.valueOf(mockedEnhet.getORGENHET_FRA_PERIODE()), Optional.of(String.valueOf(mockedEnhet.getORGENHET_TIL_PERIODE()))));

        dvhOrgtilknytningMockUtils.addToResponse(
            new DvhOrgtilknytningMock(
                ressursState.navident(),
                mockedEnhet.getORGENHET_KODE(),
                LocalDate.parse("2020-01-01"),
                LocalDate.parse("2099-12-31")
            )
        );
        loadFromDvh();

        Assertions.assertEquals(1, orgtilknytningService.getKoblingerByNavIdent(ressursState.navident()).size());
    }

    @Test
    @DisplayName("Overlappende orgtilknytninger blir slått sammen")
    public void test2() {
        final var mockedEnhet = DvhOrgEnhetMock.getBuilderWithDefault().ORGENHET_KODE("testId1").ORGENHET_NAVN("testNavn").ORGNODE_NIV("ORGENHET")
            .ENHETTYPE_NAVN("TYPEEE").build();

        final var ressursState = createRessursHendelseEventSource("");
        orgEnhetStateRepository.putEventAggregate(dvhOrgenhetMockUtils.dvhMockToState(mockedEnhet, 1, String.valueOf(mockedEnhet.getORGENHET_FRA_PERIODE()), Optional.of(String.valueOf(mockedEnhet.getORGENHET_TIL_PERIODE()))));


        dvhOrgtilknytningMockUtils.addToResponse(
            new DvhOrgtilknytningMock(
                ressursState.navident(),
                mockedEnhet.getORGENHET_KODE(),
                LocalDate.parse("2020-01-01"),
                LocalDate.parse("2099-12-31")
            ),
            new DvhOrgtilknytningMock(
                ressursState.navident(),
                mockedEnhet.getORGENHET_KODE(),
                LocalDate.parse("2019-01-01"),
                LocalDate.parse("2099-12-31")
            )
        );
        loadFromDvh();

        Assertions.assertEquals(1, orgtilknytningService.getKoblingerByNavIdent(ressursState.navident()).size());
    }

    @Test
    @DisplayName("Ikke Overlappende orgtilknytninger blir ikke slått sammen")
    public void test3() {
        dvhRepository.clearRepository();

        final var mockedEnhet = DvhOrgEnhetMock.getBuilderWithDefault().ORGENHET_KODE("testId1").ORGENHET_NAVN("testNavn").ORGNODE_NIV("ORGENHET")
            .ENHETTYPE_NAVN("TYPEEE").build();

        final var ressursState = createRessursHendelseEventSource("");
        orgEnhetStateRepository.putEventAggregate(dvhOrgenhetMockUtils.dvhMockToState(mockedEnhet, 1, String.valueOf(mockedEnhet.getORGENHET_FRA_PERIODE()), Optional.of(String.valueOf(mockedEnhet.getORGENHET_TIL_PERIODE()))));

        dvhOrgtilknytningMockUtils.addToResponse(
            new DvhOrgtilknytningMock(
                ressursState.navident(),
                mockedEnhet.getORGENHET_KODE(),
                LocalDate.parse("2020-01-01"),
                LocalDate.parse("2099-12-31")
            ),
            new DvhOrgtilknytningMock(
                ressursState.navident(),
                mockedEnhet.getORGENHET_KODE(),
                LocalDate.parse("2019-01-01"),
                LocalDate.parse("2019-12-31")
            )
        );
        loadFromDvh();

        Assertions.assertEquals(2, orgtilknytningService.getKoblingerByNavIdent(ressursState.navident()).size());
    }

    @Test
    @DisplayName("Valider at tilknytning havner på korrekt orgEnhet")
    public void validerTilknytningTilRiktigOrgEnhet() {
        final var mockedEnhet = DvhOrgEnhetMock.getBuilderWithDefault().ORGENHET_KODE("8435000").ORGENHET_NAVN("Seksjon for program og utvikling").ORGNODE_NIV("ORGENHET")
            .ENHETTYPE_NAVN("TYPEEE").ORGENHET_FRA_PERIODE(201702).ORGENHET_TIL_PERIODE(201912).build();
        final var mockedEnhet2 = DvhOrgEnhetMock.getBuilderWithDefault().ORGENHET_KODE("8435000").ORGENHET_NAVN("Utviklingsseksjonen").ORGNODE_NIV("ORGENHET")
            .ENHETTYPE_NAVN("TYPEEE").ORGENHET_FRA_PERIODE(202001).ORGENHET_TIL_PERIODE(209912).build();
        final var mockedEnhet3 = DvhOrgEnhetMock.getBuilderWithDefault().ORGENHET_KODE("2507003").ORGENHET_NAVN("Kontaktsenter Vestfold og Telemark 3").ORGNODE_NIV("ORGENHET")
            .ENHETTYPE_NAVN("TYPEEE").ORGENHET_FRA_PERIODE(201301).ORGENHET_TIL_PERIODE(201912).build();

        orgEnhetStateRepository.putEventAggregate(dvhOrgenhetMockUtils.dvhMockToState(mockedEnhet, 1, String.valueOf(mockedEnhet.getORGENHET_FRA_PERIODE()), Optional.of(String.valueOf(mockedEnhet.getORGENHET_TIL_PERIODE()))));
        orgEnhetStateRepository.putEventAggregate(dvhOrgenhetMockUtils.dvhMockToState(mockedEnhet2, 2, String.valueOf(mockedEnhet2.getORGENHET_FRA_PERIODE()), Optional.of(String.valueOf(mockedEnhet2.getORGENHET_TIL_PERIODE()))));
        orgEnhetStateRepository.putEventAggregate(dvhOrgenhetMockUtils.dvhMockToState(mockedEnhet3, 3, String.valueOf(mockedEnhet3.getORGENHET_FRA_PERIODE()), Optional.of(String.valueOf(mockedEnhet3.getORGENHET_TIL_PERIODE()))));

        final var mockedRessurs = new RessursState("FID_1", "12108000000", "L133198", Sektor.NAV_STATLIG, new Visningsnavn("Jill", Optional.empty(), "Doe"), Optional.empty());

        ressursStateRepository.putEventAggregate(mockedRessurs);

        stubStillingFromString(stillingInitDvhPayload);

        loadFromDvh();

        var ressurs = dvhRepository.getKoblinger().stream().filter( k -> k.getNavIdent().equals("L133198") && k.getAgressoId().equals("8435000")).findFirst();
        assertTrue(ressurs.get().getOrgenhetNavn().equals("Utviklingsseksjonen"));
    }

    private void stubStillingFromString(String payload) {
        stubFor(
            get("/proxy/dvh/json/Hr/NOM2/stilling2nom")
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json;charset=UTF-8")
                        .withBody(payload)
                )
        );
    }

    private static final String stillingInitDvhPayload = // language=json
        """
            {
                "stilling2nomResult": [
                    {
                        "RESOURCE_ID": "133198",
                        "NAV_ID": "L133198",
                        "FRA_DATO": "2023-02-03",
                        "TIL_DATO": "2023-12-31",
                        "POST_DATE_TO": "2023-12-31",
                        "ANSETTELSESTYPE_BESK": "Fast",
                        "PARTTIME_PCT": 0,
                        "RADVEKT": 0.0,
                        "ORGENHET": "2507003",
                        "ORGENHET_BESK": "Kontaktsenter Vestfold og Telemark 3",
                        "ORGENHET_I_BUNN": "2507003",
                        "LEDER_I_BUNN": "144675",
                        "LEDER_PAA_STILLING": "144675",
                        "STILLING_I_BUNN": 1.0,
                        "PERMISJON": "PERMISJON",
                        "LAST_UPDATE": "2023-02-02T12:16:12.000Z"
                    },
                    {
                        "RESOURCE_ID": "133198",
                        "NAV_ID": "L133198",
                        "FRA_DATO": "2023-02-03",
                        "TIL_DATO": "2023-12-31",
                        "POST_DATE_TO": "2023-12-31",
                        "ANSETTELSESTYPE_BESK": "Innlån/utlån",
                        "PARTTIME_PCT": 100,
                        "RADVEKT": 1.0,
                        "ORGENHET": "8435000",
                        "ORGENHET_BESK": "Utviklingsseksjonen",
                        "ORGENHET_I_BUNN": "2507003",
                        "LEDER_I_BUNN": "144675",
                        "LEDER_PAA_STILLING": "111278",
                        "STILLING_I_BUNN": 0.0,
                        "PERMISJON": "JOBBER",
                        "LAST_UPDATE": "2023-02-02T12:16:12.000Z"
                    } 
                ]
            }
        """;

}
