package no.nav.nom.apps.api.ressurs.implementations.dvh;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Tester basic parsing av Json objekter fra DVH")
class DvhRessursTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @SneakyThrows
    @DisplayName("Standard ekstern aktiv/permisjon DVH objekt")
    public void testStdDvhObjekt() {
        String json = """
            {
              "NAV_IDENT": "L123456",
              "FNR": "01019012345",
              "FORNAVN": "Tom",
              "ETTERNAVN": "Bombadil",
              "EPOST": null,
              "REMEDY_START_DATO": "2021-07-06",
              "REMEDY_SLUTT_DATO": "2023-03-21",
              "AGRESSO_SLUTT_DATO": "2099-12-31",
              "ANSATT_PER_IDAG": "JA",
              "ANSETTELSESTYPE_BESK": null,
              "ANSATT_TYPE": "Eksternt",
              "ANSATT_RESSURS_KODE": "E",
              "REMEDY_PERMISJON": "JA",
              "REMEDY_AKTIV": "JA",
              "BUCKET_NUM": 23
            }
        """;
        JsonNode node = mapper.readTree(json);

        DvhRessurs dvhRessurs = new DvhRessurs(node);
        assertFalse(dvhRessurs.hasPermisjon());
        assertNotNull(dvhRessurs.getSluttdato());
    }
}