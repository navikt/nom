package no.nav.nom.apps.api.dvh.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tester basic parsing av Json objekter fra DVH")
class DvhRessursStillingTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @DisplayName("Standard ok DVH objekt")
    public void testStdDvhObjekt() {
//        String json = """
//                      {
//
//                      }
//                      """;
//        JsonNode node = mapper.readTree(json);
    }
}