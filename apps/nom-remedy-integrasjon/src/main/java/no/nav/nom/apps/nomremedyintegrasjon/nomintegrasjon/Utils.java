package no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;

public class Utils {

    public static String fifteenCharLengthZeroPaddedNumber(Long number) {
        return String.format("%015d", number);
    }

    public static String capitalizeNavn(String navn){
        return navn.substring(0, 1).toUpperCase() + navn.substring(1);
    }

    public static Long epochSeconds(LocalDate localDate) {
        return localDate.toEpochSecond(LocalTime.NOON, ZoneOffset.UTC);
    }
}
