package no.nav.nom.apps.api.dvh;

import lombok.val;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.assertj.core.api.Assertions.assertThat;

public class TlfFraDvhTest extends DvhTestBase {

    @Test
    @DisplayName("hent telefonnr")
    public void test1() {
        stubTlfFromString(tlfDvhPayload);

        dvhRestConsumer.loadTlfFromDvh();

        var tlfList1 = this.dvhRepository.getTelefonNrListByNavIdent("A123123");
        assertThat(tlfList1.get(0).getNummer()).isEqualTo("4711220033");

        var tlfList2 = this.dvhRepository.getTelefonNrListByNavIdent("A503999");
        Assertions.assertTrue(tlfList2.isEmpty());

        var tlfList3 = this.dvhRepository.getTelefonNrListByNavIdent("J509999");
        assertThat(tlfList3.get(0).getNummer()).isEqualTo("4733008888");

        var tlfList4 = this.dvhRepository.getTelefonNrListByNavIdent("J501111");
        assertThat(tlfList4.get(0).getNummer()).isEqualTo("4733001111");
    }

    @Test
    @DisplayName("hent telefonnr, sjekk flere")
    public void test21() {
        stubTlfFromString(tlfDvhPayload);

        dvhRestConsumer.loadTlfFromDvh();

        val navidenter = List.of("A123123", "A321321", "A321320", "A440022", "A503020");
        for (val navident : navidenter) {
            assertThat(this.dvhRepository.getTelefonNrListByNavIdent(navident)).hasSize(1);
        }

    }


    private void stubTlfFromString(String payload) {
        stubFor(
            get("/dvh_nom/nom_ords_tlf.json")
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", "application/json;charset=UTF-8")
                                        .withBody(payload)
                        )
        );
    }

    private static final String tlfDvhPayload = // language=json
            """
            {
              "items": [
                {
                  "msisdn": "4711220033",
                  "navident": "A123123",
                  "userrefeerence1": "PK-A123123",
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
                  "userrefeerence1": "PK-A321321",
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
                  "userrefeerence1": "PT-A321320",
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
                  "userrefeerence1": "PK-A440022",
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
                  "userrefeerence1": "PT-A503020",
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
                  "userrefeerence1": "PT-A503020",
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
                  "userrefeerence1": "PT-A503020",
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
                  "userrefeerence1": "PT-A503020",
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
