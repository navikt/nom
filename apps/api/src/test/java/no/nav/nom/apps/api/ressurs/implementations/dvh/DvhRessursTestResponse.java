package no.nav.nom.apps.api.ressurs.implementations.dvh;

import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("Tester basic parsing av Json objekter fra DVH")
class DvhRessursTestResponse {

    private final JsonMapper mapper = new JsonMapper();

    @Test
    @SneakyThrows
    @DisplayName("Standard ekstern aktiv/permisjon DVH objekt")
    public void testStdOrdsObjekt() {
        //language=json
        String json = """
            {
              "nav_ident": "L123456",
              "fnr": "01019012345",
              "fornavn": "Tom",
              "etternavn": "Bombadil",
              "epost": null,
              "remedy_start_dato": "2021-07-06",
              "remedy_slutt_dato": "2023-03-21",
              "agresso_slutt_dato": "2099-12-31",
              "ansatt_per_idag": "JA",
              "ansettelsestype_besk": null,
              "ansatt_type": "Eksternt",
              "ansatt_ressurs_kode": "E",
              "remedy_permisjon": "JA",
              "remedy_aktiv": "JA",
              "ident_type": "STANDARD"
            }
        """;
        JsonNode node = mapper.readTree(json);

        DvhRessurs dvhRessurs = DvhRessurs.fromOrdsRessurs(node);
        assertFalse(dvhRessurs.hasPermisjon());
        assertNotNull(dvhRessurs.getSluttdato());
    }
}