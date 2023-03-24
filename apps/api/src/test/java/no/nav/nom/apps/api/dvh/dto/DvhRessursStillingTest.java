package no.nav.nom.apps.api.dvh.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tester basic parsing av Json objekter fra DVH")
class DvhRessursStillingTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @SneakyThrows
    @DisplayName("Standard ok DVH objekt")
    public void testStdDvhObjekt() {
        String json = """
        {
            "NAV_ID": "A112343",
            "RESOURCE_ID": "112343",
            "FIRST_NAME": "Ola",
            "SURNAME": "Nordmann",
            "FRA_DATO": "2020-01-01",
            "TIL_DATO": "2020-09-30",
            "POST_DATE_TO": "2020-11-18",
            "ANSETTELSESTYPE_BESK": "Fast",
            "PARTTIME_PCT": 70.00000000,
            "ORGENHET": "0499000",
            "ORGENHET_BESK": "Innlandet Arbeidslivssenter",
            "LEDER_PAA_STILLING": "123456",
            "PERM_KODE": "ARB(EGEN)",
            "LAST_UPDATE": "2020-11-23T08:32:12.000Z",
            "ORGENHET_PAA_RESSURS": "0499000",
            "ORGENHET_PAA_RESSURS_NIVA": "ORGENHET",
            "REMEDY_ENHET_ID": "0491",
            "STILLING_ORGENHET_PAA_RESSURS": "0499000",
            "AGRESSO_DATE_FROM": "2006-04-18",
            "AGRESSO_DATE_TO": "2099-12-31",
            "DATE_FROM": "2006-04-04",
            "DATE_TO": "2099-12-31",
            "LEDER_RESOURCE_ID": "123456",
            "SYKEMLEDER_RESOURCE_ID": "123456"
        }
        """;
        JsonNode node = mapper.readTree(json);

        DvhRessursStilling actual = new DvhRessursStilling(node);
        DvhRessursStilling expected =
            DvhRessursStilling.builder()
                .payload(node.toString())
                .navident(Optional.of("A112343"))
                .stillingDatoFom(LocalDate.of(2020,1,1))
                .stillingDatoTom(LocalDate.of(2020, 9,30))
                .stillingAgressoId(Optional.of("0499000"))
                .stillingOrgenhetNavn("Innlandet Arbeidslivssenter")
                .ressursAgressoId(Optional.of("0499000"))
                .ressursOrgenhetNiv(Optional.of("ORGENHET"))
                .ressursRemedyEnehetId(Optional.of("0491"))
                .stillingRessursAgressoId(Optional.of("0499000"))
                .ressursAgressoDatoFom(LocalDate.of(2006,04,18))
                .ressursAgressoDatoTom(LocalDate.of(2099,12,31))
                .ressursDvhDatoFom(LocalDate.of(2006,04,04))
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
            "NAV_ID":"A112343",
            "RESOURCE_ID": "112343",
            "FIRST_NAME": "Ola",
            "SURNAME": "Nordmann",
            "FRA_DATO": "2020-01-01",
            "TIL_DATO": "2020-09-30",
            "POST_DATE_TO": "2020-11-18",
            "ANSETTELSESTYPE_BESK": "Fast",
            "PARTTIME_PCT": 70.00000000,
            "ORGENHET": "0499000",
            "ORGENHET_BESK": "Innlandet Arbeidslivssenter",
            "LEDER_PAA_STILLING": "123456",
            "PERM_KODE": "ARB(EGEN)",
            "LAST_UPDATE": "2020-11-23T08:32:12.000Z",
            "ORGENHET_PAA_RESSURS": "-4",
            "ORGENHET_PAA_RESSURS_NIVA": "Mangler",
            "REMEDY_ENHET_ID": "-4",
            "STILLING_ORGENHET_PAA_RESSURS": "0499000",
            "AGRESSO_DATE_FROM": "2006-04-18",
            "AGRESSO_DATE_TO": "2099-12-31",
            "DATE_FROM": "2006-04-04",
            "DATE_TO": "2099-12-31",
            "LEDER_RESOURCE_ID": "123456",
            "SYKEMLEDER_RESOURCE_ID": "123456"
        }
        """;
        JsonNode node = mapper.readTree(json);

        DvhRessursStilling actual = new DvhRessursStilling(node);
        DvhRessursStilling expected =
            DvhRessursStilling.builder()
                .payload(node.toString())
                .navident(Optional.of("A112343"))
                .stillingDatoFom(LocalDate.of(2020,1,1))
                .stillingDatoTom(LocalDate.of(2020, 9,30))
                .stillingAgressoId(Optional.of("0499000"))
                .stillingOrgenhetNavn("Innlandet Arbeidslivssenter")
                .ressursAgressoId(Optional.empty())
                .ressursOrgenhetNiv(Optional.of("Mangler"))
                .ressursRemedyEnehetId(Optional.empty())
                .stillingRessursAgressoId(Optional.of("0499000"))
                .ressursAgressoDatoFom(LocalDate.of(2006,04,18))
                .ressursAgressoDatoTom(LocalDate.of(2099,12,31))
                .ressursDvhDatoFom(LocalDate.of(2006,04,04))
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
            "NAV_ID": null,
            "RESOURCE_ID": "112343",
            "FIRST_NAME": "Ola",
            "SURNAME": "Nordmann",
            "FRA_DATO": "2020-01-01",
            "TIL_DATO": "2020-09-30",
            "POST_DATE_TO": "2020-11-18",
            "ANSETTELSESTYPE_BESK": "Fast",
            "PARTTIME_PCT": 70.00000000,
            "ORGENHET": "0499000",
            "ORGENHET_BESK": "Innlandet Arbeidslivssenter",
            "LEDER_PAA_STILLING": "123456",
            "PERM_KODE": "ARB(EGEN)",
            "LAST_UPDATE": "2020-11-23T08:32:12.000Z",
            "ORGENHET_PAA_RESSURS": "-4",
            "ORGENHET_PAA_RESSURS_NIVA": "Mangler",
            "REMEDY_ENHET_ID": "-4",
            "STILLING_ORGENHET_PAA_RESSURS": "0499000",
            "AGRESSO_DATE_FROM": "2006-04-18",
            "AGRESSO_DATE_TO": "2099-12-31",
            "DATE_FROM": "2006-04-04",
            "DATE_TO": "2099-12-31",
            "LEDER_RESOURCE_ID": "123456",
            "SYKEMLEDER_RESOURCE_ID": "123456"
        }
        """;
        JsonNode node = mapper.readTree(json);

        DvhRessursStilling actual = new DvhRessursStilling(node);
        DvhRessursStilling expected =
            DvhRessursStilling.builder()
                .payload(node.toString())
                .navident(Optional.empty())
                .stillingDatoFom(null)
                .stillingDatoTom(null)
                .stillingAgressoId(null)
                .stillingOrgenhetNavn(null)
                .ressursAgressoId(null)
                .ressursOrgenhetNiv(null)
                .ressursRemedyEnehetId(null)
                .stillingRessursAgressoId(null)
                .ressursAgressoDatoFom(null)
                .ressursAgressoDatoTom(null)
                .ressursDvhDatoFom(null)
                .ressursDvhDatoTom(null)
                .build();
        assertEquals(expected, actual);
    }

}