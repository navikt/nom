package no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.repository.delayedevent.r2dbc;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.nom.contract.nom.eventsource.Event;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

@ReadingConverter
public class EventReadConverter implements Converter<String, Event> {

    private final ObjectMapper objectMapper;

    public EventReadConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Event convert(String source) {
        try {
            return objectMapper.readValue(source, Event.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert String to Event", e);
        }
    }
}
