package no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon;

import no.nav.nom.generated.model.Visningsnavn;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Optional;

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

    public static String formatFornavnWithMellomnavn(Visningsnavn visningsnavn) {
        return visningsnavn.getFornavn() + Optional.ofNullable(visningsnavn.getMellomnavn()).map(it -> " " + it).orElse("");
    }

    public static boolean isActive(LocalDate gyldigFom, LocalDate gyldigTom) {

        return !gyldigFom.isAfter(LocalDate.now())
            && !Optional.ofNullable(gyldigTom).orElse(LocalDate.MAX).isBefore(LocalDate.now());
    }
}
