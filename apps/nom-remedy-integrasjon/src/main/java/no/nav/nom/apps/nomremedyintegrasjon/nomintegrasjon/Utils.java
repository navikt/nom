package no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

public class Utils {

    public static ZoneId norwegianTimeZone = ZoneId.of("Europe/Oslo");

    public static String fifteenCharLengthZeroPaddedNumber(Long number) {
        return String.format("%015d", number);
    }

    public static String capitalizeNavn(String navn){
        return navn.substring(0, 1).toUpperCase() + navn.substring(1);
    }

    public static Long epochSecondsNorwegianTimeNoon(LocalDate localDate) {
        var localDateTime = localDate.atTime(LocalTime.NOON);

        return localDateTime.toEpochSecond(norwegianTimeZone.getRules().getOffset(localDateTime));
    }

    public static Long epochSecondsNorwegianTimeEndOfDay(LocalDate localDate) {
        var localDateTime = localDate.atTime(LocalTime.MAX);

        return localDateTime.toEpochSecond(norwegianTimeZone.getRules().getOffset(localDateTime));
    }
}
