package no.nav.nom.contract.nom.eventsource;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;
import no.nav.nom.contract.nom.OrgEnhetsType;
import no.nav.nom.contract.nom.NomNivaa;
import no.nav.nom.contract.nom.eventsource.orgenhetleder.OrgEnhetLederSlettetEventV1;
import no.nav.nom.contract.nom.eventsource.orgenhetleder.OrgEnhetLederSluttdatoOppdatertEventV1;
import no.nav.nom.contract.nom.eventsource.orgenhetleder.OrgEnhetLederStartdatoOppdatertEventV1;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrganisasjonV1IdKompatibilitetTest {
    private final JsonMapper mapper = JsonMapper.builder().build();

    @Test
    void extendedV1EventsKeepTypeIdAndDataOnEventRoundTrip() throws Exception {
        List<Event> events = List.of(
            new OrgEnhetAvvikletEventV1("fid-org-avviklet", new OrgEnhetAvvikletEventV1.OrgEnhetAvvikletEventData(
                "org-avviklet-id", LocalDate.parse("2025-01-01")
            ), (Event.Metadata) null),
            new OrgEnhetTypeEndretEventV1("fid-org-type", new OrgEnhetTypeEndretEventV1.OrgEnhetTypeEndretEventData(
                "org-type-id", OrgEnhetsType.NAV_KONTOR
            ), (Event.Metadata) null),
            new NomNivaaEndretEventV1("fid-org-nom-nivaa", new NomNivaaEndretEventV1.NomNivaaEndretEventData(
                "org-nom-nivaa-id", NomNivaa.LINJEENHET
            ), (Event.Metadata) null),
            new OrgEnhetEndretNavnEventV1("fid-org-navn", new OrgEnhetEndretNavnEventV1.OrgEnhetNavnEndretEventData(
                "org-navn-id", "Nytt navn"
            ), (Event.Metadata) null),
            new KorrigerGyldigFomEventV1("fid-org-gyldig-fom", new KorrigerGyldigFomEventV1.KorrigerGyldigFomEventData(
                "org-gyldig-fom-id", LocalDate.parse("2025-02-01")
            ), (Event.Metadata) null),
            new TilgangsenhetEndretEventV1("fid-tilgangsenhet", new TilgangsenhetEndretEventV1.TilgangsenhetEndretEventData(
                "org-tilgangsenhet-id", "tilgangsenhet-id"
            ), (Event.Metadata) null),
            new OrgEnhetLederSlettetEventV1("fid-leder-slettet", new OrgEnhetLederSlettetEventV1.OrgEnhetLederSlettetEventData(
                "org-leder-slettet-id"
            ), (Event.Metadata) null),
            new OrgEnhetLederStartdatoOppdatertEventV1("fid-leder-startdato", new OrgEnhetLederStartdatoOppdatertEventV1.OrgEnhetLederStartdatoOppdatertData(
                "org-leder-startdato-id", LocalDate.parse("2025-03-01")
            ), (Event.Metadata) null),
            new OrgEnhetLederSluttdatoOppdatertEventV1("fid-leder-sluttdato", new OrgEnhetLederSluttdatoOppdatertEventV1.OrgEnhetLederAvsluttetEventData(
                "org-leder-sluttdato-id", LocalDate.parse("2025-04-01")
            ), (Event.Metadata) null),
            new OrganiseringAvsluttetEventV1("fid-organisering", new OrganiseringAvsluttetEventV1.OrganiseringAvsluttetEventData(
                "organisering-id", LocalDate.parse("2025-05-01")
            ), (Event.Metadata) null)
        );

        for (Event event : events) {
            JsonNode original = mapper.readTree(mapper.writeValueAsString(event));
            Event deserialized = mapper.readValue(original.toString(), Event.class);
            JsonNode roundTripped = mapper.readTree(mapper.writeValueAsString(deserialized));

            assertEquals(event.getClass(), deserialized.getClass());
            assertEquals(event.getClass().getSimpleName(), original.path("type").asString());
            assertEquals(original.path("type").asString(), roundTripped.path("type").asString());
            assertEquals(original.path("data"), roundTripped.path("data"));
        }
    }

    @Test
    void olderEventsWithoutIdDeserializeAndSerializeIdAsNull() throws Exception {
        assertMissingOrgEnhetIdBecomesNull(new OrgEnhetAvvikletEventV1(
            "fid-org-avviklet-legacy", new OrgEnhetAvvikletEventV1.OrgEnhetAvvikletEventData("legacy-org-id", null), (Event.Metadata) null
        ), "id");
        assertMissingOrgEnhetIdBecomesNull(new OrgEnhetLederSlettetEventV1(
            "fid-leder-slettet-legacy", new OrgEnhetLederSlettetEventV1.OrgEnhetLederSlettetEventData("legacy-leder-org-id"), (Event.Metadata) null
        ), "orgEnhetId");
        assertMissingOrgEnhetIdBecomesNull(new OrganiseringAvsluttetEventV1(
            "fid-organisering-legacy", new OrganiseringAvsluttetEventV1.OrganiseringAvsluttetEventData("legacy-organisering-id", null), (Event.Metadata) null
        ), "id");
    }

    private void assertMissingOrgEnhetIdBecomesNull(Event event, String identifierField) throws Exception {
        ObjectNode oldJson = (ObjectNode) mapper.readTree(mapper.writeValueAsString(event));
        ((ObjectNode) oldJson.path("data")).remove(identifierField);

        Event deserialized = mapper.readValue(oldJson.toString(), Event.class);
        JsonNode serialized = mapper.readTree(mapper.writeValueAsString(deserialized));
        JsonNode data = serialized.path("data");

        assertEquals(event.getClass().getSimpleName(), serialized.path("type").asString());
        assertTrue(data.has(identifierField));
        assertTrue(data.get(identifierField).isNull());
        assertNotNull(data);
    }
}
