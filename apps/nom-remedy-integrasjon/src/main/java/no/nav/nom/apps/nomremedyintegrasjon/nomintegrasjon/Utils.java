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
        StringBuilder stringBuilder = new StringBuilder(navn.substring(0, 1).toUpperCase() + navn.substring(1).toLowerCase());
        for(int i = 0; i < navn.length(); i++) {
            char characterAtIndex = navn.charAt(i);
            if(characterAtIndex == '-' || Character.isWhitespace(characterAtIndex)) {
                if(i + 1 < navn.length()) {
                    stringBuilder.replace(i + 1, i + 2, String.valueOf(Character.toUpperCase(navn.charAt(i + 1))));
                }
            }
        }
        return stringBuilder.toString();
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
        // Visningsnavn fra nomApi over rest kommer med mellomnavn som tom streng "", og ikke som null
        var m = visningsnavn.getMellomnavn();
        return visningsnavn.getFornavn() + (m != null && !m.isBlank() ? " " + m : "");
    }

    public static boolean isActive(LocalDate gyldigFom, LocalDate gyldigTom) {

        return !gyldigFom.isAfter(LocalDate.now())
            && !Optional.ofNullable(gyldigTom).orElse(LocalDate.MAX).isBefore(LocalDate.now());
    }
}
