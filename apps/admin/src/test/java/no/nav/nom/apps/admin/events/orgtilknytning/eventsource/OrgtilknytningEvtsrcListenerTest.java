package no.nav.nom.apps.admin.events.orgtilknytning.eventsource;

import no.nav.nom.contract.nom.eventsource.OrgtilknytningEvent;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrgtilknytningEvtsrcListenerTest {
    private JsonMapper jsonMapper = configureJsonMapper();
    private OrgtilknytningEvtsrcListener orgtilknytningEvtsrcListener = new OrgtilknytningEvtsrcListener(null, "listenerId");

    @Test
    void replaceIllegalEnumMidlertidlig() {
        String jsonString = """
                            {
                              "type": "OrgtilknytningTilknytningtypeEndretEventV1",
                              "fid": "orgtilknytning-01JCQPY0D6M4BQXZD16PXHN0VV",
                              "data": {
                                "type": "OrgtilknytningTilknytningtypeEndretEventV1$OrgtilknytningTilknytningtypeEndretEventData",
                                "orgtilknytningType": "MIDLERTIDLIG"
                              },
                              "metadata": {
                                "inputMetadata": {
                                  "inputInterfaceName": "REST-API",
                                  "inputInterfaceMethodName": "opprettRessurs",
                                  "inputInterfaceMethodArguments": {
                                    "personident": "17416838046",
                                    "visningsfornavn": "Formbar",
                                    "visningsmellomnavn": "Akselererende",
                                    "visningsetternavn": "Mormor",
                                    "primaryTelefonnummer": "",
                                    "sektor": "NAV_STATLIG",
                                    "orgEnhetId": "fy499g",
                                    "startDato": "2024-12-01",
                                    "sluttDato": "2025-12-31",
                                    "ansettelsesType": null,
                                    "fysiskBrukerstedNr": "2700",
                                    "tilknytningtype": "MIDLERTIDLIG"
                                  },
                                  "origination": {
                                    "initiatedBy": "G169122",
                                    "initiatedFrom": "dev-gcp:org:nom-ui2"
                                  }
                                },
                                "createdTimestamp": "2024-11-15T11:47:04.455659618"
                              },
                              "additionalData": null
                            }
                            """;

        String expected = """
                          {"type":"OrgtilknytningTilknytningtypeEndretEventV1","fid":"orgtilknytning-01JCQPY0D6M4BQXZD16PXHN0VV","data":{"type":"OrgtilknytningTilknytningtypeEndretEventV1$OrgtilknytningTilknytningtypeEndretEventData","orgtilknytningType":"MIDLERTIDIG"},"metadata":{"inputMetadata":{"inputInterfaceName":"REST-API","inputInterfaceMethodName":"opprettRessurs","inputInterfaceMethodArguments":{"personident":"17416838046","visningsfornavn":"Formbar","visningsmellomnavn":"Akselererende","visningsetternavn":"Mormor","primaryTelefonnummer":"","sektor":"NAV_STATLIG","orgEnhetId":"fy499g","startDato":"2024-12-01","sluttDato":"2025-12-31","ansettelsesType":null,"fysiskBrukerstedNr":"2700","tilknytningtype":"MIDLERTIDLIG"},"origination":{"initiatedBy":"G169122","initiatedFrom":"dev-gcp:org:nom-ui2"}},"createdTimestamp":"2024-11-15T11:47:04.455659618"},"additionalData":null}""";

        JsonNode result = orgtilknytningEvtsrcListener.replaceIllegalEnumMidlertidlig(jsonString);
        assertEquals(expected, result.toString());
        try {
            jsonMapper.treeToValue(result, OrgtilknytningEvent.class);
        } catch (JacksonException e) {
            throw new RuntimeException(e);
        }
    }

    private static JsonMapper configureJsonMapper() {
        final var jsonMapper = new JsonMapper();

        return jsonMapper;
    }
}