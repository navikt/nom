package no.nav.nom.apps.api.dvh;

import lombok.val;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.assertj.core.api.Assertions.assertThat;

public class TlfFraDvhTest extends DvhTestBase {

    @Test
    @DisplayName("hent telefonnr")
    public void test1() {
        stubTlfFromString(tlfDvhPayload);
        stubResourcesFromString(resourceInitDvhPayload);

        loadFromDvh();

        var tlfList1 = this.dvhRepository.getTelefonNrListByNavIdent("A123123");
        assertThat(tlfList1.get(0).getNummer()).isEqualTo("4711220033");

        var tlfList2 = this.dvhRepository.getTelefonNrListByNavIdent("A503999");
        Assertions.assertTrue(tlfList2.isEmpty());

        var tlfList3 = this.dvhRepository.getTelefonNrListByNavIdent("J509999");
        Assertions.assertTrue(tlfList2.isEmpty());

        var tlfList4 = this.dvhRepository.getTelefonNrListByNavIdent("J501111");
        Assertions.assertTrue(tlfList2.isEmpty());
    }

    @Test
    @DisplayName("hent telefonnr, sjekk flere")
    public void test21() {
        stubTlfFromString(tlfDvhPayload);
        stubResourcesFromString(resourceInitDvhPayload);

        loadFromDvh();


        val resourceList = this.ressursService.getAll();
        for (val r : resourceList) {
            assertThat(this.dvhRepository.getTelefonNrListByNavIdent(r.navident())).hasSize(1);
        }

    }


