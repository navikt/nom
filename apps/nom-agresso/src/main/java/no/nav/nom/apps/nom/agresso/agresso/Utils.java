package no.nav.nom.apps.nom.agresso.agresso;

public class Utils {

    public static String convertName(String name){
        return name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
    }
}
