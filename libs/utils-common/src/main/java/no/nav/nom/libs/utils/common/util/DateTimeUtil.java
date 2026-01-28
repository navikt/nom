package no.nav.nom.libs.utils.common.util;

import lombok.experimental.UtilityClass;
import tools.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@UtilityClass
public class DateTimeUtil {
    public static ZoneId norwegianTimeZone = ZoneId.of("Europe/Oslo");

    public static Long epochMillisNorwegianTimeEndOfDay(LocalDateTime localDateTime) {
        return localDateTime.atZone(norwegianTimeZone).toInstant().toEpochMilli();
    }

    public static LocalDateTime parseStringToLocalDateTime(JsonNode createdTimestamp) {
        return LocalDateTime.parse(createdTimestamp.get("metadata").get("createdTimestamp").asText(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}
