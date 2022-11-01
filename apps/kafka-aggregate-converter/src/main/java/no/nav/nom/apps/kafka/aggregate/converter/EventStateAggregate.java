package no.nav.nom.apps.kafka.aggregate.converter;

import no.nav.nom.contract.nom.eventsource.Event;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public record EventStateAggregate(
        Map<String, Object> data,
        List<Event> events
){
    public Optional<?> getAsOptional(String property){
        return Optional.ofNullable(data.get(property));
    }
}
