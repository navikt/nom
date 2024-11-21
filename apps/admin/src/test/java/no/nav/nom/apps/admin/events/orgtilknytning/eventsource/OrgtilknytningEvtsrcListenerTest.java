package no.nav.nom.apps.admin.events.orgtilknytning.eventsource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrgtilknytningEvtsrcListenerTest {
    private ObjectMapper objectMapper = configureObjectMapper();
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
            objectMapper.treeToValue(result, OrgtilknytningEvent.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private static ObjectMapper configureObjectMapper() {
        final var objectMapper = new ObjectMapper();

        return objectMapper.findAndRegisterModules()
            .registerModule(new ParameterNamesModule())
            .registerModule(new Jdk8Module())
            .registerModule(new JavaTimeModule())
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }
}