    private void stubTlfFromString(String payload) {
        stubFor(
                get("/proxy/dvh/json/Hr/NOM2/NOM_TLF_v1")
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json;charset=UTF-8")
                                        .withBody(payload)
                        )
        );
    }

    private void stubResourcesFromString(String payload) {
        stubFor(
                get("/proxy/dvh/json/Hr/NOM3/NOM_ANSATTE_BUCKETS_v2").
                        willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json;charset=UTF-8")
                                        .withBody(payload)
                        )
        );
    }


    private static final String resourceInitDvhPayload = // language=json
            """
            {
                  "NOM_ANSATTE_BUCKETS_v2Response": [
                    {
                      "NAV_IDENT": "A123123",
                      "FNR": "12345678901",
                      "FORNAVN": "Navn 1",
                      "ETTERNAVN": "ENavn 1",
                      "EPOST": "epost1@nav.no",
                      "REMEDY_START_DATO": "2008-05-23",
                      "REMEDY_SLUTT_DATO": "2099-12-31",
                      "AGRESSO_SLUTT_DATO": "2099-12-31",
                      "ANSATT_PER_IDAG": "JA",
                      "ANSETTELSESTYPE_BESK": "Fast",
                      "ANSATT_TYPE": "NAV Statlig",
                      "ANSATT_RESSURS_KODE": "F",
                      "REMEDY_PERMISJON": "NEI",
                      "IDENT_TYPE": "STANDARD"
                    },
                    {
                      "NAV_IDENT": "A321321",
                      "FNR": "12345678902",
                      "FORNAVN": "Navn 2",
                      "ETTERNAVN": "ENavn 2",
                      "EPOST": "epost2@nav.no",
                      "REMEDY_START_DATO": "2008-05-26",
                      "REMEDY_SLUTT_DATO": "2099-12-31",
                      "AGRESSO_SLUTT_DATO": "2099-12-31",
                      "ANSATT_PER_IDAG": "JA",
                      "ANSETTELSESTYPE_BESK": "Fast",
                      "ANSATT_TYPE": "NAV Statlig",
                      "ANSATT_RESSURS_KODE": "F",
                      "REMEDY_PERMISJON": "NEI",
                      "IDENT_TYPE": "STANDARD"
                    },
                    {
                      "NAV_IDENT": "A321320",
                      "FNR": "12345678903",
                      "FORNAVN": "Navn 3",
                      "ETTERNAVN": "ENavn 3",
                      "EPOST": "epost3@nav.no",
                      "REMEDY_START_DATO": "2008-05-28",
                      "REMEDY_SLUTT_DATO": "2019-01-29",
                      "AGRESSO_SLUTT_DATO": "2010-11-13",
                      "ANSATT_PER_IDAG": "NEI",
                      "ANSETTELSESTYPE_BESK": null,
                      "ANSATT_TYPE": "NAV Statlig",
                      "ANSATT_RESSURS_KODE": null,
                      "REMEDY_PERMISJON": "NEI",
                      "IDENT_TYPE": "STANDARD"
                    },
                    {
                      "NAV_IDENT": "A440022",
                      "FNR": "92345678904",
                      "FORNAVN": "Navn 4",
                      "ETTERNAVN": "ENavn 4",
                      "EPOST": "epost4@nav.no",
                      "REMEDY_START_DATO": "2008-05-30",
                      "REMEDY_SLUTT_DATO": "2099-12-31",
                      "AGRESSO_SLUTT_DATO": "2099-12-31",
                      "ANSATT_PER_IDAG": "JA",
                      "ANSETTELSESTYPE_BESK": "Kommunalt ansatt",
                      "ANSATT_TYPE": "NAV Kommunalt",
                      "ANSATT_RESSURS_KODE": "N",
                      "REMEDY_PERMISJON": "JA",
                      "IDENT_TYPE": "STANDARD"
                    },
                    {
                      "NAV_IDENT": "A503020",
                      "FNR": "12345678905",
                      "FORNAVN": "Navn 5",
                      "ETTERNAVN": "ENavn 5",
                      "EPOST": "epost5@nav.no",
                      "REMEDY_START_DATO": "2008-05-30",
                      "REMEDY_SLUTT_DATO": "2099-12-31",
                      "AGRESSO_SLUTT_DATO": "2099-12-31",
                      "ANSATT_PER_IDAG": "JA",
                      "ANSETTELSESTYPE_BESK": "Kommunalt ansatt",
                      "ANSATT_TYPE": "NAV Kommunalt",
                      "ANSATT_RESSURS_KODE": "N",
                      "REMEDY_PERMISJON": "JA",
                      "IDENT_TYPE": "STANDARD"
                    }
                  ]
                }
                """;

    private static final String tlfDvhPayload = // language=json
            """
            {
              "NOM_TLF_v1Result": [
                {
                  "msisdn": "4711220033",
                  "navident": "A123123",
                  "userreference1": "PK-A123123",
                  "typeid": "53200",
                  "typename": "Bedrift Total B (50 GB)",
                  "typeaccesstype": "MOBILE",
                  "status": "ACTIVE",
                  "issecretnumber": "false",
                  "validfromdate": "2021-05-28T00:00:00.000Z",
                  "lastetdato": "2022-01-18T15:58:23.000Z",
                  "res_mobil_tlf_kontroll": "112 20 033"
                },
                {
                  "msisdn": "4711330022",
                  "navident": "A321321",
                  "userreference1": "PK-A321321",
                  "typeid": "53200",
                  "typename": "Bedrift Total B (50 GB)",
                  "typeaccesstype": "MOBILE",
                  "status": "ACTIVE",
                  "issecretnumber": "false",
                  "validfromdate": "2020-10-20T00:00:00.000Z",
                  "lastetdato": "2020-10-20T00:00:00.000Z",
                  "res_mobil_tlf_kontroll": "11330022"
                },
                {
                  "msisdn": "4721002102",
                  "navident": "A321320",
                  "userreference1": "PT-A321320",
                  "typeid": "53175",
                  "typename": "Bedrift Total A (50 GB)",
                  "typeaccesstype": "MOBILE",
                  "status": "ACTIVE",
                  "issecretnumber": "false",
                  "validfromdate": "2020-10-20T00:00:00.000Z",
                  "lastetdato": "2020-10-20T00:00:00.000Z",
                  "res_mobil_tlf_kontroll": "21 00 21 02"
                },
                {
                  "msisdn": "4722330011",
                  "navident": "A440022",
                  "userreference1": "PK-A440022",
                  "typeid": "53200",
                  "typename": "Bedrift Total B (50 GB)",
                  "typeaccesstype": "MOBILE",
                  "status": "ACTIVE",
                  "issecretnumber": "false",
                  "validfromdate": "2020-10-21T00:00:00.000Z",
                  "lastetdato": "2020-10-21T00:00:00.000Z",
                  "res_mobil_tlf_kontroll": "223 30 011"
                },
                {
                  "msisdn": "4733002200",
                  "navident": "A503020",
                  "userreference1": "PT-A503020",
                  "typeid": "53175",
                  "typename": "Bedrift Total A (50 GB)",
                  "typeaccesstype": "MOBILE",
                  "status": "ACTIVE",
                  "issecretnumber": "false",
                  "validfromdate": "2020-10-26T00:00:00.000Z",
                  "lastetdato": "2020-10-26T00:00:00.000Z",
                  "res_mobil_tlf_kontroll": "3300 2200"
                },
                {
                  "msisdn": "4733002999",
                  "navident": "A503999",
                  "userreference1": "PT-A503020",
                  "typeid": "53175",
                  "typename": "Bedrift Total A (50 GB)",
                  "typeaccesstype": "MOBILE",
                  "status": "ACTIVE",
                  "issecretnumber": "true",
                  "validfromdate": "2020-10-26T00:00:00.000Z",
                  "lastetdato": "2020-10-26T00:00:00.000Z",
                  "res_mobil_tlf_kontroll": "3300 2200"
                },
                {
                  "msisdn": "4733008888",
                  "navident": "J509999",
                  "userreference1": "PT-A503020",
                  "typeid": "53175",
                  "typename": "Bedrift Total A (50 GB)",
                  "typeaccesstype": "MOBILE",
                  "status": "ACTIVE",
                  "issecretnumber": null,
                  "validfromdate": "2020-10-26T00:00:00.000Z",
                  "lastetdato": "2020-10-26T00:00:00.000Z",
                  "res_mobil_tlf_kontroll": "3300 2200"
                },
                {
                  "msisdn": "4733001111",
                  "navident": "J501111",
                  "userreference1": "PT-A503020",
                  "typeid": "53175",
                  "typename": "Bedrift Total A (50 GB)",
                  "typeaccesstype": "MOBILE",
                  "status": "ACTIVE",
                  "validfromdate": "2020-10-26T00:00:00.000Z",
                  "lastetdato": "2020-10-26T00:00:00.000Z",
                  "res_mobil_tlf_kontroll": "3300 2200"
                }
              ]
            }
            """;
}
