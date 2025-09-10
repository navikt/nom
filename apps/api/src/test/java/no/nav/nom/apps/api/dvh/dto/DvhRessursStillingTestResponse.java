package no.nav.nom.apps.api.dvh.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Tester basic parsing av Json objekter fra DVH")
class DvhRessursStillingTestResponse {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @SneakyThrows
    @DisplayName("Standard ok DVH objekt")
    public void testStdDvhObjekt() {
        String json = """
        {
            "nav_id": "A112343",
            "resource_id": "112343",
            "first_name": "Ola",
            "surname": "Nordmann",
            "fra_dato": "2019-12-31T23:00:00Z",
            "til_dato": "2020-09-29T23:00:00Z",
            "post_date_to": "2020-11-17T23:00:00Z",
            "ansettelsestype_besk": "Fast",
            "parttime_pct": 70.00000000,
            "orgenhet": "0499000",
            "orgenhet_besk": "Innlandet Arbeidslivssenter",
            "leder_paa_stilling": "123456",
            "perm_kode": "ARB(EGEN)",
            "last_update": "2020-11-23T08:32:12.000Z",
            "orgenhet_paa_ressurs": "0499000",
            "orgenhet_paa_ressurs_niva": "ORGENHET",
            "remedy_enhet_id": "0491",
            "stilling_orgenhet_paa_ressurs": "0499000",
            "agresso_date_from": "2006-04-17T23:00:00Z",
            "agresso_date_to": "2099-12-30T23:00:00Z",
            "date_from": "2006-04-03T23:00:00Z",
            "date_to": "2099-12-30T23:00:00Z",
            "leder_resource_id": "123456",
            "sykemleder_resource_id": "123456"
        }
        """;
        JsonNode node = mapper.readTree(json);

        DvhRessursStilling actual = DvhRessursStilling.fromOrdsRessursStilling(node);
        DvhRessursStilling expected =
            DvhRessursStilling.builder()
                .payload(node.toPrettyString())
                .navident("A112343")
                .stillingDatoFom(LocalDate.of(2020,1,1))
                .stillingDatoTom(LocalDate.of(2020, 9,30))
                .stillingAgressoId("0499000")
                .stillingOrgenhetNavn("Innlandet Arbeidslivssenter")
                .ressursAgressoId("0499000")
                .ressursOrgenhetNiv("ORGENHET")
                .ressursRemedyEnhetId("0491")
                .stillingRessursAgressoId("0499000")
                .stillingAnsettelsesTypeBesk("Fast")
                .ressursAgressoDatoFom(LocalDate.of(2006,4,18))
                .ressursAgressoDatoTom(LocalDate.of(2099,12,31))
                .ressursDvhDatoFom(LocalDate.of(2006,4,4))
                .ressursDvhDatoTom(LocalDate.of(2099,12,31))
                .build();
        assertEquals(expected, actual);
    }

    @Test
    @SneakyThrows
    @DisplayName("Standard ok DVH objekt - Med litt tomme verider")
    public void testStdMedTommeVeriderDvhObjekt() {
        String json = """
        {
            "nav_id":"A112343",
            "resource_id": "112343",
            "first_name": "Ola",
            "surname": "Nordmann",
            "fra_dato": "2019-12-31T23:00:00Z",
            "til_dato": "2020-09-29T23:00:00Z",
            "post_date_to": "2020-11-17T23:00:00Z",
            "ansettelsestype_besk": "Fast",
            "parttime_pct": 70.00000000,
            "orgenhet": "0499000",
            "orgenhet_besk": "Innlandet Arbeidslivssenter",
            "leder_paa_stilling": "123456",
            "perm_kode": "ARB(EGEN)",
            "last_update": "2020-11-23T08:32:12.000Z",
            "orgenhet_paa_ressurs": "-4",
            "orgenhet_paa_ressurs_niva": "Mangler",
            "remedy_enhet_id": "-4",
            "stilling_orgenhet_paa_ressurs": "0499000",
            "agresso_date_from": "2006-04-17T23:00:00Z",
            "agresso_date_to": "2099-12-30T23:00:00Z",
            "date_from": "2006-04-03T23:00:00Z",
            "date_to": "2099-12-30T23:00:00Z",
            "leder_resource_id": "123456",
            "sykemleder_resource_id": "123456"
        }
        """;
        JsonNode node = mapper.readTree(json);

        DvhRessursStilling actual = DvhRessursStilling.fromOrdsRessursStilling(node);
        DvhRessursStilling expected =
            DvhRessursStilling.builder()
                .payload(node.toPrettyString())
                .navident("A112343")
                .stillingDatoFom(LocalDate.of(2020,1,1))
                .stillingDatoTom(LocalDate.of(2020, 9,30))
                .stillingAgressoId("0499000")
                .stillingOrgenhetNavn("Innlandet Arbeidslivssenter")
                .ressursAgressoId(null)
                .ressursOrgenhetNiv("Mangler")
                .ressursRemedyEnhetId(null)
                .stillingRessursAgressoId("0499000")
                .stillingAnsettelsesTypeBesk("Fast")
                .ressursAgressoDatoFom(LocalDate.of(2006,4,18))
                .ressursAgressoDatoTom(LocalDate.of(2099,12,31))
                .ressursDvhDatoFom(LocalDate.of(2006,4,4))
                .ressursDvhDatoTom(LocalDate.of(2099,12,31))
                .build();
        assertEquals(expected, actual);
    }

    @Test
    @SneakyThrows
    @DisplayName("Standard ok DVH objekt - Med null NavId")
    public void testStdMedNullNavIdDvhObjekt() {
        String json = """
        {
            "nav_id": null,
            "resource_id": "112343",
            "first_name": "Ola",
            "surname": "Nordmann",
            "fra_dato": "2020-12-311T23:00:00Z",
            "til_dato": "2020-09-29T23:00:00Z",
            "post_date_to": "2020-11-17T23:00:00Z",
            "ansettelsestype_besk": "Fast",
            "parttime_pct": 70.00000000,
            "orgenhet": "0499000",
            "orgenhet_besk": "Innlandet Arbeidslivssenter",
            "leder_paa_stilling": "123456",
            "perm_kode": "ARB(EGEN)",
            "last_update": "2020-11-23T08:32:12.000Z",
            "orgenhet_paa_ressurs": "-4",
            "orgenhet_paa_ressurs_niva": "Mangler",
            "remedy_enhet_id": "-4",
            "stilling_orgenhet_paa_ressurs": "0499000",
            "agresso_date_from": "2006-04-18",
            "agresso_date_to": "2099-12-31",
            "date_from": "2006-04-04",
            "date_to": "2099-12-31",
            "leder_resource_id": "123456",
            "sykemleder_resource_id": "123456"
        }
        """;
        JsonNode node = mapper.readTree(json);

        DvhRessursStilling actual = DvhRessursStilling.fromOrdsRessursStilling(node);
        DvhRessursStilling expected =
            DvhRessursStilling.builder()
                .payload(node.toPrettyString())
                .navident(null)
                // Null på resten
                .build();
        assertEquals(expected, actual);
    }

}