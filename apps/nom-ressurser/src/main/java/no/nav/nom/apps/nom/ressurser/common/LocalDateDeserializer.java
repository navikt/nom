package no.nav.nom.apps.nom.ressurser.common;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

@Slf4j
public class LocalDateDeserializer extends JsonDeserializer<LocalDate> {
    @Override
    public LocalDate deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
        log.debug("deserialize: {} to {}", p.getText(), LocalDate.ofInstant(Instant.parse(p.getText()), ZoneId.systemDefault()));
        return LocalDate.ofInstant(Instant.parse(p.getText()), ZoneId.systemDefault());
    }
}
