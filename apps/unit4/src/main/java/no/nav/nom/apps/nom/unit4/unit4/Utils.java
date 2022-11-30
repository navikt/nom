package no.nav.nom.apps.nom.unit4.unit4;

public class Utils {

    public static String convertName(String name) {
        StringBuilder stringBuilder = new StringBuilder(name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase());
        for(int i = 0; i < name.length(); i++) {
            char characterAtIndex = name.charAt(i);
            if(characterAtIndex == '-' || Character.isWhitespace(characterAtIndex)) {
                if(i + 1 < name.length()) {
                    stringBuilder.replace(i + 1, i + 2, String.valueOf(Character.toUpperCase(name.charAt(i + 1))));
                }
            }
        }
        return stringBuilder.toString();
    }

}
