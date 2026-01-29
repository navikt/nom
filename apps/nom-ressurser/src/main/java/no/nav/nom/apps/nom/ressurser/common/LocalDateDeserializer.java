package no.nav.nom.apps.nom.ressurser.common;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

@Slf4j
public class LocalDateDeserializer extends ValueDeserializer<LocalDate> {
    @Override
    public LocalDate deserialize(JsonParser p, DeserializationContext ctx) {
        log.debug("deserialize: {} to {}", p.getString(), LocalDate.ofInstant(Instant.parse(p.getString()), ZoneId.systemDefault()));
        return LocalDate.ofInstant(Instant.parse(p.getString()), ZoneId.systemDefault());
    }
}
